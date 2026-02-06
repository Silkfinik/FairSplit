package com.silkfinik.fairsplit.core.data.repository

import androidx.core.net.toUri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.silkfinik.fairsplit.core.common.util.ImageCompressor
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.TimeProvider
import com.silkfinik.fairsplit.core.common.util.asFlow
import com.silkfinik.fairsplit.core.common.util.safeCall
import com.silkfinik.fairsplit.core.data.mapper.asDomainModel
import com.silkfinik.fairsplit.core.data.mapper.asDto
import com.silkfinik.fairsplit.core.data.sync.FirestoreRoutes
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import com.silkfinik.fairsplit.core.model.User
import com.silkfinik.fairsplit.core.network.model.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val imageCompressor: ImageCompressor,
    private val timeProvider: TimeProvider
) : UserRepository {

    override fun getUser(uid: String): Flow<User?> {
        return firestore.collection(FirestoreRoutes.USERS).document(uid)
            .asFlow(UserDto::class.java)
            .map { it?.asDomainModel() }
    }

    override suspend fun createOrUpdateUser(user: User): Result<Unit> = safeCall {
        val dto = user.asDto()
        val updates = mutableMapOf<String, Any?>()
        updates["uid"] = dto.uid
        updates["display_name"] = dto.displayName
        updates["photo_url"] = dto.photoUrl
        updates["is_anonymous"] = dto.isAnonymous
        updates["updated_at"] = dto.updatedAt

        if (dto.email != null) updates["email"] = dto.email
        if (dto.linkedGhostIds != null) updates["linked_ghost_ids"] = dto.linkedGhostIds
        if (dto.fcmToken != null) updates["fcm_token"] = dto.fcmToken
        if (dto.createdAt != null) updates["created_at"] = dto.createdAt

        firestore.collection(FirestoreRoutes.USERS).document(user.id)
            .set(updates, SetOptions.merge())
            .await()
    }

    override suspend fun userExists(uid: String): Boolean {
        val snapshot = firestore.collection(FirestoreRoutes.USERS).document(uid).get().await()
        return snapshot.exists()
    }

    override suspend fun updateFcmToken(uid: String, token: String?): Result<Unit> = safeCall {
        val updates = mapOf(
            "fcm_token" to token,
            "updated_at" to timeProvider.now()
        )
        firestore.collection(FirestoreRoutes.USERS).document(uid)
            .set(updates, SetOptions.merge())
            .await()
    }

    override suspend fun deleteUser(uid: String): Result<Unit> = safeCall {
        firestore.collection(FirestoreRoutes.USERS).document(uid).delete().await()
    }

    override suspend fun uploadUserAvatar(uid: String, imageUri: String): Result<String> = safeCall {
        val uri = imageUri.toUri()
        val compressedBytes = imageCompressor.compress(uri)
            ?: throw Exception("Не удалось обработать изображение")

        val filename = "avatars/${uid}_${timeProvider.now()}.webp"
        val ref = storage.reference.child(filename)

        ref.putBytes(compressedBytes).await()
        ref.downloadUrl.await().toString()
    }
}