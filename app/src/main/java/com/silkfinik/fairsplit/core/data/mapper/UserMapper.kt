package com.silkfinik.fairsplit.core.data.mapper

import com.silkfinik.fairsplit.core.model.User
import com.silkfinik.fairsplit.core.network.model.UserDto

fun UserDto.asDomainModel(): User {
    return User(
        id = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        isAnonymous = isAnonymous,
        linkedGhostIds = linkedGhostIds ?: emptyList(),
        fcmToken = fcmToken,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun User.asDto(): UserDto {
    return UserDto(
        uid = id,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        isAnonymous = isAnonymous,
        linkedGhostIds = linkedGhostIds,
        fcmToken = fcmToken,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
