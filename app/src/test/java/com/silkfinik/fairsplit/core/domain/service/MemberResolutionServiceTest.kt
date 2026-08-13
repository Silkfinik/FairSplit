package com.silkfinik.fairsplit.core.domain.service

import com.silkfinik.fairsplit.core.model.Member
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MemberResolutionServiceTest {

    private lateinit var service: MemberResolutionService

    @Before
    fun setUp() {
        service = MemberResolutionService()
    }

    @Test
    fun `resolveId returns same id when member is not merged`() {
        val members = listOf(
            Member(id = "u1", groupId = "g1", name = "Alice", isGhost = false, createdAt = 0L, updatedAt = 0L)
        )
        val resolution = service.resolve(members)
        assertEquals("u1", resolution.resolveId("u1"))
        assertEquals("unknown_id", resolution.resolveId("unknown_id"))
    }

    @Test
    fun `resolveId redirects ghost member id to merged user uid`() {
        val members = listOf(
            Member(id = "ghost_1", groupId = "g1", name = "Bob (Ghost)", isGhost = true, mergedWithUid = "u2", createdAt = 0L, updatedAt = 0L),
            Member(id = "u2", groupId = "g1", name = "Bob", isGhost = false, createdAt = 0L, updatedAt = 0L)
        )
        val resolution = service.resolve(members)
        assertEquals("u2", resolution.resolveId("ghost_1"))
        assertEquals("u2", resolution.resolveId("u2"))
    }

    @Test
    fun `getDisplayMembers filters out merged ghost members`() {
        val members = listOf(
            Member(id = "u1", groupId = "g1", name = "Alice", isGhost = false, createdAt = 0L, updatedAt = 0L),
            Member(id = "ghost_1", groupId = "g1", name = "Bob (Ghost)", isGhost = true, mergedWithUid = "u2", createdAt = 0L, updatedAt = 0L),
            Member(id = "u2", groupId = "g1", name = "Bob", isGhost = false, createdAt = 0L, updatedAt = 0L)
        )
        val displayMembers = service.getDisplayMembers(members)
        assertEquals(2, displayMembers.size)
        assertEquals(listOf("u1", "u2"), displayMembers.map { it.id })
    }

    @Test
    fun `getDisplayMembers appends ghost names to merged member`() {
        val members = listOf(
            Member(id = "ghost_1", groupId = "g1", name = "Bobby", isGhost = true, mergedWithUid = "u2", createdAt = 0L, updatedAt = 0L),
            Member(id = "ghost_2", groupId = "g1", name = "Bob Work", isGhost = true, mergedWithUid = "u2", createdAt = 0L, updatedAt = 0L),
            Member(id = "u2", groupId = "g1", name = "Bob", isGhost = false, createdAt = 0L, updatedAt = 0L)
        )
        val displayMembers = service.getDisplayMembers(members)
        assertEquals(1, displayMembers.size)
        assertEquals("Bob (Bobby, Bob Work)", displayMembers[0].name)
    }

    @Test
    fun `getDisplayMembers preserves unmerged ghost members and regular members`() {
        val members = listOf(
            Member(id = "u1", groupId = "g1", name = "Alice", isGhost = false, createdAt = 0L, updatedAt = 0L),
            Member(id = "ghost_unmerged", groupId = "g1", name = "Charlie (Ghost)", isGhost = true, mergedWithUid = null, createdAt = 0L, updatedAt = 0L)
        )
        val displayMembers = service.getDisplayMembers(members)
        assertEquals(2, displayMembers.size)
        assertEquals("Alice", displayMembers[0].name)
        assertEquals("Charlie (Ghost)", displayMembers[1].name)
    }
}
