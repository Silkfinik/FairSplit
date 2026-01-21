package com.silkfinik.fairsplit.features.expenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.domain.usecase.expense.GetExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.GetMembersUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.SaveExpenseUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.expenses.ui.CreateExpenseUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMembersUseCase: GetMembersUseCase,
    private val getExpenseUseCase: GetExpenseUseCase,
    private val saveExpenseUseCase: SaveExpenseUseCase
) : BaseViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])
    private val expenseId: String? = savedStateHandle["expenseId"]

    private val _uiState = MutableStateFlow(CreateExpenseUiState())
    val uiState: StateFlow<CreateExpenseUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Load Members first
                val members = getMembersUseCase(groupId).first()
                
                if (expenseId != null) {
                    // Edit Mode: Load Expense
                    val expense = getExpenseUseCase(expenseId).first()
                    if (expense != null) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                isEditing = true,
                                members = members,
                                description = expense.description,
                                amount = expense.amount.toString(),
                                payerId = expense.payers.keys.firstOrNull(), // Assuming single payer for now
                                splitMemberIds = expense.splits.keys
                            ) 
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Трата не найдена", members = members) }
                    }
                } else {
                    // Create Mode
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            members = members,
                            payerId = it.payerId ?: members.firstOrNull()?.id, // Default to first member
                            splitMemberIds = if (it.splitMemberIds.isEmpty()) members.map { m -> m.id }.toSet() else it.splitMemberIds
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onAmountChange(amount: String) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun onPayerChange(payerId: String) {
        _uiState.update { it.copy(payerId = payerId) }
    }

    fun onSplitMemberToggle(memberId: String) {
        _uiState.update { state ->
            val currentIds = state.splitMemberIds.toMutableSet()
            if (currentIds.contains(memberId)) {
                currentIds.remove(memberId)
            } else {
                currentIds.add(memberId)
            }
            state.copy(splitMemberIds = currentIds)
        }
    }

    fun toggleAllMembers(selectAll: Boolean) {
        _uiState.update { state ->
            if (selectAll) {
                state.copy(splitMemberIds = state.members.map { it.id }.toSet())
            } else {
                state.copy(splitMemberIds = emptySet())
            }
        }
    }

    fun onSaveClick() {
        val currentState = _uiState.value
        val amount = currentState.amount.toDoubleOrNull()

        if (currentState.description.isBlank()) {
            sendEvent(UiEvent.ShowSnackbar("Введите описание"))
            return
        }
        if (amount == null || amount <= 0) {
            sendEvent(UiEvent.ShowSnackbar("Введите корректную сумму"))
            return
        }
        if (currentState.payerId == null) {
            sendEvent(UiEvent.ShowSnackbar("Выберите плательщика"))
            return
        }
        if (currentState.splitMemberIds.isEmpty()) {
            sendEvent(UiEvent.ShowSnackbar("Выберите, на кого делить"))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val params = SaveExpenseUseCase.Params(
                groupId = groupId,
                expenseId = expenseId,
                description = currentState.description,
                amount = amount,
                payerId = currentState.payerId,
                splitMemberIds = currentState.splitMemberIds
            )
            
            val result = saveExpenseUseCase(params)

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                    sendEvent(UiEvent.NavigateBack)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(result.message))
                }
            }
        }
    }
}