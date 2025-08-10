package net.tactware.kwire.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

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
