package com.silkfinik.fairsplit.features.members.ui

import com.silkfinik.fairsplit.core.model.Member

sealed interface MembersUiState {
    data object Loading : MembersUiState
    data class Success(val members: List<Member>) : MembersUiState
    data class Error(val message: String) : MembersUiState
}
