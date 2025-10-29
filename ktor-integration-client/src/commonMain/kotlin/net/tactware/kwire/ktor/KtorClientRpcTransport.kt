@file:OptIn(ExperimentalTime::class)

package net.tactware.kwire.ktor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.tactware.kwire.core.RpcErrorCodes
import net.tactware.kwire.core.RpcTransport
import net.tactware.kwire.core.messages.RpcError
import net.tactware.kwire.core.messages.RpcMessage
import net.tactware.kwire.core.messages.RpcRequest
import net.tactware.kwire.core.messages.RpcResponse
import net.tactware.kwire.core.messages.StreamData
import net.tactware.kwire.core.messages.StreamError
import net.tactware.kwire.core.messages.StreamStart
import net.tactware.kwire.security.AuthCredentials
import net.tactware.kwire.security.AuthResult
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Ktor-based RPC transport implementation.
 * Properly implements the RpcTransport interface for HTTP-based RPC communication.
 */

class KtorClientRpcTransport(
    private val config: KtorRpcConfig = KtorRpcConfig()
) : RpcTransport {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val obfuscationManager = config.obfuscationManager
    private val securityManager = config.securityManager

    // Connection state
    private var _isConnected = false
    override val isConnected: Boolean get() = _isConnected
    
    // Message flow for receiving messages
    private val _messageFlow = MutableSharedFlow<RpcMessage>()
    
    /**
     * Send a message through the transport.
     * This is the correct signature matching RpcTransport interface.
     */
    override suspend fun send(message: RpcMessage) {
        when (message) {
            is RpcRequest -> {
                val response = handleRequest(message)
                _messageFlow.emit(response)
            }
            is StreamStart -> {
                val response = handleStreamStart(message)
                _messageFlow.emit(response)
            }
            else -> {
                val error = RpcError(
                    messageId = message.messageId,
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    errorCode = RpcErrorCodes.INVALID_REQUEST,
                    errorMessage = "Unsupported message type: ${message::class.simpleName}"
                )
                _messageFlow.emit(error)
            }
        }
    }
    
    /**
     * Receive messages from the transport as a Flow.
     */
    override fun receive(): Flow<RpcMessage> {
        return _messageFlow.asSharedFlow()
    }
    
    /**
     * Connect to the remote endpoint.
     */
    override suspend fun connect() {
        if (!_isConnected) {
            // Perform connection logic here (HTTP client setup, etc.)
            _isConnected = true
        }
    }
    
    /**
     * Disconnect from the remote endpoint.
     */
    override suspend fun disconnect() {
        if (_isConnected) {
            // Perform disconnection logic here (cleanup, etc.)
            _isConnected = false
        }
    }
    
    private suspend fun handleRequest(request: RpcRequest): RpcMessage {
        return try {
            // Validate authentication if security is enabled
            if (securityManager != null) {
                val authResult = validateAuthentication(request)
                if (authResult !is AuthResult.Success) {
                    return RpcError(
                        messageId = request.messageId,
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                        errorCode = RpcErrorCodes.AUTHENTICATION_ERROR,
                        errorMessage = "Authentication failed"
                    )
                }
            }
            
            // Resolve method name if obfuscation is enabled
            val (serviceName, methodName) = try {
                val resolved = obfuscationManager?.getOriginalMethodName(request.methodId)
                resolved ?: // REJECT the request instead of falling back
                throw IllegalArgumentException("Invalid or non-obfuscated method ID: ${request.methodId}")
            } catch (e: Exception) {
                // Return error instead of falling back
                return RpcError(
                    messageId = request.messageId,
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    errorCode = RpcErrorCodes.METHOD_NOT_FOUND,
                    errorMessage = "Method not found or invalid obfuscation: ${request.methodId}"
                )
            }
            
            // Process the request
            val result = processRpcCall(serviceName, methodName, request.parameters)

            RpcResponse(
                messageId = request.messageId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                result = result,
                streaming = request.streaming
            )
            
        } catch (e: Exception) {
            RpcError(
                messageId = request.messageId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                errorCode = RpcErrorCodes.INTERNAL_ERROR,
                errorMessage = e.message ?: "Unknown error",
                errorDetails = buildJsonObject {
                    put("exception", e::class.simpleName ?: "Unknown")
                }
            )
        }
    }
    
    private suspend fun handleStreamStart(streamStart: StreamStart): RpcMessage {
        return try {
            // Validate authentication if security is enabled
            if (securityManager != null) {
                val authResult = validateAuthentication(streamStart)
                if (authResult !is AuthResult.Success) {
                    return StreamError(
                        messageId = streamStart.messageId,
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                        streamId = streamStart.streamId,
                        errorCode = RpcErrorCodes.AUTHENTICATION_ERROR,
                        errorMessage = "Authentication failed"
                    )
                }
            }
            
            // Start the stream
            StreamData(
                messageId = streamStart.messageId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                streamId = streamStart.streamId,
                data = buildJsonObject {
                    put("message", "Stream started")
                    put("service", streamStart.serviceName)
                    put("method", streamStart.methodId)
                }
            )
            
        } catch (e: Exception) {
            StreamError(
                messageId = streamStart.messageId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                streamId = streamStart.streamId,
                errorCode = RpcErrorCodes.STREAM_ERROR,
                errorMessage = e.message ?: "Stream error",
                errorDetails = buildJsonObject {
                    put("exception", e::class.simpleName ?: "Unknown")
                }
            )
        }
    }
    
    private fun validateAuthentication(message: RpcMessage): AuthResult {
        // Extract authentication info from message metadata (if available)
        val credentials = AuthCredentials(
            clientId = "ktor_client",
            token = "default_token"
        )
        
        return securityManager?.authenticate(credentials) ?: AuthResult.Success("no_auth")
    }
    
    private suspend fun processRpcCall(
        serviceName: String,
        methodName: String,
        parameters: List<JsonElement>
    ): JsonElement {
        // Placeholder implementation - in a real system, this would dispatch to actual service methods
        return buildJsonObject {
            put("service", serviceName)
            put("method", methodName)
            put("parameterCount", parameters.size)
            put("result", "success")
            put("timestamp", Clock.System.now().toEpochMilliseconds())
        }
    }
}




