package com.hariku.feature.auth.presentation

/**
 * Pure credential validation, kept separate from the ViewModels so the rules
 * can be unit tested without touching Firebase.
 */
object AuthValidation {

    /** Matches SignUpUseCase so the UI rejects exactly what the use case rejects. */
    const val MIN_PASSWORD_LENGTH = 8

    fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Email tidak boleh kosong"
        !email.contains("@") -> "Format email tidak valid"
        else -> null
    }

    fun validatePassword(password: String): String? = when {
        password.isBlank() -> "Kata sandi tidak boleh kosong"
        password.length < MIN_PASSWORD_LENGTH ->
            "Password minimal $MIN_PASSWORD_LENGTH karakter"
        else -> null
    }

    /** Registration additionally requires a name and a matching confirmation. */
    fun validateRegistration(
        name: String,
        email: String,
        password: String,
        confirmation: String
    ): String? = when {
        name.isBlank() -> "Nama tidak boleh kosong"
        else -> validateEmail(email)
            ?: validatePassword(password)
            ?: if (password != confirmation) "Konfirmasi kata sandi tidak sama" else null
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
