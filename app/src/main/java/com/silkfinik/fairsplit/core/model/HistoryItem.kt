package com.silkfinik.fairsplit.core.model

data class HistoryItem(
    val id: String,
    val action: String,
    val changes: Map<String, Any>,
    val timestamp: Long,
    val isMathValid: Boolean
)
