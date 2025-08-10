package com.test

import com.obfuscated.rpc.core.RpcService
import com.obfuscated.rpc.core.RpcMethod
import kotlinx.coroutines.flow.Flow

@RpcService("TestService")
interface TestService {
    @RpcMethod("hello")
    suspend fun hello(name: String): String
    
    @RpcMethod("streamNumbers")
    fun streamNumbers(): Flow<Int>
}
