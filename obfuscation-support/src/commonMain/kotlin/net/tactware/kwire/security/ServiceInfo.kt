package net.tactware.kwire.security

/**
 * Information about a service for obfuscation.
 */
data class ServiceInfo(
    val name: String,
    val methods: List<String>
)