package com.silkfinik.fairsplit.core.model

data class Member(
    val id: String,
    val groupId: String,
    val name: String,
    val isGhost: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
