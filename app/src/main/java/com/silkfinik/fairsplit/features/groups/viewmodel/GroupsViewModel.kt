package com.silkfinik.fairsplit.features.groups.viewmodel

import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupsUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.groups.ui.GroupsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    getGroupsUseCase: GetGroupsUseCase
) : BaseViewModel() {

    val uiState: StateFlow<GroupsUiState> = getGroupsUseCase()
        .map { groups ->
            GroupsUiState(groups = groups, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GroupsUiState(isLoading = true)
        )
}