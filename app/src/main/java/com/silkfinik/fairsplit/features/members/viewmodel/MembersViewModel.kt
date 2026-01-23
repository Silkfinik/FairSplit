package com.silkfinik.fairsplit.features.members.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.domain.usecase.member.AddGhostMemberUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.GetMembersUseCase
import com.silkfinik.fairsplit.core.domain.usecase.auth.GetCurrentUserIdUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.members.ui.MembersUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MembersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMembersUseCase: GetMembersUseCase,
    private val addGhostMemberUseCase: AddGhostMemberUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) : BaseViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    val uiState: StateFlow<MembersUiState> = combine(
        getMembersUseCase(groupId),
        getCurrentUserIdUseCase()
    ) { members, userId ->
        if (members.isNotEmpty()) {
            MembersUiState.Success(members, userId)
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
            val result = addGhostMemberUseCase(groupId, name)
            if (result is Result.Error) {
                sendEvent(UiEvent.ShowSnackbar(result.message))
            }
        }
    }
}