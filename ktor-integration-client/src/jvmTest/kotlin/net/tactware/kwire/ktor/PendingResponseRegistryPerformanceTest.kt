package net.tactware.kwire.ktor

import kotlinx.coroutines.runBlocking
import net.tactware.kwire.core.messages.RpcResponse
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PendingResponseRegistryPerformanceTest {

    @Test
    fun dispatchesOneMillionResponsesWithinBudget() = runBlocking {
        val registry = PendingResponseRegistry()
        val totalMessages = 1_000_000
        val maxDurationMs = System.getProperty("kwire.performance.maxMillionDispatchMs")
            ?.toLongOrNull()
            ?: 20_000L

        var checksum = 0L
        val elapsedMs = measureTimeMillis {
            repeat(totalMessages) { index ->
                val messageId = "msg-$index"
                val deferred = registry.register(messageId)
                registry.complete(
                    RpcResponse(
                        messageId = messageId,
                        timestamp = index.toLong()
                    )
                )
                val response = deferred.await()
                checksum += response.timestamp
            }
        }
        println("PendingResponseRegistry processed $totalMessages messages in ${elapsedMs}ms")

        assertTrue(checksum > 0L, "Checksum should prove responses were processed")
        assertEquals(0, registry.size(), "Registry should be empty after processing")
        assertTrue(
            elapsedMs <= maxDurationMs,
            "1,000,000 dispatches took ${elapsedMs}ms, exceeding budget ${maxDurationMs}ms. " +
                "Increase -Dkwire.performance.maxMillionDispatchMs if this machine is slower."
        )
    }
}
