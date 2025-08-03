package com.obfuscated.rpc.sample.server

import com.obfuscated.rpc.core.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.CancellationException
import io.ktor.websocket.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Ktor-based RPC server transport that handles HTTP and WebSocket connections
 */
class KtorRpcServerTransport : RpcTransport {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private var _isConnected = false
    override val isConnected: Boolean get() = _isConnected
    
    private val messageChannel = Channel<RpcMessage>(Channel.UNLIMITED)
    
    override suspend fun send(message: RpcMessage) {
        messageChannel.send(message)
    }
    
    override fun receive(): Flow<RpcMessage> {
        return messageChannel.receiveAsFlow()
    }
    
    override suspend fun connect() {
        _isConnected = true
    }
    
    override suspend fun disconnect() {
        _isConnected = false
        messageChannel.close()
    }
    
    /**
     * Configure Ktor routing for RPC endpoints
     */
    fun configureRouting(application: Application, rpcHandler: suspend (RpcMessage) -> RpcMessage, userStreamChannel: Channel<User>) {
        application.routing {
            // HTTP endpoint for regular RPC calls
            post("/rpc") {
                try {
                    val requestText = call.receiveText()
                    val rpcRequest = json.decodeFromString<RpcRequest>(requestText)
                    
                    val response = rpcHandler(rpcRequest)
                    val responseJson = json.encodeToString(response)
                    
                    call.respondText(responseJson, io.ktor.http.ContentType.Application.Json)
                } catch (e: Exception) {
                    val error = RpcError(
                        messageId = "unknown",
                        timestamp = System.currentTimeMillis(),
                        errorCode = RpcErrorCodes.INVALID_REQUEST,
                        errorMessage = e.message ?: "Invalid request"
                    )
                    val errorJson = json.encodeToString(error)
                    call.respondText(errorJson, io.ktor.http.ContentType.Application.Json)
                }
            }
            
            // WebSocket endpoint for streaming RPC
            webSocket("/rpc-stream") {
                try {
                    println("WebSocket client connected for streaming")

                    var streamingJob: Job? = null

                    try {
                        for (frame in incoming) {
                            try {
                                when (frame) {
                                    is Frame.Text -> {
                                        val text = frame.readText()
                                        println("Received WebSocket message: $text")

                                        try {
                                            val streamStart = json.decodeFromString<StreamStart>(text)
                                            println("Starting stream: ${streamStart.streamId}")

                                            streamingJob?.cancel()

                                            streamingJob = launch {
                                                try {
                                                    // Send confirmation
                                                    val confirmation = StreamData(
                                                        messageId = streamStart.messageId,
                                                        timestamp = System.currentTimeMillis(),
                                                        streamId = streamStart.streamId,
                                                        data = kotlinx.serialization.json.buildJsonObject {
                                                            put("status", kotlinx.serialization.json.JsonPrimitive("streaming_started"))
                                                            put("message", kotlinx.serialization.json.JsonPrimitive("User streaming started"))
                                                        }
                                                    )
                                                    send(Frame.Text(json.encodeToString(confirmation)))
                                                    println("Sent streaming confirmation")

                                                    // Send some test users immediately
                                                    val testUsers = listOf(
                                                        User("test1", "Test User 1", "test1@example.com", 25),
                                                        User("test2", "Test User 2", "test2@example.com", 30),
                                                        User("test3", "Test User 3", "test3@example.com", 35)
                                                    )

                                                    for (user in testUsers) {
                                                        val streamData = StreamData(
                                                            messageId = "stream-${System.currentTimeMillis()}",
                                                            timestamp = System.currentTimeMillis(),
                                                            streamId = streamStart.streamId,
                                                            data = json.encodeToJsonElement(User.serializer(), user)
                                                        )
                                                        send(Frame.Text(json.encodeToString(streamData)))
                                                        println("Streamed test user: ${user.name}")
                                                        delay(1000) // 1 second between users
                                                    }

                                                    // Then try to consume from the channel
                                                    println("Now consuming from userStreamChannel...")
                                                    while (isActive) {
                                                        val user = userStreamChannel.tryReceive().getOrNull()
                                                        if (user != null) {
                                                            val streamData = StreamData(
                                                                messageId = "stream-${System.currentTimeMillis()}",
                                                                timestamp = System.currentTimeMillis(),
                                                                streamId = streamStart.streamId,
                                                                data = json.encodeToJsonElement(User.serializer(), user)
                                                            )
                                                            send(Frame.Text(json.encodeToString(streamData)))
                                                            println("Streamed channel user: ${user.name}")
                                                        } else {
                                                            delay(1000)
                                                        }
                                                    }
                                                } catch (e: CancellationException) {
                                                    println("Streaming job cancelled")
                                                    throw e
                                                } catch (e: Exception) {
                                                    println("Streaming error: ${e.message}")
                                                    e.printStackTrace()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            println("Failed to parse stream start message: ${e.message}")
                                        }
                                    }
                                    is Frame.Binary -> {
                                        println("Received binary frame, ignoring")
                                    }
                                    is Frame.Close -> {
                                        println("WebSocket connection closed by client")
                                        streamingJob?.cancel()
                                        break
                                    }
                                    is Frame.Ping -> {
                                        try {
                                            send(Frame.Pong(frame.data))
                                        } catch (e: Exception) {
                                            println("Failed to send pong: ${e.message}")
                                        }
                                    }
                                    is Frame.Pong -> {
                                        println("Received pong frame")
                                    }
                                    else -> {
                                        println("Received unsupported frame type: ${frame.frameType}, ignoring")
                                    }
                                }
                            } catch (e: IllegalStateException) {
                                println("WebSocket frame parsing error (invalid opcode): ${e.message}")
                                // Continue processing other frames
                            } catch (e: Exception) {
                                println("Error processing WebSocket frame: ${e.message}")
                                // Continue processing other frames
                            }

                        }
                    } catch (e: CancellationException) {
                        println("WebSocket connection cancelled normally")
                    } catch (e: Exception) {
                        println("WebSocket frame handling error: ${e.message}")
                    }
                } catch (e: Exception) {
                    println("WebSocket error: ${e.message}")

                    try {
                        val errorResponse = StreamError(
                            messageId = "error-${System.currentTimeMillis()}",
                            timestamp = System.currentTimeMillis(),
                            streamId = "user-stream",
                            errorCode = RpcErrorCodes.STREAM_ERROR,
                            errorMessage = e.message ?: "Stream error"
                        )
                        val errorJson = json.encodeToString(errorResponse)
                        send(Frame.Text(errorJson))
                    } catch (sendError: Exception) {
                        println("Failed to send error response: ${sendError.message}")
                    }
                }
            }
            
            // Health check endpoint
            get("/health") {
                call.respondText("OK")
            }
        }
    }
    
    /**
     * Handle streaming requests
     */
    private suspend fun handleStreamingRequest(streamStart: StreamStart, rpcHandler: suspend (RpcMessage) -> RpcMessage): Flow<RpcMessage> {
        return flow {
            // Convert StreamStart to RpcRequest
            val rpcRequest = RpcRequest(
                messageId = streamStart.messageId,
                timestamp = streamStart.timestamp,
                serviceName = streamStart.serviceName,
                methodId = streamStart.methodId,
                parameters = streamStart.parameters,
                streaming = true
            )
            
            // Process the request
            val response = rpcHandler(rpcRequest)
            emit(response)
        }
    }
}

