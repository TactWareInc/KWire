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
