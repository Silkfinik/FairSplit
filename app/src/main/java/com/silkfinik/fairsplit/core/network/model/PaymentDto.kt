package com.silkfinik.fairsplit.core.network.model

import com.google.firebase.firestore.PropertyName

data class PaymentDto(
    val id: String = "",
    
    @get:PropertyName("payer_id")
    @set:PropertyName("payer_id")
    var payerId: String = "",
    
    @get:PropertyName("receiver_id")
    @set:PropertyName("receiver_id")
    var receiverId: String = "",
    
    val amount: Double = 0.0,
    val currency: String = "USD",
    val status: String = "PENDING",
    
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = 0,
    
    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Long = 0
)
