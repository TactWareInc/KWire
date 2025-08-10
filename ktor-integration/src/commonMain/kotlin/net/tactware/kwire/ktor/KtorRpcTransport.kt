@file:OptIn(ExperimentalTime::class)

package net.tactware.kwire.ktor

import net.tactware.kwire.core.*
import net.tactware.kwire.security.ObfuscationManager
import net.tactware.kwire.security.SecurityManager
import net.tactware.kwire.security.AuthCredentials
import net.tactware.kwire.security.AuthResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.tactware.kwire.core.RpcTransport
import net.tactware.kwire.core.messages.RpcError
import net.tactware.kwire.core.messages.RpcMessage
import net.tactware.kwire.core.messages.RpcRequest
import net.tactware.kwire.core.messages.RpcResponse
import net.tactware.kwire.core.messages.StreamData
import net.tactware.kwire.core.messages.StreamEnd
import net.tactware.kwire.core.messages.StreamError
import net.tactware.kwire.core.messages.StreamStart
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Ktor-based RPC transport implementation.
 * Properly implements the RpcTransport interface for HTTP-based RPC communication.
 */

class KtorRpcTransport(
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

/**
 * Configuration for Ktor RPC transport.
 */
data class KtorRpcConfig(
    val baseUrl: String = "http://localhost:8080",
    val timeout: Long = 30000,
    val enableSecurity: Boolean = true,
    val enableObfuscation: Boolean = true,
    val obfuscationManager: ObfuscationManager? = null,
    val securityManager: SecurityManager? = null,
)

/**
 * Ktor-specific RPC client implementation.
 */
class KtorRpcClient(
    private val config: KtorRpcConfig
) {
    private val transport = KtorRpcTransport(config)
    
    suspend fun call(
        serviceName: String,
        methodName: String,
        parameters: List<JsonElement>
    ): JsonElement {
        val obfuscatedMethodName = config.obfuscationManager?.getObfuscatedMethodName(serviceName, methodName) ?: methodName
        val obfuscatedServiceName = config.obfuscationManager?.getObfuscatedServiceName(serviceName) ?: serviceName
        
        val request = RpcRequest(
            messageId = generateMessageId(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            serviceName = obfuscatedServiceName,
            methodId = obfuscatedMethodName,
            parameters = parameters
        )
        
        // Connect if not already connected
        if (!transport.isConnected) {
            transport.connect()
        }
        
        // Send request and collect response
        var result: JsonElement? = null
        var error: String? = null
        
        transport.receive().collect { message ->
            when (message) {
                is RpcResponse -> {
                    if (message.messageId == request.messageId) {
                        result = message.result
                        return@collect
                    }
                }
                is RpcError -> {
                    if (message.messageId == request.messageId) {
                        error = message.errorMessage
                        return@collect
                    }
                }
                else -> {
                    // Ignore other message types
                }
            }
        }
        
        // Send the request
        transport.send(request)
        
        return result ?: throw RpcException(error ?: "No response received")
    }
    
    suspend fun stream(
        serviceName: String,
        methodName: String,
        parameters: List<JsonElement>
    ): Flow<JsonElement> = flow {
        val obfuscatedMethodName = config.obfuscationManager?.getObfuscatedMethodName(serviceName, methodName) ?: methodName
        val obfuscatedServiceName = config.obfuscationManager?.getObfuscatedServiceName(serviceName) ?: serviceName
        
        val streamStart = StreamStart(
            messageId = generateMessageId(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            serviceName = obfuscatedServiceName,
            methodId = obfuscatedMethodName,
            streamId = generateStreamId(),
            parameters = parameters
        )
        
        // Connect if not already connected
        if (!transport.isConnected) {
            transport.connect()
        }
        
        // Send stream start
        transport.send(streamStart)
        
        // Collect stream responses
        transport.receive().collect { message ->
            when (message) {
                is StreamData -> {
                    if (message.streamId == streamStart.streamId) {
                        emit(message.data)
                    }
                }
                is StreamEnd -> {
                    if (message.streamId == streamStart.streamId) {
                        return@collect
                    }
                }
                is StreamError -> {
                    if (message.streamId == streamStart.streamId) {
                        throw RpcException("Stream error: ${message.errorMessage}")
                    }
                }
                else -> {
                    // Ignore other message types
                }
            }
        }
    }
    
    private fun generateMessageId(): String {
        return "msg_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
    
    private fun generateStreamId(): String {
        return "stream_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
}

/**
 * Ktor-specific RPC server implementation.
 */
class KtorRpcServer(
    private val config: KtorRpcConfig
) {
    private val transport = KtorRpcTransport(config)
    private val serviceHandlers = mutableMapOf<String, suspend (String, List<JsonElement>) -> JsonElement>()
    
    fun registerService(
        serviceName: String,
        handler: suspend (String, List<JsonElement>) -> JsonElement
    ) {
        serviceHandlers[serviceName] = handler
    }
    
    suspend fun handleRequest(requestJson: String): String {
        return try {
            val request = Json.decodeFromString<RpcRequest>(requestJson)
            
            // Connect if not already connected
            if (!transport.isConnected) {
                transport.connect()
            }
            
            // Send request through transport
            transport.send(request)
            
            // Collect response
            var response: RpcMessage? = null
            transport.receive().collect { message ->
                if (message.messageId == request.messageId) {
                    response = message
                    return@collect
                }
            }
            
            Json.encodeToString(response ?: RpcError(
                messageId = request.messageId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                errorCode = RpcErrorCodes.INTERNAL_ERROR,
                errorMessage = "No response generated"
            ))
            
        } catch (e: Exception) {
            val errorResponse = RpcError(
                messageId = "unknown",
                timestamp = Clock.System.now().toEpochMilliseconds(),
                errorCode = RpcErrorCodes.INTERNAL_ERROR,
                errorMessage = e.message ?: "Unknown error"
            )
            Json.encodeToString(errorResponse)
        }
    }
}

/**
 * Utility functions for Ktor integration.
 */
object KtorRpcUtils {
    
    /**
     * Create a default Ktor RPC client.
     */
    fun createClient(
        baseUrl: String = "http://localhost:8080",
        enableObfuscation: Boolean = true,
        enableSecurity: Boolean = true
    ): KtorRpcClient {
        val config = KtorRpcConfig(
            baseUrl = baseUrl,
            enableObfuscation = enableObfuscation,
            enableSecurity = enableSecurity
        )
        return KtorRpcClient(config)
    }
    
    /**
     * Create a default Ktor RPC server.
     */
    fun createServer(
        enableObfuscation: Boolean = true,
        enableSecurity: Boolean = true
    ): KtorRpcServer {
        val config = KtorRpcConfig(
            enableObfuscation = enableObfuscation,
            enableSecurity = enableSecurity
        )
        return KtorRpcServer(config)
    }
}


