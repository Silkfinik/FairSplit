package com.silkfinik.fairsplit.features.payments.ui

import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Member

data class CreatePaymentUiState(
    val isLoading: Boolean = false,
    val amount: String = "",
    val currency: Currency = Currency.USD,
    val members: List<Member> = emptyList(),
    val payerId: String? = null, // Who is paying (Debtor)
    val receiverId: String? = null, // Who receives (Creditor)
    val currentUserId: String? = null,
    val amountError: String? = null,
    val receiverError: String? = null,
    val payerError: String? = null, // Usually fixed to current user, but good to have
    val isSaved: Boolean = false,
    val error: String? = null
)
