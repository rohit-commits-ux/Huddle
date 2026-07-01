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
class ContactsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _contactsState = MutableStateFlow<Resource<List<User>>>(Resource.Loading())
    val contactsState: StateFlow<Resource<List<User>>> = _contactsState

    private val _createChatState = MutableStateFlow<Resource<String>?>(null)
    val createChatState: StateFlow<Resource<String>?> = _createChatState

    fun loadAllUsers() {
        viewModelScope.launch {
            _contactsState.value = Resource.Loading()
            chatRepository.getAllUsers().collectLatest { resource ->
                if (resource is Resource.Success<*>) {
                    val currentUserId = authRepository.currentUser?.uid
                    val filteredUsers = (resource.data as? List<User>)?.filter { it.userId != currentUserId } ?: emptyList()
                    _contactsState.value = Resource.Success(filteredUsers)
                } else {
                    _contactsState.value = resource as Resource<List<User>>
                }
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
