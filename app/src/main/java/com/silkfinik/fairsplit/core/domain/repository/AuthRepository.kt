package com.silkfinik.fairsplit.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: Flow<String?>

    fun hasSession(): Boolean

    suspend fun signInAnonymously(): Result<Unit>

    suspend fun updateDisplayName(name: String): Result<Unit>

    suspend fun linkGoogleAccount(idToken: String): Result<Unit>

    fun getUserId(): String?
    
    fun getUserName(): String?

    fun isAnonymous(): Boolean
}