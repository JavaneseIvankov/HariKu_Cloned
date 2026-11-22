package com.hariku.feature.auth.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun validCredentialsPass() {
        assertNull(AuthValidation.validate("user@example.com", "secret123"))
    }

    @Test
    fun blankEmailIsRejected() {
        assertEquals("Email tidak boleh kosong", AuthValidation.validate("   ", "secret123"))
    }

    @Test
    fun emailWithoutAtSignIsRejected() {
        assertEquals("Format email tidak valid", AuthValidation.validate("userexample.com", "secret123"))
    }

    @Test
    fun shortPasswordIsRejected() {
        assertEquals("Password minimal 6 karakter", AuthValidation.validate("user@example.com", "12345"))
    }

    @Test
    fun sixCharacterPasswordIsAccepted() {
        assertNull(AuthValidation.validate("user@example.com", "123456"))
    }

    @Test
    fun emailIsTrimmedBeforeValidation() {
        assertNull(AuthValidation.validate("  user@example.com  ", "secret123"))
    }

    @Test
    fun weakPasswordErrorIsExplained() {
        assertEquals(
            "Password terlalu lemah, gunakan minimal 6 karakter",
            AuthValidation.describe("FirebaseAuthWeakPasswordException", null)
        )
    }

    @Test
    fun duplicateEmailErrorIsExplained() {
        assertEquals(
            "Email ini sudah terdaftar",
            AuthValidation.describe("FirebaseAuthUserCollisionException", null)
        )
    }

    @Test
    fun wrongCredentialsErrorIsExplained() {
        assertEquals(
            "Email atau password salah",
            AuthValidation.describe("FirebaseAuthInvalidCredentialsException", null)
        )
    }

    @Test
    fun unknownErrorFallsBackToSuppliedMessage() {
        assertEquals("network down", AuthValidation.describe("IOException", "network down"))
    }

    @Test
    fun unknownErrorWithoutMessageUsesDefault() {
        assertEquals("Terjadi kesalahan, coba lagi", AuthValidation.describe("Other", null))
    }
}
