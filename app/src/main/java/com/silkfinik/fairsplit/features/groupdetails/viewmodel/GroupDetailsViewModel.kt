package com.silkfinik.fairsplit.features.groupdetails.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.domain.usecase.member.AddGhostMemberUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.DeleteExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.GetCurrentUserIdUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.GetExpensesUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.CalculateGroupBalanceUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.SyncGroupExpensesUseCase
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
    private val getMembersUseCase: com.silkfinik.fairsplit.core.domain.usecase.member.GetMembersUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val addGhostMemberUseCase: AddGhostMemberUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val syncGroupExpensesUseCase: SyncGroupExpensesUseCase,
    private val groupRepository: GroupRepository,
    private val calculateGroupBalanceUseCase: CalculateGroupBalanceUseCase
) : BaseViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _isGeneratingCode = MutableStateFlow(false)
    val isGeneratingCode = _isGeneratingCode.asStateFlow()

    val uiState: StateFlow<GroupDetailsUiState> = combine(
        getGroupUseCase(groupId),
        getExpensesUseCase(groupId),
        getMembersUseCase(groupId),
        getCurrentUserIdUseCase()
    ) { group, expenses, members, userId ->
        Log.d("GroupDetails", "UI Update for group $groupId. Members found: ${members.size}. Expenses: ${expenses.size}")
        members.forEach { m -> Log.d("GroupDetails", "Member in list: ${m.id} (${m.name})") }
        
        if (group == null) {
            GroupDetailsUiState.Error("Группа не найдена")
        } else {
            val balances = calculateGroupBalanceUseCase(expenses, members)
            balances.forEach { (id, amount) -> 
                val found = members.find { it.id == id }
                Log.d("GroupDetails", "Balance for $id: $amount. Found name: ${found?.name}") 
            }
            
            GroupDetailsUiState.Success(
                group = group, 
                members = members,
                expenses = expenses,
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
    }

    override fun onCleared() {
        super.onCleared()
        syncGroupExpensesUseCase.stop(groupId)
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