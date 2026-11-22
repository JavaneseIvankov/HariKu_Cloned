package com.hariku.feature.auth.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignedIn: Boolean = false
)

class AuthViewModel(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    var uiState by mutableStateOf(
        AuthUiState(isSignedIn = firebaseAuth.currentUser != null)
    )
        private set

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    fun signIn(email: String, password: String) {
        if (!validate(email, password)) return

        uiState = uiState.copy(isLoading = true, error = null)
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                uiState = if (task.isSuccessful) {
                    AuthUiState(isSignedIn = true)
                } else {
                    AuthUiState(isLoading = false, error = describe(task.exception))
                }
            }
    }

    fun signUp(email: String, password: String) {
        if (!validate(email, password)) return

        uiState = uiState.copy(isLoading = true, error = null)
        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                uiState = if (task.isSuccessful) {
                    AuthUiState(isSignedIn = true)
                } else {
                    AuthUiState(isLoading = false, error = describe(task.exception))
                }
            }
    }

    fun signOut() {
        firebaseAuth.signOut()
        uiState = AuthUiState(isSignedIn = false)
    }

    private fun validate(email: String, password: String): Boolean {
        val problem = when {
            email.isBlank() -> "Email tidak boleh kosong"
            !email.contains("@") -> "Format email tidak valid"
            password.length < 6 -> "Password minimal 6 karakter"
            else -> null
        }
        if (problem != null) {
            uiState = uiState.copy(error = problem, isLoading = false)
            return false
        }
        return true
    }

    private fun describe(error: Throwable?): String = when (error) {
        is FirebaseAuthWeakPasswordException -> "Password terlalu lemah, gunakan minimal 6 karakter"
        is FirebaseAuthUserCollisionException -> "Email ini sudah terdaftar"
        is FirebaseAuthInvalidCredentialsException -> "Email atau password salah"
        else -> error?.message ?: "Terjadi kesalahan, coba lagi"
    }
}
