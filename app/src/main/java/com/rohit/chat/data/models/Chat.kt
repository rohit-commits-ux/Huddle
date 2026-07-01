package com.rohit.chat.data.models

data class Chat(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Map<String, Int> = emptyMap(),
    val typingStatus: Map<String, Boolean> = emptyMap(),
    val pinnedBy: List<String> = emptyList(),
    val archivedBy: List<String> = emptyList(),
    val mutedBy: Map<String, Long> = emptyMap() // userId to expiry timestamp
)
