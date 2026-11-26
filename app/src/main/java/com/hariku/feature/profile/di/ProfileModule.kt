package com.hariku.feature.profile.di

import com.hariku.feature.profile.presentation.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule = module {
    viewModel {
        ProfileViewModel(
            observeAuthStateUseCase = get(),
            getCurrentUserUseCase = get(),
            logoutUseCase = get()
        )
    }
}
