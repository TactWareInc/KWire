package net.tactware.kwire.core

import kotlinx.coroutines.flow.Flow
import net.tactware.kwire.core.messages.RpcMessage

/**
 * Abstract transport layer for RPC communication.
 * This provides a pluggable interface for different transport mechanisms.
 */
interface RpcTransport {
    /**
     * Send a message through the transport.
     */
    suspend fun send(message: RpcMessage)
    
    /**
     * Receive messages from the transport as a Flow.
     */
    fun receive(): Flow<RpcMessage>
    
    /**
     * Connect to the remote endpoint.
     * Returns a result describing the outcome.
     */
    suspend fun connect(): RpcConnectResult
    
    /**
     * Disconnect from the remote endpoint.
     */
    suspend fun disconnect()
    
    /**
     * Check if the transport is currently connected.
     */
    val isConnected: Boolean
}

/**
 * Result of attempting to connect a transport.
 */
sealed class RpcConnectResult {
    /** Successfully established a new connection. */
    data object Connected : RpcConnectResult()

    /** A connection already existed; no new connection was made. */
    data object AlreadyConnected : RpcConnectResult()

    /** The connection attempt failed with a categorized reason and optional cause. */
    data class Failed(
        val reason: ConnectFailureReason,
        val cause: Throwable? = null
    ) : RpcConnectResult()
}

/**
 * Categories for connection failures.
 */
enum class ConnectFailureReason {
    TIMEOUT,
    UNAUTHORIZED,
    NETWORK,
    UNKNOWN
}

/**
 * Configuration for RPC transport.
 */
interface RpcTransportConfig {
    /**
     * Connection timeout in milliseconds.
     */
    val connectionTimeout: Long
    
    /**
     * Request timeout in milliseconds.
     */
    val requestTimeout: Long
    
    /**
     * Maximum number of retry attempts.
     */
    val maxRetries: Int
    
    /**
     * Whether to enable automatic reconnection.
     */
    val autoReconnect: Boolean
}

/**
 * Default transport configuration.
 */
data class DefaultTransportConfig(
    override val connectionTimeout: Long = 30_000,
    override val requestTimeout: Long = 60_000,
    override val maxRetries: Int = 3,
    override val autoReconnect: Boolean = true
) : net.tactware.kwire.core.RpcTransportConfig

/**
 * Transport factory interface for creating transport instances.
 */
interface RpcTransportFactory {
    /**
     * Create a new transport instance with the given configuration.
     */
    fun create(config: net.tactware.kwire.core.RpcTransportConfig): net.tactware.kwire.core.RpcTransport
}

/**
 * Event listener for transport lifecycle events.
 */
interface RpcTransportListener {
    /**
     * Called when the transport is connected.
     */
    suspend fun onConnected() {}
    
    /**
     * Called when the transport is disconnected.
     */
    suspend fun onDisconnected() {}
    
    /**
     * Called when a connection error occurs.
     */
    suspend fun onError(error: Throwable) {}
    
    /**
     * Called when a message is sent.
     */
    suspend fun onMessageSent(message: RpcMessage) {}
    
    /**
     * Called when a message is received.
     */
    suspend fun onMessageReceived(message: RpcMessage) {}
}

