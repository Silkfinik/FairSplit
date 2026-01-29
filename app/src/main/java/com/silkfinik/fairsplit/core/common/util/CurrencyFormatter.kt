package com.silkfinik.fairsplit.core.common.util

import com.silkfinik.fairsplit.core.model.Currency
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Double, currency: Currency): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        try {
            val javaCurrency = java.util.Currency.getInstance(currency.code)
            format.currency = javaCurrency
        } catch (e: Exception) {

        }
        
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
        numberFormat.minimumFractionDigits = 0
        numberFormat.maximumFractionDigits = 2
        
        return "${numberFormat.format(amount)} ${currency.symbol}"
    }
}
