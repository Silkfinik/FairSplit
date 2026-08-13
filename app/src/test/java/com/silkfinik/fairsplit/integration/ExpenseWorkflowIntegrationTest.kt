package com.silkfinik.fairsplit.integration

import com.google.firebase.firestore.FirebaseFirestore
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.TimeProvider
import com.silkfinik.fairsplit.core.data.repository.OfflineExpenseRepository
import com.silkfinik.fairsplit.core.data.sync.listener.ExpenseRealtimeListener
import com.silkfinik.fairsplit.core.data.worker.WorkManagerSyncManager
import com.silkfinik.fairsplit.core.database.dao.ExpenseDao
import com.silkfinik.fairsplit.core.database.entity.ExpenseEntity
import com.silkfinik.fairsplit.core.domain.model.AppError
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.usecase.expense.DeleteExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.SaveExpenseUseCase
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.model.enums.SplitType
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpenseWorkflowIntegrationTest {

    private lateinit var expenseDao: InMemoryExpenseDao
    private lateinit var expenseRepository: OfflineExpenseRepository
    private lateinit var saveExpenseUseCase: SaveExpenseUseCase
    private lateinit var deleteExpenseUseCase: DeleteExpenseUseCase

    private val expenseRealtimeListener: ExpenseRealtimeListener = mockk(relaxed = true)
    private val workManagerSyncManager: WorkManagerSyncManager = mockk(relaxed = true)
    private val firestore: FirebaseFirestore = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()
    private val groupRepository: GroupRepository = mockk()

    private var currentTime = 1000L
    private val testTimeProvider = object : TimeProvider {
        override fun now(): Long = currentTime
        override suspend fun initialize() {}
    }

    private val testGroupId = "group_trip"
    private val testGroup = Group(id = testGroupId, name = "Trip to Baikal", currency = Currency.RUB)

    @Before
    fun setUp() {
        expenseDao = InMemoryExpenseDao()

        every { authRepository.getUserId() } returns "user_alice"
        every { groupRepository.getGroup(testGroupId) } returns flowOf(testGroup)

        expenseRepository = OfflineExpenseRepository(
            expenseDao = expenseDao,
            expenseRealtimeListener = expenseRealtimeListener,
            workManagerSyncManager = workManagerSyncManager,
            firestore = firestore,
            timeProvider = testTimeProvider
        )

        saveExpenseUseCase = SaveExpenseUseCase(
            expenseRepository = expenseRepository,
            groupRepository = groupRepository,
            authRepository = authRepository,
            timeProvider = testTimeProvider
        )

        deleteExpenseUseCase = DeleteExpenseUseCase(expenseRepository)
    }

    @Test
    fun `createExpense via SaveExpenseUseCase inserts entity in DAO, marks dirty, triggers sync, and emits via Flow`() = runTest {
        currentTime = 2000L

        val params = SaveExpenseUseCase.Params(
            groupId = testGroupId,
            description = "Tickets to museum",
            amount = 1800.0,
            category = "entertainment",
            payerId = "user_alice",
            splits = mapOf("user_alice" to 900.0, "user_bob" to 900.0),
            splitType = SplitType.EQUAL,
            splitData = emptyMap()
        )

        val result = saveExpenseUseCase(params)
        assertTrue(result is Result.Success)

        val allEntities = expenseDao.getDirtyExpenses()
        assertEquals(1, allEntities.size)
        val entity = allEntities[0]
        assertEquals("Tickets to museum", entity.description)
        assertEquals(1800.0, entity.amount, 0.001)
        assertEquals(Currency.RUB, entity.currency)
        assertEquals("user_alice", entity.creatorId)
        assertEquals(2000L, entity.createdAt)
        assertEquals(2000L, entity.updatedAt)
        assertTrue(entity.isDirty)
        assertEquals(false, entity.isDeleted)

        coVerify(atLeast = 1) { workManagerSyncManager.scheduleSync() }

        val domainExpenses = expenseRepository.getExpensesForGroup(testGroupId).first()
        assertEquals(1, domainExpenses.size)
        assertEquals("Tickets to museum", domainExpenses[0].description)
        assertEquals(1800.0, domainExpenses[0].amount, 0.001)
        assertEquals(2, domainExpenses[0].splits.size)
    }

    @Test
    fun `updateExpense via SaveExpenseUseCase updates existing entity, updates timestamp, and triggers sync`() = runTest {
        currentTime = 1000L
        val createParams = SaveExpenseUseCase.Params(
            groupId = testGroupId,
            description = "Lunch",
            amount = 500.0,
            category = "eating_out",
            payerId = "user_alice",
            splits = mapOf("user_alice" to 250.0, "user_bob" to 250.0),
            splitType = SplitType.EQUAL,
            splitData = emptyMap()
        )
        saveExpenseUseCase(createParams)

        val createdExpense = expenseRepository.getExpensesForGroup(testGroupId).first()[0]
        val expenseId = createdExpense.id

        currentTime = 3000L
        val updateParams = SaveExpenseUseCase.Params(
            groupId = testGroupId,
            expenseId = expenseId,
            description = "Business Lunch",
            amount = 700.0,
            category = "eating_out",
            payerId = "user_alice",
            splits = mapOf("user_alice" to 350.0, "user_bob" to 350.0),
            splitType = SplitType.EQUAL,
            splitData = emptyMap()
        )
        val updateResult = saveExpenseUseCase(updateParams)
        assertTrue(updateResult is Result.Success)

        val updatedEntity = expenseDao.getExpenseById(expenseId)
        assertNotNull(updatedEntity)
        assertEquals("Business Lunch", updatedEntity!!.description)
        assertEquals(700.0, updatedEntity.amount, 0.001)
        assertEquals(3000L, updatedEntity.updatedAt)
        assertTrue(updatedEntity.isDirty)

        val expenseModel = expenseRepository.getExpense(expenseId).first()
        assertNotNull(expenseModel)
        assertEquals("Business Lunch", expenseModel!!.description)
        assertEquals(700.0, expenseModel.amount, 0.001)
    }

    @Test
    fun `deleteExpense via DeleteExpenseUseCase marks isDeleted in DAO and filters from group expenses flow`() = runTest {
        currentTime = 1000L
        val createParams = SaveExpenseUseCase.Params(
            groupId = testGroupId,
            description = "Coffee",
            amount = 300.0,
            category = "eating_out",
            payerId = "user_alice",
            splits = mapOf("user_alice" to 300.0),
            splitType = SplitType.EXACT,
            splitData = emptyMap()
        )
        saveExpenseUseCase(createParams)

        val createdExpense = expenseRepository.getExpensesForGroup(testGroupId).first()[0]

        currentTime = 2000L
        val deleteResult = deleteExpenseUseCase(createdExpense.id)
        assertTrue(deleteResult is Result.Success)

        val entityInDao = expenseDao.getExpenseById(createdExpense.id)
        assertNotNull(entityInDao)
        assertTrue(entityInDao!!.isDeleted)
        assertTrue(entityInDao.isDirty)
        assertEquals(2000L, entityInDao.updatedAt)

        val activeExpenses = expenseRepository.getExpensesForGroup(testGroupId).first()
        assertTrue(activeExpenses.isEmpty())
    }

    @Test
    fun `saveExpense without authorization returns NotAuthorized and does not touch DAO`() = runTest {
        every { authRepository.getUserId() } returns null

        val params = SaveExpenseUseCase.Params(
            groupId = testGroupId,
            description = "Unauthorized Expense",
            amount = 100.0,
            category = "other",
            payerId = "user_alice",
            splits = mapOf("user_alice" to 100.0),
            splitType = SplitType.EQUAL,
            splitData = emptyMap()
        )

        val result = saveExpenseUseCase(params)
        assertTrue(result is Result.Error)
        assertEquals(AppError.Auth.NotAuthorized, (result as Result.Error).error)
        assertTrue(expenseDao.getDirtyExpenses().isEmpty())
    }

    private class InMemoryExpenseDao : ExpenseDao {
        private val expenses = MutableStateFlow<Map<String, ExpenseEntity>>(emptyMap())

        override fun getExpensesForGroup(groupId: String): Flow<List<ExpenseEntity>> {
            return expenses.map { map ->
                map.values
                    .filter { it.groupId == groupId && !it.isDeleted }
                    .sortedByDescending { it.date }
            }
        }

        override fun getExpense(expenseId: String): Flow<ExpenseEntity?> {
            return expenses.map { it[expenseId] }
        }

        override suspend fun getExpenseById(expenseId: String): ExpenseEntity? {
            return expenses.value[expenseId]
        }

        override suspend fun insertExpense(expense: ExpenseEntity) {
            expenses.value = expenses.value + (expense.id to expense)
        }

        override suspend fun updateExpense(expense: ExpenseEntity) {
            expenses.value = expenses.value + (expense.id to expense)
        }

        override suspend fun getDirtyExpenses(): List<ExpenseEntity> {
            return expenses.value.values.filter { it.isDirty }
        }

        override suspend fun markExpenseAsSynced(id: String, syncedAt: Long) {
            val entity = expenses.value[id]
            if (entity != null) {
                expenses.value = expenses.value + (id to entity.copy(isDirty = false, updatedAt = syncedAt))
            }
        }
    }
}
