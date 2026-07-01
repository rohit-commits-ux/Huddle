package com.rohit.chat.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.rohit.chat.data.models.User
import com.rohit.chat.data.repository.AuthRepository
import com.rohit.chat.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<FirebaseUser>?>(null)
    val authState: StateFlow<Resource<FirebaseUser>?> = _authState

    private val _usernameAvailable = MutableStateFlow<Resource<Boolean>?>(null)
    val usernameAvailable: StateFlow<Resource<Boolean>?> = _usernameAvailable

    private val _profileCreated = MutableStateFlow<Resource<Unit>?>(null)
    val profileCreated: StateFlow<Resource<Unit>?> = _profileCreated

    private val _userProfile = MutableStateFlow<Resource<User>?>(null)
    val userProfile: StateFlow<Resource<User>?> = _userProfile

    private val _imageUploadState = MutableStateFlow<Resource<String>?>(null)
    val imageUploadState: StateFlow<Resource<String>?> = _imageUploadState

    fun checkUsername(username: String) {
        if (username.length < 5) {
            _usernameAvailable.value = Resource.Error("Minimum 5 characters required")
            return
        }
        if (!username.any { !it.isLetterOrDigit() }) {
            _usernameAvailable.value = Resource.Error("Must contain at least one special character")
            return
        }
        viewModelScope.launch {
            repository.checkUsername(username).collectLatest {
                _usernameAvailable.value = it
            }
        }
    }

    fun signUp(name: String, username: String, password: String) {
        viewModelScope.launch {
            repository.signUp("", password, name, username).collectLatest {
                _authState.value = it
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            repository.login(username, password).collectLatest {
                _authState.value = it
            }
        }
    }

    fun loginWithCredential(credential: AuthCredential) {
        viewModelScope.launch {
            repository.signInWithCredential(credential).collectLatest {
                _authState.value = it
            }
        }
    }

    fun getUserProfile(userId: String) {
        viewModelScope.launch {
            repository.getUserProfile(userId).collectLatest {
                _userProfile.value = it
            }
        }
    }

    fun createUserProfile(name: String, username: String, photoUrl: String = "") {
        val currentUserId = repository.currentUser?.uid ?: return
        val user = User(
            userId = currentUserId,
            displayName = name,
            username = username,
            photoUrl = photoUrl
        )
        viewModelScope.launch {
            repository.updateUserProfile(user).collectLatest {
                _profileCreated.value = it
            }
        }
    }

    fun updateUserProfile(user: User) {
        viewModelScope.launch {
            repository.updateUserProfile(user).collectLatest {
                _profileCreated.value = it
            }
        }
    }

    fun uploadProfilePicture(file: File) {
        viewModelScope.launch {
            repository.uploadProfilePicture(file).collectLatest {
                _imageUploadState.value = it
            }
        }
    }

    fun resetAuthState() {
        _authState.value = null
        _usernameAvailable.value = null
        _profileCreated.value = null
    }
}
