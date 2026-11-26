package com.hariku.feature.home.di

import com.hariku.feature.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    viewModel {
        HomeViewModel(
            observeAuthStateUseCase = get(),
            getCurrentUserUseCase = get()
        )
    }
}
