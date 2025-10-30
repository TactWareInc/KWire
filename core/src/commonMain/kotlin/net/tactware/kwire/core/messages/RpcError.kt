package net.tactware.kwire.core.messages

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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