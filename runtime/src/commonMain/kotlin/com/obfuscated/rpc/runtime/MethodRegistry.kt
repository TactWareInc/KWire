package com.obfuscated.rpc.runtime

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Registry for mapping between obfuscated method IDs and actual method names.
 * This is crucial for obfuscation support.
 */
class MethodRegistry {
    private val methodIdToName = mutableMapOf<String, String>()
    private val methodNameToId = mutableMapOf<String, String>()
    private val serviceMethodMap = mutableMapOf<String, MutableMap<String, String>>()
    
    /**
     * Register a method mapping for a service.
     */
    fun registerMethod(serviceName: String, methodName: String, methodId: String? = null) {
        val actualMethodId = methodId ?: generateMethodId()
        
        val key = "$serviceName.$methodName"
        methodIdToName[actualMethodId] = key
        methodNameToId[key] = actualMethodId
        
        serviceMethodMap.getOrPut(serviceName) { mutableMapOf() }[methodName] = actualMethodId
    }
    
    /**
     * Get the method ID for a given service and method name.
     */
    fun getMethodId(serviceName: String, methodName: String): String? {
        return serviceMethodMap[serviceName]?.get(methodName)
    }
    
    /**
     * Get the method name for a given method ID.
     */
    fun getMethodName(methodId: String): String? {
        return methodIdToName[methodId]
    }
    
    /**
     * Get all method IDs for a service.
     */
    fun getServiceMethods(serviceName: String): Map<String, String> {
        return serviceMethodMap[serviceName] ?: emptyMap()
    }
    
    /**
     * Load method mappings from a serialized format.
     */
    fun loadMappings(mappings: MethodMappings) {
        mappings.services.forEach { (serviceName, methods) ->
            methods.forEach { (methodName, methodId) ->
                registerMethod(serviceName, methodName, methodId)
            }
        }
    }
    
    /**
     * Export current mappings to a serializable format.
     */
    fun exportMappings(): MethodMappings {
        return MethodMappings(serviceMethodMap.toMap())
    }
    
    /**
     * Generate a random method ID for obfuscation.
     */
    private fun generateMethodId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
    
    companion object {
        /**
         * Create a method registry from JSON string.
         */
        fun fromJson(json: String): MethodRegistry {
            val mappings = Json.decodeFromString<MethodMappings>(json)
            val registry = MethodRegistry()
            registry.loadMappings(mappings)
            return registry
        }
    }
}

/**
 * Serializable representation of method mappings.
 */
@Serializable
data class MethodMappings(
    val services: Map<String, Map<String, String>> = emptyMap()
)

/**
 * Builder for creating method registries with a fluent API.
 */
class MethodRegistryBuilder {
    private val registry = MethodRegistry()
    
    /**
     * Add a service with its methods.
     */
    fun service(serviceName: String, block: ServiceBuilder.() -> Unit): MethodRegistryBuilder {
        val serviceBuilder = ServiceBuilder(serviceName, registry)
        serviceBuilder.block()
        return this
    }
    
    /**
     * Build the final method registry.
     */
    fun build(): MethodRegistry = registry
    
    /**
     * Builder for service methods.
     */
    class ServiceBuilder(
        private val serviceName: String,
        private val registry: MethodRegistry
    ) {
        /**
         * Add a method to the service.
         */
        fun method(methodName: String, methodId: String? = null): ServiceBuilder {
            registry.registerMethod(serviceName, methodName, methodId)
            return this
        }
    }
}

/**
 * DSL function for creating method registries.
 */
fun methodRegistry(block: MethodRegistryBuilder.() -> Unit): MethodRegistry {
    return MethodRegistryBuilder().apply(block).build()
}

