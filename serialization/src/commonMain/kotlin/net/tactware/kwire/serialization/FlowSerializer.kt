package net.tactware.kwire.serialization

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Serializer for Flow types in RPC communication.
 * This handles the serialization of Flow items for streaming RPC methods.
 */
class FlowSerializer<T>(
    private val elementSerializer: KSerializer<T>
) : KSerializer<Flow<T>> {
    
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Flow") {
        element("items", elementSerializer.descriptor)
    }
    
    override fun serialize(encoder: Encoder, value: Flow<T>) {
        throw SerializationException("Flow serialization is handled by the RPC transport layer")
    }
    
    override fun deserialize(decoder: Decoder): Flow<T> {
        throw SerializationException("Flow deserialization is handled by the RPC transport layer")
    }
}

/**
 * Utility functions for Flow serialization in RPC context.
 */
object FlowSerializationUtils {
    
    /**
     * Serialize a Flow item to JsonElement.
     */
    fun <T> serializeFlowItem(
        item: T,
        serializer: KSerializer<T>,
        json: Json = Json.Default
    ): JsonElement {
        return json.encodeToJsonElement(serializer, item)
    }
    
    /**
     * Deserialize a JsonElement to a Flow item.
     */
    fun <T> deserializeFlowItem(
        element: JsonElement,
        serializer: KSerializer<T>,
        json: Json = Json.Default
    ): T {
        return json.decodeFromJsonElement(serializer, element)
    }
    
    /**
     * Create a Flow from a sequence of JsonElements.
     */
    fun <T> createFlowFromJsonElements(
        elements: Sequence<JsonElement>,
        serializer: KSerializer<T>,
        json: Json = Json.Default
    ): Flow<T> = flow {
        elements.forEach { element ->
            val item = deserializeFlowItem(element, serializer, json)
            emit(item)
        }
    }
    
    /**
     * Convert a Flow to a sequence of JsonElements.
     */
    suspend fun <T> flowToJsonElements(
        flow: Flow<T>,
        serializer: KSerializer<T>,
        json: Json = Json.Default
    ): Flow<JsonElement> {
        return flow.map { item ->
            serializeFlowItem(item, serializer, json)
        }
    }
}

/**
 * Extension functions for Flow serialization.
 */

/**
 * Serialize each item in the Flow using the provided serializer.
 */
fun <T> Flow<T>.serializeItems(
    serializer: KSerializer<T>,
    json: Json = Json.Default
): Flow<JsonElement> {
    return map { item ->
        FlowSerializationUtils.serializeFlowItem(item, serializer, json)
    }
}

/**
 * Deserialize each JsonElement in the Flow using the provided serializer.
 */
fun <T> Flow<JsonElement>.deserializeItems(
    serializer: KSerializer<T>,
    json: Json = Json.Default
): Flow<T> {
    return map { element ->
        FlowSerializationUtils.deserializeFlowItem(element, serializer, json)
    }
}

