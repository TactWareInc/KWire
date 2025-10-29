package net.tactware.kwire.ktor

/**
 * Configuration for Ktor WebSocket client transport
 */
data class KtorWebSocketClientConfig(
    val serverUrl: String = "ws://localhost:8080/rpc",
    val pingIntervalSeconds: Long = 15,
    val maxFrameSize: Long = 1024 * 1024, // 1MB
    val requestTimeoutMs: Long = 30_000,
    val reconnectDelayMs: Long = 5_000,
    val maxReconnectAttempts: Int = 3
)
