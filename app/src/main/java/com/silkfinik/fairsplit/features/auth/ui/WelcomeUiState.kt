package com.silkfinik.fairsplit.features.auth.ui

data class WelcomeUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val nameError: String? = null,
    val isSaved: Boolean = false
)
