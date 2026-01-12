package com.silkfinik.fairsplit.core.mode

import com.silkfinik.fairsplit.core.model.Currency

data class Group(
    val id: String,
    val name: String,
    val currency: Currency
)