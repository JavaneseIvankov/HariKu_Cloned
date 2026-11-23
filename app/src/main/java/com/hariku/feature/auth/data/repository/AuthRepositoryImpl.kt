package com.hariku.feature.auth.data.repository

import com.google.firebase.auth.FirebaseUser
import com.hariku.feature.auth.data.mapper.UserMapper
import com.hariku.feature.auth.data.remote.AuthRemoteDataSource
import com.hariku.feature.auth.domain.model.AuthUser
import com.hariku.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Translates between the Firebase SDK types and the domain model, and makes
 * sure a user document exists for every account that signs up.
 */
class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        return try {
            val firebaseUser = remoteDataSource.login(email, password)
            val userDto = remoteDataSource.getUserFromFirestore(firebaseUser.uid)
            val authUser = if (userDto != null) {
                UserMapper.fromDto(userDto)
            } else {
                UserMapper.fromFirebaseUser(firebaseUser)
            }
            Result.success(authUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): Result<AuthUser> {
        return try {
            val firebaseUser = remoteDataSource.signUp(email, password, name)

            val authUser = AuthUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                name = name,
                photoUrl = null
            )

            val userMap = UserMapper.toFirestoreMap(authUser)
            remoteDataSource.saveUserToFirestore(firebaseUser.uid, userMap)

            Result.success(authUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): AuthUser? {
        val firebaseUser = remoteDataSource.getCurrentFirebaseUser() ?: return null
        return UserMapper.fromFirebaseUser(firebaseUser)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAuthState(): Flow<AuthUser?> {
        return remoteDataSource.getAuthStateFlow()
            .flatMapLatest { firebaseUser ->
                if (firebaseUser == null) {
                    flowOf(null)
                } else {
                    flowOf(fetchUserFromFirestoreOrFallback(firebaseUser))
                }
            }
    }

    override fun logout() {
        remoteDataSource.logout()
    }

    private suspend fun fetchUserFromFirestoreOrFallback(firebaseUser: FirebaseUser): AuthUser {
        val userDto = remoteDataSource.getUserFromFirestore(firebaseUser.uid)
        return if (userDto != null) {
            UserMapper.fromDto(userDto)
        } else {
            UserMapper.fromFirebaseUser(firebaseUser)
        }
    }
}
