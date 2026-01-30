package com.silkfinik.fairsplit.features.groups.ui

import com.silkfinik.fairsplit.core.model.Group

data class GroupsUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)