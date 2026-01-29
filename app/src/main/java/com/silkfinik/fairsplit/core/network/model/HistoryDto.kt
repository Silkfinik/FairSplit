package com.silkfinik.fairsplit.core.network.model

import com.google.firebase.firestore.PropertyName

data class HistoryDto(
    val action: String = "",
    val changes: Map<String, Any> = emptyMap(),
    val timestamp: Any? = null,
    
    @get:PropertyName("is_math_valid")
    @set:PropertyName("is_math_valid")
    var isMathValid: Boolean = true
)
