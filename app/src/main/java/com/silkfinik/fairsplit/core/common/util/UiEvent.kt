package com.silkfinik.fairsplit.core.common.util

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
    data class ShowError(val message: String) : UiEvent
    data object NavigateBack : UiEvent
}
