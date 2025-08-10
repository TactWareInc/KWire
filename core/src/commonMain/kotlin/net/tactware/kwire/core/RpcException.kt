package net.tactware.kwire.core

import kotlinx.serialization.json.JsonElement

/**
 * Base exception for all RPC-related errors.
 */
open class RpcException(
    message: String,
    cause: Throwable? = null,
    val errorCode: String = _root_ide_package_.net.tactware.kwire.core.RpcErrorCodes.INTERNAL_ERROR,
    val errorDetails: JsonElement? = null
) : Exception(message, cause)

/**
 * Exception thrown when a requested service is not found.
 */
class ServiceNotFoundException(
    serviceName: String,
    cause: Throwable? = null
) : net.tactware.kwire.core.RpcException(
    message = "Service not found: $serviceName",
    cause = cause,
    errorCode = _root_ide_package_.net.tactware.kwire.core.RpcErrorCodes.SERVICE_NOT_FOUND
)

/**
 * Exception thrown when a requested method is not found.
 */
class MethodNotFoundException(
    serviceName: String,
    methodId: String,
    cause: Throwable? = null
) : net.tactware.kwire.core.RpcException(
    message = "Method not found: $serviceName.$methodId",
    cause = cause,
    errorCode = _root_ide_package_.net.tactware.kwire.core.RpcErrorCodes.METHOD_NOT_FOUND
)

/**
 * Exception thrown when method parameters are invalid.
 */
class InvalidParametersException(
    message: String,
    cause: Throwable? = null,
    errorDetails: JsonElement? = null
) : net.tactware.kwire.core.RpcException(
    message = message,
    cause = cause,
    errorCode = _root_ide_package_.net.tactware.kwire.core.RpcErrorCodes.INVALID_PARAMETERS,
    errorDetails = errorDetails
)

/**
 * Exception thrown when serialization/deserialization fails.
 */
class SerializationException(
    message: String,
    cause: Throwable? = null,
    errorDetails: JsonElement? = null
) : net.tactware.kwire.core.RpcException(
    message = message,
    cause = cause,
    errorCode = _root_ide_package_.net.tactware.kwire.core.RpcErrorCodes.SERIALIZATION_ERROR,
    errorDetails = errorDetails
)

/**
 * Exception thrown when stream operations fail.
 */
class StreamException(
    message: String,
    cause: Throwable? = null,
    val streamId: String? = null,
    errorDetails: JsonElement? = null
) : net.tactware.kwire.core.RpcException(
    message = message,
    cause = cause,
    errorCode = _root_ide_package_.net.tactware.kwire.core.RpcErrorCodes.STREAM_ERROR,
    errorDetails = errorDetails
)

/**
 * Exception thrown when RPC calls timeout.
 */
class TimeoutException(
    message: String,
    cause: Throwable? = null
) : net.tactware.kwire.core.RpcException(
    message = message,
    cause = cause,
    errorCode = _root_ide_package_.net.tactware.kwire.core.RpcErrorCodes.TIMEOUT_ERROR
)

/**
 * Exception thrown when authentication fails.
 */
class AuthenticationException(
    message: String,
    cause: Throwable? = null
) : net.tactware.kwire.core.RpcException(
    message = message,
    cause = cause,
    errorCode = _root_ide_package_.net.tactware.kwire.core.RpcErrorCodes.AUTHENTICATION_ERROR
)

/**
 * Exception thrown when authorization fails.
 */
class AuthorizationException(
    message: String,
    cause: Throwable? = null
) : net.tactware.kwire.core.RpcException(
    message = message,
    cause = cause,
    errorCode = _root_ide_package_.net.tactware.kwire.core.RpcErrorCodes.AUTHORIZATION_ERROR
)

