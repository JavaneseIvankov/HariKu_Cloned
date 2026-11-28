package com.hariku.feature.mood.di

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.hariku.core.auth.CurrentUserSession
import com.hariku.feature.mood.data.local.LocalMoodStore
import com.hariku.feature.mood.data.local.MoodLocalMigrator
import com.hariku.feature.mood.data.local.RoomMoodLocalMigrator
import com.hariku.feature.mood.data.local.SqliteLocalMoodStore
import com.hariku.feature.mood.data.remote.MoodFirebaseService
import com.hariku.feature.mood.data.remote.MoodRemoteDataSource
import com.hariku.feature.mood.data.repository.MoodRepositoryImpl
import com.hariku.feature.mood.domain.repository.MoodRepository
import com.hariku.feature.mood.domain.usecase.DeleteMoodEntryUseCase
import com.hariku.feature.mood.domain.usecase.ObserveMoodEntriesUseCase
import com.hariku.feature.mood.domain.usecase.SaveQuickMoodUseCase
import com.hariku.feature.mood.domain.usecase.UpdateMoodEntryUseCase
import com.hariku.feature.mood.presentation.QuickMoodViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val moodModule = module {
    single<MoodRemoteDataSource> {
        MoodFirebaseService(
            firestore = get(),
            isAuthenticated = {
                get<CurrentUserSession>().currentUserIdOrNull() != null &&
                    runCatching { get<FirebaseAuth>().currentUser != null }.getOrDefault(false)
            }
        )
    }
    single<LocalMoodStore> { SqliteLocalMoodStore(context = androidContext()) }
    single<MoodLocalMigrator> {
        RoomMoodLocalMigrator(
            localStore = get(),
            remoteDataSource = get(),
            onError = { error ->
                Log.w("RoomMoodLocalMigrator", "Gagal migrasi mood lokal ke Firestore", error)
            }
        )
    }
    single<MoodRepository> {
        MoodRepositoryImpl(
            remoteDataSource = get(),
            userIdFlow = get<CurrentUserSession>().userIdFlow,
            localMigrator = get()
        )
    }

    factory { ObserveMoodEntriesUseCase(repository = get()) }
    factory { SaveQuickMoodUseCase(repository = get()) }
    factory { DeleteMoodEntryUseCase(repository = get()) }
    factory { UpdateMoodEntryUseCase(repository = get()) }

    viewModel {
        QuickMoodViewModel(
            observeMoodEntriesUseCase = get(),
            saveQuickMoodUseCase = get(),
            deleteMoodEntryUseCase = get(),
            updateMoodEntryUseCase = get(),
            userIdFlow = get<CurrentUserSession>().userIdFlow
        )
    }
}
