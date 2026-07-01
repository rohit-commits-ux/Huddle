package com.rohit.chat.data.repository

import com.rohit.chat.data.models.Chat
import com.rohit.chat.data.models.ChatRequest
import com.rohit.chat.data.models.Message
import com.rohit.chat.data.models.User
import com.rohit.chat.utils.Resource
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ChatRepository {
    // Chat Room Management
    fun getChats(userId: String): Flow<Resource<List<Chat>>>
    fun getChatRoom(chatId: String): Flow<Resource<Chat>>
    fun createChat(participantIds: List<String>): Flow<Resource<String>>
    
    // Messaging
    fun getMessages(chatId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message): Resource<Unit>
    fun uploadImage(file: File): Flow<Resource<String>>
    suspend fun updateMessageStatus(chatId: String, messageId: String, status: String)
    suspend fun editMessage(chatId: String, messageId: String, newContent: String)
    suspend fun deleteMessage(chatId: String, messageId: String, forEveryone: Boolean, currentUserId: String)
    
    // Status & Presence
    suspend fun updateTypingStatus(chatId: String, userId: String, isTyping: Boolean)
    suspend fun resetUnreadCount(chatId: String, userId: String)
    fun getUserPresence(userId: String): Flow<Resource<User>>
    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean)

    // User & Search
    fun searchUsers(username: String): Flow<Resource<List<User>>>
    fun getAllUsers(): Flow<Resource<List<User>>>
    
    // Request System
    fun sendChatRequest(receiver: User): Flow<Resource<Unit>>
    fun addContact(contactId: String): Flow<Resource<Unit>>
    fun getIncomingRequests(userId: String): Flow<Resource<List<ChatRequest>>>
    fun getOutgoingRequests(userId: String): Flow<Resource<List<ChatRequest>>>
    fun updateRequestStatus(request: ChatRequest, status: String): Flow<Resource<Unit>>
}
