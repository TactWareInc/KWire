package com.obfuscated.rpc.sample.client

import com.obfuscated.rpc.core.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Ktor-based RPC client transport that handles HTTP and WebSocket connections
 */
class KtorRpcClientTransport(
    private val baseUrl: String = "http://localhost:8080"
) : RpcTransport {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets)
    }
    
    private var _isConnected = false
    override val isConnected: Boolean get() = _isConnected
    
    private val responseChannel = Channel<RpcMessage>(Channel.UNLIMITED)
    
    override suspend fun send(message: RpcMessage) {
        when (message) {
            is RpcRequest -> {
                if (message.streaming) {
                    handleStreamingRequest(message)
                } else {
                    handleHttpRequest(message)
                }
            }
            is StreamStart -> {
                handleStreamStart(message)
            }
            else -> {
                val error = RpcError(
                    messageId = message.messageId,
                    timestamp = System.currentTimeMillis(),
                    errorCode = RpcErrorCodes.INVALID_REQUEST,
                    errorMessage = "Unsupported message type for client transport"
                )
                responseChannel.send(error)
            }
        }
    }
    
    override fun receive(): Flow<RpcMessage> {
        return responseChannel.receiveAsFlow()
    }
    
    override suspend fun connect() {
        _isConnected = true
    }
    
    override suspend fun disconnect() {
        _isConnected = false
        httpClient.close()
        responseChannel.close()
    }
    
    private suspend fun handleHttpRequest(request: RpcRequest) {
        try {
            val requestJson = json.encodeToString(request)
            
            val response = httpClient.post("$baseUrl/rpc") {
                contentType(ContentType.Application.Json)
                setBody(requestJson)
            }
            
            val responseText = response.bodyAsText()
            
            if (response.status.isSuccess()) {
                try {
                    val rpcResponse = json.decodeFromString<RpcResponse>(responseText)
                    responseChannel.send(rpcResponse)
                } catch (e: Exception) {
                    // Try to decode as error
                    try {
                        val rpcError = json.decodeFromString<RpcError>(responseText)
                        responseChannel.send(rpcError)
                    } catch (e2: Exception) {
                        val error = RpcError(
                            messageId = request.messageId,
                            timestamp = System.currentTimeMillis(),
                            errorCode = RpcErrorCodes.SERIALIZATION_ERROR,
                            errorMessage = "Failed to decode response: ${e.message}"
                        )
                        responseChannel.send(error)
                    }
                }
            } else {
                val error = RpcError(
                    messageId = request.messageId,
                    timestamp = System.currentTimeMillis(),
                    errorCode = RpcErrorCodes.INTERNAL_ERROR,
                    errorMessage = "HTTP error: ${response.status}"
                )
                responseChannel.send(error)
            }
        } catch (e: Exception) {
            val error = RpcError(
                messageId = request.messageId,
                timestamp = System.currentTimeMillis(),
                errorCode = RpcErrorCodes.INTERNAL_ERROR,
                errorMessage = "Request failed: ${e.message}"
            )
            responseChannel.send(error)
        }
    }
    
    private suspend fun handleStreamingRequest(request: RpcRequest) {
        // Convert to StreamStart for WebSocket
        val streamStart = StreamStart(
            messageId = request.messageId,
            timestamp = request.timestamp,
            serviceName = request.serviceName,
            methodId = request.methodId,
            streamId = generateStreamId(),
            parameters = request.parameters
        )
        
        handleStreamStart(streamStart)
    }
    
    private suspend fun handleStreamStart(streamStart: StreamStart) {
        try {
            val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
            
            httpClient.webSocket("$wsUrl/rpc-stream") {
                // Send stream start message
                val requestJson = json.encodeToString(streamStart)
                send(Frame.Text(requestJson))

                // Receive stream responses
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            // Try to decode different message types
                            when {
                                text.contains("\"data\"") -> {
                                    val streamData = json.decodeFromString<StreamData>(text)
                                    responseChannel.send(streamData)
                                }
                                text.contains("\"errorCode\"") && text.contains("\"streamId\"") -> {
                                    val streamError = json.decodeFromString<StreamError>(text)
                                    responseChannel.send(streamError)
                                    break
                                }
                                text.contains("\"streamId\"") -> {
                                    val streamEnd = json.decodeFromString<StreamEnd>(text)
                                    responseChannel.send(streamEnd)
                                    break
                                }
                                else -> {
                                    // Try generic RPC message
                                    val rpcError = json.decodeFromString<RpcError>(text)
                                    responseChannel.send(rpcError)
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            val error = StreamError(
                                messageId = streamStart.messageId,
                                timestamp = System.currentTimeMillis(),
                                streamId = streamStart.streamId,
                                errorCode = RpcErrorCodes.SERIALIZATION_ERROR,
                                errorMessage = "Failed to decode stream message: ${e.message}"
                            )
                            responseChannel.send(error)
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            val error = StreamError(
                messageId = streamStart.messageId,
                timestamp = System.currentTimeMillis(),
                streamId = streamStart.streamId,
                errorCode = RpcErrorCodes.STREAM_ERROR,
                errorMessage = "WebSocket error: ${e.message}"
            )
            responseChannel.send(error)
        }

    }
    
    private fun generateStreamId(): String {
        return "stream_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}

