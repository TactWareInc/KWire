package net.tactware.kwire.ktor

import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.tactware.kwire.core.ConnectFailureReason
import net.tactware.kwire.core.RpcConnectResult
import net.tactware.kwire.core.RpcTransport
import net.tactware.kwire.core.messages.RpcError
import net.tactware.kwire.core.messages.RpcMessage
import net.tactware.kwire.core.messages.RpcRequest
import net.tactware.kwire.core.messages.RpcResponse
import net.tactware.kwire.core.messages.StreamData
import net.tactware.kwire.core.messages.StreamEnd
import net.tactware.kwire.core.messages.StreamError
import net.tactware.kwire.core.messages.StreamStart
import org.slf4j.LoggerFactory

/**
 * WebSocket session-based RPC transport implementation.
 * 
 * This transport works with an existing WebSocketSession instead of creating its own server.
 * Perfect for use with the new rpc<T> API where the WebSocket is already established.
 * 
 * Usage:
 * ```kotlin
 * rpc<UserService>("/users") { ctx ->
 *     val transport = WebSocketSessionTransport(ctx.session)
 *     val serverImpl = UserServiceServerImpl(
 *         transport = transport,
 *         implementation = UserServiceImpl()
 *     )
 *     serverImpl.start()
 *     
 *     // Handle incoming messages
 *     transport.handleIncomingFrames()
 *     
 *     UserServiceImpl() // Return the service
 * }
 * ```
 */
class WebSocketSessionTransport(
    private val session: WebSocketSession,
    private val config: WebSocketTransportConfig = WebSocketTransportConfig()
) : RpcTransport {
    
    private val logger = LoggerFactory.getLogger("WebSocketSessionTransport")
    private val json = config.json
    
    // Connection state
    override val isConnected: Boolean 
        get() = !session.outgoing.isClosedForSend
    
    // Message channels
    private val incomingMessages = Channel<RpcMessage>(Channel.UNLIMITED)
    private val outgoingMessages = Channel<RpcMessage>(Channel.UNLIMITED)
    
    // Coroutine scope for this transport
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Message handlers (optional - for direct handling)
    private var requestHandler: (suspend (RpcRequest) -> Unit)? = null
    private var streamHandler: (suspend (StreamStart) -> Unit)? = null
    
    init {
        logger.debug("WebSocketSessionTransport created for session")
    }
    
    /**
     * Start processing incoming frames from the WebSocket session.
     * This should be called after the transport is set up.
     */
    suspend fun handleIncomingFrames() {
        for (frame in session.incoming) {
            when (frame) {
                is Frame.Text -> {
                    val messageText = frame.readText()
                    logger.debug("Received message: ${messageText.take(100)}...")
                    
                    try {
                        val message = parseMessage(messageText)
                        
                        // Emit to the receive flow
                        incomingMessages.send(message)
                        
                        // Also handle directly if handlers are set
                        when (message) {
                            is RpcRequest -> {
                                logger.info("Handling RPC request: ${message.serviceName}.${message.methodId}")
                                requestHandler?.invoke(message)
                            }
                            is StreamStart -> {
                                logger.info("Handling stream start: ${message.serviceName}.${message.methodId}")
                                streamHandler?.invoke(message)
                            }
                            else -> {
                                logger.debug("Received message: ${message::class.simpleName}")
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Error parsing message: ${e.message}")
                        sendError("unknown", "PARSE_ERROR", e.message ?: "Failed to parse message")
                    }
                }
                is Frame.Close -> {
                    logger.info("WebSocket connection closed")
                    break
                }
                else -> {
                    logger.debug("Received non-text frame: ${frame.frameType}")
                }
            }
        }
        
        // Cleanup when done
        cleanup()
    }
    
    /**
     * Alternative: Process frames in a coroutine
     */
    fun startProcessing(): Job {
        return scope.launch {
            handleIncomingFrames()
        }
    }
    
    /**
     * Connect is a no-op since we're using an existing session
     */
    override suspend fun connect(): RpcConnectResult {
        logger.debug("Connect called - using existing WebSocket session")
        return if (isConnected) {
            RpcConnectResult.AlreadyConnected
        } else {
            RpcConnectResult.Failed(ConnectFailureReason.UNKNOWN, IllegalStateException("Session is closed"))
        }
    }
    
    /**
     * Disconnect closes the WebSocket session
     */
    override suspend fun disconnect() {
        logger.info("Disconnecting WebSocket session")
        try {
            session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Transport disconnecting"))
        } catch (e: Exception) {
            logger.warn("Error closing session: ${e.message}")
        }
        cleanup()
    }
    
    /**
     * Send a message through the WebSocket session
     */
    override suspend fun send(message: RpcMessage) {
        if (!isConnected) {
            when (val result = connect()) {
                is RpcConnectResult.Failed -> throw RuntimeException("Connect failed: ${result.reason}", result.cause)
                else -> {}
            }
        }
        try {
            val messageJson = json.encodeToString(RpcMessage.serializer(), message)
            session.send(Frame.Text(messageJson))
            logger.debug("Sent message: ${message::class.simpleName} (${message.messageId})")
        } catch (e: Exception) {
            logger.error("Failed to send message: ${e.message}")
            throw e
        }
    }
    
    /**
     * Receive messages as a Flow
     */
    override fun receive(): Flow<RpcMessage> {
        return incomingMessages.receiveAsFlow()
    }
    
    /**
     * Set a handler for RPC requests
     */
    fun setRequestHandler(handler: suspend (RpcRequest) -> Unit) {
        this.requestHandler = handler
    }
    
    /**
     * Set a handler for stream starts
     */
    fun setStreamHandler(handler: suspend (StreamStart) -> Unit) {
        this.streamHandler = handler
    }
    
    /**
     * Send an error message
     */
    private suspend fun sendError(messageId: String, errorCode: String, errorMessage: String) {
        val error = RpcError(
            messageId = messageId,
            timestamp = System.currentTimeMillis(),
            errorCode = errorCode,
            errorMessage = errorMessage
        )
        send(error)
    }
    
    /**
     * Parse incoming message text
     */
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
    
    /**
     * Cleanup resources
     */
    private fun cleanup() {
        scope.cancel()
        incomingMessages.close()
        outgoingMessages.close()
        logger.debug("Transport cleaned up")
    }
}


/**
 * Extension function to create a transport from a WebSocketSession
 */
fun WebSocketSession.asRpcTransport(
    config: WebSocketTransportConfig = WebSocketTransportConfig()
): WebSocketSessionTransport {
    return WebSocketSessionTransport(this, config)
}

/**
 * Builder function for WebSocketSessionTransport
 */
fun webSocketTransport(
    session: WebSocketSession,
    configure: WebSocketTransportConfig.() -> Unit = {}
): WebSocketSessionTransport {
    val config = WebSocketTransportConfig().apply(configure)
    return WebSocketSessionTransport(session, config)
}
