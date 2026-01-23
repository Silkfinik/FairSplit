package com.silkfinik.fairsplit.core.network.model

import com.google.firebase.firestore.PropertyName

data class UserProfileDto(
    @get:PropertyName("display_name")
    @set:PropertyName("display_name")
    var displayName: String = "",
    
    @get:PropertyName("photo_url")
    @set:PropertyName("photo_url")
    var photoUrl: String? = null
)
