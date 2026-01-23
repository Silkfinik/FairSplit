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
import kotlin.math.abs

import com.silkfinik.fairsplit.core.domain.repository.AuthRepository

@HiltViewModel
class CreateExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupUseCase: GetGroupUseCase,
    private val getMembersUseCase: GetMembersUseCase,
    private val getExpenseUseCase: GetExpenseUseCase,
    private val saveExpenseUseCase: SaveExpenseUseCase,
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])
    private val expenseId: String? = savedStateHandle["expenseId"]

    private val _uiState = MutableStateFlow(CreateExpenseUiState())
    val uiState: StateFlow<CreateExpenseUiState> = _uiState.asStateFlow()

    // Internal state to track selected members for splitting, separate from the calculated amounts
    private var selectedSplitMemberIds: Set<String> = emptySet()

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
                val currentUserId = authRepository.getUserId()
                
                if (expenseId != null) {
                    // Edit Mode: Load Expense
                    val expense = getExpenseUseCase(expenseId).first()
                    if (expense != null) {
                        selectedSplitMemberIds = expense.splits.keys
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                isEditing = true,
                                currency = group.currency,
                                members = members,
                                description = expense.description,
                                amount = expense.amount.toString(),
                                payerId = expense.payers.keys.firstOrNull(), // Assuming single payer for now
                                splits = expense.splits
                            ) 
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Трата не найдена", members = members, currency = group.currency, currentUserId = currentUserId) }
                    }
                } else {
                    // Create Mode
                    // Default: select all members
                    selectedSplitMemberIds = members.map { it.id }.toSet()
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            currency = group.currency,
                            members = members,
                            currentUserId = currentUserId,
                            payerId = it.payerId ?: members.firstOrNull()?.id // Default to first member
                        ) 
                    }
                    recalculateSplits()
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
        // Allow empty during typing
        val doubleVal = amount.toDoubleOrNull()
        val error = if (amount.isNotBlank() && (doubleVal == null || doubleVal <= 0)) "Некорректная сумма" else null
        
        _uiState.update { it.copy(amount = amount, amountError = error) }
        recalculateSplits()
    }

    fun onPayerChange(payerId: String) {
        _uiState.update { it.copy(payerId = payerId, payerError = null) }
    }

    fun onSplitMemberToggle(memberId: String) {
        val currentIds = selectedSplitMemberIds.toMutableSet()
        if (currentIds.contains(memberId)) {
            currentIds.remove(memberId)
        } else {
            currentIds.add(memberId)
        }
        selectedSplitMemberIds = currentIds
        
        recalculateSplits()
    }

    fun toggleAllMembers(selectAll: Boolean) {
        selectedSplitMemberIds = if (selectAll) {
            _uiState.value.members.map { it.id }.toSet()
        } else {
            emptySet()
        }
        recalculateSplits()
    }

    private fun recalculateSplits() {
        val amount = _uiState.value.amount.toDoubleOrNull() ?: 0.0
        val count = selectedSplitMemberIds.size
        
        val newSplits = if (count > 0 && amount > 0) {
            val splitAmount = amount / count
            // Simple equal split
            selectedSplitMemberIds.associateWith { splitAmount }
        } else {
            emptyMap()
        }
        
        val splitError = if (selectedSplitMemberIds.isEmpty()) "Выберите хотя бы одного" else null
        
        _uiState.update { it.copy(splits = newSplits, splitError = splitError) }
    }

    fun onSaveClick() {
        val currentState = _uiState.value
        
        // Final validation before save
        val descriptionError = if (currentState.description.isBlank()) "Введите описание" else null
        val amountVal = currentState.amount.toDoubleOrNull()
        val amountError = if (amountVal == null || amountVal <= 0) "Введите сумму" else null
        val payerError = if (currentState.payerId == null) "Выберите плательщика" else null
        val splitError = if (currentState.splits.isEmpty()) "Выберите, на кого делить" else null

        // STRICT VALIDATION: Sum(Splits) == Amount
        val totalSplit = currentState.splits.values.sum()
        val difference = if (amountVal != null) abs(amountVal - totalSplit) else 0.0
        val balanceError = if (difference > 0.01) "Сумма сплита не совпадает с общей суммой" else null

        if (descriptionError != null || amountError != null || payerError != null || splitError != null || balanceError != null) {
            _uiState.update { 
                it.copy(
                    descriptionError = descriptionError,
                    amountError = amountError,
                    payerError = payerError,
                    splitError = splitError ?: balanceError // Show balance error in split section if generic split error is empty
                ) 
            }
            return
        }

        val amount = amountVal!!

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val params = SaveExpenseUseCase.Params(
                groupId = groupId,
                expenseId = expenseId,
                description = currentState.description,
                amount = amount,
                payerId = currentState.payerId!!,
                splits = currentState.splits
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
    
    // Helper to check if a member is selected (since we moved the Set out of UiState for cleaner separation)
    fun isMemberSelected(memberId: String): Boolean {
        return selectedSplitMemberIds.contains(memberId)
    }
}