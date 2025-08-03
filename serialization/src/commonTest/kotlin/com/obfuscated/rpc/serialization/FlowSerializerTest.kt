package com.obfuscated.rpc.serialization

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for Flow serialization utilities.
 */
class FlowSerializerTest {
    
    private val json = Json.Default
    
    @Test
    fun testFlowItemSerialization() {
        val item = "test string"
        val serializer = serializer<String>()
        
        val jsonElement = FlowSerializationUtils.serializeFlowItem(item, serializer, json)
        
        assertNotNull(jsonElement)
        assertEquals(JsonPrimitive("test string"), jsonElement)
    }
    
    @Test
    fun testFlowItemDeserialization() {
        val jsonElement = JsonPrimitive("test string")
        val serializer = serializer<String>()
        
        val item = FlowSerializationUtils.deserializeFlowItem(jsonElement, serializer, json)
        
        assertEquals("test string", item)
    }
    
    @Test
    fun testFlowItemSerializationWithComplexType() {
        @kotlinx.serialization.Serializable
        data class TestData(val id: Int, val name: String)
        
        val item = TestData(42, "test")
        val serializer = serializer<TestData>()
        
        val jsonElement = FlowSerializationUtils.serializeFlowItem(item, serializer, json)
        val deserializedItem = FlowSerializationUtils.deserializeFlowItem(jsonElement, serializer, json)
        
        assertEquals(item, deserializedItem)
    }
    
    @Test
    fun testCreateFlowFromJsonElements() = runTest {
        val elements = sequenceOf(
            JsonPrimitive("item1"),
            JsonPrimitive("item2"),
            JsonPrimitive("item3")
        )
        val serializer = serializer<String>()
        
        val flow = FlowSerializationUtils.createFlowFromJsonElements(elements, serializer, json)
        val items = flow.toList()
        
        assertEquals(3, items.size)
        assertEquals("item1", items[0])
        assertEquals("item2", items[1])
        assertEquals("item3", items[2])
    }
    
    @Test
    fun testFlowToJsonElements() = runTest {
        val flow = flowOf("item1", "item2", "item3")
        val serializer = serializer<String>()
        
        val jsonFlow = FlowSerializationUtils.flowToJsonElements(flow, serializer, json)
        val jsonElements = jsonFlow.toList()
        
        assertEquals(3, jsonElements.size)
        assertEquals(JsonPrimitive("item1"), jsonElements[0])
        assertEquals(JsonPrimitive("item2"), jsonElements[1])
        assertEquals(JsonPrimitive("item3"), jsonElements[2])
    }
    
    @Test
    fun testFlowExtensionFunctions() = runTest {
        val flow = flowOf("hello", "world")
        val serializer = serializer<String>()
        
        // Test serializeItems extension
        val serializedFlow = flow.serializeItems(serializer, json)
        val serializedItems = serializedFlow.toList()
        
        assertEquals(2, serializedItems.size)
        assertEquals(JsonPrimitive("hello"), serializedItems[0])
        assertEquals(JsonPrimitive("world"), serializedItems[1])
        
        // Test deserializeItems extension
        val jsonFlow = flowOf(JsonPrimitive("hello"), JsonPrimitive("world"))
        val deserializedFlow = jsonFlow.deserializeItems(serializer, json)
        val deserializedItems = deserializedFlow.toList()
        
        assertEquals(2, deserializedItems.size)
        assertEquals("hello", deserializedItems[0])
        assertEquals("world", deserializedItems[1])
    }
    
    @Test
    fun testStreamingSupport() = runTest {
        val sourceFlow = flowOf(1, 2, 3, 4, 5)
        val serializer = serializer<Int>()
        
        // Test wrapping flow for RPC
        val wrappedFlow = StreamingSupport.wrapFlowForRpc(sourceFlow, serializer, json)
        val wrappedItems = wrappedFlow.toList()
        
        assertEquals(5, wrappedItems.size)
        wrappedItems.forEach { jsonElement ->
            assertTrue(jsonElement is JsonPrimitive)
        }
        
        // Test unwrapping flow from RPC
        val unwrappedFlow = StreamingSupport.unwrapFlowFromRpc(wrappedFlow, serializer, json)
        val unwrappedItems = unwrappedFlow.toList()
        
        assertEquals(listOf(1, 2, 3, 4, 5), unwrappedItems)
    }
    
    @Test
    fun testStreamingFlowBuilder() = runTest {
        val sourceFlow = flowOf("a", "b", "c")
        val config = StreamingConfig(
            bufferSize = 10,
            maxRetries = 2,
            enableBackpressure = true,
            enableErrorRecovery = true
        )
        
        var startCalled = false
        var completeCalled = false
        
        val configuredFlow = sourceFlow.streaming()
            .withConfig(config)
            .onStart { startCalled = true }
            .onComplete { completeCalled = true }
            .build()
        
        val items = configuredFlow.toList()
        
        assertEquals(listOf("a", "b", "c"), items)
        assertTrue(startCalled)
        assertTrue(completeCalled)
    }
    
    @Test
    fun testStreamItemMetadata() {
        val streamId = "test-stream-123"
        val data = "test data"
        val timestamp = 1234567890L
        
        val streamItem = StreamItem(
            streamId = streamId,
            itemIndex = 0,
            data = data,
            timestamp = timestamp
        )
        
        assertEquals(streamId, streamItem.streamId)
        assertEquals(0, streamItem.itemIndex)
        assertEquals(data, streamItem.data)
        assertEquals(timestamp, streamItem.timestamp)
    }
    
    @Test
    fun testCreateMetadataFlow() = runTest {
        val sourceFlow = flowOf("item1", "item2")
        val streamId = "test-stream"
        
        val metadataFlow = StreamingSupport.createMetadataFlow(sourceFlow, streamId)
        val metadataItems = metadataFlow.toList()
        
        assertEquals(2, metadataItems.size)
        
        assertEquals(streamId, metadataItems[0].streamId)
        assertEquals(0, metadataItems[0].itemIndex)
        assertEquals("item1", metadataItems[0].data)
        
        assertEquals(streamId, metadataItems[1].streamId)
        assertEquals(1, metadataItems[1].itemIndex)
        assertEquals("item2", metadataItems[1].data)
    }
    
    @Test
    fun testExtractDataFromMetadataFlow() = runTest {
        val metadataFlow = flowOf(
            StreamItem("stream1", 0, "data1", 123L),
            StreamItem("stream1", 1, "data2", 124L)
        )
        
        val dataFlow = StreamingSupport.extractDataFromMetadataFlow(metadataFlow)
        val dataItems = dataFlow.toList()
        
        assertEquals(listOf("data1", "data2"), dataItems)
    }
}

