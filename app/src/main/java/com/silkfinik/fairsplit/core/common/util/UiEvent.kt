package com.silkfinik.fairsplit.core.common.util

sealed interface UiEvent {
    data class ShowSnackbar(val message: UiText) : UiEvent
    data class ShowError(val message: UiText) : UiEvent
    data object NavigateBack : UiEvent
    data class NavigateToGroupDetails(val groupId: String) : UiEvent
    data object Success : UiEvent
}
