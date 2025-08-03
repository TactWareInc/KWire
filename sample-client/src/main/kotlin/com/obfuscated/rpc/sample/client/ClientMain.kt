package com.obfuscated.rpc.sample.client

import com.obfuscated.rpc.core.*
import com.obfuscated.rpc.obfuscation.ObfuscationMappings
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory

/**
 * Simplified main client application with proper obfuscation support
 */
fun main(args: Array<String>) {
    runBlocking {
        val logger = LoggerFactory.getLogger("ClientMain")

        logger.info("Starting Obfuscated RPC Client with proper obfuscation...")

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(WebSockets)
        }

        try {
            // First, fetch obfuscation mappings from the server
            logger.info("Fetching obfuscation mappings from server...")

            val mappingsResponse = httpClient.get("http://localhost:8080/mappings")
            if (!mappingsResponse.status.isSuccess()) {
                logger.error("Failed to fetch mappings: ${mappingsResponse.status}")
                return@runBlocking
            }

            val mappingsJson = mappingsResponse.bodyAsText()
            logger.info("Received mappings JSON: $mappingsJson")

            val mappings = json.decodeFromString<ObfuscationMappings>(mappingsJson)

            logger.info("Parsed obfuscation mappings:")
            mappings.services.forEach { (serviceName, methods) ->
                logger.info("  Service: $serviceName")
                methods.forEach { (originalMethod, obfuscatedMethod) ->
                    logger.info("    $originalMethod -> $obfuscatedMethod")
                }
            }

            // Helper function to get obfuscated method name
            fun getObfuscatedMethodName(serviceName: String, methodName: String): String {
                return mappings.services[serviceName]?.get(methodName) ?: methodName
            }

            logger.info("=== Testing HTTP RPC calls with obfuscated method names ===")

            // Test getUserStats with obfuscated method name
            val obfuscatedGetUserStats = getObfuscatedMethodName("UserService", "getUserStats")
            logger.info("Testing getUserStats (obfuscated: $obfuscatedGetUserStats)...")

            val statsRequest = RpcRequest(
                messageId = "test-stats-${System.currentTimeMillis()}",
                timestamp = System.currentTimeMillis(),
                serviceName = "UserService",
                methodId = obfuscatedGetUserStats, // Use obfuscated name
                parameters = listOf(),
                streaming = false
            )

            val requestJson = json.encodeToString(statsRequest)
            logger.info("Sending request: $requestJson")

            val response = httpClient.post("http://localhost:8080/rpc") {
                contentType(ContentType.Application.Json)
                setBody(requestJson)
            }

            val responseText = response.bodyAsText()
            logger.info("Response status: ${response.status}")
            logger.info("Response body: $responseText")

            if (response.status.isSuccess()) {
                try {
                    val rpcResponse = json.decodeFromString<RpcResponse>(responseText)
                    logger.info("✅ getUserStats with obfuscated name successful!")
                    logger.info("Result: ${rpcResponse.result}")
                } catch (e: Exception) {
                    logger.error("Failed to decode response: ${e.message}")
                }
            } else {
                logger.error("HTTP error: ${response.status}")
            }

            // Test getAllUsers with obfuscated method name
            val obfuscatedGetAllUsers = getObfuscatedMethodName("UserService", "getAllUsers")
            logger.info("\nTesting getAllUsers (obfuscated: $obfuscatedGetAllUsers)...")

            val usersRequest = RpcRequest(
                messageId = "test-users-${System.currentTimeMillis()}",
                timestamp = System.currentTimeMillis(),
                serviceName = "UserService",
                methodId = obfuscatedGetAllUsers, // Use obfuscated name
                parameters = listOf(),
                streaming = false
            )

            val usersRequestJson = json.encodeToString(usersRequest)
            val usersResponse = httpClient.post("http://localhost:8080/rpc") {
                contentType(ContentType.Application.Json)
                setBody(usersRequestJson)
            }

            val usersResponseText = usersResponse.bodyAsText()

            if (usersResponse.status.isSuccess()) {
                try {
                    val rpcResponse = json.decodeFromString<RpcResponse>(usersResponseText)
                    logger.info("✅ getAllUsers with obfuscated name successful!")
                    logger.info("Result: ${rpcResponse.result}")
                } catch (e: Exception) {
                    logger.error("Failed to decode getAllUsers response: ${e.message}")
                }
            }

            logger.info("✅ HTTP RPC tests with obfuscated names completed!")

            // Test WebSocket streaming with obfuscated method name
            logger.info("\n=== Testing WebSocket Streaming with Obfuscated Method Names ===")

            val obfuscatedStreamUsers = getObfuscatedMethodName("UserService", "streamUsers")
            logger.info("Using obfuscated streamUsers method: $obfuscatedStreamUsers")

            try {
                httpClient.webSocket(
                    method = HttpMethod.Get,
                    host = "localhost",
                    port = 8080,
                    path = "/rpc-stream"
                ) {
                    logger.info("Connected to WebSocket streaming endpoint")

                    // Start a job to listen for incoming messages
                    val listenerJob = launch {
                        try {
                            for (frame in incoming) {
                                when (frame) {
                                    is Frame.Text -> {
                                        val text = frame.readText()
                                        logger.info("📨 Received stream data: $text")

                                        try {
                                            val streamData = json.decodeFromString<StreamData>(text)
                                            logger.info("🔄 Stream ID: ${streamData.streamId}")
                                            logger.info("📊 Data: ${streamData.data}")
                                        } catch (e: Exception) {
                                            logger.warn("Failed to parse stream data: ${e.message}")
                                        }
                                    }
                                    is Frame.Close -> {
                                        logger.info("WebSocket connection closed by server")
                                        break
                                    }
                                    is Frame.Binary -> {
                                        logger.info("Received binary frame")
                                    }
                                    is Frame.Ping -> {
                                        logger.info("Received ping frame")
                                        send(Frame.Pong(frame.data))
                                    }
                                    is Frame.Pong -> {
                                        logger.info("Received pong frame")
                                    }
                                    else -> {
                                        logger.info("Received other frame type: ${frame.frameType}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logger.error("Error in WebSocket listener: ${e.message}")
                        }
                    }

                    // Send a stream start message with obfuscated method name
                    val streamStart = StreamStart(
                        messageId = "stream-start-${System.currentTimeMillis()}",
                        timestamp = System.currentTimeMillis(),
                        serviceName = "UserService",
                        methodId = obfuscatedStreamUsers, // Use obfuscated method name
                        streamId = "user-stream",
                        parameters = listOf()
                    )

                    val streamStartJson = json.encodeToString(streamStart)
                    logger.info("Sending stream start with obfuscated method: $streamStartJson")
                    send(Frame.Text(streamStartJson))

                    // Listen for 15 seconds to see streaming users
                    logger.info("🎧 Listening for streaming users for 15 seconds...")
                    delay(15000)

                    // Cancel the listener and close gracefully
                    listenerJob.cancel()
                    close(CloseReason(CloseReason.Codes.NORMAL, "Client closing"))
                    logger.info("WebSocket connection closed by client")
                }
            } catch (e: Exception) {
                logger.error("WebSocket streaming error: ${e.message}")
            }

            logger.info("✅ WebSocket streaming test with obfuscated names completed!")

            // Test with non-obfuscated names to verify security
            logger.info("\n=== Testing Security: Non-obfuscated method names should fail ===")

            val nonObfuscatedRequest = RpcRequest(
                messageId = "test-security-${System.currentTimeMillis()}",
                timestamp = System.currentTimeMillis(),
                serviceName = "UserService",
                methodId = "getUserStats", // Use original name (should fail)
                parameters = listOf(),
                streaming = false
            )

            val securityTestJson = json.encodeToString(nonObfuscatedRequest)
            val securityResponse = httpClient.post("http://localhost:8080/rpc") {
                contentType(ContentType.Application.Json)
                setBody(securityTestJson)
            }

            val securityResponseText = securityResponse.bodyAsText()
            logger.info("Security test response: $securityResponseText")

            if (securityResponseText.contains("error") || securityResponseText.contains("not found")) {
                logger.info("✅ Security test passed: Non-obfuscated method names are rejected")
            } else {
                logger.warn("⚠️ Security issue: Non-obfuscated method names are still accepted")
            }

        } catch (e: Exception) {
            logger.error("Client error", e)
        } finally {
            httpClient.close()
            logger.info("Client disconnected")
        }
    }
}
