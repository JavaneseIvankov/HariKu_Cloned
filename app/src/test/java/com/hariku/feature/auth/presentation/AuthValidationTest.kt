package com.hariku.feature.auth.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun validEmailPasses() {
        assertNull(AuthValidation.validateEmail("user@example.com"))
    }

    @Test
    fun blankEmailIsRejected() {
        assertEquals("Email tidak boleh kosong", AuthValidation.validateEmail("   "))
    }

    @Test
    fun emailWithoutAtSignIsRejected() {
        assertEquals("Format email tidak valid", AuthValidation.validateEmail("userexample.com"))
    }

    @Test
    fun passwordShorterThanEightIsRejected() {
        assertEquals("Password minimal 8 karakter", AuthValidation.validatePassword("1234567"))
    }

    @Test
    fun passwordOfExactlyEightIsAccepted() {
        assertNull(AuthValidation.validatePassword("12345678"))
    }

    @Test
    fun blankPasswordIsRejected() {
        assertEquals("Kata sandi tidak boleh kosong", AuthValidation.validatePassword(""))
    }

    @Test
    fun registrationRequiresAName() {
        assertEquals(
            "Nama tidak boleh kosong",
            AuthValidation.validateRegistration("  ", "user@example.com", "12345678", "12345678")
        )
    }

    @Test
    fun registrationRejectsMismatchedConfirmation() {
        assertEquals(
            "Konfirmasi kata sandi tidak sama",
            AuthValidation.validateRegistration("Budi", "user@example.com", "12345678", "87654321")
        )
    }

    @Test
    fun completeRegistrationPasses() {
        assertNull(
            AuthValidation.validateRegistration("Budi", "user@example.com", "12345678", "12345678")
        )
    }

    @Test
    fun registrationStillEnforcesPasswordLength() {
        assertEquals(
            "Password minimal 8 karakter",
            AuthValidation.validateRegistration("Budi", "user@example.com", "123", "123")
        )
    }

    @Test
    fun weakPasswordErrorIsExplained() {
        assertEquals(
            "Password terlalu lemah, gunakan minimal 8 karakter",
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
