package com.silkfinik.fairsplit.features.members.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import com.silkfinik.fairsplit.core.domain.usecase.auth.GetCurrentUserIdUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.AddGhostMemberUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.ClaimGhostUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.GetMembersUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.members.ui.MembersUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MembersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMembersUseCase: GetMembersUseCase,
    private val addGhostMemberUseCase: AddGhostMemberUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val claimGhostUseCase: ClaimGhostUseCase,
    private val userRepository: UserRepository
) : BaseViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    val uiState: StateFlow<MembersUiState> = combine(
        getMembersUseCase(groupId),
        getCurrentUserIdUseCase(),
        getCurrentUserIdUseCase().flatMapLatest { uid ->
            if (uid != null) userRepository.getUser(uid) else flowOf(null)
        }
    ) { members, userId, user ->
        if (members.isNotEmpty()) {
            val linkedIds = user?.linkedGhostIds ?: emptyList()
            val hasClaimed = members.any { 
                linkedIds.contains(it.id) || (userId != null && it.mergedWithUid == userId)
            }
            
            MembersUiState.Success(
                members = members,
                currentUserId = userId,
                linkedGhostIds = linkedIds,
                hasClaimedGhost = hasClaimed
            )
        } else {
            MembersUiState.Error("Участники не найдены")
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
                .onError { message, _ ->
                    sendEvent(UiEvent.ShowSnackbar(message))
                }
        }
    }

    fun claimGhost(memberId: String) {
        viewModelScope.launch {
            claimGhostUseCase(groupId, memberId)
                .onError { message, _ ->
                    sendEvent(UiEvent.ShowSnackbar(message))
                }
                .onSuccess {
                    sendEvent(UiEvent.ShowSnackbar("Профиль успешно объединен"))
                }
        }
    }
}