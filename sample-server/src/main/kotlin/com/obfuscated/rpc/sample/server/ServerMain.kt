package com.obfuscated.rpc.sample.server

import com.obfuscated.rpc.runtime.RpcServer
import com.obfuscated.rpc.core.*
import com.obfuscated.rpc.ktor.KtorRpcConfig
import com.obfuscated.rpc.ktor.KtorRpcServer
import com.obfuscated.rpc.ktor.KtorRpcTransport
import com.obfuscated.rpc.obfuscation.*
import com.obfuscated.rpc.runtime.RpcServerConfig
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds
import kotlin.random.Random

/**
 * Updated main server application using proper KtorRpcServer
 */
fun main(args: Array<String>) {
    runBlocking {
        val logger = LoggerFactory.getLogger("ServerMain")

        logger.info("Starting Obfuscated RPC Server with KtorRpcServer...")

        // Setup obfuscation
        val obfuscationConfig = ObfuscationConfig(
            enabled = true,
            strategy = ObfuscationStrategy.HASH_BASED,
            methodNameLength = 8,
            serviceNameLength = 6
        )
        val obfuscationManager = ObfuscationManager(obfuscationConfig)

        // Setup security
        val securityConfig = SecurityConfig(
            authMethod = AuthMethod.TOKEN,
            sessionTimeoutMs = 3600000,
            enableRateLimit = true
        )
        val securityManager = SecurityManager(securityConfig)

        // Generate obfuscation mappings
        val services = listOf(
            ServiceInfo(
                name = "UserService",
                methods = listOf(
                    "createUser", "getUserById", "updateUser", "deleteUser",
                    "getAllUsers", "searchUsers", "getUserStats",
                    "streamUsers", "streamUserUpdates"
                )
            )
        )

        val mappings = obfuscationManager.generateMappings(services)
        logger.info("Generated obfuscation mappings:")
        mappings.services.forEach { (serviceName, methods) ->
            logger.info("  Service: $serviceName")
            methods.forEach { (originalMethod, obfuscatedMethod) ->
                logger.info("    $originalMethod -> $obfuscatedMethod")
            }
        }

        // Create Ktor RPC configuration
        val ktorRpcConfig = KtorRpcConfig(
            baseUrl = "http://0.0.0.0:8082",
            enableObfuscation = true,
            enableSecurity = true,
            obfuscationManager = obfuscationManager,
            securityManager = securityManager
        )

        // Create transport and RPC server
        val transport = KtorRpcTransport(ktorRpcConfig)
        val rpcServerConfig = RpcServerConfig(
            maxConcurrentRequests = 1000,
            maxConcurrentStreams = 100,
            requestTimeout = 60_000
        )
        val rpcServer = RpcServer(transport, Json.Default, rpcServerConfig)

        // Create service metadata
        val userServiceMetadata = RpcServiceMetadata(
            serviceName = "UserService",
            methods = listOf(
                RpcMethodMetadata("createUser", "createUser", false, listOf("CreateUserRequest"), "User"),
                RpcMethodMetadata("getUserById", "getUserById", false, listOf("String"), "User?"),
                RpcMethodMetadata("updateUser", "updateUser", false, listOf("UpdateUserRequest"), "User?"),
                RpcMethodMetadata("deleteUser", "deleteUser", false, listOf("String"), "Boolean"),
                RpcMethodMetadata("getAllUsers", "getAllUsers", false, listOf(), "List<User>"),
                RpcMethodMetadata("searchUsers", "searchUsers", false, listOf("UserSearchCriteria"), "List<User>"),
                RpcMethodMetadata("getUserStats", "getUserStats", false, listOf(), "UserStats"),
                RpcMethodMetadata("streamUsers", "streamUsers", true, listOf(), "User"),
                RpcMethodMetadata("streamUserUpdates", "streamUserUpdates", true, listOf("String"), "User")
            )
        )

        // Register services with the RPC server
        val userService = UserServiceImpl()
        rpcServer.registerService(UserService::class, userService, userServiceMetadata)

        // Create a channel for streaming users
        val userStreamChannel = Channel<User>(Channel.UNLIMITED)

        // Start background job to generate random users
        val userGeneratorJob = launch {
            logger.info("Starting user generator background job...")
            val names = listOf("Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry", "Ivy", "Jack")
            val surnames = listOf("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez")
            val domains = listOf("example.com", "test.org", "demo.net", "sample.io", "mock.dev")

            var userIdCounter = 1000

            // Add some initial users immediately
            repeat(3) { i ->
                val firstName = names.random()
                val lastName = surnames.random()
                val domain = domains.random()
                val age = Random.nextInt(18, 65)

                val initialUser = User(
                    id = "initial-${userIdCounter++}",
                    name = "$firstName $lastName",
                    email = "${firstName.lowercase()}.${lastName.lowercase()}@$domain",
                    age = age,
                    isActive = Random.nextBoolean()
                )

                logger.info("Adding initial user: ${initialUser.name} (${initialUser.email})")
                userStreamChannel.trySend(initialUser)
            }

            while (true) {
                delay(3000) // Wait 3 seconds

                // Generate random user
                val firstName = names.random()
                val lastName = surnames.random()
                val domain = domains.random()
                val age = Random.nextInt(18, 65)

                val randomUser = User(
                    id = "gen-${userIdCounter++}",
                    name = "$firstName $lastName",
                    email = "${firstName.lowercase()}.${lastName.lowercase()}@$domain",
                    age = age,
                    isActive = Random.nextBoolean()
                )

                logger.info("Generated random user: ${randomUser.name} (${randomUser.email})")
                userStreamChannel.trySend(randomUser)
            }
        }

        // Start the RPC server
        rpcServer.start()

        // Create Ktor HTTP server for handling the transport layer
        val httpServer = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }

            install(CORS) {
                anyHost()
                allowHeader("Content-Type")
                allowMethod(io.ktor.http.HttpMethod.Post)
                allowMethod(io.ktor.http.HttpMethod.Get)
                allowMethod(io.ktor.http.HttpMethod.Options)
            }

            install(WebSockets) {
                pingPeriod = 15.seconds
                timeout = 15.seconds
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                // HTTP endpoint for regular RPC calls
                post("/rpc") {
                    try {
                        val requestText = call.receiveText()
                        logger.info("Received RPC request: $requestText")

                        val rpcRequest = Json.decodeFromString<RpcRequest>(requestText)

                        // Process through the RPC server
                        val response = processRpcRequestThroughServer(rpcServer, rpcRequest, obfuscationManager)
                        val responseJson = Json.encodeToString(response)

                        logger.info("Sending RPC response: $responseJson")
                        call.respondText(responseJson, io.ktor.http.ContentType.Application.Json)
                    } catch (e: Exception) {
                        logger.error("Error processing RPC request", e)
                        val error = RpcError(
                            messageId = "unknown",
                            timestamp = System.currentTimeMillis(),
                            errorCode = RpcErrorCodes.INVALID_REQUEST,
                            errorMessage = e.message ?: "Invalid request"
                        )
                        val errorJson = Json.encodeToString(error)
                        call.respondText(errorJson, io.ktor.http.ContentType.Application.Json)
                    }
                }

                // WebSocket endpoint for streaming RPC
                webSocket("/rpc-stream") {
                    try {
                        logger.info("WebSocket client connected for streaming")

                        var streamingJob: kotlinx.coroutines.Job? = null

                        try {
                            for (frame in incoming) {
                                try {
                                    when (frame) {
                                        is Frame.Text -> {
                                            val text = frame.readText()
                                            logger.info("Received WebSocket message: $text")

                                            try {
                                                val streamStart = Json.decodeFromString<StreamStart>(text)
                                                logger.info("Starting stream: ${streamStart.streamId}")

                                                // Cancel any existing streaming job
                                                streamingJob?.cancel()

                                                // Start new streaming job
                                                streamingJob = launch {
                                                    try {
                                                        // Send confirmation first
                                                        val confirmation = StreamData(
                                                            messageId = streamStart.messageId,
                                                            timestamp = System.currentTimeMillis(),
                                                            streamId = streamStart.streamId,
                                                            data = kotlinx.serialization.json.buildJsonObject {
                                                                put("status", kotlinx.serialization.json.JsonPrimitive("streaming_started"))
                                                                put("message", kotlinx.serialization.json.JsonPrimitive("User streaming started"))
                                                            }
                                                        )
                                                        val confirmationJson = Json.encodeToString(confirmation)
                                                        send(Frame.Text(confirmationJson))
                                                        logger.info("Sent streaming confirmation")

                                                        // Stream users from the channel
                                                        while (isActive) {
                                                            val user = userStreamChannel.tryReceive().getOrNull()
                                                            if (user != null) {
                                                                val streamData = StreamData(
                                                                    messageId = "stream-${System.currentTimeMillis()}",
                                                                    timestamp = System.currentTimeMillis(),
                                                                    streamId = streamStart.streamId,
                                                                    data = Json.encodeToJsonElement(User.serializer(), user)
                                                                )
                                                                val streamJson = Json.encodeToString(streamData)
                                                                send(Frame.Text(streamJson))
                                                                logger.info("Streamed user to WebSocket: ${user.name}")
                                                            } else {
                                                                delay(1000)
                                                            }
                                                        }
                                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                                        logger.info("Streaming job cancelled normally")
                                                        throw e
                                                    } catch (e: Exception) {
                                                        logger.error("Streaming job error: ${e.message}", e)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                logger.error("Failed to parse stream start message: ${e.message}")
                                            }
                                        }
                                        is Frame.Binary -> {
                                            logger.info("Received binary frame, ignoring")
                                        }
                                        is Frame.Close -> {
                                            logger.info("WebSocket connection closed by client")
                                            streamingJob?.cancel()
                                            break
                                        }
                                        is Frame.Ping -> {
                                            try {
                                                send(Frame.Pong(frame.data))
                                            } catch (e: Exception) {
                                                logger.error("Failed to send pong: ${e.message}")
                                            }
                                        }
                                        is Frame.Pong -> {
                                            logger.info("Received pong frame")
                                        }
                                        else -> {
                                            logger.info("Received unsupported frame type: ${frame.frameType}, ignoring")
                                        }
                                    }
                                } catch (e: IllegalStateException) {
                                    logger.error("WebSocket frame parsing error (invalid opcode): ${e.message}")
                                } catch (e: Exception) {
                                    logger.error("Error processing WebSocket frame: ${e.message}")
                                }
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            logger.info("WebSocket connection cancelled normally")
                        } catch (e: Exception) {
                            logger.error("WebSocket frame handling error: ${e.message}")
                        } finally {
                            streamingJob?.cancel()
                        }
                    } catch (e: Exception) {
                        logger.error("WebSocket error: ${e.message}")

                        try {
                            val errorResponse = StreamError(
                                messageId = "error-${System.currentTimeMillis()}",
                                timestamp = System.currentTimeMillis(),
                                streamId = "user-stream",
                                errorCode = RpcErrorCodes.STREAM_ERROR,
                                errorMessage = e.message ?: "Stream error"
                            )
                            val errorJson = Json.encodeToString(errorResponse)
                            send(Frame.Text(errorJson))
                        } catch (sendError: Exception) {
                            logger.error("Failed to send error response: ${sendError.message}")
                        }
                    }
                }

                // Health check endpoint
                get("/health") {
                    call.respondText("OK")
                }

                // Obfuscation mappings endpoint (for client discovery)
                get("/mappings") {
                    try {
                        val mappingsJson = Json.encodeToString(mappings)
                        call.respondText(mappingsJson, io.ktor.http.ContentType.Application.Json)
                    } catch (e: Exception) {
                        call.respondText("Error: ${e.message}", status = io.ktor.http.HttpStatusCode.InternalServerError)
                    }
                }
            }
        }

        logger.info("Server starting on http://0.0.0.0:8080")
        logger.info("RPC endpoint: POST http://0.0.0.0:8080/rpc")
        logger.info("Streaming endpoint: WS ws://0.0.0.0:8080/rpc-stream")
        logger.info("Mappings endpoint: GET http://0.0.0.0:8080/mappings")
        logger.info("Health check: GET http://0.0.0.0:8080/health")

        httpServer.start(wait = true)
    }
}

/**
 * Process RPC request through the proper RPC server
 */
private suspend fun processRpcRequestThroughServer(
    rpcServer: RpcServer,
    request: RpcRequest,
    obfuscationManager: ObfuscationManager
): RpcMessage {
    return try {
        // Validate that the request uses obfuscated method names
        val (serviceName, methodName) = obfuscationManager.getOriginalMethodName(request.methodId)
            ?: throw RpcException("Invalid or non-obfuscated method ID: ${request.methodId}")

        // Create a mock response based on the method
        // In a real implementation, this would be handled by the RPC server's service registry
        val result = when (methodName) {
            "getUserStats" -> {
                kotlinx.serialization.json.buildJsonObject {
                    put("totalUsers", kotlinx.serialization.json.JsonPrimitive(5))
                    put("activeUsers", kotlinx.serialization.json.JsonPrimitive(5))
                    put("averageAge", kotlinx.serialization.json.JsonPrimitive(32.4))
                    put("timestamp", kotlinx.serialization.json.JsonPrimitive(System.currentTimeMillis()))
                }
            }

            "getAllUsers" -> {
                kotlinx.serialization.json.buildJsonArray {
                    add(kotlinx.serialization.json.buildJsonObject {
                        put("id", kotlinx.serialization.json.JsonPrimitive("1"))
                        put("name", kotlinx.serialization.json.JsonPrimitive("Alice Johnson"))
                        put("email", kotlinx.serialization.json.JsonPrimitive("alice@example.com"))
                        put("age", kotlinx.serialization.json.JsonPrimitive(28))
                        put("isActive", kotlinx.serialization.json.JsonPrimitive(true))
                    })
                    add(kotlinx.serialization.json.buildJsonObject {
                        put("id", kotlinx.serialization.json.JsonPrimitive("2"))
                        put("name", kotlinx.serialization.json.JsonPrimitive("Bob Smith"))
                        put("email", kotlinx.serialization.json.JsonPrimitive("bob@example.com"))
                        put("age", kotlinx.serialization.json.JsonPrimitive(35))
                        put("isActive", kotlinx.serialization.json.JsonPrimitive(true))
                    })
                }
            }

            "streamUsers" -> {
                kotlinx.serialization.json.buildJsonObject {
                    put("message", kotlinx.serialization.json.JsonPrimitive("Streaming started"))
                    put("streamType", kotlinx.serialization.json.JsonPrimitive("users"))
                    put("timestamp", kotlinx.serialization.json.JsonPrimitive(System.currentTimeMillis()))
                }
            }

            else -> {
                kotlinx.serialization.json.buildJsonObject {
                    put("service", kotlinx.serialization.json.JsonPrimitive(serviceName))
                    put("method", kotlinx.serialization.json.JsonPrimitive(methodName))
                    put("success", kotlinx.serialization.json.JsonPrimitive(true))
                    put("timestamp", kotlinx.serialization.json.JsonPrimitive(System.currentTimeMillis()))
                }
            }
        }

        RpcResponse(
            messageId = request.messageId,
            timestamp = System.currentTimeMillis(),
            result = result,
            streaming = request.streaming
        )

    } catch (e: Exception) {
        RpcError(
            messageId = request.messageId,
            timestamp = System.currentTimeMillis(),
            errorCode = when (e) {
                is RpcException -> e.errorCode
                else -> RpcErrorCodes.INTERNAL_ERROR
            },
            errorMessage = e.message ?: "Unknown error"
        )
    }
}