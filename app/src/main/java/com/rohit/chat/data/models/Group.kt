package com.rohit.chat.data.models

data class Group(
    val groupId: String = "",
    val groupName: String = "",
    val groupImage: String = "",
    val groupDescription: String = "",
    val admins: List<String> = emptyList(),
    val members: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L
)
