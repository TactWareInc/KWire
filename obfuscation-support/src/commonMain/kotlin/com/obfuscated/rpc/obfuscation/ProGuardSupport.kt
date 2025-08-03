package com.obfuscated.rpc.obfuscation

/**
 * ProGuard integration support for RPC obfuscation.
 * Provides utilities to work with ProGuard obfuscation and maintain RPC functionality.
 */
object ProGuardSupport {
    
    /**
     * Generate ProGuard keep rules for RPC classes.
     */
    fun generateKeepRules(services: List<ServiceInfo>): String {
        val rules = StringBuilder()
        
        // Keep RPC service interfaces
        rules.appendLine("# Keep RPC service interfaces")
        services.forEach { service ->
            rules.appendLine("-keep interface **${service.name} { *; }")
        }
        
        // Keep RPC annotations
        rules.appendLine("\n# Keep RPC annotations")
        rules.appendLine("-keep @interface com.obfuscated.rpc.core.RpcService")
        rules.appendLine("-keep @interface com.obfuscated.rpc.core.RpcMethod")
        rules.appendLine("-keep @interface com.obfuscated.rpc.serialization.RpcSerializable")
        
        // Keep serialization classes
        rules.appendLine("\n# Keep kotlinx.serialization classes")
        rules.appendLine("-keep @kotlinx.serialization.Serializable class * { *; }")
        rules.appendLine("-keep class kotlinx.serialization.** { *; }")
        
        // Keep RPC runtime classes
        rules.appendLine("\n# Keep RPC runtime classes")
        rules.appendLine("-keep class com.obfuscated.rpc.runtime.** { *; }")
        rules.appendLine("-keep class com.obfuscated.rpc.core.** { *; }")
        
        // Keep generated client and server stubs
        rules.appendLine("\n# Keep generated RPC stubs")
        services.forEach { service ->
            rules.appendLine("-keep class **${service.name}Client { *; }")
            rules.appendLine("-keep class **${service.name}Server { *; }")
            rules.appendLine("-keep class **${service.name}ServerHandler { *; }")
        }
        
        return rules.toString()
    }
    
    /**
     * Generate ProGuard mapping file compatible format.
     */
    fun generateMappingFile(obfuscationMappings: ObfuscationMappings): String {
        val mappingLines = mutableListOf<String>()
        
        obfuscationMappings.services.forEach { (serviceName, methods) ->
            mappingLines.add("# Service: $serviceName")
            methods.forEach { (originalMethod, obfuscatedMethod) ->
                mappingLines.add("$serviceName.$originalMethod -> $obfuscatedMethod")
            }
            mappingLines.add("")
        }
        
        return mappingLines.joinToString("\n")
    }
    
    /**
     * Parse ProGuard mapping file to extract method mappings.
     */
    fun parseMappingFile(mappingContent: String): Map<String, String> {
        val mappings = mutableMapOf<String, String>()
        
        mappingContent.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains(" -> ")) {
                val parts = trimmed.split(" -> ")
                if (parts.size == 2) {
                    mappings[parts[0].trim()] = parts[1].trim()
                }
            }
        }
        
        return mappings
    }
    
    /**
     * Create obfuscation-aware method resolver.
     */
    fun createMethodResolver(
        obfuscationManager: ObfuscationManager,
        proguardMappings: Map<String, String> = emptyMap()
    ): MethodResolver {
        return MethodResolver(obfuscationManager, proguardMappings)
    }
    
    /**
     * Generate configuration for obfuscation-friendly RPC setup.
     */
    fun generateObfuscationConfig(
        enableProGuard: Boolean = true,
        enableCustomObfuscation: Boolean = true,
        preserveDebugInfo: Boolean = false
    ): ObfuscationIntegrationConfig {
        return ObfuscationIntegrationConfig(
            enableProGuard = enableProGuard,
            enableCustomObfuscation = enableCustomObfuscation,
            preserveDebugInfo = preserveDebugInfo
        )
    }
}

/**
 * Method resolver that handles both custom obfuscation and ProGuard obfuscation.
 */
class MethodResolver(
    private val obfuscationManager: ObfuscationManager,
    private val proguardMappings: Map<String, String> = emptyMap()
) {
    
    /**
     * Resolve a method call to its original service and method name.
     */
    fun resolveMethod(obfuscatedMethodId: String, serviceName: String): ResolvedMethod? {
        // First try custom obfuscation resolution
        val customResolution = obfuscationManager.getOriginalMethodName(obfuscatedMethodId)
        if (customResolution != null) {
            return ResolvedMethod(
                originalServiceName = customResolution.first,
                originalMethodName = customResolution.second,
                resolutionType = ResolutionType.CUSTOM_OBFUSCATION
            )
        }
        
        // Then try ProGuard mapping resolution
        val proguardKey = "$serviceName.$obfuscatedMethodId"
        val proguardResolution = proguardMappings[proguardKey]
        if (proguardResolution != null) {
            val parts = proguardResolution.split(".", limit = 2)
            if (parts.size == 2) {
                return ResolvedMethod(
                    originalServiceName = parts[0],
                    originalMethodName = parts[1],
                    resolutionType = ResolutionType.PROGUARD_MAPPING
                )
            }
        }
        
        // Fallback to no obfuscation
        return ResolvedMethod(
            originalServiceName = serviceName,
            originalMethodName = obfuscatedMethodId,
            resolutionType = ResolutionType.NO_OBFUSCATION
        )
    }
    
    /**
     * Get the obfuscated method ID for a service method.
     */
    fun getObfuscatedMethodId(serviceName: String, methodName: String): String {
        return obfuscationManager.getObfuscatedMethodName(serviceName, methodName)
    }
}

/**
 * Resolved method information.
 */
data class ResolvedMethod(
    val originalServiceName: String,
    val originalMethodName: String,
    val resolutionType: ResolutionType
)

/**
 * Method resolution types.
 */
enum class ResolutionType {
    CUSTOM_OBFUSCATION,
    PROGUARD_MAPPING,
    NO_OBFUSCATION
}

/**
 * Configuration for obfuscation integration.
 */
data class ObfuscationIntegrationConfig(
    val enableProGuard: Boolean = true,
    val enableCustomObfuscation: Boolean = true,
    val preserveDebugInfo: Boolean = false,
    val generateKeepRules: Boolean = true,
    val generateMappingFile: Boolean = true
)

/**
 * Utility functions for ProGuard integration.
 */
object ProGuardUtils {
    
    /**
     * Check if a class name appears to be obfuscated by ProGuard.
     */
    fun isObfuscatedClassName(className: String): Boolean {
        // ProGuard typically generates short, non-descriptive names
        val simpleName = className.substringAfterLast('.')
        return simpleName.length <= 2 && simpleName.all { it.isLetter() }
    }
    
    /**
     * Check if a method name appears to be obfuscated by ProGuard.
     */
    fun isObfuscatedMethodName(methodName: String): Boolean {
        // ProGuard typically generates short, single-letter method names
        return methodName.length <= 2 && methodName.all { it.isLetter() }
    }
    
    /**
     * Generate a ProGuard configuration template.
     */
    fun generateProGuardTemplate(): String {
        return """
            # ProGuard configuration for Obfuscated RPC
            
            # Keep RPC framework classes
            -keep class com.obfuscated.rpc.** { *; }
            
            # Keep kotlinx.serialization
            -keep @kotlinx.serialization.Serializable class * { *; }
            -keep class kotlinx.serialization.** { *; }
            
            # Keep Kotlin coroutines
            -keep class kotlinx.coroutines.** { *; }
            
            # Keep Ktor classes
            -keep class io.ktor.** { *; }
            
            # Obfuscation settings
            -obfuscationdictionary obfuscation-dictionary.txt
            -classobfuscationdictionary obfuscation-dictionary.txt
            -packageobfuscationdictionary obfuscation-dictionary.txt
            
            # Optimization settings
            -optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
            -optimizationpasses 5
            -allowaccessmodification
            
            # Keep line numbers for debugging (optional)
            # -keepattributes SourceFile,LineNumberTable
        """.trimIndent()
    }
}

