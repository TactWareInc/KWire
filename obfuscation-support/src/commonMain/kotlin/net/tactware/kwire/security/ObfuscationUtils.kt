package net.tactware.kwire.security

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