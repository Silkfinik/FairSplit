package com.silkfinik.fairsplit.core.model

data class Expense(
    val id: String,
    val groupId: String,
    val description: String,
    val amount: Double,
    val currency: Currency,
    val date: Long,
    val creatorId: String,
    val payers: Map<String, Double>,
    val splits: Map<String, Double>,
    val category: String? = null,
    val isDeleted: Boolean = false,
    val isMathValid: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)
