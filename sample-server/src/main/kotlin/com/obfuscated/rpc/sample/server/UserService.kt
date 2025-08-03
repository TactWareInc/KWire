package com.obfuscated.rpc.sample.server

import com.obfuscated.rpc.core.RpcService
import com.obfuscated.rpc.core.RpcMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

/**
 * User data model
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
 * User service interface with RPC annotations
 */
@RpcService("UserService")
interface UserService {
    
    @RpcMethod("createUser")
    suspend fun createUser(request: CreateUserRequest): User
    
    @RpcMethod("getUserById")
    suspend fun getUserById(userId: String): User?
    
    @RpcMethod("updateUser")
    suspend fun updateUser(request: UpdateUserRequest): User?
    
    @RpcMethod("deleteUser")
    suspend fun deleteUser(userId: String): Boolean
    
    @RpcMethod("getAllUsers")
    suspend fun getAllUsers(): List<User>
    
    @RpcMethod("searchUsers")
    suspend fun searchUsers(criteria: UserSearchCriteria): List<User>
    
    @RpcMethod("getUserStats")
    suspend fun getUserStats(): UserStats
    
    @RpcMethod("streamUsers", streaming = true)
    fun streamUsers(): Flow<User>
    
    @RpcMethod("streamUserUpdates", streaming = true)
    fun streamUserUpdates(userId: String): Flow<User>
}

/**
 * User service implementation
 */
class UserServiceImpl : UserService {
    
    private val users = mutableMapOf<String, User>()
    private var nextId = 1
    
    init {
        // Add some sample users
        val sampleUsers = listOf(
            User("1", "Alice Johnson", "alice@example.com", 28),
            User("2", "Bob Smith", "bob@example.com", 35),
            User("3", "Carol Davis", "carol@example.com", 42),
            User("4", "David Wilson", "david@example.com", 31),
            User("5", "Eve Brown", "eve@example.com", 26)
        )
        
        sampleUsers.forEach { user ->
            users[user.id] = user
        }
        nextId = 6
    }
    
    override suspend fun createUser(request: CreateUserRequest): User {
        val user = User(
            id = nextId++.toString(),
            name = request.name,
            email = request.email,
            age = request.age
        )
        users[user.id] = user
        return user
    }
    
    override suspend fun getUserById(userId: String): User? {
        return users[userId]
    }
    
    override suspend fun updateUser(request: UpdateUserRequest): User? {
        val existingUser = users[request.id] ?: return null
        
        val updatedUser = existingUser.copy(
            name = request.name ?: existingUser.name,
            email = request.email ?: existingUser.email,
            age = request.age ?: existingUser.age,
            isActive = request.isActive ?: existingUser.isActive
        )
        
        users[request.id] = updatedUser
        return updatedUser
    }
    
    override suspend fun deleteUser(userId: String): Boolean {
        return users.remove(userId) != null
    }
    
    override suspend fun getAllUsers(): List<User> {
        return users.values.toList()
    }
    
    override suspend fun searchUsers(criteria: UserSearchCriteria): List<User> {
        return users.values.filter { user ->
            val nameMatch = criteria.namePattern?.let { pattern ->
                user.name.contains(pattern, ignoreCase = true)
            } ?: true
            
            val minAgeMatch = criteria.minAge?.let { minAge ->
                user.age >= minAge
            } ?: true
            
            val maxAgeMatch = criteria.maxAge?.let { maxAge ->
                user.age <= maxAge
            } ?: true
            
            val activeMatch = criteria.isActive?.let { isActive ->
                user.isActive == isActive
            } ?: true
            
            nameMatch && minAgeMatch && maxAgeMatch && activeMatch
        }
    }
    
    override suspend fun getUserStats(): UserStats {
        val allUsers = users.values
        val activeUsers = allUsers.filter { it.isActive }
        val averageAge = if (allUsers.isNotEmpty()) {
            allUsers.map { it.age }.average()
        } else 0.0
        
        return UserStats(
            totalUsers = allUsers.size,
            activeUsers = activeUsers.size,
            averageAge = averageAge,
            timestamp = System.currentTimeMillis()
        )
    }
    
    override fun streamUsers(): Flow<User> = flow {
        users.values.forEach { user ->
            emit(user)
            delay(100) // Simulate streaming delay
        }
    }
    
    override fun streamUserUpdates(userId: String): Flow<User> = flow {
        val user = users[userId]
        if (user != null) {
            // Simulate user updates over time
            repeat(5) { i ->
                val updatedUser = user.copy(
                    name = "${user.name} (Update $i)"
                )
                users[userId] = updatedUser
                emit(updatedUser)
                delay(1000)
            }
        }
    }
}

