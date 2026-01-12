package com.silkfinik.fairsplit.core.data.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: Flow<String?>

    fun hasSession(): Boolean

    suspend fun signInAnonymously(): Result<Unit>

    fun getUserId(): String?
}