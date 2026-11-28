package com.hariku.feature_statistic.di

import com.hariku.core.auth.CurrentUserSession
import com.hariku.feature_statistic.presentation.StatisticViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val statisticModule = module {
    viewModel {
        StatisticViewModel(
            observeMoodEntriesUseCase = get(),
            userIdFlow = get<CurrentUserSession>().userIdFlow
        )
    }
}
