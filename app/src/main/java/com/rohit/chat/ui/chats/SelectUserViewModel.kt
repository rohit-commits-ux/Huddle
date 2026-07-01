package com.rohit.chat.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.chat.data.models.User
import com.rohit.chat.data.repository.AuthRepository
import com.rohit.chat.data.repository.ChatRepository
import com.rohit.chat.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectUserViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _usersState = MutableStateFlow<Resource<List<User>>>(Resource.Loading())
    val usersState: StateFlow<Resource<List<User>>> = _usersState

    private val _createChatState = MutableStateFlow<Resource<String>?>(null)
    val createChatState: StateFlow<Resource<String>?> = _createChatState

    private val _addContactState = MutableStateFlow<Resource<Unit>?>(null)
    val addContactState: StateFlow<Resource<Unit>?> = _addContactState

    init {
        getUsers()
    }

    fun getUsers() {
        viewModelScope.launch {
            chatRepository.getAllUsers().collectLatest { resource ->
                if (resource is Resource.Success<*>) {
                    val currentUserId = authRepository.currentUser?.uid
                    val filteredUsers = (resource.data as? List<User>)?.filter { it.userId != currentUserId } ?: emptyList()
                    _usersState.value = Resource.Success(filteredUsers)
                } else {
                    _usersState.value = resource as Resource<List<User>>
                }
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isEmpty()) {
            getUsers()
            return
        }
        viewModelScope.launch {
            chatRepository.searchUsers(query).collectLatest { resource ->
                if (resource is Resource.Success<*>) {
                    val currentUserId = authRepository.currentUser?.uid
                    val filteredUsers = (resource.data as? List<User>)?.filter { it.userId != currentUserId } ?: emptyList()
                    _usersState.value = Resource.Success(filteredUsers)
                } else {
                    _usersState.value = resource as Resource<List<User>>
                }
            }
        }
    }

    fun addContact(contactId: String) {
        viewModelScope.launch {
            chatRepository.addContact(contactId).collectLatest {
                _addContactState.value = it
            }
        }
    }

    fun createChat(participantId: String) {
        val currentUserId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.createChat(listOf(currentUserId, participantId)).collectLatest {
                _createChatState.value = it
            }
        }
    }
}