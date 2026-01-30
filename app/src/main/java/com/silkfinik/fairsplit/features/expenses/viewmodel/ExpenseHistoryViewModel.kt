package com.silkfinik.fairsplit.features.expenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.domain.usecase.expense.GetExpenseHistoryUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.GetMembersUseCase
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.HistoryItem
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ExpenseHistoryUiState {
    data object Loading : ExpenseHistoryUiState
    data class Success(
        val history: List<HistoryItem>,
        val members: Map<String, Member>,
        val currency: Currency
    ) : ExpenseHistoryUiState
    data class Error(val message: String) : ExpenseHistoryUiState
}

@HiltViewModel
class ExpenseHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExpenseHistoryUseCase: GetExpenseHistoryUseCase,
    private val getMembersUseCase: GetMembersUseCase,
    private val getGroupUseCase: GetGroupUseCase
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
            combine(
                getExpenseHistoryUseCase(groupId, expenseId),
                getMembersUseCase(groupId),
                getGroupUseCase(groupId)
            ) { history, members, group ->
                if (group != null) {
                    ExpenseHistoryUiState.Success(
                        history = history,
                        members = members.associateBy { it.id },
                        currency = group.currency
                    )
                } else {
                    ExpenseHistoryUiState.Error("Group not found")
                }
            }
            .catch { e ->
                _uiState.value = ExpenseHistoryUiState.Error(e.message ?: "Unknown error")
            }
            .collect { state ->
                _uiState.value = state
            }
        }
    }
}
