package com.silkfinik.fairsplit.core.data.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.silkfinik.fairsplit.core.common.di.ApplicationScope
import com.silkfinik.fairsplit.core.data.mapper.asEntity
import com.silkfinik.fairsplit.core.data.mapper.asMemberEntity
import com.silkfinik.fairsplit.core.database.dao.GroupDao
import com.silkfinik.fairsplit.core.database.dao.MemberDao
import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import com.silkfinik.fairsplit.core.database.entity.MemberEntity
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.network.model.GroupDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRealtimeListener @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val groupDao: GroupDao,
    private val memberDao: MemberDao,
    private val authRepository: AuthRepository,
    @param:ApplicationScope private val externalScope: CoroutineScope
) {

    private var groupListener: ListenerRegistration? = null

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
                if (localEntity != null) {
                    groupDao.updateGroup(dto.asEntity())
                } else {
                    groupDao.insertGroup(dto.asEntity())
                }
                syncMembers(dto)
            } else {
                Log.d("Sync", "Skipping update for ${dto.name}")
            }
        }
    }

    private suspend fun syncMembers(dto: GroupDto) {
        dto.ghosts.forEach { (ghostId, ghostDto) ->
            val localMember = memberDao.getMemberById(ghostId)
            if (localMember == null) {
                val newMember = ghostDto.asMemberEntity(ghostId, dto.id)
                memberDao.insertMember(newMember)
            } else {
                if (!localMember.is_dirty && localMember.name != ghostDto.name) {
                    memberDao.updateMember(localMember.copy(name = ghostDto.name))
                }
            }
        }

        dto.members.forEach { memberId ->
            val localMember = memberDao.getMemberById(memberId)
            if (localMember == null) {
                 val newMember = MemberEntity(
                    id = memberId,
                    group_id = dto.id,
                    name = "Участник",
                    is_ghost = false,
                    created_at = System.currentTimeMillis(),
                    updated_at = System.currentTimeMillis(),
                    is_dirty = false
                )
                memberDao.insertMember(newMember)
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