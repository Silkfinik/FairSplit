package com.silkfinik.fairsplit.core.common.util

interface TimeProvider {
    fun now(): Long

    suspend fun initialize()
}