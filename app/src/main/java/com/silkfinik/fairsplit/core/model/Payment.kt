package com.silkfinik.fairsplit.core.model

import com.silkfinik.fairsplit.core.model.enums.PaymentStatus

data class Payment(
    val id: String,
    val groupId: String,
    val payerId: String,
    val receiverId: String,
    val amount: Double,
    val currency: Currency,
    val status: PaymentStatus,
    val createdAt: Long,
    val updatedAt: Long
)
