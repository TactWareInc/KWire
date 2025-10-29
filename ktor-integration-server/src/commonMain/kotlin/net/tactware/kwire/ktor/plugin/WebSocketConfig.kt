package net.tactware.kwire.ktor.plugin

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * WebSocket configuration
 */
class WebSocketConfig {
    var pingPeriod: Duration = 15.seconds
    var timeout: Duration = 60.seconds
    var maxFrameSize: Long = 1024 * 1024 // 1MB
}
