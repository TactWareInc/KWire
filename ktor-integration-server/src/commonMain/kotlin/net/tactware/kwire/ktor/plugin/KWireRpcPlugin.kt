package net.tactware.kwire.ktor.plugin

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import net.tactware.kwire.core.RpcTransport
import net.tactware.kwire.ktor.WebSocketSessionTransport
import net.tactware.kwire.ktor.WebSocketTransportConfig
import net.tactware.kwire.ktor.generateConnectionId
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * KWire RPC Plugin for Ktor.
 * 
 * This plugin provides a clean way to install and configure RPC services in a Ktor application.
 * 
 * Example usage:
 * ```kotlin
 * install(KWireRpc) {
 *     // Global configuration
 *     webSocket {
 *         pingPeriod = 15.seconds
 *         timeout = 60.seconds
 *         maxFrameSize = 1024 * 1024
 *     }
 *     
 *     serialization {
 *         prettyPrint = false
 *         ignoreUnknownKeys = true
 *     }
 *     
 *     // Register services
 *     service<UserService>("/users") {
 *         implementation { UserServiceImpl() }
 *         useGeneratedServer = true
 *     }
 *     
 *     service<AdminService>("/admin") {
 *         implementation { AdminServiceImpl() }
 *         requireAuth = true
 *     }
 * }
 * ```
 */
class KWireRpc(configuration: Configuration) {
    private val logger = LoggerFactory.getLogger("KWireRpc")
    private val config = configuration
    
    class Configuration {
        val services = mutableListOf<ServiceConfiguration<*>>()
        internal var webSocketConfig = WebSocketConfig()
        internal var jsonConfig = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
        
        /**
         * Configure WebSocket settings
         */
        fun webSocket(block: WebSocketConfig.() -> Unit) {
            webSocketConfig = WebSocketConfig().apply(block)
        }
        
        /**
         * Configure JSON serialization
         */
        fun serialization(block: JsonConfig.() -> Unit) {
            val jsonConfig = JsonConfig()
            jsonConfig.block()
            this.jsonConfig = Json {
                ignoreUnknownKeys = jsonConfig.ignoreUnknownKeys
                isLenient = jsonConfig.isLenient
                encodeDefaults = jsonConfig.encodeDefaults
                prettyPrint = jsonConfig.prettyPrint
            }
        }
        
        /**
         * Register a service
         */
        inline fun <reified T : Any> service(
            path: String,
            noinline configure: ServiceConfiguration<T>.() -> Unit
        ) {
            val config = ServiceConfiguration<T>(path, T::class).apply(configure)
            services.add(config)
        }
    }
    
    companion object Plugin : BaseApplicationPlugin<Application, Configuration, KWireRpc> {
        override val key = AttributeKey<KWireRpc>("KWireRpc")
        
        override fun install(
            pipeline: Application,
            configure: Configuration.() -> Unit
        ): KWireRpc {
            val configuration = Configuration().apply(configure)
            val plugin = KWireRpc(configuration)
            
            // Install WebSockets if not already installed
            if (pipeline.pluginOrNull(WebSockets) == null) {
                pipeline.install(WebSockets) {
                    pingPeriod = configuration.webSocketConfig.pingPeriod
                    timeout = configuration.webSocketConfig.timeout
                    maxFrameSize = configuration.webSocketConfig.maxFrameSize
                    masking = false
                }
            }
            
            // Install routes for each service
            pipeline.routing {
                configuration.services.forEach { serviceConfig ->
                    plugin.installService(this, serviceConfig, configuration)
                }
            }
            
            return plugin
        }
    }
    
    private fun <T : Any> installService(
        routing: Route,
        serviceConfig: ServiceConfiguration<T>,
        globalConfig: Configuration
    ) {
        routing.webSocket(serviceConfig.path) {
            val connectionId = generateConnectionId()
            logger.info("New RPC connection at ${serviceConfig.path}: $connectionId")
            
            try {
                // Create service implementation
                val implementation = serviceConfig.implementationFactory()
                
                if (serviceConfig.useGeneratedServer) {
                    // Use generated server implementation with transport
                    handleWithGeneratedServer(
                        session = this,
                        implementation = implementation,
                        serviceConfig = serviceConfig,
                        json = globalConfig.jsonConfig
                    )
                } else {
                    // Use direct implementation (requires proper method dispatch)
                    handleDirectImplementation(
                        session = this,
                        implementation = implementation,
                        serviceConfig = serviceConfig,
                        json = globalConfig.jsonConfig
                    )
                }
            } catch (e: Exception) {
                logger.error("Error in RPC connection $connectionId: ${e.message}", e)
            } finally {
                logger.info("RPC connection closed: $connectionId")
            }
        }
    }
    
    private suspend fun <T : Any> handleWithGeneratedServer(
        session: WebSocketSession,
        implementation: T,
        serviceConfig: ServiceConfiguration<T>,
        json: Json
    ) {
        // Create transport for this session
        val transport = WebSocketSessionTransport(
            session = session,
            config = WebSocketTransportConfig(json = json)
        )
        
        // Create and start the generated server
        val serverImpl = serviceConfig.serverFactory?.invoke(transport, implementation)
        
        if (serverImpl != null) {
            // Start the server if it has a start method
            try {
                serverImpl::class.members.find { it.name == "start" }?.call(serverImpl)
                logger.info("Started generated server for ${serviceConfig.path}")
            } catch (e: Exception) {
                logger.warn("Could not start server: ${e.message}")
            }
            
            // Handle incoming frames
            transport.handleIncomingFrames()
            
            // Stop the server if it has a stop method
            try {
                serverImpl::class.members.find { it.name == "stop" }?.call(serverImpl)
            } catch (e: Exception) {
                logger.warn("Could not stop server: ${e.message}")
            }
        } else {
            logger.error("No server factory configured for ${serviceConfig.path}")
        }
    }
    
    private suspend fun <T : Any> handleDirectImplementation(
        session: WebSocketSession,
        implementation: T,
        serviceConfig: ServiceConfiguration<T>,
        json: Json
    ) {
        // For direct implementation, we need proper method dispatch
        // This is a placeholder - in production, you'd use reflection or code generation
        logger.warn("Direct implementation at ${serviceConfig.path} requires method dispatch implementation")
        
        for (frame in session.incoming) {
            when (frame) {
                is Frame.Text -> {
                    val messageText = frame.readText()
                    logger.debug("Received message at ${serviceConfig.path}: ${messageText.take(100)}...")
                    
                    // Send error response for now
                    val errorResponse = """
                        {
                            "type": "net.tactware.kwire.core.messages.RpcError",
                            "messageId": "unknown",
                            "timestamp": ${System.currentTimeMillis()},
                            "errorCode": "NOT_IMPLEMENTED",
                            "errorMessage": "Direct implementation method dispatch not yet implemented. Use generated server."
                        }
                    """.trimIndent()
                    
                    session.send(Frame.Text(errorResponse))
                }
                is Frame.Close -> break
                else -> {
                    // Ignore other frame types
                }
            }
        }
    }
}

/**
 * Extension function for easy service registration with generated server
 */
inline fun <reified T : Any> ServiceConfiguration<T>.withGeneratedServer(
    crossinline serverFactory: (RpcTransport, T) -> Any
) {
    generatedServer { transport, impl -> serverFactory(transport, impl) }
}
