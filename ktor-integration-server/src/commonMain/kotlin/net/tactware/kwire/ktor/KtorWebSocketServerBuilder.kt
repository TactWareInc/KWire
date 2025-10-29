package net.tactware.kwire.ktor
/**
 * Builder for creating Ktor WebSocket server transport
 */
class KtorWebSocketServerBuilder {
    private var config = KtorWebSocketServerConfig()
    
    fun host(host: String) = apply { config = config.copy(host = host) }
    fun port(port: Int) = apply { config = config.copy(port = port) }
    fun path(path: String) = apply { config = config.copy(path = path) }
    fun pingInterval(seconds: Long) = apply { config = config.copy(pingIntervalSeconds = seconds) }
    fun timeout(seconds: Long) = apply { config = config.copy(timeoutSeconds = seconds) }
    fun maxFrameSize(bytes: Long) = apply { config = config.copy(maxFrameSize = bytes) }
    fun maxConnections(count: Int) = apply { config = config.copy(maxConnections = count) }
    
    fun build(): KtorWebSocketServerTransport = KtorWebSocketServerTransport(config)
}