package net.tactware.kwire.core.messages

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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