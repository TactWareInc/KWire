package net.tactware.kwire.ktor

/**
 * Configuration for Ktor WebSocket server transport
 */
data class KtorWebSocketServerConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val path: String = "/rpc",
    val pingIntervalSeconds: Long = 15,
    val timeoutSeconds: Long = 60,
    val maxFrameSize: Long = 1024 * 1024, // 1MB
    val maxConnections: Int = 1000
)
