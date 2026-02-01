package com.silkfinik.fairsplit.features.groupdetails.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.usecase.member.AddGhostMemberUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.DeleteExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.GetCurrentUserIdUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.GetExpensesUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.CalculateGroupBalanceUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.SyncGroupExpensesUseCase
import com.silkfinik.fairsplit.core.domain.usecase.payment.GetGroupPaymentsUseCase
import com.silkfinik.fairsplit.core.domain.usecase.payment.SyncGroupPaymentsUseCase
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.groupdetails.ui.GroupDetailsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupUseCase: GetGroupUseCase,
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getGroupPaymentsUseCase: GetGroupPaymentsUseCase,
    private val getMembersUseCase: com.silkfinik.fairsplit.core.domain.usecase.member.GetMembersUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val addGhostMemberUseCase: AddGhostMemberUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val syncGroupExpensesUseCase: SyncGroupExpensesUseCase,
    private val syncGroupPaymentsUseCase: SyncGroupPaymentsUseCase,
    private val groupRepository: GroupRepository,
    private val calculateGroupBalanceUseCase: CalculateGroupBalanceUseCase,
    private val updatePaymentStatusUseCase: com.silkfinik.fairsplit.core.domain.usecase.payment.UpdatePaymentStatusUseCase
) : BaseViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _isGeneratingCode = MutableStateFlow(false)
    val isGeneratingCode = _isGeneratingCode.asStateFlow()

    val uiState: StateFlow<GroupDetailsUiState> = combine(
        getGroupUseCase(groupId),
        getExpensesUseCase(groupId),
        getGroupPaymentsUseCase(groupId),
        getMembersUseCase(groupId),
        getCurrentUserIdUseCase()
    ) { group, expenses, payments, members, userId ->
        if (group == null) {
            GroupDetailsUiState.Error("Группа не найдена")
        } else {
            val balances = calculateGroupBalanceUseCase(expenses, members, payments)
            
            GroupDetailsUiState.Success(
                group = group, 
                members = members,
                expenses = expenses,
                payments = payments,
                balances = balances,
                currentUserId = userId
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GroupDetailsUiState.Loading
    )

    init {
        syncGroupExpensesUseCase.start(groupId)
        syncGroupPaymentsUseCase.start(groupId)
    }

    override fun onCleared() {
        super.onCleared()
        syncGroupExpensesUseCase.stop(groupId)
        syncGroupPaymentsUseCase.stop(groupId)
    }

    fun addGhostMember(name: String) {
        viewModelScope.launch {
            addGhostMemberUseCase(groupId, name)
                .onError { message, _ ->
                    sendEvent(UiEvent.ShowSnackbar(message))
                }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            deleteExpenseUseCase(expenseId)
                .onError { message, _ ->
                    sendEvent(UiEvent.ShowSnackbar(message))
                }
        }
    }

    fun updatePaymentStatus(paymentId: String, newStatus: com.silkfinik.fairsplit.core.model.enums.PaymentStatus) {
        viewModelScope.launch {
            updatePaymentStatusUseCase(paymentId, newStatus)
                .onError { message, _ ->
                    sendEvent(UiEvent.ShowSnackbar(message))
                }
                .onSuccess {
                    sendEvent(UiEvent.ShowSnackbar("Статус обновлен"))
                }
        }
    }

    fun generateInviteCode() {
        viewModelScope.launch {
            _isGeneratingCode.value = true
            groupRepository.generateInviteCode(groupId)
                .onError { message, _ ->
                    sendEvent(UiEvent.ShowSnackbar(message))
                }
            _isGeneratingCode.value = false
        }
    }
}