package com.hariku.core.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Scope data lokal saat belum ada user login / Firebase belum terkonfigurasi. */
const val LOCAL_GUEST_USER_ID = "local_guest"

fun String?.toScopedUserId(): String = this?.takeIf { it.isNotBlank() } ?: LOCAL_GUEST_USER_ID

/**
 * Stream uid user aktif. Emit null bila belum login atau Firebase tidak tersedia,
 * dan emit ulang setiap kali akun berganti (login / logout / switch account).
 */
fun observeCurrentUserIdFlow(
    authProvider: () -> FirebaseAuth? = { runCatching { FirebaseAuth.getInstance() }.getOrNull() }
): Flow<String?> = callbackFlow {
    val auth = runCatching { authProvider() }.getOrNull()
    if (auth == null) {
        trySend(null)
        close()
        return@callbackFlow
    }
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser?.uid)
    }
    trySend(auth.currentUser?.uid)
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
}.distinctUntilChanged()
