package com.rohit.chat.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.rohit.chat.data.remote.CloudinaryApi
import com.rohit.chat.data.models.User
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

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val cloudinaryApi: CloudinaryApi
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override fun checkUsername(username: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val result = firestore.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            emit(Resource.Success(result.isEmpty))
        } catch (e: Exception) {
            val errorMessage = if (e.localizedMessage?.contains("PERMISSION_DENIED") == true) {
                "Permission Denied: Ensure Firestore rules allow public read for username check."
            } else {
                e.localizedMessage ?: "An error occurred"
            }
            emit(Resource.Error(errorMessage))
        }
    }

    override fun signUp(email: String, password: String, name: String, username: String): Flow<Resource<FirebaseUser>> = flow {
        emit(Resource.Loading())
        try {
            val internalEmail = email.ifEmpty { "$username@huddle.app" }
            val result = auth.createUserWithEmailAndPassword(internalEmail, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val user = User(
                    userId = firebaseUser.uid,
                    displayName = name,
                    username = username,
                    email = internalEmail
                )
                firestore.collection("users").document(firebaseUser.uid).set(user).await()
                emit(Resource.Success(firebaseUser))
            } else {
                emit(Resource.Error("Registration failed"))
            }
        } catch (e: Exception) {
            val errorMessage = if (e.localizedMessage?.contains("operation is not allowed") == true) {
                "Registration Error: Email/Password provider is disabled in Firebase Console."
            } else {
                e.localizedMessage ?: "An error occurred"
            }
            emit(Resource.Error(errorMessage))
        }
    }

    override fun login(username: String, password: String): Flow<Resource<FirebaseUser>> = flow {
        emit(Resource.Loading())
        try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                emit(Resource.Error("User ID not found"))
                return@flow
            }
            
            val email = querySnapshot.documents[0].getString("email") ?: "$username@huddle.app"
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                emit(Resource.Success(firebaseUser))
            } else {
                emit(Resource.Error("Login failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Invalid User ID or password"))
        }
    }

    override fun signInWithCredential(credential: AuthCredential): Flow<Resource<FirebaseUser>> = flow {
        emit(Resource.Loading())
        try {
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                emit(Resource.Success(firebaseUser))
            } else {
                emit(Resource.Error("Sign in failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An error occurred"))
        }
    }

    override fun logout() {
        auth.signOut()
    }

    override fun getUserProfile(userId: String): Flow<Resource<User>> = callbackFlow {
        val subscription = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Error fetching profile"))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    if (user != null) {
                        trySend(Resource.Success(user))
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun updateUserProfile(user: User): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("users").document(user.userId).set(user).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An error occurred"))
        }
    }

    override fun uploadProfilePicture(file: File): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val cloudName = Constants.CLOUDINARY_CLOUD_NAME
            val apiKey = Constants.CLOUDINARY_API_KEY
            val apiSecret = Constants.CLOUDINARY_API_SECRET
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            
            // Signature: sha1("timestamp=" + timestamp + apiSecret)
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
}
