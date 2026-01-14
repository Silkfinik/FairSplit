package com.silkfinik.fairsplit.core.network.model

import com.google.firebase.firestore.PropertyName

data class ExpenseDto(
    val id: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val date: Long = 0,
    
    @get:PropertyName("creator_id")
    @set:PropertyName("creator_id")
    var creatorId: String = "",
    
    val payers: Map<String, Double> = emptyMap(),
    val splits: Map<String, Double> = emptyMap(),
    val category: String? = null,
    
    @get:PropertyName("is_deleted")
    @set:PropertyName("is_deleted")
    var isDeleted: Boolean = false,
    
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = 0,
    
    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Long = 0
)
