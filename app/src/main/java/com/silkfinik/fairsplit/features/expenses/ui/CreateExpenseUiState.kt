package com.silkfinik.fairsplit.features.expenses.ui

import com.silkfinik.fairsplit.core.model.Member

data class CreateExpenseUiState(
    val isLoading: Boolean = false,
    val description: String = "",
    val amount: String = "",
    val members: List<Member> = emptyList(),
    val payerId: String? = null,
    val splitMemberIds: Set<String> = emptySet(),
    val error: String? = null,
    val isSaved: Boolean = false
)
