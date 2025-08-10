@file:OptIn(ExperimentalTime::class)

package net.tactware.kwire.serialization

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Streaming support utilities for RPC Flow operations.
 */
object StreamingSupport {
    
    /**
     * Wrap a Flow with serialization support for RPC streaming.
     */
    fun <T> wrapFlowForRpc(
        flow: Flow<T>,
        serializer: KSerializer<T>,
        json: Json = Json.Default
    ): Flow<JsonElement> {
        return flow
            .map { item ->
                FlowSerializationUtils.serializeFlowItem(item, serializer, json)
            }
            .catch { error ->
                // Convert exceptions to serializable error objects
                throw RpcStreamingException("Stream error: ${error.message}", error)
            }
    }
    
    /**
     * Unwrap a Flow from RPC streaming with deserialization.
     */
    fun <T> unwrapFlowFromRpc(
        flow: Flow<JsonElement>,
        serializer: KSerializer<T>,
        json: Json = Json.Default
    ): Flow<T> {
        return flow
            .map { element ->
                FlowSerializationUtils.deserializeFlowItem(element, serializer, json)
            }
            .catch { error ->
                throw RpcStreamingException("Stream deserialization error: ${error.message}", error)
            }
    }
    
    /**
     * Create a Flow with backpressure handling for RPC streaming.
     */
    fun <T> createBackpressureAwareFlow(
        source: Flow<T>,
        bufferSize: Int = 64
    ): Flow<T> = flow {
        // Simple backpressure implementation
        // In a real implementation, this would use more sophisticated buffering
        source.collect { item ->
            emit(item)
        }
    }
    
    /**
     * Add error handling and recovery to a streaming Flow.
     */
    fun <T> addStreamErrorHandling(
        flow: Flow<T>,
        onError: (Throwable) -> T? = { null },
        maxRetries: Int = 3
    ): Flow<T> {
        var retryCount = 0
        
        return flow
            .catch { error ->
                if (retryCount < maxRetries) {
                    retryCount++
                    val recoveryValue = onError(error)
                    if (recoveryValue != null) {
                        emit(recoveryValue)
                    }
                } else {
                    throw RpcStreamingException("Stream failed after $maxRetries retries", error)
                }
            }
    }
    
    /**
     * Add lifecycle callbacks to a streaming Flow.
     */
    fun <T> addStreamLifecycle(
        flow: Flow<T>,
        onStart: suspend () -> Unit = {},
        onComplete: suspend () -> Unit = {},
        onError: suspend (Throwable) -> Unit = {}
    ): Flow<T> {
        return flow
            .onStart { onStart() }
            .onCompletion { error ->
                if (error != null) {
                    onError(error)
                } else {
                    onComplete()
                }
            }
    }
    
    /**
     * Create a Flow that emits items with metadata.
     */
    fun <T> createMetadataFlow(
        flow: Flow<T>,
        streamId: String
    ): Flow<StreamItem<T>> {
        var itemIndex = 0L
        
        return flow.map { item ->
            StreamItem(
                streamId = streamId,
                itemIndex = itemIndex++,
                data = item,
                timestamp = Clock.System.now().toEpochMilliseconds(),
            )
        }
    }
    
    /**
     * Extract data from a metadata Flow.
     */
    fun <T> extractDataFromMetadataFlow(
        flow: Flow<StreamItem<T>>
    ): Flow<T> {
        return flow.map { streamItem ->
            streamItem.data
        }
    }
}

/**
 * Represents a single item in a streaming RPC call with metadata.
 */
data class StreamItem<T>(
    val streamId: String,
    val itemIndex: Long,
    val data: T,
    val timestamp: Long
)

/**
 * Exception specific to RPC streaming operations.
 */
class RpcStreamingException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Configuration for streaming RPC operations.
 */
data class StreamingConfig(
    val bufferSize: Int = 64,
    val maxRetries: Int = 3,
    val timeoutMs: Long = 30_000,
    val enableBackpressure: Boolean = true,
    val enableErrorRecovery: Boolean = true
)

/**
 * Builder for creating configured streaming Flows.
 */
class StreamingFlowBuilder<T>(
    private val source: Flow<T>
) {
    private var config = StreamingConfig()
    private var serializer: KSerializer<T>? = null
    private var onStart: (suspend () -> Unit)? = null
    private var onComplete: (suspend () -> Unit)? = null
    private var onError: (suspend (Throwable) -> Unit)? = null
    
    fun withConfig(config: StreamingConfig): StreamingFlowBuilder<T> {
        this.config = config
        return this
    }
    
    fun withSerializer(serializer: KSerializer<T>): StreamingFlowBuilder<T> {
        this.serializer = serializer
        return this
    }
    
    fun onStart(callback: suspend () -> Unit): StreamingFlowBuilder<T> {
        this.onStart = callback
        return this
    }
    
    fun onComplete(callback: suspend () -> Unit): StreamingFlowBuilder<T> {
        this.onComplete = callback
        return this
    }
    
    fun onError(callback: suspend (Throwable) -> Unit): StreamingFlowBuilder<T> {
        this.onError = callback
        return this
    }
    
    fun build(): Flow<T> {
        var flow = source
        
        // Add backpressure if enabled
        if (config.enableBackpressure) {
            flow = StreamingSupport.createBackpressureAwareFlow(flow, config.bufferSize)
        }
        
        // Add error handling if enabled
        if (config.enableErrorRecovery) {
            flow = StreamingSupport.addStreamErrorHandling(flow, maxRetries = config.maxRetries)
        }
        
        // Add lifecycle callbacks
        flow = StreamingSupport.addStreamLifecycle(
            flow,
            onStart = onStart ?: {},
            onComplete = onComplete ?: {},
            onError = onError ?: {}
        )
        
        return flow
    }
}

/**
 * Extension function to create a streaming Flow builder.
 */
fun <T> Flow<T>.streaming(): StreamingFlowBuilder<T> {
    return StreamingFlowBuilder(this)
}



