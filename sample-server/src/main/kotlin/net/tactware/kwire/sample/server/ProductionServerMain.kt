package net.tactware.kwire.sample.server

import net.tactware.kwire.core.*
import net.tactware.kwire.ktor.*
import net.tactware.kwire.sample.api.CreateUserRequest
import net.tactware.kwire.sample.api.User
import net.tactware.kwire.sample.api.UserStats
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import net.tactware.kwire.core.messages.RpcError
import net.tactware.kwire.core.messages.RpcMessage
import net.tactware.kwire.core.messages.RpcRequest
import net.tactware.kwire.core.messages.RpcResponse
import net.tactware.kwire.core.messages.StreamData
import net.tactware.kwire.core.messages.StreamEnd
import net.tactware.kwire.core.messages.StreamError
import net.tactware.kwire.core.messages.StreamStart
import org.slf4j.LoggerFactory

/**
 * Working production server using real Ktor WebSocket transport.
 * This version implements manual method dispatch to avoid the code generation requirement.
 */
fun main(args: Array<String>) {
    runBlocking {
        val logger = LoggerFactory.getLogger("WorkingProductionServer")

        logger.info("🚀 Starting Working KWire Production RPC Server with Ktor WebSocket")
        logger.info("=" * 70)

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = true
        }

        // Create real Ktor WebSocket server transport
        logger.info("🔧 Configuring Ktor WebSocket Server Transport...")
        val transport = ktorWebSocketServerTransport {
            host("0.0.0.0")  // Listen on all interfaces
            port(8082)       // Standard HTTP port
            path("/rpc")     // WebSocket endpoint path
            pingInterval(15) // Ping every 15 seconds
            timeout(60)      // 60 second timeout
            maxFrameSize(1024 * 1024) // 1MB max frame size
        }

        logger.info("✅ Ktor WebSocket Server Transport configured")
        logger.info("🌐 Server will listen on: ws://0.0.0.0:8080/rpc")

        // Create UserService implementation
        val userService = WorkingUserServiceImpl()

        // Set up manual method dispatch handlers for the WebSocket transport
        logger.info("🔧 Setting up manual method dispatch handlers...")

        transport.setRequestHandler { request, session ->
            logger.info("🔄 Processing RPC request: ${request.serviceName}.${request.methodId}")

            try {
                val response = when (request.serviceName) {
                    "UserService" -> handleUserServiceRequest(request, userService, json)
                    else -> RpcError(
                        messageId = request.messageId,
                        timestamp = System.currentTimeMillis(),
                        errorCode = "SERVICE_NOT_FOUND",
                        errorMessage = "Service not found: ${request.serviceName}"
                    )
                }

                // Send response back to client
                val responseJson = json.encodeToString<RpcMessage>(response)
                session.send(io.ktor.websocket.Frame.Text(responseJson))
                logger.info("✅ Response sent for ${request.methodId}")

            } catch (e: Exception) {
                logger.error("❌ Error processing request: ${e.message}")
                val error = RpcError(
                    messageId = request.messageId,
                    timestamp = System.currentTimeMillis(),
                    errorCode = "INTERNAL_ERROR",
                    errorMessage = e.message ?: "Unknown error"
                )
                val errorJson = json.encodeToString<RpcMessage>(error)
                session.send(io.ktor.websocket.Frame.Text(errorJson))
            }
        }

        transport.setStreamHandler { streamStart, session ->
            logger.info("🌊 Processing stream request: ${streamStart.serviceName}.${streamStart.methodId}")

            try {
                when (streamStart.serviceName) {
                    "UserService" -> handleUserServiceStream(streamStart, session, userService, json)
                    else -> {
                        val error = StreamError(
                            messageId = streamStart.messageId,
                            timestamp = System.currentTimeMillis(),
                            streamId = streamStart.streamId,
                            errorCode = "SERVICE_NOT_FOUND",
                            errorMessage = "Service not found: ${streamStart.serviceName}"
                        )
                        val errorJson = json.encodeToString<RpcMessage>(error)
                        session.send(io.ktor.websocket.Frame.Text(errorJson))
                    }
                }

            } catch (e: Exception) {
                logger.error("❌ Error processing stream: ${e.message}")
                val error = StreamError(
                    messageId = streamStart.messageId,
                    timestamp = System.currentTimeMillis(),
                    streamId = streamStart.streamId,
                    errorCode = "STREAM_ERROR",
                    errorMessage = e.message ?: "Unknown stream error"
                )
                val errorJson = json.encodeToString<RpcMessage>(error)
                session.send(io.ktor.websocket.Frame.Text(errorJson))
            }
        }

        logger.info("✅ Manual method dispatch handlers configured")

        // Start the WebSocket server
        logger.info("🎬 Starting Ktor WebSocket Server...")
        transport.connect()
        logger.info("✅ Ktor WebSocket Server started successfully!")

        // Start background monitoring
        val serverJobs = mutableListOf<Job>()

        // Connection monitor
        serverJobs.add(launch {
            logger.info("📈 Starting connection monitor...")
            var monitorCount = 0
            while (isActive) {
                delay(10000) // Every 10 seconds
                monitorCount++

                val connectionCount = transport.getActiveConnectionCount()
                logger.info("📊 Server Status #$monitorCount:")
                logger.info("   🔄 Active Connections: $connectionCount")
                logger.info("   📡 Transport Status: ${if (transport.isConnected) "CONNECTED" else "DISCONNECTED"}")
                logger.info("   🎯 Manual Method Dispatch: ACTIVE")
                logger.info("   🌐 WebSocket Endpoint: ws://0.0.0.0:8080/rpc")

                if (connectionCount > 0) {
                    logger.info("   👥 Connection IDs: ${transport.getActiveConnectionIds().take(3)}")
                }
            }
        })

        logger.info("🎉 Working Production RPC Server is fully operational!")
        logger.info("=" * 70)
        logger.info("🎯 Working Production Features Active:")
        logger.info("   ✅ Real Ktor WebSocket transport")
        logger.info("   ✅ Manual method dispatch (no code generation required)")
        logger.info("   ✅ Network communication over ws://0.0.0.0:8080/rpc")
        logger.info("   ✅ Connection management and monitoring")
        logger.info("   ✅ Request/Response and Streaming support")
        logger.info("   ✅ Professional logging and error handling")
        logger.info("=" * 70)

        // Keep the server running
        try {
            logger.info("🏃 Working production server running... Press Ctrl+C to stop")

            // Run indefinitely until interrupted
            while (isActive) {
                delay(1000)
            }

        } catch (e: Exception) {
            logger.error("❌ Server error: ${e.message}")
        } finally {
            logger.info("🛑 Stopping Working Production RPC Server...")
            serverJobs.forEach { it.cancel() }
            transport.disconnect()
            logger.info("✅ Working Production RPC Server stopped gracefully")
        }
    }
}

/**
 * Handle UserService RPC requests with manual method dispatch
 */
private suspend fun handleUserServiceRequest(
    request: RpcRequest,
    userService: WorkingUserServiceImpl,
    json: Json
): RpcMessage {
    return try {
        val result = when (request.methodId) {
            "createUser" -> {
                val createRequest = json.decodeFromString<CreateUserRequest>(request.parameters.first().toString())
                val user = userService.createUser(createRequest)
                json.parseToJsonElement(json.encodeToString(user))
            }
            "getUserById" -> {
                val userId = request.parameters.first().toString().removeSurrounding("\"")
                val user = userService.getUserById(userId)
                if (user != null) json.parseToJsonElement(json.encodeToString(user)) else null
            }
            "getAllUsers" -> {
                val users = userService.getAllUsers()
                json.parseToJsonElement(json.encodeToString(users))
            }
            "getUserStats" -> {
                val stats = userService.getUserStats()
                json.parseToJsonElement(json.encodeToString(stats))
            }
            else -> {
                return RpcError(
                    messageId = request.messageId,
                    timestamp = System.currentTimeMillis(),
                    errorCode = "METHOD_NOT_FOUND",
                    errorMessage = "Method not found: ${request.methodId}"
                )
            }
        }

        RpcResponse(
            messageId = request.messageId,
            timestamp = System.currentTimeMillis(),
            result = result
        )

    } catch (e: Exception) {
        RpcError(
            messageId = request.messageId,
            timestamp = System.currentTimeMillis(),
            errorCode = "INTERNAL_ERROR",
            errorMessage = e.message ?: "Unknown error"
        )
    }
}

/**
 * Handle UserService streaming requests with manual method dispatch
 */
private suspend fun handleUserServiceStream(
    streamStart: StreamStart,
    session: io.ktor.websocket.WebSocketSession,
    userService: WorkingUserServiceImpl,
    json: Json
) {
    when (streamStart.methodId) {
        "streamUsers" -> {
            // Start streaming users in a coroutine
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    userService.streamUsers().collect { user ->
                        val streamData = StreamData(
                            messageId = streamStart.messageId,
                            timestamp = System.currentTimeMillis(),
                            streamId = streamStart.streamId,
                            data = json.parseToJsonElement(json.encodeToString(user))
                        )
                        val streamJson = json.encodeToString<RpcMessage>(streamData)
                        session.send(io.ktor.websocket.Frame.Text(streamJson))
                        LoggerFactory.getLogger("WorkingProductionServer").info("📤 Streamed user: ${user.name}")
                    }

                    // Send stream end
                    val streamEnd = StreamEnd(
                        messageId = streamStart.messageId,
                        timestamp = System.currentTimeMillis(),
                        streamId = streamStart.streamId
                    )
                    val endJson = json.encodeToString<RpcMessage>(streamEnd)
                    session.send(io.ktor.websocket.Frame.Text(endJson))
                    LoggerFactory.getLogger("WorkingProductionServer").info("✅ Stream completed for ${streamStart.methodId}")

                } catch (e: Exception) {
                    val error = StreamError(
                        messageId = streamStart.messageId,
                        timestamp = System.currentTimeMillis(),
                        streamId = streamStart.streamId,
                        errorCode = "STREAM_ERROR",
                        errorMessage = e.message ?: "Unknown stream error"
                    )
                    val errorJson = json.encodeToString<RpcMessage>(error)
                    session.send(io.ktor.websocket.Frame.Text(errorJson))
                }
            }
        }
        else -> {
            val error = StreamError(
                messageId = streamStart.messageId,
                timestamp = System.currentTimeMillis(),
                streamId = streamStart.streamId,
                errorCode = "METHOD_NOT_FOUND",
                errorMessage = "Stream method not found: ${streamStart.methodId}"
            )
            val errorJson = json.encodeToString<RpcMessage>(error)
            session.send(io.ktor.websocket.Frame.Text(errorJson))
        }
    }
}

/**
 * Working UserService implementation
 */
class WorkingUserServiceImpl {
    private val logger = LoggerFactory.getLogger("WorkingUserService")
    private val users = mutableMapOf<String, User>()

    init {
        // Pre-populate with test users
        repeat(5) { i ->
            val user = User(
                id = "user-${i + 1}",
                name = listOf("Alice Johnson", "Bob Smith", "Charlie Brown", "Diana Garcia", "Eve Miller")[i],
                email = "${listOf("alice", "bob", "charlie", "diana", "eve")[i]}@production.com",
                age = 25 + i * 5,
                isActive = true
            )
            users[user.id] = user
        }
        logger.info("👥 Working service initialized with ${users.size} users")
    }

    suspend fun createUser(request: CreateUserRequest): User {
        logger.info("🆕 Creating user: ${request.name}")
        val user = User(
            id = "user-${System.currentTimeMillis()}",
            name = request.name,
            email = request.email,
            age = request.age,
            isActive = true
        )
        users[user.id] = user
        logger.info("✅ User created: ${user.id}")
        return user
    }

    suspend fun getUserById(id: String): User? {
        logger.info("🔍 Getting user by ID: $id")
        val user = users[id]
        logger.info(if (user != null) "✅ User found: ${user.name}" else "❌ User not found")
        return user
    }

    suspend fun getAllUsers(): List<User> {
        logger.info("📋 Getting all users (${users.size} total)")
        return users.values.toList()
    }

    suspend fun getUserStats(): UserStats {
        logger.info("📈 Calculating user statistics")
        val stats = UserStats(
            totalUsers = users.size,
            activeUsers = users.values.count { it.isActive },
            averageAge = users.values.map { it.age }.average(),
            timestamp = System.currentTimeMillis()
        )
        logger.info("✅ Stats calculated: ${stats.totalUsers} total, ${stats.activeUsers} active")
        return stats
    }

    fun streamUsers(): Flow<User> {
        logger.info("🌊 Starting user stream (${users.size} users)")
        return flow {
            users.values.forEachIndexed { index, user ->
                logger.info("📤 Streaming user ${index + 1}/${users.size}: ${user.name}")
                emit(user)
                delay(2000) // 2 seconds between emissions for demo
            }
            logger.info("✅ User stream completed")
        }
    }
}

// Extension function for string repetition
private operator fun String.times(n: Int): String = this.repeat(n)

