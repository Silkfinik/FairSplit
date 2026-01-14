package com.silkfinik.fairsplit.core.database.util

import androidx.room.TypeConverter
import com.silkfinik.fairsplit.core.model.Currency
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    @TypeConverter
    fun fromStringDoubleMap(value: Map<String, Double>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringDoubleMap(value: String): Map<String, Double> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}