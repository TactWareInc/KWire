package net.tactware.kwire.core

/**
 * Common error codes used in RPC communication.
 */
object RpcErrorCodes {
    const val INVALID_REQUEST = "INVALID_REQUEST"
    const val METHOD_NOT_FOUND = "METHOD_NOT_FOUND"
    const val SERVICE_NOT_FOUND = "SERVICE_NOT_FOUND"
    const val INVALID_PARAMETERS = "INVALID_PARAMETERS"
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
    const val SERIALIZATION_ERROR = "SERIALIZATION_ERROR"
    const val STREAM_ERROR = "STREAM_ERROR"
    const val TIMEOUT_ERROR = "TIMEOUT_ERROR"
    const val AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR"
    const val AUTHORIZATION_ERROR = "AUTHORIZATION_ERROR"
}
