package com.hariku.feature.auth.domain.model

/**
 * Domain model for an authenticated user.
 * Pure data, independent of Firebase so it can be used by the domain layer.
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val name: String?,
    val photoUrl: String? = null
)
