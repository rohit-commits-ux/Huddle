package com.rohit.chat.data.models

import com.google.firebase.firestore.PropertyName

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val messageType: String = "text", // text, image, audio, file
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sent", // sending, sent, delivered, read
    @get:PropertyName("isEdited")
    @set:PropertyName("isEdited")
    var isEdited: Boolean = false,
    val replyTo: Message? = null,
    val starredBy: List<String> = emptyList(),
    val deletedFor: List<String> = emptyList()
)
