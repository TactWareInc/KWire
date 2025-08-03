package com.obfuscated.rpc.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.serialization.json.JsonElement

/**
 * Base class for all RPC messages.
 */
@Serializable
sealed class RpcMessage {
    abstract val messageId: String
    abstract val timestamp: Long
}

/**
 * RPC request message.
 */
@Serializable
data class RpcRequest(
    override val messageId: String,
    override val timestamp: Long,
    val serviceName: String,
    val methodId: String,
    val parameters: List<@Contextual JsonElement> = emptyList(),
    val streaming: Boolean = false
) : RpcMessage()

/**
 * RPC response message for successful calls.
 */
@Serializable
data class RpcResponse(
    override val messageId: String,
    override val timestamp: Long,
    val result: @Contextual JsonElement? = null,
    val streaming: Boolean = false
) : RpcMessage()

/**
 * RPC error response message.
 */
@Serializable
data class RpcError(
    override val messageId: String,
    override val timestamp: Long,
    val errorCode: String,
    val errorMessage: String,
    val errorDetails: @Contextual JsonElement? = null
) : RpcMessage()

/**
 * Stream-specific messages for Flow handling.
 */
@Serializable
sealed class StreamMessage : RpcMessage()

/**
 * Stream start message.
 */
@Serializable
data class StreamStart(
    override val messageId: String,
    override val timestamp: Long,
    val streamId: String,
    val serviceName: String,
    val methodId: String,
    val parameters: List<@Contextual JsonElement> = emptyList()
) : StreamMessage()

/**
 * Stream data message containing a single item.
 */
@Serializable
data class StreamData(
    override val messageId: String,
    override val timestamp: Long,
    val streamId: String,
    val data: @Contextual JsonElement
) : StreamMessage()

/**
 * Stream end message indicating completion.
 */
@Serializable
data class StreamEnd(
    override val messageId: String,
    override val timestamp: Long,
    val streamId: String
) : StreamMessage()

/**
 * Stream error message.
 */
@Serializable
data class StreamError(
    override val messageId: String,
    override val timestamp: Long,
    val streamId: String,
    val errorCode: String,
    val errorMessage: String,
    val errorDetails: @Contextual JsonElement? = null
) : StreamMessage()

/**
 * Common error codes used in RPC communication.
 */
object RpcErrorCodes {
    const val INVALID_REQUEST = "INVALID_REQUEST"
    const val METHOD_NOT_FOUND = "METHOD_NOT_FOUND"
    const val SERVICE_NOT_FOUND = "SERVICE_NOT_FOUND"
    const val INVALID_PARAMETERS = "INVALID_PARAMETERS"
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
    const val SERIALIZATION_ERROR = "SERIALIZATION_ERROR"
    const val STREAM_ERROR = "STREAM_ERROR"
    const val TIMEOUT_ERROR = "TIMEOUT_ERROR"
    const val AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR"
    const val AUTHORIZATION_ERROR = "AUTHORIZATION_ERROR"
}
