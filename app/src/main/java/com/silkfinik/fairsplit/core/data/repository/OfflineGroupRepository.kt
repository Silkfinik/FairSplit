package com.silkfinik.fairsplit.core.data.repository

import androidx.core.net.toUri
import com.google.firebase.storage.FirebaseStorage
import com.silkfinik.fairsplit.core.common.util.ImageCompressor
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.TimeProvider
import com.silkfinik.fairsplit.core.common.util.safeCall
import com.silkfinik.fairsplit.core.data.datasource.CloudFunctionsDataSource
import com.silkfinik.fairsplit.core.data.mapper.asDomainModel
import com.silkfinik.fairsplit.core.data.sync.listener.GroupRealtimeListener
import com.silkfinik.fairsplit.core.data.worker.WorkManagerSyncManager
import com.silkfinik.fairsplit.core.database.dao.GroupDao
import com.silkfinik.fairsplit.core.database.dao.MemberDao
import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import com.silkfinik.fairsplit.core.database.entity.MemberEntity
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import kotlinx.coroutines.tasks.await
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Group
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineGroupRepository @Inject constructor(
    private val groupDao: GroupDao,
    private val memberDao: MemberDao,
    private val groupRealtimeListener: GroupRealtimeListener,
    private val workManagerSyncManager: WorkManagerSyncManager,
    private val authRepository: AuthRepository,
    private val cloudFunctionsDataSource: CloudFunctionsDataSource,
    private val timeProvider: TimeProvider,
    private val storage: FirebaseStorage,
    private val imageCompressor: ImageCompressor
) : GroupRepository {

    override fun getGroups(): Flow<List<Group>> {
        return groupDao.getGroups().map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    override fun getGroup(id: String): Flow<Group?> {
        return groupDao.getGroup(id).map { it?.asDomainModel() }
    }

    override suspend fun createGroup(name: String, currency: Currency, ownerId: String): Result<String> = safeCall {
        val newId = UUID.randomUUID().toString()
        val timestamp = timeProvider.now()

        val group = GroupEntity(
            id = newId,
            name = name,
            currency = currency,
            ownerId = ownerId,
            createdAt = timestamp,
            updatedAt = timestamp,
            isDirty = true
        )

        groupDao.insertGroup(group)

        val member = MemberEntity(
            id = ownerId,
            groupId = newId,
            name = authRepository.getUserName() ?: "Я",
            photoUrl = authRepository.getPhotoUrl(),
            isGhost = false,
            createdAt = timestamp,
            updatedAt = timestamp,
            isDirty = true
        )
        memberDao.insertMember(member)

        workManagerSyncManager.scheduleSync()
        newId
    }

    override suspend fun updateGroup(group: Group): Result<Unit> = safeCall {
        val existingEntity = groupDao.getGroupById(group.id) ?: throw Exception("Группа не найдена")

        val updatedGroup = existingEntity.copy(
            name = group.name,
            currency = group.currency,
            updatedAt = timeProvider.now(),
            isDirty = true
        )
        groupDao.updateGroup(updatedGroup)
        workManagerSyncManager.scheduleSync()
    }

    override suspend fun joinGroup(code: String): Result<String> {
        val result = cloudFunctionsDataSource.joinByInviteCode(code)
        if (result is Result.Success) {
            workManagerSyncManager.scheduleSync()
        }
        return result
    }

    override suspend fun generateInviteCode(groupId: String): Result<String> {
        val result = cloudFunctionsDataSource.createInviteCode(groupId)

        if (result is Result.Success) {
            val code = result.data
            val localGroup = groupDao.getGroupById(groupId)
            if (localGroup != null) {
                groupDao.updateGroup(localGroup.copy(inviteCode = code))
            }
        }
        return result
    }

    override suspend fun uploadGroupAvatar(groupId: String, imageUri: String): Result<String> = safeCall {
        val uri = imageUri.toUri()
        val compressedBytes = imageCompressor.compress(uri)
            ?: throw Exception("Не удалось обрезать и сжать изображение")

        val filename = "group_avatars/${groupId}_${timeProvider.now()}.webp"
        val ref = storage.reference.child(filename)

        ref.putBytes(compressedBytes).await()
        val downloadUrl = ref.downloadUrl.await().toString()

        val existingEntity = groupDao.getGroupById(groupId)
        if (existingEntity != null) {
            val updatedGroup = existingEntity.copy(
                avatarUrl = downloadUrl,
                updatedAt = timeProvider.now(),
                isDirty = true
            )
            groupDao.updateGroup(updatedGroup)
            workManagerSyncManager.scheduleSync()
        }
        
        downloadUrl
    }

    override fun startSync() {
        groupRealtimeListener.startListening()
    }

    override fun stopSync() {
        groupRealtimeListener.stopListening()
    }
}