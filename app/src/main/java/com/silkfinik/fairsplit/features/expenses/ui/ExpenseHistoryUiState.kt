package com.silkfinik.fairsplit.features.expenses.ui

import com.silkfinik.fairsplit.core.common.util.UiText
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.HistoryItem
import com.silkfinik.fairsplit.core.model.Member

sealed interface ExpenseHistoryUiState {
    data object Loading : ExpenseHistoryUiState
    data class Success(
        val history: List<HistoryItem>,
        val members: Map<String, Member>,
        val currency: Currency
    ) : ExpenseHistoryUiState
    data class Error(val message: UiText) : ExpenseHistoryUiState
}