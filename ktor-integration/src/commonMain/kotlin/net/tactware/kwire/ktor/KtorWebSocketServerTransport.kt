package net.tactware.kwire.ktor

import com.obfuscated.rpc.core.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import net.tactware.kwire.core.messages.RpcMessage
import net.tactware.kwire.core.messages.RpcRequest
import net.tactware.kwire.core.messages.StreamStart
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds
import java.util.concurrent.ConcurrentHashMap

/**
 * Production-ready Ktor WebSocket server transport implementation.
 * Handles real network communication with WebSocket clients.
 */
class KtorWebSocketServerTransport(
    private val config: KtorWebSocketServerConfig = KtorWebSocketServerConfig()
) : RpcTransport {
    
    private val logger = LoggerFactory.getLogger("KtorWebSocketServerTransport")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    // Server state
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var _isConnected = false
    override val isConnected: Boolean get() = _isConnected
    
    // Connection management
    private val activeConnections = ConcurrentHashMap<String, WebSocketSession>()
    private val messageChannel = Channel<RpcMessage>(Channel.UNLIMITED)
    
    // Message handlers
    private var requestHandler: (suspend (RpcRequest, WebSocketSession) -> Unit)? = null
    private var streamHandler: (suspend (StreamStart, WebSocketSession) -> Unit)? = null
    
    /**
     * Start the WebSocket server
     */
    override suspend fun connect() {
        if (_isConnected) {
            logger.info("🔌 Server already connected")
            return
        }
        
        logger.info("🚀 Starting Ktor WebSocket Server on ${config.host}:${config.port}")
        
        server = embeddedServer(Netty, port = config.port, host = config.host) {
            install(WebSockets) {
                pingPeriod = config.pingIntervalSeconds.seconds
                timeout = config.timeoutSeconds.seconds
                maxFrameSize = config.maxFrameSize
                masking = false
            }
            
            routing {
                webSocket(config.path) {
                    val connectionId = generateConnectionId()
                    logger.info("🔌 New WebSocket connection: $connectionId")
                    
                    try {
                        // Register connection
                        activeConnections[connectionId] = this
                        
                        // Handle incoming messages
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val messageText = frame.readText()
                                    logger.info("📨 Received message from $connectionId: ${messageText.take(100)}...")
                                    
                                    try {
                                        handleIncomingMessage(messageText, this)
                                    } catch (e: Exception) {
                                        logger.error("❌ Error handling message from $connectionId: ${e.message}")
                                        sendError(this, "unknown", "MESSAGE_PARSE_ERROR", e.message ?: "Unknown error")
                                    }
                                }
                                is Frame.Close -> {
                                    logger.info("🔌 Connection closed: $connectionId")
                                    break
                                }
                                else -> {
                                    logger.debug("📨 Received non-text frame from $connectionId: ${frame.frameType}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("❌ WebSocket connection error for $connectionId: ${e.message}")
                    } finally {
                        // Cleanup connection
                        activeConnections.remove(connectionId)
                        logger.info("🔌 Connection $connectionId cleaned up")
                    }
                }
            }
        }
        
        server?.start(wait = false)
        _isConnected = true
        
        logger.info("✅ Ktor WebSocket Server started successfully")
        logger.info("🌐 WebSocket endpoint: ws://${config.host}:${config.port}${config.path}")
    }
    
    /**
     * Stop the WebSocket server
     */
    override suspend fun disconnect() {
        if (!_isConnected) {
            return
        }
        
        logger.info("🛑 Stopping Ktor WebSocket Server...")
        
        // Close all active connections
        activeConnections.values.forEach { session ->
            try {
                session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server shutting down"))
            } catch (e: Exception) {
                logger.warn("⚠️ Error closing connection: ${e.message}")
            }
        }
        activeConnections.clear()
        
        // Stop server
        server?.stop(1000, 2000)
        server = null
        _isConnected = false
        messageChannel.close()
        
        logger.info("✅ Ktor WebSocket Server stopped")
    }
    
    /**
     * Send message to all connected clients (broadcast)
     */
    override suspend fun send(message: RpcMessage) {
        if (!_isConnected) {
            logger.warn("⚠️ Cannot send message - server not connected")
            return
        }
        
        val messageJson = json.encodeToString<RpcMessage>(message)
        logger.info("📤 Broadcasting message: ${message::class.simpleName} (${message.messageId})")
        
        // Send to all active connections
        val deadConnections = mutableListOf<String>()
        
        activeConnections.forEach { (connectionId, session) ->
            try {
                session.send(Frame.Text(messageJson))
                logger.debug("📤 Sent to $connectionId")
            } catch (e: Exception) {
                logger.warn("⚠️ Failed to send to $connectionId: ${e.message}")
                deadConnections.add(connectionId)
            }
        }
        
        // Clean up dead connections
        deadConnections.forEach { connectionId ->
            activeConnections.remove(connectionId)
            logger.info("🧹 Removed dead connection: $connectionId")
        }
    }
    
    /**
     * Send message to specific client
     */
    suspend fun sendToClient(message: RpcMessage, connectionId: String) {
        val session = activeConnections[connectionId]
        if (session == null) {
            logger.warn("⚠️ Connection not found: $connectionId")
            return
        }
        
        try {
            val messageJson = json.encodeToString<RpcMessage>(message)
            session.send(Frame.Text(messageJson))
            logger.info("📤 Sent to $connectionId: ${message::class.simpleName} (${message.messageId})")
        } catch (e: Exception) {
            logger.error("❌ Failed to send to $connectionId: ${e.message}")
            activeConnections.remove(connectionId)
        }
    }
    
    /**
     * Receive messages from clients
     */
    override fun receive(): Flow<RpcMessage> {
        return messageChannel.receiveAsFlow()
    }
    
    /**
     * Set request handler for RPC calls
     */
    fun setRequestHandler(handler: suspend (RpcRequest, WebSocketSession) -> Unit) {
        this.requestHandler = handler
    }
    
    /**
     * Set stream handler for streaming calls
     */
    fun setStreamHandler(handler: suspend (StreamStart, WebSocketSession) -> Unit) {
        this.streamHandler = handler
    }
    
    /**
     * Get active connection count
     */
    fun getActiveConnectionCount(): Int = activeConnections.size
    
    /**
     * Get active connection IDs
     */
    fun getActiveConnectionIds(): Set<String> = activeConnections.keys.toSet()
    
    private suspend fun handleIncomingMessage(messageText: String, session: WebSocketSession) {
        try {
            // Try to parse as different message types
            val message = when {
                messageText.contains("\"serviceName\"") && messageText.contains("\"methodId\"") -> {
                    if (messageText.contains("\"streaming\":true") || messageText.contains("\"streamId\"")) {
                        json.decodeFromString<StreamStart>(messageText)
                    } else {
                        json.decodeFromString<RpcRequest>(messageText)
                    }
                }
                messageText.contains("\"streamId\"") -> {
                    when {
                        messageText.contains("\"data\"") -> json.decodeFromString<StreamData>(messageText)
                        messageText.contains("\"errorCode\"") -> json.decodeFromString<StreamError>(messageText)
                        else -> json.decodeFromString<StreamEnd>(messageText)
                    }
                }
                messageText.contains("\"errorCode\"") -> {
                    json.decodeFromString<RpcError>(messageText)
                }
                messageText.contains("\"result\"") -> {
                    json.decodeFromString<RpcResponse>(messageText)
                }
                else -> {
                    throw IllegalArgumentException("Unknown message format")
                }
            }
            
            // Emit message to flow
            messageChannel.send(message)
            
            // Handle message based on type
            when (message) {
                is RpcRequest -> {
                    logger.info("🔄 Handling RPC request: ${message.serviceName}.${message.methodId}")
                    requestHandler?.invoke(message, session)
                }
                is StreamStart -> {
                    logger.info("🌊 Handling stream start: ${message.serviceName}.${message.methodId}")
                    streamHandler?.invoke(message, session)
                }
                else -> {
                    logger.debug("📨 Received message: ${message::class.simpleName}")
                }
            }
            
        } catch (e: Exception) {
            logger.error("❌ Error parsing message: ${e.message}")
            sendError(session, "unknown", "PARSE_ERROR", "Failed to parse message: ${e.message}")
        }
    }
    
    private suspend fun sendError(session: WebSocketSession, messageId: String, errorCode: String, errorMessage: String) {
        try {
            val error = RpcError(
                messageId = messageId,
                timestamp = System.currentTimeMillis(),
                errorCode = errorCode,
                errorMessage = errorMessage
            )
            val errorJson = json.encodeToString<RpcMessage>(error)
            session.send(Frame.Text(errorJson))
        } catch (e: Exception) {
            logger.error("❌ Failed to send error message: ${e.message}")
        }
    }
    
    private fun generateConnectionId(): String {
        return "conn_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

/**
 * Configuration for Ktor WebSocket server transport
 */
data class KtorWebSocketServerConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val path: String = "/rpc",
    val pingIntervalSeconds: Long = 15,
    val timeoutSeconds: Long = 60,
    val maxFrameSize: Long = 1024 * 1024, // 1MB
    val maxConnections: Int = 1000
)

/**
 * Builder for creating Ktor WebSocket server transport
 */
class KtorWebSocketServerBuilder {
    private var config = KtorWebSocketServerConfig()
    
    fun host(host: String) = apply { config = config.copy(host = host) }
    fun port(port: Int) = apply { config = config.copy(port = port) }
    fun path(path: String) = apply { config = config.copy(path = path) }
    fun pingInterval(seconds: Long) = apply { config = config.copy(pingIntervalSeconds = seconds) }
    fun timeout(seconds: Long) = apply { config = config.copy(timeoutSeconds = seconds) }
    fun maxFrameSize(bytes: Long) = apply { config = config.copy(maxFrameSize = bytes) }
    fun maxConnections(count: Int) = apply { config = config.copy(maxConnections = count) }
    
    fun build(): KtorWebSocketServerTransport = KtorWebSocketServerTransport(config)
}

/**
 * DSL function for creating Ktor WebSocket server transport
 */
fun ktorWebSocketServerTransport(block: KtorWebSocketServerBuilder.() -> Unit = {}): KtorWebSocketServerTransport {
    return KtorWebSocketServerBuilder().apply(block).build()
}

