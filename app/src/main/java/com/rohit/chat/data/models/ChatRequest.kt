package com.rohit.chat.data.models

data class ChatRequest(
    val requestId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderName: String = "",
    val senderUsername: String = "",
    val senderPhotoUrl: String = "",
    val status: String = "pending", // pending, accepted, declined
    val createdAt: Long = System.currentTimeMillis()
)
