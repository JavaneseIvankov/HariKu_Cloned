package com.hariku.feature.auth.domain.usecase

import com.hariku.feature.auth.domain.model.AuthUser
import com.hariku.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveAuthStateUseCase(
    private val repository: AuthRepository
) {
    operator fun invoke(): Flow<AuthUser?> = repository.getAuthState()
}
