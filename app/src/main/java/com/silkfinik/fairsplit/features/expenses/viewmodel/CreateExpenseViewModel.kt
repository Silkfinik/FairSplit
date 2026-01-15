package com.silkfinik.fairsplit.features.expenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.features.expenses.ui.CreateExpenseUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    private val memberRepository: MemberRepository,
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

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
                val members = memberRepository.getMembers(groupId).first()
                
                if (expenseId != null) {
                    // Edit Mode: Load Expense
                    val expense = expenseRepository.getExpense(expenseId).first()
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

    fun onSaveClick() {
        val currentState = _uiState.value
        val amount = currentState.amount.toDoubleOrNull()

        if (currentState.description.isBlank()) {
            _uiState.update { it.copy(error = "Введите описание") }
            return
        }
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Введите корректную сумму") }
            return
        }
        if (currentState.payerId == null) {
            _uiState.update { it.copy(error = "Выберите плательщика") }
            return
        }
        if (currentState.splitMemberIds.isEmpty()) {
            _uiState.update { it.copy(error = "Выберите, на кого делить") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val group = groupRepository.getGroup(groupId).first() ?: throw Exception("Группа не найдена")
                val userId = authRepository.getUserId() ?: throw Exception("Не авторизован")

                // Equal split among selected members
                val splitAmount = amount / currentState.splitMemberIds.size
                val splits = currentState.splitMemberIds.associateWith { splitAmount }

                if (expenseId != null) {
                    // Update existing
                    val existingExpense = expenseRepository.getExpense(expenseId).first() ?: throw Exception("Трата не найдена")
                    val updatedExpense = existingExpense.copy(
                        description = currentState.description,
                        amount = amount,
                        payers = mapOf(currentState.payerId to amount),
                        splits = splits,
                        updatedAt = System.currentTimeMillis()
                    )
                    expenseRepository.updateExpense(updatedExpense)
                } else {
                    // Create new
                    val expense = Expense(
                        id = UUID.randomUUID().toString(),
                        groupId = groupId,
                        description = currentState.description,
                        amount = amount,
                        currency = group.currency,
                        date = System.currentTimeMillis(),
                        creatorId = userId,
                        payers = mapOf(currentState.payerId to amount),
                        splits = splits,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    expenseRepository.createExpense(expense)
                }
                
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
