package com.silkfinik.fairsplit.features.groupdetails.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.features.groupdetails.ui.GroupDetailsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GroupDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    val uiState: StateFlow<GroupDetailsUiState> = combine(
        groupRepository.getGroup(groupId),
        expenseRepository.getExpensesForGroup(groupId),
        authRepository.currentUserId
    ) { group, expenses, userId ->
        if (group == null) {
            GroupDetailsUiState.Error("Группа не найдена")
        } else {
            GroupDetailsUiState.Success(
                group = group, 
                expenses = expenses,
                currentUserId = userId
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GroupDetailsUiState.Loading
    )

    init {
        expenseRepository.startSync(groupId)
    }

    override fun onCleared() {
        super.onCleared()
        expenseRepository.stopSync(groupId)
    }

    fun addGhostMember(name: String) {
        viewModelScope.launch {
            val group = groupRepository.getGroup(groupId).first() ?: return@launch
            
            val newMember = Member(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                name = name,
                isGhost = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            memberRepository.addMember(newMember)
            
            // Mark group as dirty to trigger sync
            groupRepository.updateGroup(group)
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expenseId)
        }
    }
}
