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
            .whereArrayContains("members", userId)

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
        val currentUserId = authRepository.getUserId()
        Log.d("Sync", "Syncing members for group ${dto.name} (${dto.id}). Members: ${dto.members.size}, Profiles: ${dto.memberProfiles?.size ?: 0}")
        
        dto.ghosts.forEach { (ghostId, ghostDto) ->
            val localMember = memberDao.getMember(dto.id, ghostId)
            if (localMember == null) {
                val newMember = ghostDto.asMemberEntity(ghostId, dto.id)
                memberDao.insertMember(newMember)
            } else {
                if (!localMember.isDirty) {
                    val updatedMember = localMember.copy(
                        name = ghostDto.name,
                        mergedWithUid = ghostDto.mergedWithUid
                    )
                    if (updatedMember != localMember) {
                        memberDao.updateMember(updatedMember)
                    }
                }
            }
        }

        dto.members.forEach { memberId ->
            val profile = dto.memberProfiles?.get(memberId)
            Log.d("Sync", "Processing member $memberId. Profile found: ${profile != null}, Name: ${profile?.displayName}")
            
            var memberName = profile?.displayName?.takeIf { it.isNotBlank() }
            val photoUrl = profile?.photoUrl
            
            if (memberName == null && memberId == currentUserId) {
                memberName = authRepository.getUserName()
            }
            
            val finalName = memberName ?: "Участник"
            
            val localMember = memberDao.getMember(dto.id, memberId)
            if (localMember == null) {
                 val newMember = MemberEntity(
                    id = memberId,
                    groupId = dto.id,
                    name = finalName,
                    photoUrl = photoUrl,
                    isGhost = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isDirty = false
                )
                memberDao.insertMember(newMember)
                Log.d("Sync", "Inserted new member $memberId with name $finalName")
            } else {
                if (!localMember.isDirty && (localMember.name != finalName || localMember.photoUrl != photoUrl)) {
                    memberDao.updateMember(localMember.copy(name = finalName, photoUrl = photoUrl))
                    Log.d("Sync", "Updated member $memberId to name $finalName")
                }
            }
        }
    }

    private fun shouldUpdateLocal(localEntity: GroupEntity?, dto: GroupDto): Boolean {
        if (localEntity == null) {
            Log.d("Sync", "Group ${dto.name} (ID: ${dto.id}) is new -> saving.")
            return true
        }
        
        if (!localEntity.isDirty) {
            Log.d("Sync", "Group ${dto.name} is locally clean -> updating from server.")
            return true
        }

        val isServerNewer = dto.updatedAt > localEntity.updatedAt
        Log.d("Sync", "Conflict for ${dto.name}: LocalTime=${localEntity.updatedAt}, ServerTime=${dto.updatedAt}. ServerNewer=$isServerNewer")
        return isServerNewer
    }
}