package com.silkfinik.fairsplit.core.network.model

import com.google.firebase.firestore.PropertyName

data class UserDto(
    val uid: String = "",
    val email: String? = null,
    
    @get:PropertyName("display_name")
    @set:PropertyName("display_name")
    var displayName: String? = null,
    
    @get:PropertyName("photo_url")
    @set:PropertyName("photo_url")
    var photoUrl: String? = null,
    
    @get:PropertyName("is_anonymous")
    @set:PropertyName("is_anonymous")
    var isAnonymous: Boolean = true,
    
    @get:PropertyName("linked_ghost_ids")
    @set:PropertyName("linked_ghost_ids")
    var linkedGhostIds: List<String> = emptyList(),
    
    @get:PropertyName("fcm_token")
    @set:PropertyName("fcm_token")
    var fcmToken: String? = null,
    
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = 0,
    
    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Long = 0
)
