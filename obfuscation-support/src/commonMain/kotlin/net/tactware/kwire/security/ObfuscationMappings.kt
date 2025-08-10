package net.tactware.kwire.security

import kotlinx.serialization.Serializable

/**
 * Represents obfuscation mappings for services and methods.
 */
@Serializable
data class ObfuscationMappings(
    val services: Map<String, Map<String, String>>
)