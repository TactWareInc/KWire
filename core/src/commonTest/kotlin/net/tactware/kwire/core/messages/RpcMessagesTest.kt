package net.tactware.kwire.core.messages

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RpcMessagesTest {
    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    @Test
    fun rpcRequest_roundTrip_preservesValues() {
        val params = listOf(JsonPrimitive(42), JsonPrimitive("foo"), JsonObject(mapOf("k" to JsonPrimitive("v"))))
        val original: RpcRequest = RpcRequest(
            messageId = "msg-1",
            timestamp = 123456789L,
            serviceName = "UserService",
            methodId = "getUser",
            parameters = params,
            streaming = false
        )

        val encoded = json.encodeToString(RpcRequest.serializer(), original)
        val decoded = json.decodeFromString(RpcRequest.serializer(), encoded)
        assertEquals(original, decoded)

        // Polymorphic via RpcMessage
        val encodedPoly = json.encodeToString(RpcMessage.serializer(), original)
        val decodedPoly = json.decodeFromString(RpcMessage.serializer(), encodedPoly)
        val asRequest = assertIs<RpcRequest>(decodedPoly)
        assertEquals(original, asRequest)
    }

    @Test
    fun rpcResponse_roundTrip_withResult() {
        val result = buildJsonObject { put("id", 7); put("name", "Alice") }
        val original = RpcResponse(
            messageId = "msg-2",
            timestamp = 987654321L,
            result = result,
            streaming = true
        )
        val encoded = json.encodeToString(RpcResponse.serializer(), original)
        val decoded = json.decodeFromString(RpcResponse.serializer(), encoded)
        assertEquals(original, decoded)

        val encodedPoly = json.encodeToString(RpcMessage.serializer(), original)
        val decodedPoly = json.decodeFromString(RpcMessage.serializer(), encodedPoly)
        val asResponse = assertIs<RpcResponse>(decodedPoly)
        assertEquals(original, asResponse)
    }

    @Test
    fun rpcError_roundTrip_withDetailsNullable() {
        val details = JsonArray(listOf(JsonPrimitive("detail1"), JsonPrimitive("detail2")))
        val withDetails = RpcError(
            messageId = "msg-3",
            timestamp = 1L,
            errorCode = "INVALID_REQUEST",
            errorMessage = "Bad request",
            errorDetails = details
        )
        val noDetails = RpcError(
            messageId = "msg-4",
            timestamp = 2L,
            errorCode = "INTERNAL_ERROR",
            errorMessage = "Oops",
            errorDetails = null
        )

        val roundTrip = json.decodeFromString(RpcError.serializer(), json.encodeToString(RpcError.serializer(), withDetails))
        assertEquals(withDetails, roundTrip)

        val roundTrip2 = json.decodeFromString(RpcError.serializer(), json.encodeToString(RpcError.serializer(), noDetails))
        assertEquals(noDetails, roundTrip2)

        // Polymorphic
        val decodedPoly = json.decodeFromString(RpcMessage.serializer(), json.encodeToString<RpcMessage>(withDetails))
        assertIs<RpcError>(decodedPoly)
    }

    @Test
    fun streamMessages_roundTrip_eachType() {
        val start = StreamStart(
            messageId = "s-1",
            timestamp = 10L,
            streamId = "stream-xyz",
            serviceName = "UserService",
            methodId = "streamUsers",
            parameters = listOf(JsonPrimitive(true))
        )
        val data = StreamData(
            messageId = "s-2",
            timestamp = 11L,
            streamId = "stream-xyz",
            data = JsonPrimitive("payload")
        )
        val end = StreamEnd(
            messageId = "s-3",
            timestamp = 12L,
            streamId = "stream-xyz"
        )
        val error = StreamError(
            messageId = "s-4",
            timestamp = 13L,
            streamId = "stream-xyz",
            errorCode = "STREAM_ERROR",
            errorMessage = "boom",
            errorDetails = JsonPrimitive(123)
        )

        val msgs: List<RpcMessage> = listOf(start, data, end, error)
        val encoded = json.encodeToString(ListSerializer(RpcMessage.serializer()), msgs)
        val decoded = json.decodeFromString(ListSerializer(RpcMessage.serializer()), encoded)
        assertEquals(msgs.size, decoded.size)
        assertIs<StreamStart>(decoded[0])
        assertIs<StreamData>(decoded[1])
        assertIs<StreamEnd>(decoded[2])
        assertIs<StreamError>(decoded[3])
        assertEquals(msgs, decoded)
    }
}
