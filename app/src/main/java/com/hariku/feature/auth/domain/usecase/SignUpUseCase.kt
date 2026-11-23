package com.hariku.feature.auth.domain.usecase

import com.hariku.feature.auth.domain.model.AuthUser
import com.hariku.feature.auth.domain.repository.AuthRepository

class SignUpUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String
    ): Result<AuthUser> {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Semua field harus diisi"))
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Password minimal $MIN_PASSWORD_LENGTH karakter")
            )
        }

        return repository.signUp(email.trim(), password, name.trim())
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
