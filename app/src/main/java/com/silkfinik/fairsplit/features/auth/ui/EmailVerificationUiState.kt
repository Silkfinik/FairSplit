package com.silkfinik.fairsplit.features.auth.ui

import com.silkfinik.fairsplit.core.common.util.UiText

data class EmailVerificationUiState(
    val isLoading: Boolean = false,
    val message: UiText? = null,
    val error: UiText? = null
)