package com.silkfinik.fairsplit.core.data.mapper

import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.network.model.GroupDto
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupMapperTest {

    private val testGroupEntity = GroupEntity(
        id = "group1",
        name = "Trip",
        currency = Currency.EUR,
        ownerId = "user1",
        createdAt = 1000L,
        updatedAt = 2000L,
        isDirty = true
    )

    private val testGroup = Group(
        id = "group1",
        name = "Trip",
        currency = Currency.EUR
    )

    private val testGroupDto = GroupDto(
        id = "group1",
        name = "Trip",
        currency = "EUR",
        ownerId = "user1",
        createdAt = 1000L,
        updatedAt = 2000L
    )

    @Test
    fun `GroupEntity to Domain Model`() {
        val domainModel = testGroupEntity.asDomainModel()
        assertEquals(testGroup, domainModel)
    }

    @Test
    fun `GroupEntity to Dto`() {
        val dto = testGroupEntity.asDto()
        assertEquals(testGroupDto, dto)
    }

    @Test
    fun `Dto to GroupEntity`() {
        val entity = testGroupDto.asEntity()
        val expectedEntity = testGroupEntity.copy(isDirty = false)
        assertEquals(expectedEntity, entity)
    }
}
