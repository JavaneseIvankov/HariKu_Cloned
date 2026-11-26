package com.hariku.core.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

/** Scope data lokal saat belum ada user login / Firebase belum terkonfigurasi. */
const val LOCAL_GUEST_USER_ID = "local_guest"

fun String?.toScopedUserId(): String = this?.takeIf { it.isNotBlank() } ?: LOCAL_GUEST_USER_ID

/**
 * Satu sumber kebenaran sesi user aktif yang di-share di DI agar seluruh
 * repository, provider, dan ViewModel mengamati perubahan login/logout secara serentak.
 */
class CurrentUserSession(
    source: Flow<String?>,
    initialUserId: String? = null,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    val userIdFlow: StateFlow<String?> = source
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, initialUserId)

    fun currentUserIdOrNull(): String? = userIdFlow.value
}

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
