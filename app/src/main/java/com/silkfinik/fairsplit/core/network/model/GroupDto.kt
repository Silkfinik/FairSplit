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
    @get:PropertyName("ghosts")
    @set:PropertyName("ghosts")
    var ghosts: Map<String, GhostDto> = emptyMap(),
    
    @get:PropertyName("member_profiles")
    @set:PropertyName("member_profiles")
    var memberProfiles: Map<String, UserProfileDto>? = null,
    
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = 0,

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Long = 0,

    @get:PropertyName("invite_code")
    @set:PropertyName("invite_code")
    var inviteCode: String? = null
)