package com.obfuscated.rpc.sample.client

import com.obfuscated.rpc.runtime.RpcClient
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable

/**
 * User data model (shared with server)
 */
@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val age: Int,
    val isActive: Boolean = true
)

/**
 * User creation request
 */
@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String,
    val age: Int
)

/**
 * User update request
 */
@Serializable
data class UpdateUserRequest(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val age: Int? = null,
    val isActive: Boolean? = null
)

/**
 * User search criteria
 */
@Serializable
data class UserSearchCriteria(
    val namePattern: String? = null,
    val minAge: Int? = null,
    val maxAge: Int? = null,
    val isActive: Boolean? = null
)

/**
 * User statistics
 */
@Serializable
data class UserStats(
    val totalUsers: Int,
    val activeUsers: Int,
    val averageAge: Double,
    val timestamp: Long
)

/**
 * User service client wrapper
 */
class UserServiceClient(private val rpcClient: RpcClient) {
    
    private val serviceName = "UserService"
    
    suspend fun createUser(request: CreateUserRequest): User {
        return rpcClient.call(
            serviceName = serviceName,
            methodId = "createUser",
            parameters = listOf(request),
            resultSerializer = User.serializer()
        )
    }
    
    suspend fun getUserById(userId: String): User? {
        return rpcClient.call(
            serviceName = serviceName,
            methodId = "getUserById",
            parameters = listOf(userId),
            resultSerializer = User.serializer().nullable
        )
    }
    
    suspend fun updateUser(request: UpdateUserRequest): User? {
        return rpcClient.call(
            serviceName = serviceName,
            methodId = "updateUser",
            parameters = listOf(request),
            resultSerializer = User.serializer().nullable
        )
    }
    
    suspend fun deleteUser(userId: String): Boolean {
        return rpcClient.call(
            serviceName = serviceName,
            methodId = "deleteUser",
            parameters = listOf(userId),
            resultSerializer = Boolean.serializer()
        )
    }
    
    suspend fun getAllUsers(): List<User> {
        return rpcClient.call(
            serviceName = serviceName,
            methodId = "getAllUsers",
            parameters = emptyList(),
            resultSerializer = ListSerializer(User.serializer())
        )
    }
    
    suspend fun searchUsers(criteria: UserSearchCriteria): List<User> {
        return rpcClient.call(
            serviceName = serviceName,
            methodId = "searchUsers",
            parameters = listOf(criteria),
            resultSerializer = ListSerializer(User.serializer())
        )
    }
    
    suspend fun getUserStats(): UserStats {
        return rpcClient.call(
            serviceName = serviceName,
            methodId = "getUserStats",
            parameters = emptyList(),
            resultSerializer = UserStats.serializer()
        )
    }
    
    fun streamUsers(): Flow<User> {
        return rpcClient.stream(
            serviceName = serviceName,
            methodId = "streamUsers",
            parameters = emptyList(),
            resultSerializer = User.serializer()
        )
    }
    
    fun streamUserUpdates(userId: String): Flow<User> {
        return rpcClient.stream(
            serviceName = serviceName,
            methodId = "streamUserUpdates",
            parameters = listOf(userId),
            resultSerializer = User.serializer()
        )
    }
}

