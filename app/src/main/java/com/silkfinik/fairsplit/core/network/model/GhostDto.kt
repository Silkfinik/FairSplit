package com.silkfinik.fairsplit.core.network.model

import com.google.firebase.firestore.PropertyName

data class GhostDto(
    val name: String = "",
    @get:PropertyName("is_merged")
    @set:PropertyName("is_merged")
    var isMerged: Boolean = false,
    @get:PropertyName("merged_with_uid")
    @set:PropertyName("merged_with_uid")
    var mergedWithUid: String? = null
)
