package com.rohit.chat.data.models

data class Status(
    val statusId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImage: String = "",
    val mediaUrl: String = "",
    val text: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L
)
