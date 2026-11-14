package com.hariku

import android.app.Application

class HariKuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Dependency injection is started once the feature modules exist;
        // see the DI wiring commit that follows.
    }
}
