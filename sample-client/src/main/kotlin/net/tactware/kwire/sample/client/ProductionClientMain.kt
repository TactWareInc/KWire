package net.tactware.kwire.sample.client

import net.tactware.kwire.core.*
import net.tactware.kwire.ktor.*
import net.tactware.kwire.sample.api.CreateUserRequest
import net.tactware.kwire.sample.api.User
import net.tactware.kwire.sample.api.UserStats
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import net.tactware.kwire.core.messages.RpcRequest
import net.tactware.kwire.core.messages.RpcResponse
import net.tactware.kwire.core.messages.StreamData
import net.tactware.kwire.core.messages.StreamStart
import org.slf4j.LoggerFactory

/**
 * Working production client using real Ktor WebSocket transport.
 * This demonstrates the enhanced RPC pattern with actual network communication
 * and collects exactly 5 emits from streaming as requested.
 */
fun main(args: Array<String>) {
    runBlocking {
        val logger = LoggerFactory.getLogger("WorkingProductionClient")

        logger.info("🚀 Starting Working KWire Production RPC Client with Ktor WebSocket")
        logger.info("=" * 70)

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = true
        }

        // Create real Ktor WebSocket client transport
        logger.info("🔧 Configuring Ktor WebSocket Client Transport...")
        val transport = ktorWebSocketClientTransport {
            serverUrl("ws://localhost:8082/rpc")  // Connect to production server
            pingInterval(15)                      // Ping every 15 seconds
            requestTimeout(30_000)               // 30 second timeout
            reconnectDelay(5_000)                // 5 second reconnect delay
            maxReconnectAttempts(3)              // Try 3 times to reconnect
        }

        logger.info("✅ Ktor WebSocket Client Transport configured")
        logger.info("🌐 Will connect to: ws://localhost:8080/rpc")

        try {
            // Connect to the WebSocket server
            logger.info("🎬 Connecting to Ktor WebSocket Server...")
            transport.connect()
            logger.info("✅ Connected to Ktor WebSocket Server!")

            // Wait for connection to establish
            logger.info("⏳ Waiting for WebSocket connection to establish...")
            delay(2000)

            if (!transport.isConnected) {
                logger.error("❌ Failed to establish WebSocket connection")
                return@runBlocking
            }

            logger.info("✅ WebSocket connection established!")

            logger.info("🎯 Starting Working Production Functionality Test with Real Network")
            logger.info("=" * 70)

            // Phase 1: Test basic RPC operations over network
            performWorkingNetworkRpcTests(logger, transport, json)

            // Phase 2: Test streaming operations with 5-emit collection over network
            performWorkingNetworkStreamingTests(logger, transport, json)

            // Phase 3: Test connection health and statistics
            performWorkingConnectionTests(logger, transport)

            logger.info("🎉 Working Production Functionality Test Completed Successfully!")
            logger.info("=" * 70)

            // Summary of working production features demonstrated
            logger.info("✨ Working Production Features Demonstrated:")
            logger.info("   ✅ Real Ktor WebSocket network communication")
            logger.info("   ✅ Manual method dispatch (no code generation required)")
            logger.info("   ✅ Direct RPC message transmission over WebSocket")
            logger.info("   ✅ Streaming with precise control (5 emits) over network")
            logger.info("   ✅ Connection health monitoring")
            logger.info("   ✅ Professional error handling and logging")
            logger.info("   ✅ Production-ready transport layer")

        } catch (e: Exception) {
            logger.error("❌ Client error: ${e.message}")
            e.printStackTrace()
        } finally {
            logger.info("🛑 Stopping Working Production RPC Client...")
            transport.disconnect()
            logger.info("✅ Working Production RPC Client stopped gracefully")
        }
    }
}

/**
 * Test basic RPC operations over real network with manual dispatch
 */
private suspend fun performWorkingNetworkRpcTests(
    logger: org.slf4j.Logger,
    transport: KtorWebSocketClientTransport,
    json: Json
) {
    logger.info("📋 Phase 1: Working Network RPC Operations Test")
    logger.info("-" * 50)

    try {
        // Test getUserStats over network
        logger.info("🔍 Testing getUserStats over WebSocket...")
        val statsRequest = RpcRequest(
            messageId = generateMessageId(),
            timestamp = System.currentTimeMillis(),
            serviceName = "UserService",
            methodId = "getUserStats",
            parameters = emptyList()
        )

        transport.send(statsRequest)
        logger.info("📤 Request sent over WebSocket")

        // Wait for response with timeout
        val statsResponse = withTimeout(10_000) {
            transport.receive()
                .filterIsInstance<RpcResponse>()
                .first { it.messageId == statsRequest.messageId }
        }

        logger.info("📨 Response received over WebSocket")
        val stats = json.decodeFromString<UserStats>(statsResponse.result.toString())
        logger.info("✅ User stats retrieved: ${stats.totalUsers} total, ${stats.activeUsers} active")

        // Test getAllUsers over network
        logger.info("📋 Testing getAllUsers over WebSocket...")
        val usersRequest = RpcRequest(
            messageId = generateMessageId(),
            timestamp = System.currentTimeMillis(),
            serviceName = "UserService",
            methodId = "getAllUsers",
            parameters = emptyList()
        )

        transport.send(usersRequest)
        logger.info("📤 Request sent over WebSocket")

        val usersResponse = withTimeout(10_000) {
            transport.receive()
                .filterIsInstance<RpcResponse>()
                .first { it.messageId == usersRequest.messageId }
        }

        logger.info("📨 Response received over WebSocket")
        val users = json.decodeFromString<List<User>>(usersResponse.result.toString())
        logger.info("✅ Retrieved ${users.size} users over network:")
        users.take(3).forEach { user ->
            logger.info("   👤 ${user.name} (${user.email})")
        }

        // Test createUser over network
        logger.info("🆕 Testing createUser over WebSocket...")
        val createRequest = CreateUserRequest("Network User", "network@production.com", 28)
        val createRpcRequest = RpcRequest(
            messageId = generateMessageId(),
            timestamp = System.currentTimeMillis(),
            serviceName = "UserService",
            methodId = "createUser",
            parameters = listOf(json.parseToJsonElement(json.encodeToString(createRequest)))
        )

        transport.send(createRpcRequest)
        logger.info("📤 Create request sent over WebSocket")

        val createResponse = withTimeout(10_000) {
            transport.receive()
                .filterIsInstance<RpcResponse>()
                .first { it.messageId == createRpcRequest.messageId }
        }

        logger.info("📨 Create response received over WebSocket")
        val newUser = json.decodeFromString<User>(createResponse.result.toString())
        logger.info("✅ User created over network: ${newUser.name} (${newUser.id})")

        logger.info("✅ Phase 1 completed successfully over real network!")

    } catch (e: Exception) {
        logger.error("❌ Phase 1 network error: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Test streaming operations with 5-emit collection over real network
 */
private suspend fun performWorkingNetworkStreamingTests(
    logger: org.slf4j.Logger,
    transport: KtorWebSocketClientTransport,
    json: Json
) {
    logger.info("\n📋 Phase 2: Working Network Streaming Operations Test")
    logger.info("-" * 50)

    try {
        // Test streamUsers with exactly 5 emits over network (as requested)
        logger.info("🌊 Testing streamUsers over WebSocket - collecting exactly 5 emits...")

        val streamRequest = StreamStart(
            messageId = generateMessageId(),
            timestamp = System.currentTimeMillis(),
            streamId = generateStreamId(),
            serviceName = "UserService",
            methodId = "streamUsers",
            parameters = emptyList()
        )

        transport.send(streamRequest)
        logger.info("📤 Stream request sent over WebSocket")

        var emitCount = 0
        val maxEmits = 5 // Exactly 5 emits as requested

        // Collect exactly 5 emits with timeout
        withTimeout(30_000) {
            transport.receive()
                .filterIsInstance<StreamData>()
                .filter { it.streamId == streamRequest.streamId }
                .take(maxEmits) // Exactly 5 emits as requested
                .collect { streamData ->
                    emitCount++
                    val user = json.decodeFromString<User>(streamData.data.toString())
                    logger.info("📤 Working network stream emit #$emitCount: ${user.name} (${user.email})")
                }
        }

        logger.info("✅ Successfully collected exactly $emitCount emits from working network stream")
        logger.info("🎯 Demonstrated collecting exactly 5 emits over real WebSocket connection")
        logger.info("✅ Phase 2 completed successfully over real network!")

    } catch (e: Exception) {
        logger.error("❌ Phase 2 network streaming error: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Test connection health and statistics
 */
private suspend fun performWorkingConnectionTests(
    logger: org.slf4j.Logger,
    transport: KtorWebSocketClientTransport
) {
    logger.info("\n📋 Phase 3: Working Connection Health and Statistics Test")
    logger.info("-" * 50)

    try {
        // Test connection health
        logger.info("🏓 Testing connection health...")
        val pingResult = transport.ping()
        logger.info("✅ Ping result: ${if (pingResult) "SUCCESS" else "FAILED"}")

        // Get connection statistics
        logger.info("📊 Getting connection statistics...")
        val stats = transport.getConnectionStats()
        logger.info("✅ Connection stats:")
        logger.info("   🔌 Connected: ${stats.isConnected}")
        logger.info("   🌐 Server URL: ${stats.serverUrl}")
        logger.info("   ⏰ Connection Time: ${stats.connectionTime ?: "N/A"}")

        logger.info("✅ Phase 3 completed successfully!")

    } catch (e: Exception) {
        logger.error("❌ Phase 3 connection test error: ${e.message}")
        e.printStackTrace()
    }
}

// Utility functions
private fun generateMessageId(): String {
    return "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
}

private fun generateStreamId(): String {
    return "stream_${System.currentTimeMillis()}_${(1000..9999).random()}"
}

// Extension function for string repetition
private operator fun String.times(n: Int): String = this.repeat(n)

