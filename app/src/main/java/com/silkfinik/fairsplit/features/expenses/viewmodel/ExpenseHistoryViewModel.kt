package com.silkfinik.fairsplit.features.expenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.usecase.expense.GetExpenseHistoryUseCase
import com.silkfinik.fairsplit.core.model.HistoryItem
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ExpenseHistoryUiState {
    data object Loading : ExpenseHistoryUiState
    data class Success(val history: List<HistoryItem>) : ExpenseHistoryUiState
    data class Error(val message: String) : ExpenseHistoryUiState
}

@HiltViewModel
class ExpenseHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExpenseHistoryUseCase: GetExpenseHistoryUseCase
) : BaseViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])
    private val expenseId: String = checkNotNull(savedStateHandle["expenseId"])

    private val _uiState = MutableStateFlow<ExpenseHistoryUiState>(ExpenseHistoryUiState.Loading)
    val uiState: StateFlow<ExpenseHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = ExpenseHistoryUiState.Loading
            when (val result = getExpenseHistoryUseCase(groupId, expenseId)) {
                is Result.Success -> {
                    _uiState.value = ExpenseHistoryUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = ExpenseHistoryUiState.Error(result.message)
                }
            }
        }
    }
}
