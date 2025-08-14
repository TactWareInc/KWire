package net.tactware.kwire.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RpcExceptionTest {
    @Test
    fun baseException_hasDefaultInternalError() {
        val ex = RpcException("msg")
        assertEquals(RpcErrorCodes.INTERNAL_ERROR, ex.errorCode)
        assertTrue(ex.message!!.contains("msg"))
    }

    @Test
    fun serviceNotFound_setsProperCodeAndMessage() {
        val ex = ServiceNotFoundException("UserService")
        assertEquals(RpcErrorCodes.SERVICE_NOT_FOUND, ex.errorCode)
        assertTrue(ex.message!!.contains("UserService"))
    }

    @Test
    fun methodNotFound_setsProperCodeAndMessage() {
        val ex = MethodNotFoundException("UserService", "getUser")
        assertEquals(RpcErrorCodes.METHOD_NOT_FOUND, ex.errorCode)
        assertTrue(ex.message!!.contains("UserService.getUser"))
    }

    @Test
    fun invalidParameters_usesInvalidParametersCode() {
        val ex = InvalidParametersException("Invalid arg")
        assertEquals(RpcErrorCodes.INVALID_PARAMETERS, ex.errorCode)
    }

    @Test
    fun serializationException_usesSerializationCode() {
        val ex = SerializationException("Bad json")
        assertEquals(RpcErrorCodes.SERIALIZATION_ERROR, ex.errorCode)
    }

    @Test
    fun streamException_usesStreamErrorCode_andHoldsStreamIdOptional() {
        val ex = StreamException("boom", streamId = "stream-1")
        assertEquals(RpcErrorCodes.STREAM_ERROR, ex.errorCode)
        // streamId is specific to this subclass, ensure property exists and value retained
        assertEquals("stream-1", ex.streamId)
    }

    @Test
    fun timeout_authentication_authorization_codes() {
        assertEquals(RpcErrorCodes.TIMEOUT_ERROR, TimeoutException("t").errorCode)
        assertEquals(RpcErrorCodes.AUTHENTICATION_ERROR, AuthenticationException("a").errorCode)
        assertEquals(RpcErrorCodes.AUTHORIZATION_ERROR, AuthorizationException("a2").errorCode)
    }
}
