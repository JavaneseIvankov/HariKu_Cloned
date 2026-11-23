package com.hariku.feature.auth.data.mapper

import com.google.firebase.auth.FirebaseUser
import com.hariku.feature.auth.data.dto.UserDto
import com.hariku.feature.auth.domain.model.AuthUser

object UserMapper {

    /** FirebaseUser is the SDK type; the domain only sees AuthUser. */
    fun fromFirebaseUser(firebaseUser: FirebaseUser, name: String? = null): AuthUser {
        return AuthUser(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            name = name ?: firebaseUser.displayName,
            photoUrl = firebaseUser.photoUrl?.toString()
        )
    }

    fun fromDto(dto: UserDto): AuthUser {
        return AuthUser(
            uid = dto.uid.orEmpty(),
            email = dto.email,
            name = dto.name,
            photoUrl = dto.photoUrl
        )
    }

    fun toDto(authUser: AuthUser): UserDto {
        return UserDto(
            uid = authUser.uid,
            email = authUser.email,
            name = authUser.name,
            photoUrl = authUser.photoUrl
        )
    }

    fun toFirestoreMap(authUser: AuthUser): Map<String, Any?> {
        return hashMapOf(
            "uid" to authUser.uid,
            "email" to authUser.email,
            "name" to authUser.name,
            "photoUrl" to authUser.photoUrl,
            "createdAt" to System.currentTimeMillis()
        )
    }
}
