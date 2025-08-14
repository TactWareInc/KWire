package net.tactware.kwire.core.messages

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.serialization.json.JsonElement

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