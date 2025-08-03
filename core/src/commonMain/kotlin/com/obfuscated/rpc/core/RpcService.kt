package com.obfuscated.rpc.core

import kotlinx.serialization.Serializable

/**
 * Marker annotation for RPC service interfaces.
 * Classes annotated with this will have client and server stubs generated.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RpcService(
    /**
     * Service name used for registration and routing.
     * If empty, the class simple name will be used.
     */
    val name: String = "",
    
    /**
     * Whether to enable obfuscation for this service.
     */
    val obfuscated: Boolean = true
)

/**
 * Annotation for RPC methods within a service interface.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RpcMethod(
    /**
     * Custom method identifier for obfuscation.
     * If empty, one will be generated automatically.
     */
    val methodId: String = "",
    
    /**
     * Whether this method supports streaming (returns Flow).
     */
    val streaming: Boolean = false
)

/**
 * Base interface for all RPC services.
 * This provides common functionality and metadata.
 */
interface RpcServiceBase {
    /**
     * Service metadata containing information about methods and configuration.
     */
    val serviceMetadata: RpcServiceMetadata
}

/**
 * Metadata about an RPC service.
 */
@Serializable
data class RpcServiceMetadata(
    val serviceName: String,
    val methods: List<RpcMethodMetadata>,
    val obfuscated: Boolean = true
)

/**
 * Metadata about an RPC method.
 */
@Serializable
data class RpcMethodMetadata(
    val methodName: String,
    val methodId: String,
    val streaming: Boolean = false,
    val parameterTypes: List<String> = emptyList(),
    val returnType: String
)

