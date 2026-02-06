package com.silkfinik.fairsplit.features.auth.ui

import com.silkfinik.fairsplit.core.common.util.UiText

data class WelcomeUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val nameError: UiText? = null
)
