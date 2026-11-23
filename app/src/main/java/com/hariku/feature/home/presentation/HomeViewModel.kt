package com.hariku.feature.home.presentation

import androidx.lifecycle.ViewModel
import com.hariku.feature.auth.domain.model.AuthUser
import com.hariku.feature.auth.domain.usecase.GetCurrentUserUseCase

class HomeViewModel(getCurrentUserUseCase: GetCurrentUserUseCase) : ViewModel() {
    val currentUser: AuthUser? = getCurrentUserUseCase()
}
