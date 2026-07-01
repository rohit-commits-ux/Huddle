package com.rohit.chat.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.chat.data.models.Chat
import com.rohit.chat.data.models.ChatRequest
import com.rohit.chat.data.models.Message
import com.rohit.chat.data.models.User
import com.rohit.chat.data.repository.ChatRepository
import com.rohit.chat.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _chats = MutableStateFlow<Resource<List<Chat>>>(Resource.Loading())
    val chats: StateFlow<Resource<List<Chat>>> = _chats

    private val _currentChat = MutableStateFlow<Resource<Chat>?>(null)
    val currentChat: StateFlow<Resource<Chat>?> = _currentChat

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _imageUploadState = MutableStateFlow<Resource<String>?>(null)
    val imageUploadState: StateFlow<Resource<String>?> = _imageUploadState

    private val _otherUserPresence = MutableStateFlow<Resource<User>?>(null)
    val otherUserPresence: StateFlow<Resource<User>?> = _otherUserPresence

    private val _searchResults = MutableStateFlow<Resource<List<User>>>(Resource.Success(emptyList()))
    val searchResults: StateFlow<Resource<List<User>>> = _searchResults

    private val _requestStatus = MutableStateFlow<Resource<Unit>?>(null)
    val requestStatus: StateFlow<Resource<Unit>?> = _requestStatus

    private val _incomingRequests = MutableStateFlow<Resource<List<ChatRequest>>>(Resource.Loading())
    val incomingRequests: StateFlow<Resource<List<ChatRequest>>> = _incomingRequests

    private val _outgoingRequests = MutableStateFlow<Resource<List<ChatRequest>>>(Resource.Loading())
    val outgoingRequests: StateFlow<Resource<List<ChatRequest>>> = _outgoingRequests

    fun getChats(userId: String) {
        viewModelScope.launch {
            repository.getChats(userId).collectLatest {
                _chats.value = it
            }
        }
    }

    fun observeChatRoom(chatId: String) {
        viewModelScope.launch {
            repository.getChatRoom(chatId).collectLatest {
                _currentChat.value = it
            }
        }
    }

    fun getMessages(chatId: String) {
        viewModelScope.launch {
            repository.getMessages(chatId).collectLatest {
                _messages.value = it
            }
        }
    }

    fun sendMessage(message: Message) {
        viewModelScope.launch {
            repository.sendMessage(message)
        }
    }

    fun uploadImage(file: java.io.File) {
        viewModelScope.launch {
            repository.uploadImage(file).collectLatest {
                _imageUploadState.value = it
            }
        }
    }

    fun resetImageUploadState() {
        _imageUploadState.value = null
    }

    fun updateMessageStatus(chatId: String, messageId: String, status: String) {
        viewModelScope.launch {
            repository.updateMessageStatus(chatId, messageId, status)
        }
    }

    fun editMessage(chatId: String, messageId: String, content: String) {
        viewModelScope.launch {
            repository.editMessage(chatId, messageId, content)
        }
    }

    fun deleteMessage(chatId: String, messageId: String, forEveryone: Boolean, currentUserId: String) {
        viewModelScope.launch {
            repository.deleteMessage(chatId, messageId, forEveryone, currentUserId)
        }
    }

    fun setTyping(chatId: String, userId: String, isTyping: Boolean) {
        viewModelScope.launch {
            repository.updateTypingStatus(chatId, userId, isTyping)
        }
    }

    fun resetUnread(chatId: String, userId: String) {
        viewModelScope.launch {
            repository.resetUnreadCount(chatId, userId)
        }
    }

    fun observeUserPresence(userId: String) {
        viewModelScope.launch {
            repository.getUserPresence(userId).collectLatest {
                _otherUserPresence.value = it
            }
        }
    }

    fun updateOnlineStatus(userId: String, isOnline: Boolean) {
        viewModelScope.launch {
            repository.updateUserOnlineStatus(userId, isOnline)
        }
    }

    // Requests
    fun searchUsers(username: String) {
        viewModelScope.launch {
            repository.searchUsers(username).collectLatest {
                _searchResults.value = it
            }
        }
    }

    fun sendChatRequest(receiver: User) {
        viewModelScope.launch {
            repository.sendChatRequest(receiver).collectLatest {
                _requestStatus.value = it
            }
        }
    }

    fun getIncomingRequests(userId: String) {
        viewModelScope.launch {
            repository.getIncomingRequests(userId).collectLatest {
                _incomingRequests.value = it
            }
        }
    }

    fun getOutgoingRequests(userId: String) {
        viewModelScope.launch {
            repository.getOutgoingRequests(userId).collectLatest {
                _outgoingRequests.value = it
            }
        }
    }

    fun updateRequestStatus(request: ChatRequest, status: String) {
        viewModelScope.launch {
            repository.updateRequestStatus(request, status).collectLatest { }
        }
    }
}
