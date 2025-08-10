package net.tactware.kwire.security

import kotlinx.serialization.Serializable

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
