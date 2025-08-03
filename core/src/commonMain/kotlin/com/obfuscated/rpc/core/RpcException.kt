package com.obfuscated.rpc.core

import kotlinx.serialization.Contextual
import kotlinx.serialization.json.JsonElement

/**
 * Base exception for all RPC-related errors.
 */
open class RpcException(
    message: String,
    cause: Throwable? = null,
    val errorCode: String = RpcErrorCodes.INTERNAL_ERROR,
    val errorDetails: JsonElement? = null
) : Exception(message, cause)

/**
 * Exception thrown when a requested service is not found.
 */
class ServiceNotFoundException(
    serviceName: String,
    cause: Throwable? = null
) : RpcException(
    message = "Service not found: $serviceName",
    cause = cause,
    errorCode = RpcErrorCodes.SERVICE_NOT_FOUND
)

/**
 * Exception thrown when a requested method is not found.
 */
class MethodNotFoundException(
    serviceName: String,
    methodId: String,
    cause: Throwable? = null
) : RpcException(
    message = "Method not found: $serviceName.$methodId",
    cause = cause,
    errorCode = RpcErrorCodes.METHOD_NOT_FOUND
)

/**
 * Exception thrown when method parameters are invalid.
 */
class InvalidParametersException(
    message: String,
    cause: Throwable? = null,
    errorDetails: JsonElement? = null
) : RpcException(
    message = message,
    cause = cause,
    errorCode = RpcErrorCodes.INVALID_PARAMETERS,
    errorDetails = errorDetails
)

/**
 * Exception thrown when serialization/deserialization fails.
 */
class SerializationException(
    message: String,
    cause: Throwable? = null,
    errorDetails: JsonElement? = null
) : RpcException(
    message = message,
    cause = cause,
    errorCode = RpcErrorCodes.SERIALIZATION_ERROR,
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
) : RpcException(
    message = message,
    cause = cause,
    errorCode = RpcErrorCodes.STREAM_ERROR,
    errorDetails = errorDetails
)

/**
 * Exception thrown when RPC calls timeout.
 */
class TimeoutException(
    message: String,
    cause: Throwable? = null
) : RpcException(
    message = message,
    cause = cause,
    errorCode = RpcErrorCodes.TIMEOUT_ERROR
)

/**
 * Exception thrown when authentication fails.
 */
class AuthenticationException(
    message: String,
    cause: Throwable? = null
) : RpcException(
    message = message,
    cause = cause,
    errorCode = RpcErrorCodes.AUTHENTICATION_ERROR
)

/**
 * Exception thrown when authorization fails.
 */
class AuthorizationException(
    message: String,
    cause: Throwable? = null
) : RpcException(
    message = message,
    cause = cause,
    errorCode = RpcErrorCodes.AUTHORIZATION_ERROR
)

