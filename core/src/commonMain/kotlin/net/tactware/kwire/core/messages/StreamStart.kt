package net.tactware.kwire.core.messages


import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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