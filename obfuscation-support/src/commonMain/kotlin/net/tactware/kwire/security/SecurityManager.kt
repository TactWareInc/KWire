@file:OptIn(ExperimentalTime::class)

package net.tactware.kwire.security

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Security manager for RPC operations.
 * Handles authentication, authorization, and security policies.
 */
class SecurityManager(
    private val config: SecurityConfig = SecurityConfig()
) {
    private val authenticatedSessions = mutableMapOf<String, SessionInfo>()
    private val accessPolicies = mutableMapOf<String, AccessPolicy>()
    private val rateLimiters = mutableMapOf<String, RateLimiter>()
    
    /**
     * Authenticate a client and create a session.
     */
    fun authenticate(credentials: AuthCredentials): AuthResult {
        return try {
            when (config.authMethod) {
                AuthMethod.TOKEN -> authenticateWithToken(credentials)
                AuthMethod.API_KEY -> authenticateWithApiKey(credentials)
                AuthMethod.CUSTOM -> authenticateCustom(credentials)
                AuthMethod.NONE -> AuthResult.Success(generateSessionToken())
            }
        } catch (e: Exception) {
            AuthResult.Failure("Authentication failed: ${e.message}")
        }
    }
    
    /**
     * Validate a session token.
     */
    fun validateSession(sessionToken: String): SessionValidationResult {
        val session = authenticatedSessions[sessionToken]
            ?: return SessionValidationResult.Invalid("Session not found")
        
        if (session.isExpired()) {
            authenticatedSessions.remove(sessionToken)
            return SessionValidationResult.Invalid("Session expired")
        }
        
        return SessionValidationResult.Valid(session)
    }
    
    /**
     * Check if a client is authorized to call a specific method.
     */
    fun authorize(sessionToken: String, serviceName: String, methodName: String): AuthorizationResult {
        val sessionResult = validateSession(sessionToken)
        if (sessionResult !is SessionValidationResult.Valid) {
            return AuthorizationResult.Denied("Invalid session")
        }
        
        val session = sessionResult.session
        val policyKey = "$serviceName.$methodName"
        val policy = accessPolicies[policyKey] ?: accessPolicies["*"] ?: AccessPolicy.ALLOW_ALL
        
        return when {
            policy.allowedRoles.isEmpty() -> AuthorizationResult.Allowed
            session.roles.any { it in policy.allowedRoles } -> AuthorizationResult.Allowed
            else -> AuthorizationResult.Denied("Insufficient permissions")
        }
    }
    
    /**
     * Check rate limiting for a client.
     */
    fun checkRateLimit(clientId: String, serviceName: String, methodName: String): RateLimitResult {
        if (!config.enableRateLimit) {
            return RateLimitResult.Allowed
        }
        
        val rateLimiterKey = "$clientId:$serviceName.$methodName"
        val rateLimiter = rateLimiters.getOrPut(rateLimiterKey) {
            RateLimiter(config.rateLimitConfig)
        }
        
        return if (rateLimiter.allowRequest()) {
            RateLimitResult.Allowed
        } else {
            RateLimitResult.Exceeded(rateLimiter.getResetTime())
        }
    }
    
    /**
     * Add an access policy for a service method.
     */
    fun addAccessPolicy(serviceName: String, methodName: String, policy: AccessPolicy) {
        val key = "$serviceName.$methodName"
        accessPolicies[key] = policy
    }
    
    /**
     * Remove a session (logout).
     */
    fun removeSession(sessionToken: String) {
        authenticatedSessions.remove(sessionToken)
    }
    
    /**
     * Clean up expired sessions.
     */
    fun cleanupExpiredSessions() {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        authenticatedSessions.entries.removeAll { (_, session) ->
            session.expiresAt < currentTime
        }
    }
    
    private fun authenticateWithToken(credentials: AuthCredentials): AuthResult {
        // In a real implementation, this would validate against a token service
        val token = credentials.token ?: return AuthResult.Failure("Token required")
        
        if (token.length < 10) {
            return AuthResult.Failure("Invalid token format")
        }
        
        val sessionToken = generateSessionToken()
        val session = SessionInfo(
            sessionToken = sessionToken,
            clientId = credentials.clientId ?: "unknown",
            roles = listOf("user"), // Would be determined by token validation
            createdAt = Clock.System.now().toEpochMilliseconds(),
            expiresAt = Clock.System.now().toEpochMilliseconds() + config.sessionTimeoutMs
        )
        
        authenticatedSessions[sessionToken] = session
        return AuthResult.Success(sessionToken)
    }
    
    private fun authenticateWithApiKey(credentials: AuthCredentials): AuthResult {
        val apiKey = credentials.apiKey ?: return AuthResult.Failure("API key required")
        
        // In a real implementation, this would validate against a database
        if (apiKey.startsWith("ak_") && apiKey.length > 10) {
            val sessionToken = generateSessionToken()
            val session = SessionInfo(
                sessionToken = sessionToken,
                clientId = credentials.clientId ?: "api_client",
                roles = listOf("api_user"),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                expiresAt = Clock.System.now().toEpochMilliseconds() + config.sessionTimeoutMs
            )
            
            authenticatedSessions[sessionToken] = session
            return AuthResult.Success(sessionToken)
        }
        
        return AuthResult.Failure("Invalid API key")
    }
    
    private fun authenticateCustom(credentials: AuthCredentials): AuthResult {
        // Placeholder for custom authentication logic
        return AuthResult.Failure("Custom authentication not implemented")
    }
    
    private fun generateSessionToken(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return "sess_" + (1..32).map { chars.random() }.joinToString("")
    }
}

/**
 * Security configuration.
 */
@Serializable
data class SecurityConfig(
    val authMethod: AuthMethod = AuthMethod.TOKEN,
    val sessionTimeoutMs: Long = 3600000, // 1 hour
    val enableRateLimit: Boolean = true,
    val rateLimitConfig: RateLimitConfig = RateLimitConfig(),
    val enableEncryption: Boolean = false,
    val encryptionKey: String? = null
)

/**
 * Authentication methods.
 */
@Serializable
enum class AuthMethod {
    NONE,
    TOKEN,
    API_KEY,
    CUSTOM
}

/**
 * Authentication credentials.
 */
@Serializable
data class AuthCredentials(
    val clientId: String? = null,
    val token: String? = null,
    val apiKey: String? = null,
    val customData: Map<String, String> = emptyMap()
)

/**
 * Authentication result.
 */
sealed class AuthResult {
    data class Success(val sessionToken: String) : AuthResult()
    data class Failure(val reason: String) : AuthResult()
}

/**
 * Session validation result.
 */
sealed class SessionValidationResult {
    data class Valid(val session: SessionInfo) : SessionValidationResult()
    data class Invalid(val reason: String) : SessionValidationResult()
}

/**
 * Authorization result.
 */
sealed class AuthorizationResult {
    object Allowed : AuthorizationResult()
    data class Denied(val reason: String) : AuthorizationResult()
}

/**
 * Rate limiting result.
 */
sealed class RateLimitResult {
    object Allowed : RateLimitResult()
    data class Exceeded(val resetTimeMs: Long) : RateLimitResult()
}

/**
 * Session information.
 */
@Serializable
data class SessionInfo(
    val sessionToken: String,
    val clientId: String,
    val roles: List<String>,
    val createdAt: Long,
    val expiresAt: Long
) {
    fun isExpired(): Boolean =Clock.System.now().toEpochMilliseconds() > expiresAt
}

/**
 * Access policy for service methods.
 */
@Serializable
data class AccessPolicy(
    val allowedRoles: List<String> = emptyList(),
    val deniedRoles: List<String> = emptyList(),
    val requiresAuth: Boolean = true
) {
    companion object {
        val ALLOW_ALL = AccessPolicy(requiresAuth = false)
        val REQUIRE_AUTH = AccessPolicy(requiresAuth = true)
        fun requireRoles(vararg roles: String) = AccessPolicy(allowedRoles = roles.toList())
    }
}

/**
 * Rate limiting configuration.
 */
@Serializable
data class RateLimitConfig(
    val requestsPerMinute: Int = 60,
    val burstSize: Int = 10,
    val windowSizeMs: Long = 60000 // 1 minute
)

/**
 * Simple rate limiter implementation.
 */
class RateLimiter(private val config: RateLimitConfig) {
    private val requestTimes = mutableListOf<Long>()
    
    fun allowRequest(): Boolean {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        
        // Remove old requests outside the window
        requestTimes.removeAll { it < currentTime - config.windowSizeMs }
        
        return if (requestTimes.size < config.requestsPerMinute) {
            requestTimes.add(currentTime)
            true
        } else {
            false
        }
    }
    
    fun getResetTime(): Long {
        return if (requestTimes.isNotEmpty()) {
            requestTimes.first() + config.windowSizeMs
        } else {
            Clock.System.now().toEpochMilliseconds() + config.windowSizeMs
        }
    }
}

/**
 * Security utilities.
 */
object SecurityUtils {
    
    /**
     * Create a security manager with default configuration.
     */
    fun createDefaultManager(): SecurityManager {
        return SecurityManager(SecurityConfig())
    }
    
    /**
     * Create a security manager with custom configuration.
     */
    fun createManager(config: SecurityConfig): SecurityManager {
        return SecurityManager(config)
    }
    
    /**
     * Generate a secure random string.
     */
    fun generateSecureToken(length: Int = 32): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }
    
    /**
     * Hash a password or sensitive data (placeholder implementation).
     */
    fun hashSensitiveData(data: String): String {
        // In a real implementation, use a proper cryptographic hash function
        return data.hashCode().toString(36)
    }
}



