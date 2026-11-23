package com.hariku.feature.auth.domain.usecase

import com.hariku.feature.auth.domain.repository.AuthRepository

class CheckLoginStatusUseCase(
    private val repository: AuthRepository
) {
    operator fun invoke(): Boolean = repository.getCurrentUser() != null
}
