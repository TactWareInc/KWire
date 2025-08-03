package com.obfuscated.rpc.obfuscation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Central manager for RPC obfuscation operations.
 * Handles method name mapping, service obfuscation, and runtime resolution.
 */
class ObfuscationManager(
    private val config: ObfuscationConfig = ObfuscationConfig()
) {
    private val methodMappings = mutableMapOf<String, String>()
    private val reverseMappings = mutableMapOf<String, String>()
    private val serviceMappings = mutableMapOf<String, String>()
    private val reverseServiceMappings = mutableMapOf<String, String>()
    
    /**
     * Load method mappings from a JSON string.
     */
    fun loadMappings(mappingsJson: String) {
        try {
            val mappings = Json.decodeFromString<ObfuscationMappings>(mappingsJson)
            
            mappings.services.forEach { (serviceName, methods) ->
                val obfuscatedServiceName = if (config.obfuscateServiceNames) {
                    generateObfuscatedName(serviceName, config.serviceNameLength)
                } else {
                    serviceName
                }
                
                serviceMappings[serviceName] = obfuscatedServiceName
                reverseServiceMappings[obfuscatedServiceName] = serviceName
                
                methods.forEach { (methodName, obfuscatedName) ->
                    val fullMethodKey = "$serviceName.$methodName"
                    val fullObfuscatedKey = "$obfuscatedServiceName.$obfuscatedName"
                    
                    methodMappings[fullMethodKey] = obfuscatedName
                    reverseMappings[obfuscatedName] = fullMethodKey
                }
            }
        } catch (e: Exception) {
            throw ObfuscationException("Failed to load obfuscation mappings", e)
        }
    }
    
    /**
     * Generate obfuscation mappings for a list of services.
     */
    fun generateMappings(services: List<ServiceInfo>): ObfuscationMappings {
        val mappings = mutableMapOf<String, Map<String, String>>()
        
        services.forEach { service ->
            val methodMap = mutableMapOf<String, String>()
            
            service.methods.forEach { method ->
                val obfuscatedName = generateObfuscatedName(method, config.methodNameLength)
                methodMap[method] = obfuscatedName
                
                val fullMethodKey = "${service.name}.$method"
                methodMappings[fullMethodKey] = obfuscatedName
                reverseMappings[obfuscatedName] = fullMethodKey
            }
            
            mappings[service.name] = methodMap
            
            if (config.obfuscateServiceNames) {
                val obfuscatedServiceName = generateObfuscatedName(service.name, config.serviceNameLength)
                serviceMappings[service.name] = obfuscatedServiceName
                reverseServiceMappings[obfuscatedServiceName] = service.name
            }
        }
        
        return ObfuscationMappings(mappings)
    }
    
    /**
     * Get the obfuscated method name for a service method.
     */
    fun getObfuscatedMethodName(serviceName: String, methodName: String): String {
        val fullKey = "$serviceName.$methodName"
        return methodMappings[fullKey] ?: methodName
    }
    
    /**
     * Get the original method name from an obfuscated name.
     */
    fun getOriginalMethodName(obfuscatedName: String): Pair<String, String>? {
        val fullKey = reverseMappings[obfuscatedName] ?: return null
        val parts = fullKey.split(".", limit = 2)
        return if (parts.size == 2) {
            Pair(parts[0], parts[1])
        } else {
            null
        }
    }
    
    /**
     * Get the obfuscated service name.
     */
    fun getObfuscatedServiceName(serviceName: String): String {
        return serviceMappings[serviceName] ?: serviceName
    }
    
    /**
     * Get the original service name from an obfuscated name.
     */
    fun getOriginalServiceName(obfuscatedName: String): String {
        return reverseServiceMappings[obfuscatedName] ?: obfuscatedName
    }
    
    /**
     * Check if obfuscation is enabled.
     */
    fun isObfuscationEnabled(): Boolean = config.enabled
    
    /**
     * Export current mappings to JSON.
     */
    fun exportMappings(): String {
        val mappings = mutableMapOf<String, Map<String, String>>()
        
        serviceMappings.keys.forEach { serviceName ->
            val methodMap = mutableMapOf<String, String>()
            methodMappings.entries
                .filter { it.key.startsWith("$serviceName.") }
                .forEach { (fullKey, obfuscatedName) ->
                    val methodName = fullKey.removePrefix("$serviceName.")
                    methodMap[methodName] = obfuscatedName
                }
            mappings[serviceName] = methodMap
        }
        
        return Json.encodeToString(ObfuscationMappings(mappings))
    }
    
    /**
     * Generate an obfuscated name using the configured strategy.
     */
    private fun generateObfuscatedName(originalName: String, length: Int): String {
        return when (config.strategy) {
            ObfuscationStrategy.RANDOM -> generateRandomName(length)
            ObfuscationStrategy.HASH_BASED -> generateHashBasedName(originalName, length)
            ObfuscationStrategy.SEQUENTIAL -> generateSequentialName(originalName)
        }
    }
    
    private fun generateRandomName(length: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }
    
    private fun generateHashBasedName(originalName: String, length: Int): String {
        val positiveHash = kotlin.math.abs(originalName.hashCode()).toString(36)
        return if (positiveHash.length >= length) {
            positiveHash.take(length)
        } else {
            positiveHash + generateRandomName(length - positiveHash.length)
        }
    }
    
    private var sequentialCounter = 0
    private fun generateSequentialName(originalName: String): String {
        return "${config.prefix}${sequentialCounter++}"
    }
}

/**
 * Configuration for obfuscation behavior.
 */
@Serializable
data class ObfuscationConfig(
    val enabled: Boolean = true,
    val strategy: ObfuscationStrategy = ObfuscationStrategy.HASH_BASED,
    val methodNameLength: Int = 8,
    val serviceNameLength: Int = 6,
    val obfuscateServiceNames: Boolean = true,
    val prefix: String = "m"
)

/**
 * Obfuscation strategies available.
 */
@Serializable
enum class ObfuscationStrategy {
    RANDOM,      // Generate completely random names
    HASH_BASED,  // Use hash of original name as base
    SEQUENTIAL   // Use sequential numbering
}

/**
 * Represents obfuscation mappings for services and methods.
 */
@Serializable
data class ObfuscationMappings(
    val services: Map<String, Map<String, String>>
)

/**
 * Information about a service for obfuscation.
 */
data class ServiceInfo(
    val name: String,
    val methods: List<String>
)

/**
 * Exception thrown during obfuscation operations.
 */
class ObfuscationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Utility functions for obfuscation.
 */
object ObfuscationUtils {
    
    /**
     * Create a default obfuscation manager with standard configuration.
     */
    fun createDefaultManager(): ObfuscationManager {
        return ObfuscationManager(
            ObfuscationConfig(
                enabled = true,
                strategy = ObfuscationStrategy.HASH_BASED,
                methodNameLength = 8,
                serviceNameLength = 6
            )
        )
    }
    
    /**
     * Create an obfuscation manager with custom configuration.
     */
    fun createManager(config: ObfuscationConfig): ObfuscationManager {
        return ObfuscationManager(config)
    }
    
    /**
     * Validate obfuscation mappings.
     */
    fun validateMappings(mappings: ObfuscationMappings): Boolean {
        return try {
            // Check for duplicate obfuscated names
            val allObfuscatedNames = mutableSetOf<String>()
            mappings.services.values.forEach { methods ->
                methods.values.forEach { obfuscatedName ->
                    if (obfuscatedName in allObfuscatedNames) {
                        return false
                    }
                    allObfuscatedNames.add(obfuscatedName)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

