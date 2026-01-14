package com.silkfinik.fairsplit.core.data.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.silkfinik.fairsplit.core.data.mapper.asDto
import com.silkfinik.fairsplit.core.database.dao.GroupDao
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupUploader @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val groupDao: GroupDao,
    private val authRepository: AuthRepository
) {

    suspend fun syncLocalChanges() {
        val userId = authRepository.getUserId() ?: return

        val dirtyGroups = groupDao.getDirtyGroups()

        if (dirtyGroups.isEmpty()) return

        val batch = firestore.batch()
        val groupsCollection = firestore.collection("groups")

        dirtyGroups.forEach { entity ->
            val docRef = groupsCollection.document(entity.id)
            val dto = entity.asDto()

            batch.set(docRef, dto, SetOptions.merge())
        }

        batch.commit().await()
        Log.d("Sync", "Successfully sent ${dirtyGroups.size} groups")

        dirtyGroups.forEach { group ->
            groupDao.markGroupAsSynced(group.id, group.updated_at)
        }
    }
}