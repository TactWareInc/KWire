@file:OptIn(ExperimentalTime::class)

package net.tactware.kwire.runtime

import com.obfuscated.rpc.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * RPC server for handling remote procedure calls.
 */
class RpcServer(
    private val transport: RpcTransport,
    private val json: Json = Json.Default,
    private val config: RpcServerConfig = RpcServerConfig()
) {
    private val serviceRegistry = mutableMapOf<String, ServiceHandler>()
    private val activeStreams = mutableMapOf<String, Job>()
    private var isStarted = false
    
    /**
     * Register a service implementation.
     */
    fun <T : Any> registerService(
        serviceClass: KClass<T>,
        serviceInstance: T,
        metadata: RpcServiceMetadata
    ) {
        val handler = ServiceHandler(serviceInstance, metadata)
        serviceRegistry[metadata.serviceName] = handler
    }
    
    /**
     * Start the RPC server.
     */
    suspend fun start() {
        if (isStarted) return
        
        transport.connect()
        startMessageHandler()
        isStarted = true
    }
    
    /**
     * Stop the RPC server.
     */
    suspend fun stop() {
        if (!isStarted) return
        
        // Cancel all active streams
        activeStreams.values.forEach { it.cancel() }
        activeStreams.clear()
        
        transport.disconnect()
        isStarted = false
    }
    
    private fun startMessageHandler() {
        GlobalScope.launch {
            transport.receive().collect { message ->
                handleMessage(message)
            }
        }
    }
    
    private suspend fun handleMessage(message: RpcMessage) {
        try {
            when (message) {
                is RpcRequest -> handleRequest(message)
                is StreamStart -> handleStreamStart(message)
                else -> {
                    // Ignore other message types
                }
            }
        } catch (e: Exception) {
            val errorResponse = RpcError(
                messageId = message.messageId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                errorCode = RpcErrorCodes.INTERNAL_ERROR,
                errorMessage = e.message ?: "Unknown error"
            )
            transport.send(errorResponse)
        }
    }
    
    private suspend fun handleRequest(request: RpcRequest) {
        val serviceHandler = serviceRegistry[request.serviceName]
            ?: throw ServiceNotFoundException(request.serviceName)
        
        val methodMetadata = serviceHandler.metadata.methods.find { it.methodId == request.methodId }
            ?: throw MethodNotFoundException(request.serviceName, request.methodId)
        
        try {
            val result = serviceHandler.invoke(request.methodId, request.parameters)
            
            val response = RpcResponse(
                messageId = request.messageId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                result = result?.let { json.encodeToJsonElement(it) },
                streaming = false
            )
            
            transport.send(response)
        } catch (e: Exception) {
            val errorResponse = RpcError(
                messageId = request.messageId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                errorCode = when (e) {
                    is RpcException -> e.errorCode
                    else -> RpcErrorCodes.INTERNAL_ERROR
                },
                errorMessage = e.message ?: "Unknown error"
            )
            transport.send(errorResponse)
        }
    }
    
    private suspend fun handleStreamStart(streamStart: StreamStart) {
        val serviceHandler = serviceRegistry[streamStart.serviceName]
            ?: throw ServiceNotFoundException(streamStart.serviceName)
        
        val methodMetadata = serviceHandler.metadata.methods.find { it.methodId == streamStart.methodId }
            ?: throw MethodNotFoundException(streamStart.serviceName, streamStart.methodId)
        
        if (!methodMetadata.streaming) {
            throw RpcException("Method ${streamStart.methodId} is not a streaming method")
        }
        
        val streamJob = MainScope().launch(Dispatchers.Default) {
            try {
                val resultFlow = serviceHandler.invokeStream(streamStart.methodId, streamStart.parameters)
                
                resultFlow.collect { item ->
                    val streamData = StreamData(
                        messageId = generateMessageId(),
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                        streamId = streamStart.streamId,
                        data = json.encodeToJsonElement(item)
                    )
                    transport.send(streamData)
                }
                
                val streamEnd = StreamEnd(
                    messageId = generateMessageId(),
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    streamId = streamStart.streamId
                )
                transport.send(streamEnd)
                
            } catch (e: Exception) {
                val streamError = StreamError(
                    messageId = generateMessageId(),
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    streamId = streamStart.streamId,
                    errorCode = when (e) {
                        is RpcException -> e.errorCode
                        else -> RpcErrorCodes.STREAM_ERROR
                    },
                    errorMessage = e.message ?: "Stream error"
                )
                transport.send(streamError)
            } finally {
                activeStreams.remove(streamStart.streamId)
            }
        }
        
        activeStreams[streamStart.streamId] = streamJob
    }
    
    private fun generateMessageId(): String {
        return "msg_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
    
    /**
     * Handler for a registered service.
     */
    private class ServiceHandler(
        private val serviceInstance: Any,
        val metadata: RpcServiceMetadata
    ) {
        /**
         * Invoke a regular (non-streaming) method.
         */
        suspend fun invoke(methodId: String, parameters: List<JsonElement>): Any? {
            // This would use reflection or generated code to invoke the actual method
            // For now, this is a placeholder that would be implemented by the code generator
            throw NotImplementedError("Method invocation not implemented - requires code generation")
        }
        
        /**
         * Invoke a streaming method that returns a Flow.
         */
        suspend fun invokeStream(methodId: String, parameters: List<JsonElement>): Flow<Any> {
            // This would use reflection or generated code to invoke the actual streaming method
            // For now, this is a placeholder that would be implemented by the code generator
            throw NotImplementedError("Stream method invocation not implemented - requires code generation")
        }
    }
}

/**
 * Configuration for RPC server.
 */
data class RpcServerConfig(
    val maxConcurrentRequests: Int = 1000,
    val maxConcurrentStreams: Int = 100,
    val requestTimeout: Long = 60_000
)

