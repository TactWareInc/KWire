package net.tactware.kwire.sample.api

import kotlinx.serialization.Serializable

// User-related data classes
@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val age: Int,
    val isActive: Boolean = true
)

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String,
    val age: Int
)

@Serializable
data class UpdateUserRequest(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val age: Int? = null
)

@Serializable
data class UserSearchCriteria(
    val query: String,
    val includeInactive: Boolean = false,
    val maxResults: Int = 100
)

@Serializable
data class UserStats(
    val totalUsers: Int,
    val activeUsers: Int,
    val averageAge: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class UserActivity(
    val userId: String,
    val action: String,
    val timestamp: Long,
    val details: String
)

// Notification-related data classes
@Serializable
data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: String = "info"
)

@Serializable
data class NotificationRequest(
    val userId: String,
    val title: String,
    val message: String,
    val type: String = "info"
)
