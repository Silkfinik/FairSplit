package com.silkfinik.fairsplit.features.payments.ui

import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Member

data class CreatePaymentUiState(
    val isLoading: Boolean = false,
    val amount: String = "",
    val currency: Currency = Currency.USD,
    val members: List<Member> = emptyList(),
    val payerId: String? = null,
    val receiverId: String? = null,
    val currentUserId: String? = null,
    val amountError: String? = null,
    val receiverError: String? = null,
    val payerError: String? = null,
    val isSaved: Boolean = false,
    val error: String? = null
)
