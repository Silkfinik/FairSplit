package com.silkfinik.fairsplit.features.groups.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.data.repository.GroupRepository
import com.silkfinik.fairsplit.core.mode.Group
import com.silkfinik.fairsplit.features.groups.ui.GroupsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    groupRepository: GroupRepository
) : ViewModel() {

    val uiState: StateFlow<GroupsUiState> = groupRepository.getGroups()
        .map { entities ->
            val domainGroups = entities.map { entity ->
                Group(
                    id = entity.id,
                    name = entity.name,
                    currency = entity.currency
                )
            }
            GroupsUiState(groups = domainGroups)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GroupsUiState()
        )
}