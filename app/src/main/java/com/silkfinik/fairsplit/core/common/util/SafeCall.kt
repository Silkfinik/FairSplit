package com.silkfinik.fairsplit.core.common.util

import kotlinx.coroutines.CancellationException

suspend fun <T> safeCall(
    errorMessage: String = "Произошла ошибка",
    action: suspend () -> T
): Result<T> {
    return try {
        val data = action()
        Result.Success(data)
    } catch (e: Exception) {
        if (e is CancellationException) throw e

        Result.Error(e.message ?: errorMessage, e)
    }
}