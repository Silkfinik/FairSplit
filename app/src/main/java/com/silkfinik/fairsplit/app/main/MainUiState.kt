package com.silkfinik.fairsplit.app.main

sealed interface MainUiState {
    data object Loading : MainUiState
    data object Success : MainUiState
    data object NeedsName : MainUiState
    data object ErrorNoInternet : MainUiState
    data class ErrorAuthFailed(val message: String) : MainUiState
}