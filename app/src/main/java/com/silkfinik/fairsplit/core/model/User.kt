package com.silkfinik.fairsplit.core.model

data class User(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = true,
    val linkedGhostIds: List<String> = emptyList(),
    val fcmToken: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
