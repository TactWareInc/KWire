package net.tactware.kwire.ktor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.tactware.kwire.core.messages.RpcResponse

/**
 * Tracks in-flight RPC responses keyed by message id.
 */
internal class PendingResponseRegistry {
    private val mutex = Mutex()
    private val pending = mutableMapOf<String, CompletableDeferred<RpcResponse>>()

    suspend fun register(messageId: String): CompletableDeferred<RpcResponse> {
        val deferred = CompletableDeferred<RpcResponse>()
        mutex.withLock {
            pending[messageId] = deferred
        }
        return deferred
    }

    suspend fun complete(response: RpcResponse): Boolean {
        val deferred = mutex.withLock {
            pending.remove(response.messageId)
        } ?: return false
        deferred.complete(response)
        return true
    }

    suspend fun remove(messageId: String) {
        mutex.withLock {
            pending.remove(messageId)
        }
    }

    suspend fun failAll(cause: Throwable) {
        val toFail = mutex.withLock {
            val snapshot = pending.values.toList()
            pending.clear()
            snapshot
        }
        toFail.forEach { deferred ->
            deferred.completeExceptionally(cause)
        }
    }

    suspend fun size(): Int {
        return mutex.withLock { pending.size }
    }
}
