package com.silkfinik.fairsplit.core.database.util

import androidx.room.TypeConverter
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus
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
    fun fromPaymentStatus(status: PaymentStatus): String {
        return status.name
    }

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus {
        return try {
            PaymentStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            PaymentStatus.PENDING
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