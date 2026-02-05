package com.silkfinik.fairsplit.features.auth.ui

data class EmailVerificationUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)