package com.silkfinik.fairsplit.core.model

data class Member(
    val id: String,
    val groupId: String,
    val name: String,
    val photoUrl: String? = null,
    val isGhost: Boolean,
    val mergedWithUid: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
