package com.silkfinik.fairsplit.core.network.model

import com.google.firebase.firestore.PropertyName

data class GroupDto(
    val id: String = "",
    val name: String = "",
    val currency: String = "USD",

    @get:PropertyName("owner_id")
    @set:PropertyName("owner_id")
    var ownerId: String = "",

    val members: List<String> = emptyList(),
    val ghosts: Map<String, GhostDto> = emptyMap(),

    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = 0,

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Long = 0
)