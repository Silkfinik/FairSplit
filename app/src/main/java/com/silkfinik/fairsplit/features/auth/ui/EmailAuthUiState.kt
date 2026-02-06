package com.silkfinik.fairsplit.features.auth.ui

import com.silkfinik.fairsplit.core.common.util.UiText

data class EmailAuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null
)