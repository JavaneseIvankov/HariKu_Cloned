package com.hariku.di

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hariku.core.auth.CurrentUserSession
import com.hariku.core.auth.observeCurrentUserIdFlow
import kotlinx.coroutines.flow.flowOf
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Shared Firebase instances. Feature modules consume these through Koin so
 * each feature does not have to reach for the singletons itself.
 */
val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single {
        val hasFirebase = FirebaseApp.getApps(androidContext()).isNotEmpty()
        if (!hasFirebase) {
            CurrentUserSession(source = flowOf(null), initialUserId = null)
        } else {
            val auth = runCatching { get<FirebaseAuth>() }.getOrNull()
            CurrentUserSession(
                source = observeCurrentUserIdFlow { auth },
                initialUserId = auth?.currentUser?.uid
            )
        }
    }
}
