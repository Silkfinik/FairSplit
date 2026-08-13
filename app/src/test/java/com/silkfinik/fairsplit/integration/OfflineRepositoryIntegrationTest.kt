package com.silkfinik.fairsplit.integration

import com.google.firebase.storage.FirebaseStorage
import com.silkfinik.fairsplit.core.common.util.ImageCompressor
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.TimeProvider
import com.silkfinik.fairsplit.core.data.datasource.CloudFunctionsDataSource
import com.silkfinik.fairsplit.core.data.repository.OfflineGroupRepository
import com.silkfinik.fairsplit.core.data.repository.OfflineMemberRepository
import com.silkfinik.fairsplit.core.data.sync.listener.GroupRealtimeListener
import com.silkfinik.fairsplit.core.data.worker.WorkManagerSyncManager
import com.silkfinik.fairsplit.core.database.dao.GroupDao
import com.silkfinik.fairsplit.core.database.dao.MemberDao
import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import com.silkfinik.fairsplit.core.database.entity.MemberEntity
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.model.Member
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineRepositoryIntegrationTest {

    private lateinit var groupDao: InMemoryGroupDao
    private lateinit var memberDao: InMemoryMemberDao

    private lateinit var groupRepository: OfflineGroupRepository
    private lateinit var memberRepository: OfflineMemberRepository

    private val groupRealtimeListener: GroupRealtimeListener = mockk(relaxed = true)
    private val workManagerSyncManager: WorkManagerSyncManager = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()
    private val cloudFunctionsDataSource: CloudFunctionsDataSource = mockk()
    private val storage: FirebaseStorage = mockk(relaxed = true)
    private val imageCompressor: ImageCompressor = mockk(relaxed = true)

    private var currentTime = 1000L
    private val testTimeProvider = object : TimeProvider {
        override fun now(): Long = currentTime
        override suspend fun initialize() {}
    }

    @Before
    fun setUp() {
        groupDao = InMemoryGroupDao()
        memberDao = InMemoryMemberDao()

        every { authRepository.getUserName() } returns "Test Owner"
        every { authRepository.getPhotoUrl() } returns "https://example.com/avatar.png"
        every { authRepository.getUserId() } returns "owner_user_id"

        groupRepository = OfflineGroupRepository(
            groupDao = groupDao,
            memberDao = memberDao,
            groupRealtimeListener = groupRealtimeListener,
            workManagerSyncManager = workManagerSyncManager,
            authRepository = authRepository,
            cloudFunctionsDataSource = cloudFunctionsDataSource,
            timeProvider = testTimeProvider,
            storage = storage,
            imageCompressor = imageCompressor
        )

        memberRepository = OfflineMemberRepository(
            memberDao = memberDao,
            cloudFunctionsDataSource = cloudFunctionsDataSource,
            authRepository = authRepository,
            workManagerSyncManager = workManagerSyncManager,
            timeProvider = testTimeProvider
        )
    }

    @Test
    fun `createGroup persists group and owner member to DAOs, sets dirty flag, triggers sync, and emits via Flow`() = runTest {
        currentTime = 5000L

        val result = groupRepository.createGroup("Vacation in Spain", Currency.EUR, "owner_user_id")
        assertTrue(result is Result.Success)
        val createdGroupId = (result as Result.Success).data

        val savedGroupEntity = groupDao.getGroupById(createdGroupId)
        assertNotNull(savedGroupEntity)
        assertEquals("Vacation in Spain", savedGroupEntity!!.name)
        assertEquals(Currency.EUR, savedGroupEntity.currency)
        assertEquals("owner_user_id", savedGroupEntity.ownerId)
        assertEquals(5000L, savedGroupEntity.createdAt)
        assertEquals(5000L, savedGroupEntity.updatedAt)
        assertTrue(savedGroupEntity.isDirty)

        val savedMemberEntity = memberDao.getMember(createdGroupId, "owner_user_id")
        assertNotNull(savedMemberEntity)
        assertEquals("Test Owner", savedMemberEntity!!.name)
        assertEquals("https://example.com/avatar.png", savedMemberEntity.photoUrl)
        assertEquals(false, savedMemberEntity.isGhost)
        assertTrue(savedMemberEntity.isDirty)

        coVerify(atLeast = 1) { workManagerSyncManager.scheduleSync() }

        val allGroups = groupRepository.getGroups().first()
        assertEquals(1, allGroups.size)
        assertEquals(createdGroupId, allGroups[0].id)
        assertEquals("Vacation in Spain", allGroups[0].name)
        assertEquals(Currency.EUR, allGroups[0].currency)

        val groupMembers = memberRepository.getMembers(createdGroupId).first()
        assertEquals(1, groupMembers.size)
        assertEquals("owner_user_id", groupMembers[0].id)
        assertEquals("Test Owner", groupMembers[0].name)
    }

    @Test
    fun `updateGroup modifies entity in DAO with new timestamp, marks dirty, and updates reactive flow`() = runTest {
        currentTime = 1000L
        val createResult = groupRepository.createGroup("Road Trip", Currency.USD, "owner_user_id")
        val groupId = (createResult as Result.Success).data

        currentTime = 2500L
        val updateResult = groupRepository.updateGroup(
            Group(id = groupId, name = "Road Trip: West Coast", currency = Currency.USD)
        )
        assertTrue(updateResult is Result.Success)

        val updatedEntity = groupDao.getGroupById(groupId)
        assertNotNull(updatedEntity)
        assertEquals("Road Trip: West Coast", updatedEntity!!.name)
        assertEquals(2500L, updatedEntity.updatedAt)
        assertTrue(updatedEntity.isDirty)

        val groupModel = groupRepository.getGroup(groupId).first()
        assertNotNull(groupModel)
        assertEquals("Road Trip: West Coast", groupModel!!.name)
    }

    @Test
    fun `addMember and deleteMember updates DAO and emits through member flow`() = runTest {
        val groupId = "group_members_test"
        val member2 = Member(
            id = "user_bob",
            groupId = groupId,
            name = "Bob",
            isGhost = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val addResult = memberRepository.addMember(member2)
        assertTrue(addResult is Result.Success)

        val membersAfterAdd = memberRepository.getMembers(groupId).first()
        assertEquals(1, membersAfterAdd.size)
        assertEquals("user_bob", membersAfterAdd[0].id)
        assertEquals("Bob", membersAfterAdd[0].name)

        val deleteResult = memberRepository.deleteMember(groupId, "user_bob")
        assertTrue(deleteResult is Result.Success)

        val membersAfterDelete = memberRepository.getMembers(groupId).first()
        assertEquals(0, membersAfterDelete.size)
    }

    @Test
    fun `claimGhost integrates cloud functions with local member resolution and clears dirty flag`() = runTest {
        val groupId = "group_ghost_test"
        val ghostId = "ghost_charlie"

        val ghostEntity = MemberEntity(
            id = ghostId,
            groupId = groupId,
            name = "Charlie (Ghost)",
            isGhost = true,
            mergedWithUid = null,
            createdAt = 1000L,
            updatedAt = 1000L,
            isDirty = false
        )
        memberDao.insertMember(ghostEntity)

        coEvery { cloudFunctionsDataSource.claimGhost(groupId, ghostId) } returns Result.Success(Unit)
        every { authRepository.getUserId() } returns "real_charlie_uid"

        currentTime = 3500L
        val claimResult = memberRepository.claimGhost(groupId, ghostId)
        assertTrue(claimResult is Result.Success)

        val resolvedMember = memberDao.getMember(groupId, ghostId)
        assertNotNull(resolvedMember)
        assertEquals("real_charlie_uid", resolvedMember!!.mergedWithUid)
        assertEquals(3500L, resolvedMember.updatedAt)
        assertEquals(false, resolvedMember.isDirty)

        val members = memberRepository.getMembers(groupId).first()
        assertEquals(1, members.size)
        assertEquals("real_charlie_uid", members[0].mergedWithUid)
    }

    private class InMemoryGroupDao : GroupDao {
        private val groups = MutableStateFlow<Map<String, GroupEntity>>(emptyMap())

        override suspend fun insertGroup(group: GroupEntity): Long {
            groups.value = groups.value + (group.id to group)
            return 1L
        }

        override suspend fun updateGroup(group: GroupEntity) {
            groups.value = groups.value + (group.id to group)
        }

        override suspend fun deleteGroup(group: GroupEntity) {
            groups.value = groups.value - group.id
        }

        override fun getGroups(): Flow<List<GroupEntity>> {
            return groups.map { it.values.toList().sortedByDescending { g -> g.createdAt } }
        }

        override fun getGroup(groupId: String): Flow<GroupEntity?> {
            return groups.map { it[groupId] }
        }

        override suspend fun getGroupById(groupId: String): GroupEntity? {
            return groups.value[groupId]
        }

        override suspend fun getDirtyGroups(): List<GroupEntity> {
            return groups.value.values.filter { it.isDirty }
        }

        override suspend fun markGroupAsSynced(id: String, lastUpdated: Long) {
            val group = groups.value[id]
            if (group != null && group.updatedAt == lastUpdated) {
                groups.value = groups.value + (id to group.copy(isDirty = false))
            }
        }
    }

    private class InMemoryMemberDao : MemberDao {
        private val members = MutableStateFlow<Map<String, MemberEntity>>(emptyMap())

        override suspend fun insertMember(member: MemberEntity): Long {
            val key = "${member.groupId}_${member.id}"
            members.value = members.value + (key to member)
            return 1L
        }

        override suspend fun updateMember(member: MemberEntity) {
            val key = "${member.groupId}_${member.id}"
            members.value = members.value + (key to member)
        }

        override suspend fun deleteMember(member: MemberEntity) {
            val key = "${member.groupId}_${member.id}"
            members.value = members.value - key
        }

        override fun getMembers(groupId: String): Flow<List<MemberEntity>> {
            return members.map { map ->
                map.values.filter { it.groupId == groupId }.sortedBy { it.name }
            }
        }

        override suspend fun getMembersSync(groupId: String): List<MemberEntity> {
            return members.value.values.filter { it.groupId == groupId }
        }

        override suspend fun getMember(groupId: String, memberId: String): MemberEntity? {
            val key = "${groupId}_${memberId}"
            return members.value[key]
        }

        override suspend fun markMembersAsSynced(groupId: String) {
            members.value = members.value.mapValues { (_, member) ->
                if (member.groupId == groupId) member.copy(isDirty = false) else member
            }
        }
    }
}
