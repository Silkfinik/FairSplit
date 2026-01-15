package com.silkfinik.fairsplit.features.members.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.features.members.ui.MembersUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MembersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: MemberRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    val uiState: StateFlow<MembersUiState> = memberRepository.getMembers(groupId)
        .map { members ->
            if (members.isNotEmpty()) {
                MembersUiState.Success(members)
            } else {
                MembersUiState.Error("Участники не найдены") // Should not happen usually
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MembersUiState.Loading
        )

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
}
