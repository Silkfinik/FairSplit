package com.silkfinik.fairsplit.core.common.util

import com.silkfinik.fairsplit.core.data.mapper.toAppError
import kotlinx.coroutines.CancellationException

suspend fun <T> safeCall(
    action: suspend () -> T
): Result<T> {
    return try {
        val data = action()
        Result.Success(data)
    } catch (e: Exception) {
        if (e is CancellationException) throw e

        Result.Error(e.toAppError())
    }
}