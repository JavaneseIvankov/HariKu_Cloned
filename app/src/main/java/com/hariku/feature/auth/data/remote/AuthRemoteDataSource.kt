package com.hariku.feature.auth.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.hariku.feature.auth.data.dto.UserDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * All Firebase Auth and Firestore access for this feature lives here, so the
 * repository and above never touch the SDK directly.
 */
class AuthRemoteDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val usersCollection = firestore.collection(USERS_COLLECTION)

    suspend fun login(email: String, password: String): FirebaseUser {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        return authResult.user
            ?: throw IllegalStateException("User null setelah login")
    }

    suspend fun signUp(email: String, password: String, name: String): FirebaseUser {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user
            ?: throw IllegalStateException("User null setelah sign up")

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        firebaseUser.updateProfile(profileUpdates).await()

        return firebaseUser
    }

    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    suspend fun getUserFromFirestore(uid: String): UserDto? {
        return try {
            val document = usersCollection.document(uid).get().await()
            document.toObject(UserDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveUserToFirestore(uid: String, userData: Map<String, Any?>) {
        usersCollection.document(uid).set(userData).await()
    }

    fun logout() {
        auth.signOut()
    }

    fun getAuthStateFlow(): Flow<FirebaseUser?> {
        return callbackFlow {
            val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                // Null once the user signs out.
                trySend(firebaseAuth.currentUser)
            }
            auth.addAuthStateListener(authStateListener)
            awaitClose { auth.removeAuthStateListener(authStateListener) }
        }
    }

    companion object {
        // The rules file references the same capitalised collection.
        const val USERS_COLLECTION = "Users"
    }
}
