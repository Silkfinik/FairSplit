package com.silkfinik.fairsplit.core.data.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.silkfinik.fairsplit.core.common.di.ApplicationScope
import com.silkfinik.fairsplit.core.data.mapper.asDto
import com.silkfinik.fairsplit.core.data.mapper.asEntity
import com.silkfinik.fairsplit.core.data.repository.AuthRepository
import com.silkfinik.fairsplit.core.database.dao.GroupDao
import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import com.silkfinik.fairsplit.core.network.model.GroupDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupSynchronizer @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val groupDao: GroupDao,
    private val authRepository: AuthRepository,
    @ApplicationScope private val externalScope: CoroutineScope
) {

    private var groupListener: ListenerRegistration? = null

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

        try {
            batch.commit().await()
            Log.d("Sync", "Successfully sent ${dirtyGroups.size} groups")

            val syncedIds = dirtyGroups.map { it.id }
            groupDao.markGroupsAsSynced(syncedIds)
        } catch (e: Exception) {
            Log.e("Sync", "Sync error: ${e.message}")
        }
    }

    fun startListening() {
        val userId = authRepository.getUserId() ?: return

        groupListener?.remove()

        val query = firestore.collection("groups")
            .whereEqualTo("owner_id", userId)

        groupListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("Sync", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val dtos = snapshot.toObjects(GroupDto::class.java)
                externalScope.launch {
                    saveServerDataToLocal(dtos)
                }
            }
        }
    }

    fun stopListening() {
        groupListener?.remove()
        groupListener = null
    }

    private suspend fun saveServerDataToLocal(dtos: List<GroupDto>) {
        dtos.forEach { dto ->
            val localEntity = groupDao.getGroupById(dto.id)

            if (shouldUpdateLocal(localEntity, dto)) {
                groupDao.insertGroup(dto.asEntity())
            } else {
                Log.d("Sync", "Skipping update for ${dto.name}")
            }
        }
    }

    private fun shouldUpdateLocal(localEntity: GroupEntity?, dto: GroupDto): Boolean {
        if (localEntity == null) {
            Log.d("Sync", "Group ${dto.name} (ID: ${dto.id}) is new -> saving.")
            return true
        }
        
        if (!localEntity.is_dirty) {
            Log.d("Sync", "Group ${dto.name} is locally clean -> updating from server.")
            return true
        }

        val isServerNewer = dto.updatedAt > localEntity.updated_at
        Log.d("Sync", "Conflict for ${dto.name}: LocalTime=${localEntity.updated_at}, ServerTime=${dto.updatedAt}. ServerNewer=$isServerNewer")
        return isServerNewer
    }
}