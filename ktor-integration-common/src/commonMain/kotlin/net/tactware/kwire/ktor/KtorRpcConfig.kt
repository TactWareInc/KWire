package net.tactware.kwire.ktor

import net.tactware.kwire.security.ObfuscationManager
import net.tactware.kwire.security.SecurityManager

/**
 * Configuration for Ktor RPC transport.
 */
data class KtorRpcConfig(
    val baseUrl: String = "http://localhost:8080",
    val timeout: Long = 30000,
    val enableSecurity: Boolean = true,
    val enableObfuscation: Boolean = true,
    val obfuscationManager: ObfuscationManager? = null,
    val securityManager: SecurityManager? = null,
)
