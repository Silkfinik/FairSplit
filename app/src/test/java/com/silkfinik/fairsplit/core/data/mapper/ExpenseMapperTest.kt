package com.silkfinik.fairsplit.core.data.mapper

import com.silkfinik.fairsplit.core.database.entity.ExpenseEntity
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.network.model.ExpenseDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseMapperTest {

    private val testExpenseEntity = ExpenseEntity(
        id = "exp1",
        groupId = "group1",
        description = "Dinner",
        amount = 100.0,
        currency = Currency.USD,
        date = 1000L,
        creatorId = "user1",
        payers = mapOf("user1" to 100.0),
        splits = mapOf("user1" to 50.0, "user2" to 50.0),
        category = "Food",
        isDeleted = false,
        createdAt = 2000L,
        updatedAt = 3000L,
        isDirty = true
    )

    private val testExpense = Expense(
        id = "exp1",
        groupId = "group1",
        description = "Dinner",
        amount = 100.0,
        currency = Currency.USD,
        date = 1000L,
        creatorId = "user1",
        payers = mapOf("user1" to 100.0),
        splits = mapOf("user1" to 50.0, "user2" to 50.0),
        category = "Food",
        isDeleted = false,
        createdAt = 2000L,
        updatedAt = 3000L
    )

    private val testExpenseDto = ExpenseDto(
        id = "exp1",
        description = "Dinner",
        amount = 100.0,
        currency = "USD",
        date = 1000L,
        creatorId = "user1",
        payers = mapOf("user1" to 100.0),
        splits = mapOf("user1" to 50.0, "user2" to 50.0),
        category = "Food",
        isDeleted = false,
        createdAt = 2000L,
        updatedAt = 3000L
    )

    @Test
    fun `ExpenseEntity to Domain Model`() {
        val domainModel = testExpenseEntity.asDomainModel()
        assertEquals(testExpense, domainModel)
    }

    @Test
    fun `Domain Model to ExpenseEntity`() {
        val entity = testExpense.asEntity(isDirty = true)
        assertEquals(testExpenseEntity, entity)
    }

    @Test
    fun `ExpenseEntity to Dto`() {
        val dto = testExpenseEntity.asDto()
        assertEquals(testExpenseDto, dto)
    }

    @Test
    fun `Dto to ExpenseEntity`() {
        val entity = testExpenseDto.asEntity(groupId = "group1")
        val expectedEntity = testExpenseEntity.copy(isDirty = false)
        assertEquals(expectedEntity, entity)
    }
}
