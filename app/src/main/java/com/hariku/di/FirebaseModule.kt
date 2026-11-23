package com.hariku.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.dsl.module

/**
 * Shared Firebase instances. Feature modules consume these through Koin so
 * each feature does not have to reach for the singletons itself.
 */
val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
}
