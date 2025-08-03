@file:OptIn(ExperimentalTime::class)

package com.obfuscated.rpc.runtime

import com.obfuscated.rpc.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.KSerializer
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * RPC client for making remote procedure calls.
 */
class RpcClient(
    private val transport: RpcTransport,
    private val json: Json = Json.Default,
    private val config: RpcClientConfig = RpcClientConfig()
) {
    private val pendingRequests = mutableMapOf<String, CompletableDeferred<RpcMessage>>()
    private val activeStreams = mutableMapOf<String, Channel<StreamMessage>>()
    private var isStarted = false
    
    /**
     * Start the RPC client.
     */
    suspend fun start() {
        if (isStarted) return
        
        transport.connect()
        startMessageHandler()
        isStarted = true
    }
    
    /**
     * Stop the RPC client.
     */
    suspend fun stop() {
        if (!isStarted) return
        
        // Cancel all pending requests
        pendingRequests.values.forEach { it.cancel() }
        pendingRequests.clear()
        
        // Close all active streams
        activeStreams.values.forEach { it.close() }
        activeStreams.clear()
        
        transport.disconnect()
        isStarted = false
    }
    
    /**
     * Make a synchronous RPC call.
     */
    suspend fun <T> call(
        serviceName: String,
        methodId: String,
        parameters: List<Any> = emptyList(),
        resultSerializer: KSerializer<T>
    ): T {
        val messageId = generateMessageId()
        
        // Convert parameters to JsonElement
        val jsonParameters = parameters.map { param ->
            when (param) {
                is String -> json.encodeToJsonElement(param)
                is Int -> json.encodeToJsonElement(param)
                is Double -> json.encodeToJsonElement(param)
                is Boolean -> json.encodeToJsonElement(param)
                is JsonElement -> param
                else -> json.encodeToJsonElement(param.toString())
            }
        }
        
        val request = RpcRequest(
            messageId = messageId,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            serviceName = serviceName,
            methodId = methodId,
            parameters = jsonParameters,
            streaming = false
        )
        
        val deferred = CompletableDeferred<RpcMessage>()
        pendingRequests[messageId] = deferred
        
        try {
            transport.send(request)
            
            val response = withTimeout(config.requestTimeout) {
                deferred.await()
            }
            
            return when (response) {
                is RpcResponse -> {
                    response.result?.let { json.decodeFromJsonElement(resultSerializer, it) }
                        ?: throw RpcException("Null result received")
                }
                is RpcError -> {
                    throw RpcException(response.errorMessage, errorCode = response.errorCode)
                }
                else -> throw RpcException("Unexpected response type: ${response::class.simpleName}")
            }
        } finally {
            pendingRequests.remove(messageId)
        }
    }
    
    /**
     * Make a streaming RPC call that returns a Flow.
     */
    fun <T> stream(
        serviceName: String,
        methodId: String,
        parameters: List<Any> = emptyList(),
        resultSerializer: KSerializer<T>
    ): Flow<T> = flow {
        val streamId = generateStreamId()
        val messageId = generateMessageId()
        
        val streamChannel = Channel<StreamMessage>(Channel.UNLIMITED)
        activeStreams[streamId] = streamChannel
        
        try {
            // Convert parameters to JsonElement
            val jsonParameters = parameters.map { param ->
                when (param) {
                    is String -> json.encodeToJsonElement(param)
                    is Int -> json.encodeToJsonElement(param)
                    is Double -> json.encodeToJsonElement(param)
                    is Boolean -> json.encodeToJsonElement(param)
                    is JsonElement -> param
                    else -> json.encodeToJsonElement(param.toString())
                }
            }
            
            val streamStart = StreamStart(
                messageId = messageId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                streamId = streamId,
                serviceName = serviceName,
                methodId = methodId,
                parameters = jsonParameters
            )
            
            transport.send(streamStart)
            
            for (message in streamChannel) {
                when (message) {
                    is StreamData -> {
                        val item = json.decodeFromJsonElement(resultSerializer, message.data)
                        emit(item)
                    }
                    is StreamEnd -> {
                        break
                    }
                    is StreamError -> {
                        throw StreamException(message.errorMessage, streamId = message.streamId)
                    }
                    else -> {
                        // Ignore other message types
                    }
                }
            }
        } finally {
            activeStreams.remove(streamId)
            streamChannel.close()
        }
    }
    
    private fun startMessageHandler() {
        GlobalScope.launch {
            transport.receive().collect { message ->
                handleMessage(message)
            }
        }
    }
    
    private suspend fun handleMessage(message: RpcMessage) {
        when (message) {
            is RpcResponse, is RpcError -> {
                pendingRequests[message.messageId]?.complete(message)
            }
            is StreamMessage -> {
                when (message) {
                    is StreamData, is StreamEnd, is StreamError -> {
                        val streamId = when (message) {
                            is StreamData -> message.streamId
                            is StreamEnd -> message.streamId
                            is StreamError -> message.streamId
                            else -> return
                        }
                        activeStreams[streamId]?.send(message)
                    }
                    else -> {
                        // Ignore other stream message types
                    }
                }
            }
            else -> {
                // Ignore other message types
            }
        }
    }
    
    private fun generateMessageId(): String {
        return "msg_${Random.nextLong()}"
    }
    
    private fun generateStreamId(): String {
        return "stream_${Random.nextLong()}"
    }
}

/**
 * Configuration for RPC client.
 */
data class RpcClientConfig(
    val requestTimeout: Long = 30_000,
    val maxConcurrentStreams: Int = 100
)



