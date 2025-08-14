package net.tactware.kwire.core.messages

import kotlinx.serialization.Serializable

/**
 * Stream end message indicating completion.
 */
@Serializable
data class StreamEnd(
    override val messageId: String,
    override val timestamp: Long,
    val streamId: String
) : StreamMessage()