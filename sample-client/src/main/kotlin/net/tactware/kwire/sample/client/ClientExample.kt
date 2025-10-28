package net.tactware.kwire.sample.client

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.tactware.kwire.ktor.ktorWebSocketClientTransport
import net.tactware.kwire.sample.api.CreateUserRequest
import org.slf4j.LoggerFactory

fun main() = runBlocking {

    val logger = LoggerFactory.getLogger("WorkingProductionClient")

    val transport = ktorWebSocketClientTransport(CoroutineScope(Dispatchers.IO)) {
        serverUrl("ws://localhost:8082/users")  // Connect to production server
        pingInterval(15)                      // Ping every 15 seconds
        requestTimeout(30_000)               // 30 second timeout
        reconnectDelay(5_000)                // 5 second reconnect delay
        maxReconnectAttempts(3)              // Try 3 times to reconnect
    }

    val client = UserServiceClientImpl(transport, timeProvider = { System.currentTimeMillis() })

    transport.connect()

    client.createUser(
        CreateUserRequest(
            name = "Alice",
            email = "alice@wonderland.com",
            age = 30
        )
    )

    logger.info("${client} started, waiting for server...")

    CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler {
        _, exception ->
        logger.error("Unhandled exception in client scope", exception)
    }).launch {
        for (i in 1..5) {
            val users = client.getAllUsers()
            logger.info("All users (attempt $i): $users")
            kotlinx.coroutines.delay(1_000)
        }
    }


    client.streamUsersStats().collect {
        logger.info("User stats: $it")
    }
}