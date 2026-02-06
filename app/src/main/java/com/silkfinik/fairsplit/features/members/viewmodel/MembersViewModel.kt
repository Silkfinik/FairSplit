package com.silkfinik.fairsplit.features.members.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.UiText
import com.silkfinik.fairsplit.core.common.util.asUiText
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.usecase.group.GenerateInviteCodeUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.AddGhostMemberUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.ClaimGhostUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.GetMembersScreenDataUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.UpdateMemberUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.members.ui.MembersUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MembersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMembersScreenDataUseCase: GetMembersScreenDataUseCase,
    private val addGhostMemberUseCase: AddGhostMemberUseCase,
    private val updateMemberUseCase: UpdateMemberUseCase,
    private val claimGhostUseCase: ClaimGhostUseCase,
    private val generateInviteCodeUseCase: GenerateInviteCodeUseCase
) : BaseViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _isGeneratingCode = MutableStateFlow(false)
    val isGeneratingCode = _isGeneratingCode.asStateFlow()

    val uiState: StateFlow<MembersUiState> = getMembersScreenDataUseCase(groupId)
        .map { data ->
            if (data.members.isNotEmpty()) {
                MembersUiState.Success(
                    members = data.members,
                    currentUserId = data.currentUserId,
                    linkedGhostIds = data.linkedGhostIds,
                    hasClaimedGhost = data.hasClaimedGhost,
                    inviteCode = data.inviteCode
                )
            } else {
                MembersUiState.Error(UiText.StringResource(R.string.error_members_not_found))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MembersUiState.Loading
        )

    fun addGhostMember(name: String) {
        viewModelScope.launch {
            addGhostMemberUseCase(groupId, name)
                .onError { error ->
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
        }
    }

    fun updateMemberName(memberId: String, newName: String) {
        viewModelScope.launch {
            val state = uiState.value
            if (state is MembersUiState.Success) {
                val member = state.members.find { it.id == memberId }
                if (member != null) {
                    updateMemberUseCase(member.copy(name = newName))
                        .onError { error ->
                            sendEvent(UiEvent.ShowError(error.asUiText()))
                        }
                }
            }
        }
    }

    fun claimGhost(memberId: String) {
        viewModelScope.launch {
            claimGhostUseCase(groupId, memberId)
                .onError { error ->
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
                .onSuccess {
                    sendEvent(UiEvent.ShowSnackbar(UiText.StringResource(R.string.success_ghost_claimed)))
                }
        }
    }

    fun generateInviteCode() {
        viewModelScope.launch {
            _isGeneratingCode.value = true

            generateInviteCodeUseCase(groupId)
                .onError { error ->
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }

            _isGeneratingCode.value = false
        }
    }
}