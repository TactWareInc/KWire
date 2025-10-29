package net.tactware.kwire.ktor

import kotlinx.coroutines.CoroutineScope

/**
 * Builder for creating Ktor WebSocket client transport
 */
class KtorWebSocketClientBuilder {
    private var config = KtorWebSocketClientConfig()
    
    fun serverUrl(url: String) = apply { config = config.copy(serverUrl = url) }
    fun pingInterval(seconds: Long) = apply { config = config.copy(pingIntervalSeconds = seconds) }
    fun maxFrameSize(bytes: Long) = apply { config = config.copy(maxFrameSize = bytes) }
    fun requestTimeout(ms: Long) = apply { config = config.copy(requestTimeoutMs = ms) }
    fun reconnectDelay(ms: Long) = apply { config = config.copy(reconnectDelayMs = ms) }
    fun maxReconnectAttempts(attempts: Int) = apply { config = config.copy(maxReconnectAttempts = attempts) }
    
    fun build(scope : CoroutineScope): KtorWebSocketClientTransport = KtorWebSocketClientTransport(config, scope)
}
