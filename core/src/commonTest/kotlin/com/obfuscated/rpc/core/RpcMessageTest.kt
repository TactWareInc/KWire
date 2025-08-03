package com.obfuscated.rpc.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Unit tests for RPC message serialization and deserialization.
 */
class RpcMessageTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    @Test
    fun testRpcRequestSerialization() {
        val request = RpcRequest(
            messageId = "test-123",
            timestamp = 1234567890L,
            serviceName = "TestService",
            methodId = "testMethod",
            parameters = listOf(
                JsonPrimitive("hello"),
                JsonPrimitive(42)
            )
        )
        
        val serialized = json.encodeToString(request)
        assertNotNull(serialized)
        assertTrue(serialized.contains("test-123"))
        assertTrue(serialized.contains("TestService"))
        assertTrue(serialized.contains("testMethod"))
        
        val deserialized = json.decodeFromString<RpcRequest>(serialized)
        assertEquals(request.messageId, deserialized.messageId)
        assertEquals(request.serviceName, deserialized.serviceName)
        assertEquals(request.methodId, deserialized.methodId)
        assertEquals(request.parameters.size, deserialized.parameters.size)
    }
    
    @Test
    fun testRpcResponseSerialization() {
        val response = RpcResponse(
            messageId = "test-456",
            timestamp = 1234567890L,
            result = JsonPrimitive("success")
        )
        
        val serialized = json.encodeToString(response)
        assertNotNull(serialized)
        assertTrue(serialized.contains("test-456"))
        
        val deserialized = json.decodeFromString<RpcResponse>(serialized)
        assertEquals(response.messageId, deserialized.messageId)
        assertEquals(response.result, deserialized.result)
    }
    
    @Test
    fun testRpcErrorSerialization() {
        val error = RpcError(
            messageId = "test-789",
            timestamp = 1234567890L,
            errorCode = RpcErrorCodes.INTERNAL_ERROR,
            errorMessage = "Test error message"
        )
        
        val serialized = json.encodeToString(error)
        assertNotNull(serialized)
        assertTrue(serialized.contains("test-789"))
        assertTrue(serialized.contains("Test error message"))
        
        val deserialized = json.decodeFromString<RpcError>(serialized)
        assertEquals(error.messageId, deserialized.messageId)
        assertEquals(error.errorCode, deserialized.errorCode)
        assertEquals(error.errorMessage, deserialized.errorMessage)
    }
    
    @Test
    fun testStreamStartSerialization() {
        val streamStart = StreamStart(
            messageId = "stream-123",
            timestamp = 1234567890L,
            serviceName = "StreamService",
            methodId = "streamMethod",
            streamId = "stream-456",
            parameters = listOf(JsonPrimitive("param1"))
        )
        
        val serialized = json.encodeToString(streamStart)
        assertNotNull(serialized)
        assertTrue(serialized.contains("stream-123"))
        assertTrue(serialized.contains("stream-456"))
        
        val deserialized = json.decodeFromString<StreamStart>(serialized)
        assertEquals(streamStart.messageId, deserialized.messageId)
        assertEquals(streamStart.streamId, deserialized.streamId)
        assertEquals(streamStart.serviceName, deserialized.serviceName)
    }
    
    @Test
    fun testStreamDataSerialization() {
        val streamData = StreamData(
            messageId = "stream-data-123",
            streamId = "stream-456",
            timestamp = 1234567890L,
            data = JsonPrimitive("stream data")
        )
        
        val serialized = json.encodeToString(streamData)
        assertNotNull(serialized)
        assertTrue(serialized.contains("stream-data-123"))
        assertTrue(serialized.contains("stream data"))
        
        val deserialized = json.decodeFromString<StreamData>(serialized)
        assertEquals(streamData.messageId, deserialized.messageId)
        assertEquals(streamData.streamId, deserialized.streamId)
        assertEquals(streamData.data, deserialized.data)
    }
    
    @Test
    fun testStreamEndSerialization() {
        val streamEnd = StreamEnd(
            messageId = "stream-end-123",
            streamId = "stream-456",
            timestamp = 1234567890L
        )
        
        val serialized = json.encodeToString(streamEnd)
        assertNotNull(serialized)
        assertTrue(serialized.contains("stream-end-123"))
        
        val deserialized = json.decodeFromString<StreamEnd>(serialized)
        assertEquals(streamEnd.messageId, deserialized.messageId)
        assertEquals(streamEnd.streamId, deserialized.streamId)
    }
    
    @Test
    fun testRpcServiceMetadataSerialization() {
        val metadata = RpcServiceMetadata(
            serviceName = "TestService",
            methods = listOf(
                RpcMethodMetadata(
                    methodName = "testMethod",
                    methodId = "tm1",
                    streaming = false,
                    parameterTypes = listOf("String", "Int"),
                    returnType = "String"
                )
            ),
            obfuscated = true
        )
        
        val serialized = json.encodeToString(metadata)
        assertNotNull(serialized)
        assertTrue(serialized.contains("TestService"))
        assertTrue(serialized.contains("testMethod"))
        
        val deserialized = json.decodeFromString<RpcServiceMetadata>(serialized)
        assertEquals(metadata.serviceName, deserialized.serviceName)
        assertEquals(metadata.methods.size, deserialized.methods.size)
        assertEquals(metadata.obfuscated, deserialized.obfuscated)
    }
}

