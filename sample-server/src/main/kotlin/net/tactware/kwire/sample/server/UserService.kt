package net.tactware.kwire.sample.server

import com.obfuscated.rpc.sample.api.CreateUserRequest
import com.obfuscated.rpc.sample.api.User
import com.obfuscated.rpc.sample.api.UserService
import com.obfuscated.rpc.sample.api.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


/**
 * Simple UserService implementation for basic examples
 */
class UserServiceImpl : UserService {
    private val users = mutableMapOf<String, User>()
    
    init {
        // Pre-populate with test data
        users["1"] = User("1", "Alice", "alice@example.com", 25)
        users["2"] = User("2", "Bob", "bob@example.com", 30)
        users["3"] = User("3", "Charlie", "charlie@example.com", 35)
    }
    
    override suspend fun createUser(request: CreateUserRequest): User {
        val user = User(
            id = (users.size + 1).toString(),
            name = request.name,
            email = request.email,
            age = request.age
        )
        users[user.id] = user
        return user
    }
    
    override suspend fun getUserById(id: String): User? {
        return users[id]
    }
    
    override suspend fun getAllUsers(): List<User> {
        return users.values.toList()
    }
    
    override suspend fun getUserStats(): UserStats {
        return UserStats(
            totalUsers = users.size,
            activeUsers = users.values.count { it.isActive },
            averageAge = users.values.map { it.age }.average()
        )
    }
    
    override fun streamUsers(): Flow<List<User>> = flow {
        emit(users.values.toList())
    }
}

