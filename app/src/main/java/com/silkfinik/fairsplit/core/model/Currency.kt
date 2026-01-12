package com.silkfinik.fairsplit.core.model

enum class Currency(val symbol: String, val code: String) {
    USD("$", "USD"),
    EUR("€", "EUR"),
    RUB("₽", "RUB"),
    KZT("₸", "KZT");
}