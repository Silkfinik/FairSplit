package com.silkfinik.fairsplit.features.groups.ui

import com.silkfinik.fairsplit.core.model.Currency

data class CreateGroupUiState(
    val name: String = "",
    val selectedCurrency: Currency = Currency.USD,
    val imageUri: String? = null,
    val isLoading: Boolean = false
)