package com.silkfinik.fairsplit.features.groups.viewmodel

import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.asResult
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupsUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.groups.ui.GroupsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    getGroupsUseCase: GetGroupsUseCase,
    private val groupRepository: GroupRepository
) : BaseViewModel() {

    val uiState: StateFlow<GroupsUiState> = getGroupsUseCase()
        .asResult()
        .map { result ->
            when (result) {
                is Result.Success -> GroupsUiState(groups = result.data, isLoading = false)
                is Result.Error -> GroupsUiState(isLoading = false, errorMessage = result.message)
                is Result.Loading -> GroupsUiState(isLoading = true)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GroupsUiState(isLoading = true)
        )

    fun joinGroup(code: String) {
        viewModelScope.launch {
            groupRepository.joinGroup(code)
                .onSuccess { groupId ->
                    sendEvent(UiEvent.NavigateToGroupDetails(groupId))
                }
                .onError { message, _ ->
                    sendEvent(UiEvent.ShowSnackbar(message))
                }
        }
    }
}