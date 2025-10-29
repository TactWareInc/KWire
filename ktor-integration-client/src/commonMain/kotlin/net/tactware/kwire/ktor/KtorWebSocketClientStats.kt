package net.tactware.kwire.ktor
/**
 * Connection statistics for the client
 */
data class KtorWebSocketClientStats(
    val isConnected: Boolean,
    val serverUrl: String,
    val connectionTime: Long? = null
)