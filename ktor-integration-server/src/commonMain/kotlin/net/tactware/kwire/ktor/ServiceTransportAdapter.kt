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