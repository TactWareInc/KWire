@file:OptIn(ExperimentalTime::class)

package net.tactware.kwire.ktor

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
import net.tactware.kwire.core.RpcException
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
 * Ktor-specific RPC client implementation.
 */
class KtorRpcClient(
    private val config: KtorRpcConfig
) {
    private val transport = KtorClientRpcTransport(config)
    
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