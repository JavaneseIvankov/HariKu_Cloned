package com.hariku

import android.app.Application
import com.hariku.feature.mood.MoodDatabase
import com.hariku.di.firebaseModule
import com.hariku.feature.auth.di.authModule
import com.hariku.feature.chatbot.di.chatbotModule
import com.hariku.feature.profile.di.profileModule
import com.hariku.feature.home.di.homeModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class HariKuApp : Application() {
    private val moodDatabases = mutableMapOf<String, MoodDatabase>()

    @Synchronized
    fun moodDao(userId: String) =
        moodDatabases.getOrPut(userId) { MoodDatabase.open(this, userId) }.moodDao()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HariKuApp)
            modules(firebaseModule, authModule, chatbotModule, profileModule, homeModule)
        }
    }
}
