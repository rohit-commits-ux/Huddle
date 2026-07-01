package com.rohit.chat.data.models

data class User(
    val userId: String = "", // Firebase UID
    val displayName: String = "",
    val username: String = "", // Unique User ID
    val photoUrl: String = "",
    val email: String = "", // Internal mapping
    val about: String = "Hey there! I am using Huddle.",
    val online: Boolean = false,
    val lastSeen: Long = 0L,
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
