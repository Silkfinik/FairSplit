package com.silkfinik.fairsplit.core.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.silkfinik.fairsplit.core.data.mapper.asDomainModel
import com.silkfinik.fairsplit.core.data.mapper.asDto
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import com.silkfinik.fairsplit.core.model.User
import com.silkfinik.fairsplit.core.network.model.UserDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun getUser(uid: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        // User likely signed out or lost access. Close gracefully.
                        close() 
                    } else {
                        close(e)
                    }
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val dto = snapshot.toObject(UserDto::class.java)
                    trySend(dto?.asDomainModel())
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createOrUpdateUser(user: User) {
        val dto = user.asDto()
        
        // Convert to map to filter out nulls (especially createdAt if it was 0/null)
        val updates = mutableMapOf<String, Any?>()
        updates["uid"] = dto.uid
        updates["display_name"] = dto.displayName
        updates["photo_url"] = dto.photoUrl
        updates["is_anonymous"] = dto.isAnonymous
        updates["updated_at"] = dto.updatedAt
        
        if (dto.email != null) updates["email"] = dto.email
        if (dto.linkedGhostIds != null) updates["linked_ghost_ids"] = dto.linkedGhostIds
        if (dto.fcmToken != null) updates["fcm_token"] = dto.fcmToken
        
        // Only include createdAt if it's set (not null)
        if (dto.createdAt != null) {
            updates["created_at"] = dto.createdAt
        }

        firestore.collection("users").document(user.id)
            .set(updates, SetOptions.merge())
            .await()
    }

    override suspend fun userExists(uid: String): Boolean {
        val snapshot = firestore.collection("users").document(uid).get().await()
        return snapshot.exists()
    }

    override suspend fun updateFcmToken(uid: String, token: String?) {
        val updates = mapOf(
            "fcm_token" to token,
            "updated_at" to System.currentTimeMillis()
        )
        firestore.collection("users").document(uid)
            .set(updates, SetOptions.merge())
            .await()
    }

    override suspend fun deleteUser(uid: String) {
        firestore.collection("users").document(uid).delete().await()
    }
}