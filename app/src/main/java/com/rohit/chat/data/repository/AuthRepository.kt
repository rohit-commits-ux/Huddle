package com.rohit.chat.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.rohit.chat.data.models.User
import com.rohit.chat.utils.Resource
import java.io.File
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: FirebaseUser?
    fun checkUsername(username: String): Flow<Resource<Boolean>>
    fun signUp(email: String, password: String, name: String, username: String): Flow<Resource<FirebaseUser>>
    fun login(username: String, password: String): Flow<Resource<FirebaseUser>>
    fun signInWithCredential(credential: AuthCredential): Flow<Resource<FirebaseUser>>
    fun logout()
    fun getUserProfile(userId: String): Flow<Resource<User>>
    fun updateUserProfile(user: User): Flow<Resource<Unit>>
    fun uploadProfilePicture(file: File): Flow<Resource<String>>
}
