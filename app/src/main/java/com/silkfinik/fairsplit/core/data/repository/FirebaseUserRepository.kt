package com.silkfinik.fairsplit.core.data.repository

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

class FirebaseUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun getUser(uid: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
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
        firestore.collection("users").document(user.id)
            .set(dto, SetOptions.merge())
            .await()
    }

    override suspend fun userExists(uid: String): Boolean {
        val snapshot = firestore.collection("users").document(uid).get().await()
        return snapshot.exists()
    }
}

