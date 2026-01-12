package com.silkfinik.fairsplit.features.groups.ui

import com.silkfinik.fairsplit.core.mode.Group

data class GroupsUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false
)