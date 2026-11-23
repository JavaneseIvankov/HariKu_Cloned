package com.hariku.feature.auth.domain.usecase

import com.hariku.feature.auth.domain.model.AuthUser
import com.hariku.feature.auth.domain.repository.AuthRepository

class GetCurrentUserUseCase(
    private val repository: AuthRepository
) {
    operator fun invoke(): AuthUser? = repository.getCurrentUser()
}
