package com.hariku.feature.auth.domain.usecase

import com.hariku.feature.auth.domain.model.AuthUser
import com.hariku.feature.auth.domain.repository.AuthRepository

class LoginUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthUser> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email dan kata sandi harus diisi"))
        }
        return repository.login(email.trim(), password)
    }
}
