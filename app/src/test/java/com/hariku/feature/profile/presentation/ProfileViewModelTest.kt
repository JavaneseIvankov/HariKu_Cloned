package com.hariku.feature.profile.presentation

import com.hariku.feature.auth.domain.model.AuthUser
import com.hariku.feature.auth.domain.repository.AuthRepository
import com.hariku.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.hariku.feature.auth.domain.usecase.LogoutUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ProfileViewModelTest {
    @Test
    fun exposesCurrentUserFromAuthUseCase() {
        val user = AuthUser(uid = "u1", email = "user@example.com", name = "Ayu")
        val repository = FakeAuthRepository(user)
        val viewModel = ProfileViewModel(
            getCurrentUserUseCase = GetCurrentUserUseCase(repository),
            logoutUseCase = LogoutUseCase(repository)
        )

        assertEquals(user, viewModel.currentUser)
    }

    @Test
    fun exposesNullWhenNoUserIsSignedIn() {
        val repository = FakeAuthRepository(null)
        val viewModel = ProfileViewModel(
            getCurrentUserUseCase = GetCurrentUserUseCase(repository),
            logoutUseCase = LogoutUseCase(repository)
        )

        assertNull(viewModel.currentUser)
    }

    @Test
    fun logoutDelegatesToAuthUseCase() {
        val repository = FakeAuthRepository(null)
        val viewModel = ProfileViewModel(
            getCurrentUserUseCase = GetCurrentUserUseCase(repository),
            logoutUseCase = LogoutUseCase(repository)
        )

        viewModel.logout()

        assertEquals(1, repository.logoutCalls)
    }

    private class FakeAuthRepository(private val user: AuthUser?) : AuthRepository {
        var logoutCalls = 0

        override suspend fun login(email: String, password: String) = Result.failure<AuthUser>(NotImplementedError())
        override suspend fun signUp(email: String, password: String, name: String) = Result.failure<AuthUser>(NotImplementedError())
        override fun logout() { logoutCalls++ }
        override fun getCurrentUser(): AuthUser? = user
        override fun getAuthState(): Flow<AuthUser?> = flowOf(user)
    }
}
