package com.hariku

import android.app.Application
import com.hariku.di.firebaseModule
import com.hariku.feature.chatbot.di.chatbotModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class HariKuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HariKuApp)
            modules(firebaseModule, chatbotModule)
        }
    }
}
