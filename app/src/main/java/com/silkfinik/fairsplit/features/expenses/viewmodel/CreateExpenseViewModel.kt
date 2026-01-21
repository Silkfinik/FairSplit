package com.silkfinik.fairsplit.features.expenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.domain.usecase.expense.GetExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupUseCase
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
    private val getGroupUseCase: GetGroupUseCase,
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
                // Load Group and Members
                val group = getGroupUseCase(groupId).first() ?: throw Exception("Группа не найдена")
                val members = getMembersUseCase(groupId).first()
                
                if (expenseId != null) {
                    // Edit Mode: Load Expense
                    val expense = getExpenseUseCase(expenseId).first()
                    if (expense != null) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                isEditing = true,
                                currency = group.currency,
                                members = members,
                                description = expense.description,
                                amount = expense.amount.toString(),
                                payerId = expense.payers.keys.firstOrNull(), // Assuming single payer for now
                                splitMemberIds = expense.splits.keys
                            ) 
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Трата не найдена", members = members, currency = group.currency) }
                    }
                } else {
                    // Create Mode
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            currency = group.currency,
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
        val error = if (description.isBlank()) "Введите описание" else null
        _uiState.update { it.copy(description = description, descriptionError = error) }
    }

    fun onAmountChange(amount: String) {
        // Allow empty during typing, but validate format
        val doubleVal = amount.toDoubleOrNull()
        val error = if (amount.isNotBlank() && (doubleVal == null || doubleVal <= 0)) "Некорректная сумма" else null
        _uiState.update { it.copy(amount = amount, amountError = error) }
    }

    fun onPayerChange(payerId: String) {
        _uiState.update { it.copy(payerId = payerId, payerError = null) }
    }

    fun onSplitMemberToggle(memberId: String) {
        _uiState.update { state ->
            val currentIds = state.splitMemberIds.toMutableSet()
            if (currentIds.contains(memberId)) {
                currentIds.remove(memberId)
            } else {
                currentIds.add(memberId)
            }
            val error = if (currentIds.isEmpty()) "Выберите хотя бы одного" else null
            state.copy(splitMemberIds = currentIds, splitError = error)
        }
    }

    fun toggleAllMembers(selectAll: Boolean) {
        _uiState.update { state ->
            val newIds = if (selectAll) {
                state.members.map { it.id }.toSet()
            } else {
                emptySet()
            }
            val error = if (newIds.isEmpty()) "Выберите хотя бы одного" else null
            state.copy(splitMemberIds = newIds, splitError = error)
        }
    }

    fun onSaveClick() {
        val currentState = _uiState.value
        
        // Final validation before save
        val descriptionError = if (currentState.description.isBlank()) "Введите описание" else null
        val amountVal = currentState.amount.toDoubleOrNull()
        val amountError = if (amountVal == null || amountVal <= 0) "Введите сумму" else null
        val payerError = if (currentState.payerId == null) "Выберите плательщика" else null
        val splitError = if (currentState.splitMemberIds.isEmpty()) "Выберите, на кого делить" else null

        if (descriptionError != null || amountError != null || payerError != null || splitError != null) {
            _uiState.update { 
                it.copy(
                    descriptionError = descriptionError,
                    amountError = amountError,
                    payerError = payerError,
                    splitError = splitError
                ) 
            }
            return
        }

        val amount = amountVal!! // Safe per check above

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val params = SaveExpenseUseCase.Params(
                groupId = groupId,
                expenseId = expenseId,
                description = currentState.description,
                amount = amount,
                payerId = currentState.payerId!!,
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