package com.silkfinik.fairsplit.features.expenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.usecase.expense.GetExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.SaveExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.GetMembersUseCase
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory
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

@HiltViewModel
class CreateExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupUseCase: GetGroupUseCase,
    private val getMembersUseCase: GetMembersUseCase,
    private val getExpenseUseCase: GetExpenseUseCase,
    private val saveExpenseUseCase: SaveExpenseUseCase,
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val groupId: String = savedStateHandle["groupId"] ?: ""
    private val expenseId: String? = savedStateHandle["expenseId"]

    private val _uiState = MutableStateFlow(CreateExpenseUiState())
    val uiState: StateFlow<CreateExpenseUiState> = _uiState.asStateFlow()

    init {
        if (groupId.isBlank()) {
            _uiState.update { it.copy(error = "Некорректный ID группы") }
        } else {
            loadData()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val group = getGroupUseCase(groupId).first() ?: throw Exception("Группа не найдена")
                val members = getMembersUseCase(groupId).first()
                val currentUserId = authRepository.getUserId()

                if (expenseId != null) {
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
                                category = ExpenseCategory.fromId(expense.category),
                                payerId = expense.payers.keys.firstOrNull(),
                                splits = expense.splits,
                                selectedSplitMemberIds = expense.splits.keys
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Трата не найдена",
                                members = members,
                                currency = group.currency,
                                currentUserId = currentUserId
                            )
                        }
                    }
                } else {
                    val allMemberIds = members.map { it.id }.toSet()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currency = group.currency,
                            members = members,
                            currentUserId = currentUserId,
                            payerId = it.payerId ?: members.firstOrNull()?.id,
                            selectedSplitMemberIds = allMemberIds
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
        val doubleVal = amount.toDoubleOrNull()
        val error = if (amount.isNotBlank() && (doubleVal == null || doubleVal <= 0)) "Некорректная сумма" else null
        _uiState.update { it.copy(amount = amount, amountError = error) }
        recalculateSplits()
    }
    
    fun onCategoryChange(category: ExpenseCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun onPayerChange(payerId: String) {
        _uiState.update { it.copy(payerId = payerId, payerError = null) }
    }

    fun onSplitMemberToggle(memberId: String) {
        val currentIds = _uiState.value.selectedSplitMemberIds.toMutableSet()
        if (currentIds.contains(memberId)) {
            currentIds.remove(memberId)
        } else {
            currentIds.add(memberId)
        }
        
        _uiState.update { it.copy(selectedSplitMemberIds = currentIds) }
        recalculateSplits()
    }

    fun toggleAllMembers(selectAll: Boolean) {
        val allIds = if (selectAll) {
            _uiState.value.members.map { it.id }.toSet()
        } else {
            emptySet()
        }
        _uiState.update { it.copy(selectedSplitMemberIds = allIds) }
        recalculateSplits()
    }

    private fun recalculateSplits() {
        val amount = _uiState.value.amount.toDoubleOrNull() ?: 0.0
        val selectedIds = _uiState.value.selectedSplitMemberIds
        val count = selectedIds.size

        val newSplits = if (count > 0 && amount > 0) {
            val splitAmount = amount / count
            selectedIds.associateWith { splitAmount }
        } else {
            emptyMap()
        }

        val splitError = if (selectedIds.isEmpty()) "Выберите хотя бы одного" else null
        _uiState.update { it.copy(splits = newSplits, splitError = splitError) }
    }

    fun onSaveClick() {
        val currentState = _uiState.value

        val descriptionError = if (currentState.description.isBlank()) "Введите описание" else null
        val amountVal = currentState.amount.toDoubleOrNull()
        val amountError = if (amountVal == null || amountVal <= 0) "Введите сумму" else null
        val payerError = if (currentState.payerId == null) "Выберите плательщика" else null
        val splitError = if (currentState.splits.isEmpty()) "Выберите, на кого делить" else null

        val totalSplit = currentState.splits.values.sum()
        val difference = if (amountVal != null) abs(amountVal - totalSplit) else 0.0
        val balanceError = if (difference > 0.02) "Сумма сплита не совпадает с общей суммой" else null

        if (descriptionError != null || amountError != null || payerError != null || splitError != null || balanceError != null) {
            _uiState.update {
                it.copy(
                    descriptionError = descriptionError,
                    amountError = amountError,
                    payerError = payerError,
                    splitError = splitError ?: balanceError
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
                category = currentState.category.id,
                payerId = currentState.payerId!!,
                splits = currentState.splits
            )

            when (val result = saveExpenseUseCase(params)) {
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
    
    fun isMemberSelected(memberId: String): Boolean {
        return _uiState.value.selectedSplitMemberIds.contains(memberId)
    }
}