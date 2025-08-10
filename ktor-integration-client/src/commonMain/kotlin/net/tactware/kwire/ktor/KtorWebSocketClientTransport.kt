package net.tactware.kwire.ktor

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.util.logging.*
import io.ktor.utils.io.core.toByteArray
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import net.tactware.kwire.core.RpcTransport
import net.tactware.kwire.core.messages.RpcError
import net.tactware.kwire.core.messages.RpcMessage
import net.tactware.kwire.core.messages.RpcRequest
import net.tactware.kwire.core.messages.RpcResponse
import net.tactware.kwire.core.messages.StreamData
import net.tactware.kwire.core.messages.StreamEnd
import net.tactware.kwire.core.messages.StreamError
import net.tactware.kwire.core.messages.StreamStart
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Production-ready Ktor WebSocket client transport implementation.
 * Handles real network communication with WebSocket servers.
 */
class KtorWebSocketClientTransport(
    private val config: KtorWebSocketClientConfig = KtorWebSocketClientConfig(),
    private val scope : CoroutineScope
) : RpcTransport {

    val logger = KtorSimpleLogger("KtorWebSocketClientTransport")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    // Client state
    private var client: HttpClient? = null
    private var webSocketSession: DefaultClientWebSocketSession? = null
    private var _isConnected = false
    override val isConnected: Boolean get() = _isConnected
    
    // Message handling
    private val messageChannel = Channel<RpcMessage>(Channel.UNLIMITED)
    private var connectionJob: Job? = null
    
    /**
     * Connect to the WebSocket server
     */
    override suspend fun connect() {
        if (_isConnected) {
            logger.info("🔌 Client already connected")
            return
        }
        
        logger.info("🚀 Connecting to WebSocket server at ${config.serverUrl}")
        
        try {
            // Create HTTP client with WebSocket support
            client = HttpClient(CIO) {
                install(WebSockets) {
                    pingInterval = config.pingIntervalSeconds.seconds
                    maxFrameSize = config.maxFrameSize
                }
            }
            
            // Connect to WebSocket
            connectionJob = scope.launch {
                try {
                    client!!.webSocket(
                        urlString = config.serverUrl
                    ) {
                        webSocketSession = this
                        _isConnected = true
                        
                        logger.info("✅ Connected to WebSocket server")
                        logger.info("🌐 Connected to: ${config.serverUrl}")
                        
                        // Start message handling
                        handleIncomingMessages()
                    }
                } catch (e: Exception) {
                    logger.error("❌ WebSocket connection failed: ${e.message}")
                    _isConnected = false
                    webSocketSession = null
                } finally {
                    logger.info("🔌 WebSocket connection closed")
                    _isConnected = false
                    webSocketSession = null
                }
            }
            
            // Wait a bit for connection to establish
            delay(1000)
            
            if (!_isConnected) {
                throw RuntimeException("Failed to establish WebSocket connection")
            }
            
        } catch (e: Exception) {
            logger.error("❌ Failed to connect: ${e.message}")
            disconnect()
            throw e
        }
    }
    
    /**
     * Disconnect from the WebSocket server
     */
    override suspend fun disconnect() {
        if (!_isConnected) {
            return
        }
        
        logger.info("🛑 Disconnecting from WebSocket server...")
        
        try {
            // Close WebSocket session
            webSocketSession?.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Client disconnecting"))
            webSocketSession = null
            
            // Cancel connection job
            connectionJob?.cancel()
            connectionJob = null
            
            // Close HTTP client
            client?.close()
            client = null
            
            _isConnected = false
            messageChannel.close()
            
            logger.info("✅ Disconnected from WebSocket server")
            
        } catch (e: Exception) {
            logger.warn("⚠️ Error during disconnect: ${e.message}")
        }
    }
    
    /**
     * Send message to the WebSocket server
     */
    override suspend fun send(message: RpcMessage) {
        if (!_isConnected || webSocketSession == null) {
            logger.warn("⚠️ Cannot send message - not connected")
            throw RuntimeException("Not connected to WebSocket server")
        }
        
        try {
            val messageJson = json.encodeToString<RpcMessage>(message)
            webSocketSession!!.send(Frame.Text(messageJson))
            
            logger.info("📤 Sent message: ${message::class.simpleName} (${message.messageId})")
            logger.debug("📤 Message content: ${messageJson.take(200)}...")
            
        } catch (e: Exception) {
            logger.error("❌ Failed to send message: ${e.message}")
            throw e
        }
    }
    
    /**
     * Receive messages from the WebSocket server
     */
    override fun receive(): Flow<RpcMessage> {
        return messageChannel.receiveAsFlow()
    }
    
    /**
     * Send RPC request and wait for response
     */
    suspend fun sendRequest(request: RpcRequest): RpcResponse {
        send(request)
        
        // Wait for response with timeout
        return withTimeout(config.requestTimeoutMs) {
            receive()
                .filterIsInstance<RpcResponse>()
                .first { it.messageId == request.messageId }
        }
    }
    
    /**
     * Send stream request and return flow of stream data
     */
    fun sendStreamRequest(streamStart: StreamStart): Flow<StreamData> {
        return flow {
            send(streamStart)
            
            receive()
                .filterIsInstance<StreamData>()
                .filter { it.streamId == streamStart.streamId }
                .collect { streamData ->
                    emit(streamData)
                }
        }
    }
    
    /**
     * Check connection health
     */
    suspend fun ping(): Boolean {
        return try {
            if (_isConnected && webSocketSession != null) {
                webSocketSession!!.send(Frame.Ping("ping".toByteArray()))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.warn("⚠️ Ping failed: ${e.message}")
            false
        }
    }
    
    /**
     * Reconnect to the server
     */
    suspend fun reconnect() {
        logger.info("🔄 Reconnecting to WebSocket server...")
        disconnect()
        delay(config.reconnectDelayMs)
        connect()
    }
    
    /**
     * Get connection statistics
     */
    fun getConnectionStats(): KtorWebSocketClientStats {
        return KtorWebSocketClientStats(
            isConnected = _isConnected,
            serverUrl = config.serverUrl,
            connectionTime = if (_isConnected) Clock.System.now().toEpochMilliseconds() else null
        )
    }
    
    private suspend fun handleIncomingMessages() {
        try {
            val session = webSocketSession ?: return
            
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val messageText = frame.readText()
                        logger.info("📨 Received message: ${messageText.take(100)}...")
                        
                        try {
                            val message = parseIncomingMessage(messageText)
                            messageChannel.send(message)
                            
                            logger.debug("📨 Parsed message: ${message::class.simpleName} (${message.messageId})")
                            
                        } catch (e: Exception) {
                            logger.error("❌ Error parsing message: ${e.message}")
                            logger.debug("❌ Raw message: $messageText")
                        }
                    }
                    is Frame.Close -> {
                        val reason = frame.readReason()
                        logger.info("🔌 Server closed connection: ${reason?.message ?: "Unknown reason"}")
                        break
                    }
                    is Frame.Pong -> {
                        logger.debug("🏓 Received pong from server")
                    }
                    else -> {
                        logger.debug("📨 Received non-text frame: ${frame.frameType}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Error handling incoming messages: ${e.message}")
        }
    }
    
    private fun parseIncomingMessage(messageText: String): RpcMessage {
        return try {
            // Try to parse as different message types based on content
            when {
                messageText.contains("\"result\"") && !messageText.contains("\"errorCode\"") -> {
                    json.decodeFromString<RpcResponse>(messageText)
                }
                messageText.contains("\"errorCode\"") && messageText.contains("\"streamId\"") -> {
                    json.decodeFromString<StreamError>(messageText)
                }
                messageText.contains("\"errorCode\"") -> {
                    json.decodeFromString<RpcError>(messageText)
                }
                messageText.contains("\"streamId\"") && messageText.contains("\"data\"") -> {
                    json.decodeFromString<StreamData>(messageText)
                }
                messageText.contains("\"streamId\"") && !messageText.contains("\"data\"") -> {
                    if (messageText.contains("\"serviceName\"")) {
                        json.decodeFromString<StreamStart>(messageText)
                    } else {
                        json.decodeFromString<StreamEnd>(messageText)
                    }
                }
                messageText.contains("\"serviceName\"") && messageText.contains("\"methodId\"") -> {
                    json.decodeFromString<RpcRequest>(messageText)
                }
                else -> {
                    throw IllegalArgumentException("Unknown message format")
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to parse message: ${e.message}")
            logger.debug("❌ Message content: $messageText")
            throw e
        }
    }
}

/**
 * Configuration for Ktor WebSocket client transport
 */
data class KtorWebSocketClientConfig(
    val serverUrl: String = "ws://localhost:8080/rpc",
    val pingIntervalSeconds: Long = 15,
    val maxFrameSize: Long = 1024 * 1024, // 1MB
    val requestTimeoutMs: Long = 30_000,
    val reconnectDelayMs: Long = 5_000,
    val maxReconnectAttempts: Int = 3
)

/**
 * Connection statistics for the client
 */
data class KtorWebSocketClientStats(
    val isConnected: Boolean,
    val serverUrl: String,
    val connectionTime: Long? = null
)

/**
 * Builder for creating Ktor WebSocket client transport
 */
class KtorWebSocketClientBuilder {
    private var config = KtorWebSocketClientConfig()
    
    fun serverUrl(url: String) = apply { config = config.copy(serverUrl = url) }
    fun pingInterval(seconds: Long) = apply { config = config.copy(pingIntervalSeconds = seconds) }
    fun maxFrameSize(bytes: Long) = apply { config = config.copy(maxFrameSize = bytes) }
    fun requestTimeout(ms: Long) = apply { config = config.copy(requestTimeoutMs = ms) }
    fun reconnectDelay(ms: Long) = apply { config = config.copy(reconnectDelayMs = ms) }
    fun maxReconnectAttempts(attempts: Int) = apply { config = config.copy(maxReconnectAttempts = attempts) }
    
    fun build(scope : CoroutineScope): KtorWebSocketClientTransport = KtorWebSocketClientTransport(config, scope)
}

/**
 * DSL function for creating Ktor WebSocket client transport
 */
fun ktorWebSocketClientTransport(scope: CoroutineScope, block: KtorWebSocketClientBuilder.() -> Unit = {}): KtorWebSocketClientTransport {
    return KtorWebSocketClientBuilder().apply(block).build(scope)
}

