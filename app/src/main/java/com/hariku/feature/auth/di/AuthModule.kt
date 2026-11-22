package com.hariku.feature.auth.di

import com.hariku.feature.auth.presentation.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    viewModel { AuthViewModel(firebaseAuth = get()) }
}
