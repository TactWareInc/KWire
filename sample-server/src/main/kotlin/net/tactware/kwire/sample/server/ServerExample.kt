package net.tactware.kwire.sample.server

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.tactware.kwire.ktor.ktorWebSocketServerTransport

fun main() = runBlocking {

    val transport = ktorWebSocketServerTransport {
        host("0.0.0.0")  // Listen on all interfaces
        port(8082)       // Standard HTTP port
        path("/rpc")     // WebSocket endpoint path
        pingInterval(15) // Ping every 15 seconds
        timeout(60)      // 60 second timeout
        maxFrameSize(1024 * 1024) // 1MB max frame size
    }
    UserServiceServerImpl(
        transport = transport,
        implementation = UserServiceImpl()
    ).start()

    transport.connect()

    delay(Long.MAX_VALUE)
}