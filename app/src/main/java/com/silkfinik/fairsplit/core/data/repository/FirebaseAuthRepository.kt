package com.silkfinik.fairsplit.core.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.safeCall
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import androidx.core.net.toUri
import com.silkfinik.fairsplit.core.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val appDatabase: AppDatabase
) : AuthRepository {

    override val currentUserId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun hasSession(): Boolean = auth.currentUser != null

    override fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true

    override fun getUserId(): String? = auth.currentUser?.uid

    override fun getUserName(): String? = auth.currentUser?.displayName

    override fun getUserEmail(): String? = auth.currentUser?.email

    override fun getPhotoUrl(): String? = auth.currentUser?.photoUrl?.toString()

    override fun isAnonymous(): Boolean = auth.currentUser?.isAnonymous == true

    override suspend fun sendEmailVerification(): Result<Unit> = safeCall {
        auth.currentUser?.sendEmailVerification()?.await()
    }

    override suspend fun updateEmail(newEmail: String): Result<Unit> = safeCall {
        val user = auth.currentUser ?: throw Exception("User not found")
        try {
            user.verifyBeforeUpdateEmail(newEmail).await()
        } catch (e: Exception) {
            if (e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                throw Exception("Need re-login for email")
            }
            throw e
        }
    }

    override suspend fun reloadUser(): Result<Unit> = safeCall {
        auth.currentUser?.reload()?.await()
    }

    override suspend fun signInAnonymously(): Result<Unit> = safeCall {
        auth.signInAnonymously().await()
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> = safeCall {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signUpWithEmail(name: String, email: String, password: String): Result<Unit> = safeCall {
        auth.createUserWithEmailAndPassword(email, password).await()

        val user = auth.currentUser
        if (user != null) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()
        }
    }

    override suspend fun linkEmailAccount(email: String, password: String): Result<Unit> = safeCall {
        val user = auth.currentUser ?: throw Exception("User not logged in")
        val credential = EmailAuthProvider.getCredential(email, password)
        user.linkWithCredential(credential).await()
    }

    override suspend fun updateDisplayName(name: String): Result<Unit> = safeCall {
        val user = auth.currentUser ?: throw Exception("User not logged in")
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        user.updateProfile(profileUpdates).await()
    }

    override suspend fun updateProfile(name: String, photoUrl: String?): Result<Unit> = safeCall {
        val user = auth.currentUser ?: throw Exception("User not logged in")
        val builder = UserProfileChangeRequest.Builder()
            .setDisplayName(name)

        if (photoUrl != null) {
            builder.setPhotoUri(photoUrl.toUri())
        }

        user.updateProfile(builder.build()).await()
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = safeCall {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    override suspend fun linkGoogleAccount(idToken: String): Result<Unit> = safeCall {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.currentUser ?: throw Exception("User not logged in")

        try {
            user.linkWithCredential(credential).await()
        } catch (e: FirebaseAuthUserCollisionException) {
            auth.signInWithCredential(credential).await()
        }
    }

    override suspend fun signOut(): Result<Unit> = safeCall {
        withContext(Dispatchers.IO) {
            appDatabase.clearAllTables()
        }

        auth.signOut()
    }
}