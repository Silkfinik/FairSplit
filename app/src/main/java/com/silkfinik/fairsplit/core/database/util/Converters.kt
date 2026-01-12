package com.silkfinik.fairsplit.core.database.util

import androidx.room.TypeConverter
import com.silkfinik.fairsplit.core.model.Currency

class Converters {
    @TypeConverter
    fun fromCurrency(currency: Currency): String {
        return currency.name
    }

    @TypeConverter
    fun toCurrency(value: String): Currency {
        return try {
            Currency.valueOf(value)
        } catch (e: IllegalArgumentException) {
            Currency.USD
        }
    }
}