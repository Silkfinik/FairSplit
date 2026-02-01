package com.silkfinik.fairsplit.core.model.enums

enum class SplitType {
    EQUAL,
    EXACT,
    PERCENT,
    SHARES;

    companion object {
        fun fromName(name: String?): SplitType {
            return try {
                if (name.isNullOrBlank()) EQUAL else valueOf(name)
            } catch (e: IllegalArgumentException) {
                EQUAL
            }
        }
    }
}
