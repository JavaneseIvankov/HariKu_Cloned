package com.hariku.feature.auth.presentation

/**
 * Pure credential validation, kept separate from AuthViewModel so the rules
 * can be unit tested without touching Firebase.
 */
object AuthValidation {

    const val MIN_PASSWORD_LENGTH = 6

    /** Returns a user-facing problem, or null when the input is acceptable. */
    fun validate(email: String, password: String): String? = when {
        email.isBlank() -> "Email tidak boleh kosong"
        !email.trim().contains("@") -> "Format email tidak valid"
        password.length < MIN_PASSWORD_LENGTH ->
            "Password minimal $MIN_PASSWORD_LENGTH karakter"
        else -> null
    }

    /** Maps a Firebase auth failure to a readable Indonesian message. */
    fun describe(errorClass: String?, fallback: String?): String = when (errorClass) {
        "FirebaseAuthWeakPasswordException" ->
            "Password terlalu lemah, gunakan minimal $MIN_PASSWORD_LENGTH karakter"
        "FirebaseAuthUserCollisionException" -> "Email ini sudah terdaftar"
        "FirebaseAuthInvalidCredentialsException" -> "Email atau password salah"
        else -> fallback ?: "Terjadi kesalahan, coba lagi"
    }
}
