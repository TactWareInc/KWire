package net.tactware.kwire.core.messages

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * RPC response message for successful calls.
 */
@Serializable
data class RpcResponse(
    override val messageId: String,
    override val timestamp: Long,
    val result: @Contextual JsonElement? = null,
    val streaming: Boolean = false
) : net.tactware.kwire.core.messages.RpcMessage()