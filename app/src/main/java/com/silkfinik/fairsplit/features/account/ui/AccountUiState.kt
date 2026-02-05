package com.silkfinik.fairsplit.features.account.ui

import com.silkfinik.fairsplit.core.model.User

data class AccountUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val isNotificationsEnabled: Boolean = false,
    val isAnonymous: Boolean = false,
    val isEmailVerified: Boolean = true
)