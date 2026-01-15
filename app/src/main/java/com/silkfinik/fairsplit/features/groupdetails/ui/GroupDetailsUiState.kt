package com.silkfinik.fairsplit.features.groupdetails.ui

import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.Group

sealed interface GroupDetailsUiState {
    data object Loading : GroupDetailsUiState
    data class Success(
        val group: Group,
        val expenses: List<Expense>,
        val currentUserId: String? = null
    ) : GroupDetailsUiState
    data class Error(val message: String) : GroupDetailsUiState
}
