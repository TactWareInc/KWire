package net.tactware.kwire.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Central serialization manager for RPC operations.
 * Handles serialization of method parameters, return values, and streaming data.
 */
class RpcSerializer(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        classDiscriminator = "_type"
    }
) {
    
    /**
     * Serialize a value using a specific serializer.
     */
    fun <T> serialize(value: T, serializer: KSerializer<T>): JsonElement {
        return json.encodeToJsonElement(serializer, value)
    }
    
    /**
     * Deserialize a JsonElement using a specific serializer.
     */
    fun <T> deserialize(element: JsonElement, serializer: KSerializer<T>): T {
        return json.decodeFromJsonElement(serializer, element)
    }
    
    /**
     * Serialize method parameters to a list of JsonElements.
     */
    fun serializeParameters(parameters: List<JsonElement>): List<JsonElement> {
        return parameters // Already serialized
    }
    
    /**
     * Deserialize method parameters from JsonElements.
     */
    fun deserializeParameters(elements: List<JsonElement>): List<JsonElement> {
        return elements // Return as JsonElements for further processing
    }
    
    /**
     * Check if a serializer is valid.
     */
    fun isSerializerValid(serializer: KSerializer<*>): Boolean {
        return try {
            // Basic validation
            serializer.descriptor
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Default RPC serializer instance.
 */
val DefaultRpcSerializer = RpcSerializer()

/**
 * Exception thrown when serialization operations fail in RPC context.
 */
class RpcSerializationException(
    message: String,
    cause: Throwable? = null
) : SerializationException(message, cause)

/**
 * Utility functions for common serialization operations.
 */
object RpcSerializationUtils {
    
    /**
     * Create a serializer configuration optimized for RPC operations.
     */
    fun createRpcJsonConfig(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        classDiscriminator = "_type"
        prettyPrint = false // Optimize for network transfer
        coerceInputValues = true // Handle type mismatches gracefully
    }
    
    /**
     * Validate that serializers are available.
     */
    fun validateSerializers(
        serializers: List<KSerializer<*>>,
        rpcSerializer: RpcSerializer = DefaultRpcSerializer
    ): Boolean {
        return serializers.all { rpcSerializer.isSerializerValid(it) }
    }
}

