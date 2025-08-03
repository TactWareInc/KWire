package com.obfuscated.rpc.serialization

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow

/**
 * Serialization module for RPC operations.
 * Provides centralized configuration for kotlinx.serialization.
 */
object RpcSerializationModule {
    
    /**
     * Create a SerializersModule configured for RPC operations.
     */
    fun createModule(): SerializersModule = SerializersModule {
        // Add contextual serializers for common types
        contextual(FlowContextualSerializer)
        
        // Add polymorphic serializers if needed
        // polymorphic(Any::class) { ... }
    }
    
    /**
     * Create a Json configuration optimized for RPC.
     */
    fun createJsonConfig(
        prettyPrint: Boolean = false,
        ignoreUnknownKeys: Boolean = true,
        encodeDefaults: Boolean = true
    ): Json = Json {
        this.prettyPrint = prettyPrint
        this.ignoreUnknownKeys = ignoreUnknownKeys
        this.encodeDefaults = encodeDefaults
        this.isLenient = true
        this.coerceInputValues = true
        this.classDiscriminator = "_type"
        this.serializersModule = createModule()
    }
    
    /**
     * Default Json instance for RPC operations.
     */
    val defaultJson: Json = createJsonConfig()
}

/**
 * Contextual serializer for Flow types.
 * This is used when Flow appears as a contextual type in serialization.
 */
object FlowContextualSerializer : kotlinx.serialization.KSerializer<Flow<*>> {
    override val descriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("Flow")
    
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Flow<*>) {
        throw kotlinx.serialization.SerializationException("Flow serialization should be handled by RPC transport")
    }
    
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Flow<*> {
        throw kotlinx.serialization.SerializationException("Flow deserialization should be handled by RPC transport")
    }
}

/**
 * Extension functions for easy serialization configuration.
 */

/**
 * Create an RpcSerializer with custom Json configuration.
 */
fun createRpcSerializer(
    prettyPrint: Boolean = false,
    ignoreUnknownKeys: Boolean = true,
    encodeDefaults: Boolean = true
): RpcSerializer {
    val json = RpcSerializationModule.createJsonConfig(
        prettyPrint = prettyPrint,
        ignoreUnknownKeys = ignoreUnknownKeys,
        encodeDefaults = encodeDefaults
    )
    return RpcSerializer(json)
}

/**
 * Get the default RPC serializer.
 */
fun getDefaultRpcSerializer(): RpcSerializer {
    return RpcSerializer(RpcSerializationModule.defaultJson)
}

/**
 * Annotation for marking classes as RPC serializable.
 * This can be used by the code generator to identify serializable types.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RpcSerializable

/**
 * Annotation for customizing serialization behavior of RPC methods.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RpcSerialization(
    /**
     * Whether to use custom serialization for this method.
     */
    val custom: Boolean = false,
    
    /**
     * Custom serializer class name (if custom = true).
     */
    val serializerClass: String = "",
    
    /**
     * Whether to enable compression for large payloads.
     */
    val compressed: Boolean = false
)

