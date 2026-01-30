package com.silkfinik.fairsplit.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.common.util.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import androidx.core.net.toUri

class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUserId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun hasSession(): Boolean {
        return auth.currentUser != null
    }

    override suspend fun signInAnonymously(): Result<Unit> {
        return try {
            auth.signInAnonymously().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка анонимного входа", e)
        }
    }

    override suspend fun updateDisplayName(name: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка обновления имени", e)
        }
    }

    override suspend fun updateProfile(name: String, photoUrl: String?): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            val builder = UserProfileChangeRequest.Builder()
                .setDisplayName(name)

            if (photoUrl != null) {
                builder.setPhotoUri(photoUrl.toUri())
            }

            val profileUpdates = builder.build()
            user.updateProfile(profileUpdates).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка обновления профиля", e)
        }
    }

    override suspend fun linkGoogleAccount(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val user = auth.currentUser ?: throw Exception("User not logged in")
            
            try {
                user.linkWithCredential(credential).await()
            } catch (e: FirebaseAuthUserCollisionException) {
                auth.signInWithCredential(credential).await()
            } catch (e: Exception) {
                throw e
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка привязки аккаунта", e)
        }
    }

    override fun getUserId(): String? = auth.currentUser?.uid
    
    override fun getUserName(): String? = auth.currentUser?.displayName

    override fun isAnonymous(): Boolean = auth.currentUser?.isAnonymous == true
}
