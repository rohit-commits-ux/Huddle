package com.rohit.chat.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rohit.chat.data.database.MessageDao
import com.rohit.chat.data.models.Chat
import com.rohit.chat.data.models.ChatRequest
import com.rohit.chat.data.models.Message
import com.rohit.chat.data.models.User
import com.rohit.chat.data.remote.CloudinaryApi
import com.rohit.chat.utils.Constants
import com.rohit.chat.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val messageDao: MessageDao,
    private val cloudinaryApi: CloudinaryApi
) : ChatRepository {

    private val currentUserId get() = auth.currentUser?.uid ?: ""

    override fun getChats(userId: String): Flow<Resource<List<Chat>>> = callbackFlow {
        val subscription = firestore.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Error fetching chats"))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.toObjects(Chat::class.java)
                    trySend(Resource.Success(chats))
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getChatRoom(chatId: String): Flow<Resource<Chat>> = callbackFlow {
        val subscription = firestore.collection("chats").document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Error"))
                    return@addSnapshotListener
                }
                snapshot?.toObject(Chat::class.java)?.let {
                    trySend(Resource.Success(it))
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        val subscription = firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java)
                    
                    // Mark messages as read
                    messages.filter { it.senderId != currentUserId && it.status != "read" }.forEach { msg ->
                        firestore.collection("chats").document(chatId)
                            .collection("messages").document(msg.messageId)
                            .update("status", "read")
                    }

                    trySend(messages)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun sendMessage(message: Message): Resource<Unit> {
        return try {
            val chatRef = firestore.collection("chats").document(message.chatId)
            val messageRef = chatRef.collection("messages").document()
            val finalMessage = message.copy(messageId = messageRef.id)
            
            firestore.runBatch { batch ->
                batch.set(messageRef, finalMessage)
                
                val updates = mutableMapOf<String, Any>(
                    "lastMessage" to if (finalMessage.messageType == "image") "📷 Photo" else finalMessage.content,
                    "lastMessageTime" to finalMessage.timestamp
                )
                
                // Increment unread count for receiver
                val receiverId = message.receiverId
                updates["unreadCount.$receiverId"] = FieldValue.increment(1)
                
                batch.update(chatRef, updates)
            }.await()
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send message")
        }
    }

    override fun uploadImage(file: File): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val cloudName = Constants.CLOUDINARY_CLOUD_NAME
            val apiKey = Constants.CLOUDINARY_API_KEY
            val apiSecret = Constants.CLOUDINARY_API_SECRET
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            
            val signature = sha1("timestamp=$timestamp$apiSecret")
            
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            
            val apiKeyBody = apiKey.toRequestBody("text/plain".toMediaTypeOrNull())
            val timestampBody = timestamp.toRequestBody("text/plain".toMediaTypeOrNull())
            val signatureBody = signature.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = cloudinaryApi.uploadImage(
                cloudName = cloudName,
                file = body,
                apiKey = apiKeyBody,
                timestamp = timestampBody,
                signature = signatureBody
            )
            
            emit(Resource.Success(response.secureUrl))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Upload failed"))
        }
    }

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override suspend fun updateMessageStatus(chatId: String, messageId: String, status: String) {
        try {
            firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update("status", status).await()
        } catch (e: Exception) {}
    }

    override suspend fun editMessage(chatId: String, messageId: String, newContent: String) {
        try {
            firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(mapOf(
                    "content" to newContent,
                    "isEdited" to true
                )).await()
        } catch (e: Exception) {}
    }

    override suspend fun deleteMessage(chatId: String, messageId: String, forEveryone: Boolean, currentUserId: String) {
        try {
            val ref = firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
            if (forEveryone) {
                ref.update("content", "This message was deleted").await()
            } else {
                ref.update("deletedFor", FieldValue.arrayUnion(currentUserId)).await()
            }
        } catch (e: Exception) {}
    }

    override suspend fun updateTypingStatus(chatId: String, userId: String, isTyping: Boolean) {
        try {
            firestore.collection("chats").document(chatId)
                .update("typingStatus.$userId", isTyping).await()
        } catch (e: Exception) {}
    }

    override suspend fun resetUnreadCount(chatId: String, userId: String) {
        try {
            firestore.collection("chats").document(chatId)
                .update("unreadCount.$userId", 0).await()
        } catch (e: Exception) {}
    }

    override fun getUserPresence(userId: String): Flow<Resource<User>> = callbackFlow {
        val subscription = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                snapshot?.toObject(User::class.java)?.let {
                    trySend(Resource.Success(it))
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean) {
        try {
            firestore.collection("users").document(userId)
                .update(mapOf(
                    "online" to isOnline,
                    "lastSeen" to System.currentTimeMillis()
                )).await()
        } catch (e: Exception) {}
    }

    override fun searchUsers(username: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        try {
            val result = firestore.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            val users = result.toObjects(User::class.java).filter { it.userId != currentUserId }
            emit(Resource.Success(users))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Search failed"))
        }
    }

    override fun getAllUsers(): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        try {
            val result = firestore.collection("users").get().await()
            val users = result.toObjects(User::class.java).filter { it.userId != currentUserId }
            emit(Resource.Success(users))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to fetch users"))
        }
    }

    override fun sendChatRequest(receiver: User): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val existing = firestore.collection("chatRequests")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", receiver.userId)
                .get().await()
            
            if (!existing.isEmpty) {
                emit(Resource.Error("Request already sent"))
                return@flow
            }

            val currentUserDoc = firestore.collection("users").document(currentUserId).get().await()
            val currentUser = currentUserDoc.toObject(User::class.java)

            val requestId = firestore.collection("chatRequests").document().id
            val request = ChatRequest(
                requestId = requestId,
                senderId = currentUserId,
                receiverId = receiver.userId,
                senderName = currentUser?.displayName ?: "Unknown",
                senderUsername = currentUser?.username ?: "",
                senderPhotoUrl = currentUser?.photoUrl ?: "",
                status = "pending"
            )
            firestore.collection("chatRequests").document(requestId).set(request).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to send request"))
        }
    }

    override fun addContact(contactId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("users").document(currentUserId)
                .collection("contacts").document(contactId).set(mapOf("addedAt" to System.currentTimeMillis())).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to add contact"))
        }
    }

    override fun createChat(participantIds: List<String>): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val chatId = participantIds.sorted().joinToString("_")
            val existing = firestore.collection("chats").document(chatId).get().await()
            if (!existing.exists()) {
                val chat = Chat(
                    chatId = chatId,
                    participants = participantIds,
                    lastMessage = "Started a new chat",
                    lastMessageTime = System.currentTimeMillis(),
                    unreadCount = participantIds.associateWith { 0 }
                )
                firestore.collection("chats").document(chatId).set(chat).await()
            }
            emit(Resource.Success(chatId))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to create chat"))
        }
    }

    override fun getIncomingRequests(userId: String): Flow<Resource<List<ChatRequest>>> = callbackFlow {
        val subscription = firestore.collection("chatRequests")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    trySend(Resource.Success(snapshot.toObjects(ChatRequest::class.java)))
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getOutgoingRequests(userId: String): Flow<Resource<List<ChatRequest>>> = callbackFlow {
        val subscription = firestore.collection("chatRequests")
            .whereEqualTo("senderId", userId)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    trySend(Resource.Success(snapshot.toObjects(ChatRequest::class.java)))
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun updateRequestStatus(request: ChatRequest, status: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("chatRequests").document(request.requestId)
                .update("status", status).await()
            
            if (status == "accepted") {
                val participantIds = listOf(currentUserId, request.senderId)
                val chatId = participantIds.sorted().joinToString("_")
                
                val chat = Chat(
                    chatId = chatId,
                    participants = participantIds,
                    lastMessage = "Request Accepted",
                    lastMessageTime = System.currentTimeMillis(),
                    unreadCount = participantIds.associateWith { 0 }
                )
                firestore.collection("chats").document(chatId).set(chat).await()
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to update request"))
        }
    }
}
