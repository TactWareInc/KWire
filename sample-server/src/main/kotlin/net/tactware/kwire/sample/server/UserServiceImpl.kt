package net.tactware.kwire.sample.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tactware.kwire.sample.api.CreateUserRequest
import net.tactware.kwire.sample.api.User
import net.tactware.kwire.sample.api.UserService
import net.tactware.kwire.sample.api.UserStats
import kotlin.random.Random
import kotlin.random.nextInt

class UserServiceImpl : UserService {

    private val _users = MutableStateFlow(emptyList<User>())

    val users: Flow<List<User>> get() = _users.asStateFlow()

    private val _userStats = MutableStateFlow(UserStats(totalUsers = 0, activeUsers = 0, averageAge = 21.2, ))
    val userStats: Flow<UserStats> get() = _userStats.asStateFlow()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                // Simulate user activity
                kotlinx.coroutines.delay(Random.nextLong(1000, 50000))
                if (_users.value.isNotEmpty()) {
                    val updatedUsers = _users.value.map { user ->
                        if (Random.nextBoolean()) {
                            user.copy(isActive = !user.isActive)
                        } else {
                            user
                        }
                    }
                    _users.value = updatedUsers
                    // Update stats
                    val totalUsers = _users.value.size
                    val activeUsers = _users.value.count { it.isActive }
                    val averageAge = Random.nextInt(18..65).toDouble() // Simulate average age
                    _userStats.value = UserStats(totalUsers, activeUsers, averageAge)
                }
            }
        }
    }

    override suspend fun createUser(request: CreateUserRequest): User {
        val newUser = User(
            id = "user_${(_users.value.size + 1)}",
            name = request.name,
            email = request.email,
            age = request.age,
            isActive = true
        )
        _users.value += newUser
        // Update stats
        val totalUsers = _users.value.size
        val activeUsers = _users.value.count { it.isActive }
        val averageAge = if (totalUsers > 0) _users.value.map { it.age }.average() else 0.0
        _userStats.value = UserStats(totalUsers, activeUsers, averageAge)
        return newUser
    }

    override suspend fun getUserById(id: String): User? {
        return _users.value.find { it.id == id }
    }

    override suspend fun getAllUsers(): List<User> {
        return _users.value
    }

    override suspend fun getUserStats(): UserStats {
        return _userStats.value
    }

    override fun streamUsers(): Flow<List<User>> {
        return users
    }

    override fun streamUsersStats(): Flow<UserStats> {
       return userStats
    }
}