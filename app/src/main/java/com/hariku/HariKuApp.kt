package com.hariku

import android.app.Application
import com.hariku.di.firebaseModule
import com.hariku.feature.auth.di.authModule
import com.hariku.feature.chatbot.di.chatbotModule
import com.hariku.feature.profile.di.profileModule
import com.hariku.feature.home.di.homeModule
import com.hariku.feature.journal.di.journalModule
import com.hariku.feature.mood.di.moodModule
import com.hariku.feature_statistic.di.statisticModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class HariKuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HariKuApp)
            modules(
                firebaseModule,
                authModule,
                chatbotModule,
                profileModule,
                homeModule,
                moodModule,
                journalModule,
                statisticModule
            )
        }
    }
}
