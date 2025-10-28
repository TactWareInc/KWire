package net.tactware.kwire.ktor

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.builtins.ListSerializer
import net.tactware.kwire.core.*
import net.tactware.kwire.core.messages.*
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.memberFunctions

/**
 * Simple RPC builder for Ktor that provides a clean API similar to krpc.
 * 
 * Usage:
 * ```kotlin
 * routing {
 *     rpc("/device")<DeviceStatus> {
 *         DeviceStatusImpl() // or koin.get { parametersOf(it) }
 *     }
 * }
 * ```
 */

/**
 * Data class to hold RPC endpoint configuration
 */
data class RpcEndpointConfig<T : Any>(
    val path: String,
    val serviceClass: KClass<T>,
    val factory: suspend (RpcConnectionContext) -> T,
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
)

/**
 * Context provided to service factories
 */
data class RpcConnectionContext(
    val connectionId: String,
    val session: WebSocketSession,
    val path: String,
    val remoteAddress: String? = null
)

/**
 * Extension function for Route to register an RPC endpoint
 * 
 * @param path The WebSocket path for this RPC service
 * @param factory Lambda to create the service implementation
 */
inline fun <reified T : Any> Route.rpc(
    path: String,
    noinline factory: suspend (RpcConnectionContext) -> T
) {
    val logger = LoggerFactory.getLogger("KtorRpc")
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    // Install WebSocket handler for this path
    webSocket(path) {
        val connectionId = generateConnectionId()
        val context = RpcConnectionContext(
            connectionId = connectionId,
            session = this,
            path = path,
            remoteAddress = call.request.local.remoteHost
        )
        
        logger.info("New RPC connection: $connectionId at $path from ${context.remoteAddress}")
        
            try {
                // Create service instance for this connection
                val service = factory(context)
                logger.info("Created service instance: ${T::class.simpleName} for connection $connectionId")
                
                // Check if service has a property named "transport" (generated server pattern)
                val hasTransport = try {
                    service::class.members.any { it.name == "transport" }
                } catch (e: Exception) {
                    false
                }
                
                if (hasTransport) {
                    // Service appears to be using a transport (generated server or custom)
                    // It should already be configured with its transport and started
                    // Just handle the incoming frames normally
                    logger.info("Service has transport configured, processing frames")
                    
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                // Forward to the service's transport if possible
                                logger.debug("Forwarding frame to service transport")
                            }
                            is Frame.Close -> {
                                logger.info("Connection closed: $connectionId")
                                break
                            }
                            else -> {
                                logger.debug("Received non-text frame: ${frame.frameType}")
                            }
                        }
                    }
                } else {
                    // Service is a plain implementation
                    // Use the adapter to handle message routing
                    val transport = ServiceTransportAdapter(
                        service = service,
                        serviceClass = T::class,
                        session = this,
                        json = json,
                        logger = logger
                    )
                    
                    // Handle incoming messages
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val messageText = frame.readText()
                                logger.debug("Received at $path: ${messageText.take(100)}...")
                                
                                try {
                                    transport.handleMessage(messageText)
                                } catch (e: Exception) {
                                    logger.error("Error handling message: ${e.message}", e)
                                    transport.sendError("unknown", "MESSAGE_ERROR", e.message ?: "Unknown error")
                                }
                            }
                            is Frame.Close -> {
                                logger.info("Connection closed: $connectionId")
                                break
                            }
                            else -> {
                                logger.debug("Received non-text frame: ${frame.frameType}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            logger.error("WebSocket error for $connectionId: ${e.message}", e)
        } finally {
            logger.info("Connection $connectionId cleaned up")
        }
    }
}

/**
 * Alternative syntax with configuration block
 */
inline fun <reified T : Any> Route.rpc(
    path: String,
    crossinline configure: RpcConfigBuilder<T>.() -> Unit
) {
    val builder = RpcConfigBuilder<T>(path).apply(configure)
    val config = builder.build()
    
    val logger = LoggerFactory.getLogger("KtorRpc")
    
    webSocket(path) {
        val connectionId = generateConnectionId()
        val context = RpcConnectionContext(
            connectionId = connectionId,
            session = this,
            path = path,
            remoteAddress = call.request.local.remoteHost
        )
        
        logger.info("New RPC connection: $connectionId at $path")
        
        try {
            val service = config.factory(context)
            logger.info("Created service instance: ${config.serviceClass.simpleName} for connection $connectionId")
            
            val transport = ServiceTransportAdapter(
                service = service,
                serviceClass = config.serviceClass,
                session = this,
                json = config.json,
                logger = logger
            )
            
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val messageText = frame.readText()
                        try {
                            transport.handleMessage(messageText)
                        } catch (e: Exception) {
                            logger.error("Error handling message: ${e.message}", e)
                            transport.sendError("unknown", "MESSAGE_ERROR", e.message ?: "Unknown error")
                        }
                    }
                    is Frame.Close -> {
                        logger.info("Connection closed: $connectionId")
                        break
                    }
                    else -> {
                        logger.debug("Received non-text frame: ${frame.frameType}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("WebSocket error for $connectionId: ${e.message}", e)
        } finally {
            logger.info("Connection $connectionId cleaned up")
        }
    }
}

/**
 * Configuration builder for RPC endpoints
 */
class RpcConfigBuilder<T : Any>(private val path: String) {
    lateinit var factory: suspend (RpcConnectionContext) -> T
    lateinit var serviceClass: KClass<T>
    
    var json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    fun serialization(block: JsonBuilder.() -> Unit) {
        json = Json {
            val builder = JsonBuilder().apply(block)
            ignoreUnknownKeys = builder.ignoreUnknownKeys
            isLenient = builder.isLenient
            encodeDefaults = builder.encodeDefaults
            prettyPrint = builder.prettyPrint
        }
    }
    
    inline fun <reified S : T> registerService(noinline factory: suspend (RpcConnectionContext) -> S) {
        this.factory = factory as suspend (RpcConnectionContext) -> T
        this.serviceClass = S::class as KClass<T>
    }
    
    fun build(): RpcEndpointConfig<T> {
        return RpcEndpointConfig(
            path = path,
            serviceClass = serviceClass,
            factory = factory,
            json = json
        )
    }
}

/**
 * Transport adapter that handles RPC communication for a service
 * 
 * This adapter should be used with generated server implementations
 * or can work with reflection to invoke methods dynamically.
 */
class ServiceTransportAdapter<T : Any>(
    private val service: T,
    private val serviceClass: KClass<T>,
    private val session: WebSocketSession,
    private val json: Json,
    private val logger: org.slf4j.Logger
) {
    
    suspend fun handleMessage(messageText: String) {
        val message = parseMessage(messageText)
        
        when (message) {
            is RpcRequest -> handleRequest(message)
            is StreamStart -> handleStreamStart(message)
            else -> {
                logger.warn("Unexpected message type: ${message::class.simpleName}")
            }
        }
    }
    
    private suspend fun handleRequest(request: RpcRequest) {
        logger.info("Processing request: ${request.serviceName}.${request.methodId}")
        
        // NOTE: This is a basic implementation that doesn't actually invoke methods
        // In production, you would either:
        // 1. Use the generated server implementation (UserServiceServerImpl)
        // 2. Implement proper reflection-based method invocation
        // 3. Use code generation to create proper dispatchers
        
        val response = RpcError(
            messageId = request.messageId,
            timestamp = System.currentTimeMillis(),
            errorCode = RpcErrorCodes.METHOD_NOT_FOUND,
            errorMessage = "Method dispatch not implemented for plain service implementations. " +
                        "Use the generated server implementation instead."
        )
        
        val responseJson = json.encodeToString(RpcMessage.serializer(), response)
        session.send(Frame.Text(responseJson))
        logger.warn("ServiceTransportAdapter doesn't support method dispatch. Use generated server implementations.")
    }
    
    private suspend fun handleStreamStart(streamStart: StreamStart) {
        logger.info("Processing stream: ${streamStart.serviceName}.${streamStart.methodId}")
        
        // Handle streaming - in a real implementation this would
        // invoke the streaming method and send data
        
        val streamEnd = StreamEnd(
            messageId = streamStart.messageId,
            timestamp = System.currentTimeMillis(),
            streamId = streamStart.streamId
        )
        
        val endJson = json.encodeToString(RpcMessage.serializer(), streamEnd)
        session.send(Frame.Text(endJson))
    }
    
    suspend fun sendError(messageId: String, errorCode: String, errorMessage: String) {
        val error = RpcError(
            messageId = messageId,
            timestamp = System.currentTimeMillis(),
            errorCode = errorCode,
            errorMessage = errorMessage
        )
        val errorJson = json.encodeToString(RpcMessage.serializer(), error)
        session.send(Frame.Text(errorJson))
    }
    
    private fun parseMessage(messageText: String): RpcMessage {
        return when {
            messageText.contains("\"serviceName\"") && messageText.contains("\"methodId\"") -> {
                if (messageText.contains("\"streaming\":true") || messageText.contains("\"streamId\"")) {
                    json.decodeFromString(StreamStart.serializer(), messageText)
                } else {
                    json.decodeFromString(RpcRequest.serializer(), messageText)
                }
            }
            messageText.contains("\"streamId\"") -> {
                when {
                    messageText.contains("\"data\"") -> json.decodeFromString(StreamData.serializer(), messageText)
                    messageText.contains("\"errorCode\"") -> json.decodeFromString(StreamError.serializer(), messageText)
                    else -> json.decodeFromString(StreamEnd.serializer(), messageText)
                }
            }
            messageText.contains("\"errorCode\"") -> {
                json.decodeFromString(RpcError.serializer(), messageText)
            }
            messageText.contains("\"result\"") -> {
                json.decodeFromString(RpcResponse.serializer(), messageText)
            }
            else -> {
                throw IllegalArgumentException("Unknown message format")
            }
        }
    }
}

/**
 * JSON configuration builder
 */
class JsonBuilder {
    var ignoreUnknownKeys: Boolean = true
    var isLenient: Boolean = true
    var encodeDefaults: Boolean = true
    var prettyPrint: Boolean = false
}

/**
 * Generate a unique connection ID
 */
fun generateConnectionId(): String {
    return "conn_${System.currentTimeMillis()}_${(1000..9999).random()}"
}

/**
 * Simplified RPC builder that creates a transport directly
 * This version is closer to the original request
 */
inline fun <reified T : Any> Route.rpcTransport(
    path: String,
    crossinline factory: suspend (RpcConnectionContext) -> T
): KtorServerRpcTransport {
    val logger = LoggerFactory.getLogger("KtorRpcTransport")
    
    // Create a transport instance
    val transport = KtorServerRpcTransport()
    
    // Install WebSocket handler
    webSocket(path) {
        val connectionId = generateConnectionId()
        val context = RpcConnectionContext(
            connectionId = connectionId,
            session = this,
            path = path,
            remoteAddress = call.request.local.remoteHost
        )
        
        logger.info("New RPC connection via transport: $connectionId at $path")
        
        try {
            // Create service instance
            val service = factory(context)
            
            // Set up the transport to handle this service
            // This would integrate with the existing KtorServerRpcTransport
            // to route messages to the service methods
            
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val messageText = frame.readText()
                        // Forward to transport for processing
                        // transport.processMessage(messageText, service)
                    }
                    is Frame.Close -> {
                        logger.info("Connection closed: $connectionId")
                        break
                    }
                    else -> {
                        // Ignore other frames
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Transport error for $connectionId: ${e.message}", e)
        }
    }
    
    return transport
}
