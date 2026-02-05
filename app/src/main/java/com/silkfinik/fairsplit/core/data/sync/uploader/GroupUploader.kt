package com.silkfinik.fairsplit.core.data.sync.uploader

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.silkfinik.fairsplit.core.data.mapper.asDto
import com.silkfinik.fairsplit.core.data.mapper.asGhostDto
import com.silkfinik.fairsplit.core.data.sync.FirestoreRoutes
import com.silkfinik.fairsplit.core.database.dao.GroupDao
import com.silkfinik.fairsplit.core.database.dao.MemberDao
import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupUploader @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val groupDao: GroupDao,
    private val memberDao: MemberDao,
    authRepository: AuthRepository
) : BaseFirestoreUploader<GroupEntity>(firestore, authRepository) {

    override val logTag: String = "GroupSync"

    override suspend fun getDirtyItems(): List<GroupEntity> {
        return groupDao.getDirtyGroups()
    }

    override suspend fun addToBatch(batch: WriteBatch, item: GroupEntity, userId: String) {
        val docRef = firestore.collection(FirestoreRoutes.GROUPS).document(item.id)

        val members = memberDao.getMembersSync(item.id)

        var dto = item.asDto()

        val realMemberIds = members.filter { !it.isGhost }.map { it.id }
        val ghostsMap = members.filter { it.isGhost }.associate { member ->
            member.id to member.asGhostDto()
        }

        dto = dto.copy(
            members = realMemberIds,
            ghosts = ghostsMap
        )

        batch.set(docRef, dto, SetOptions.merge())
    }

    override suspend fun markAsSynced(items: List<GroupEntity>) {
        items.forEach { group ->
            groupDao.markGroupAsSynced(group.id, group.updatedAt)
            memberDao.markMembersAsSynced(group.id)
        }
    }
}