package net.tactware.kwire.sample.api


import kotlinx.coroutines.flow.Flow
import net.tactware.kwire.core.RpcMethod
import net.tactware.kwire.core.RpcService

/**
 * Simple UserService interface for basic RPC examples.
 * For comprehensive examples, see ComprehensiveInterfaces.kt
 */
@RpcService("UserService")
interface UserService {
    @RpcMethod("createUser")
    suspend fun createUser(request: CreateUserRequest): User
    
    @RpcMethod("getUserById")
    suspend fun getUserById(id: String): User?
    
    @RpcMethod("getAllUsers")
    suspend fun getAllUsers(): List<User>
    
    @RpcMethod("getUserStats")
    suspend fun getUserStats(): UserStats
    
    @RpcMethod("streamUsers")
    fun streamUsers(): Flow<List<User>>
}


