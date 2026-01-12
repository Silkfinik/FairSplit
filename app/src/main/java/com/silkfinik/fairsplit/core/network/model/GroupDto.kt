package com.silkfinik.fairsplit.core.network.model

import com.google.firebase.firestore.PropertyName

data class GroupDto(
    val id: String = "",
    val name: String = "",
    val currency: String = "USD",

    @get:PropertyName("owner_id")
    val ownerId: String = "",

    @get:PropertyName("created_at")
    val createdAt: Long = 0,

    @get:PropertyName("updated_at")
    val updatedAt: Long = 0
)