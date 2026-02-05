package com.silkfinik.fairsplit.core.domain.repository

import com.silkfinik.fairsplit.core.common.util.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: Flow<String?>

    fun hasSession(): Boolean

    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun reloadUser(): Result<Unit>
    fun isEmailVerified(): Boolean

    suspend fun signInAnonymously(): Result<Unit>

    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun signUpWithEmail(name: String, email: String, password: String): Result<Unit>

    suspend fun linkEmailAccount(email: String, password: String): Result<Unit>

    suspend fun updateEmail(newEmail: String): Result<Unit>

    suspend fun updateDisplayName(name: String): Result<Unit>

    suspend fun updateProfile(name: String, photoUrl: String?): Result<Unit>

    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    suspend fun linkGoogleAccount(idToken: String): Result<Unit>

    suspend fun signOut(): Result<Unit>

    fun getUserId(): String?
    fun getUserName(): String?
    fun isAnonymous(): Boolean
    fun getUserEmail(): String?
    fun getPhotoUrl(): String?
}