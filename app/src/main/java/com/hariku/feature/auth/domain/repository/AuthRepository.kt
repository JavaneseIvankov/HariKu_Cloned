package com.hariku.feature.auth.domain.repository

import com.hariku.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

/**
 * Contract for authentication. The data layer implements this; the domain
 * and presentation layers depend only on this interface.
 */
interface AuthRepository {

    suspend fun login(email: String, password: String): Result<AuthUser>

    /**
     * Creates the account, sets the display name and writes the user document.
     */
    suspend fun signUp(email: String, password: String, name: String): Result<AuthUser>

    fun logout()

    fun getCurrentUser(): AuthUser?

    /** Emits on every sign-in and sign-out, including a null user. */
    fun getAuthState(): Flow<AuthUser?>
}
