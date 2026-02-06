package com.silkfinik.fairsplit.features.expenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.common.util.UiText
import com.silkfinik.fairsplit.core.domain.usecase.expense.GetExpenseHistoryScreenDataUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.expenses.ui.ExpenseHistoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ExpenseHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getExpenseHistoryScreenDataUseCase: GetExpenseHistoryScreenDataUseCase
) : BaseViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])
    private val expenseId: String = checkNotNull(savedStateHandle["expenseId"])

    val uiState: StateFlow<ExpenseHistoryUiState> = getExpenseHistoryScreenDataUseCase(groupId, expenseId)
        .map { data ->
            if (data.currency != null) {
                ExpenseHistoryUiState.Success(
                    history = data.history,
                    members = data.members,
                    currency = data.currency
                )
            } else {
                ExpenseHistoryUiState.Error(UiText.StringResource(R.string.error_group_not_found))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExpenseHistoryUiState.Loading
        )
}