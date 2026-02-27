package com.silkfinik.fairsplit.features.account.ui

import com.silkfinik.fairsplit.core.common.util.UiText
import com.silkfinik.fairsplit.core.model.User

data class AccountUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val isNotificationsEnabled: Boolean = false,
    val isAnonymous: Boolean = false,
    val isEmailVerified: Boolean = true,

    val linkEmail: String = "",
    val linkPassword: String = "",
    val linkConfirmPassword: String = "",
    val linkEmailError: UiText? = null,
    val linkPasswordError: UiText? = null,
    val isLinkSheetVisible: Boolean = false
)