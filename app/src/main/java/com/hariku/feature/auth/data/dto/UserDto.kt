package com.hariku.feature.auth.data.dto

/**
 * User document as stored in Firestore.
 * Fields are nullable because stored documents may be incomplete.
 */
data class UserDto(
    val uid: String? = null,
    val email: String? = null,
    val name: String? = null,
    val photoUrl: String? = null,
    val createdAt: Long? = null
)
