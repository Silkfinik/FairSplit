package com.silkfinik.fairsplit.features.expenses.ui

import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory

data class CreateExpenseUiState(
    val isLoading: Boolean = false,
    val description: String = "",
    val amount: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val currency: Currency = Currency.RUB,
    val members: List<Member> = emptyList(),
    val payerId: String? = null,
    val currentUserId: String? = null,
    val selectedSplitMemberIds: Set<String> = emptySet(),
    val splits: Map<String, Double> = emptyMap(),
    val error: String? = null,
    val descriptionError: String? = null,
    val amountError: String? = null,
    val payerError: String? = null,
    val splitError: String? = null,
    val isEditing: Boolean = false,
    val isSaved: Boolean = false
) {
    val isValid: Boolean
        get() = description.isNotBlank() && 
                amount.toDoubleOrNull()?.let { it > 0 } == true &&
                payerId != null && 
                splits.isNotEmpty() &&
                descriptionError == null && amountError == null
}
