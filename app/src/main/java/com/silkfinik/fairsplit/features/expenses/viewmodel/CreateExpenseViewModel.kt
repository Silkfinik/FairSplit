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

    private val _uiState = MutableStateFlow(CreateExpenseUiState())
    val uiState: StateFlow<CreateExpenseUiState> = _uiState.asStateFlow()

    init {
        loadMembers()
    }

    private fun loadMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                memberRepository.getMembers(groupId).collect { members ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            members = members,
                            payerId = it.payerId ?: members.firstOrNull()?.id
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

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val group = groupRepository.getGroup(groupId).first() ?: throw Exception("Группа не найдена")
                val userId = authRepository.getUserId() ?: throw Exception("Не авторизован")

                val splitAmount = amount / currentState.members.size
                val splits = currentState.members.associate { it.id to splitAmount }

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
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
