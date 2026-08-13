package com.silkfinik.fairsplit.core.domain.usecase.group

import com.silkfinik.fairsplit.core.domain.service.MemberResolutionService
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.Payment
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus
import com.silkfinik.fairsplit.core.model.enums.SplitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculateGroupBalanceUseCaseTest {

    private lateinit var useCase: CalculateGroupBalanceUseCase
    private val memberResolutionService = MemberResolutionService()

    private val members = listOf(
        Member(id = "u1", groupId = "g1", name = "Alice", isGhost = false, createdAt = 0L, updatedAt = 0L),
        Member(id = "u2", groupId = "g1", name = "Bob", isGhost = false, createdAt = 0L, updatedAt = 0L),
        Member(id = "u3", groupId = "g1", name = "Charlie", isGhost = false, createdAt = 0L, updatedAt = 0L)
    )

    @Before
    fun setUp() {
        useCase = CalculateGroupBalanceUseCase(memberResolutionService)
    }

    @Test
    fun `empty expenses and payments returns empty balance map`() {
        val result = useCase(emptyList(), members, emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `single expense calculates correct positive and negative balances`() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Groceries",
            amount = 90.0,
            currency = Currency.USD,
            date = 0L,
            creatorId = "u1",
            payers = mapOf("u1" to 90.0),
            splits = mapOf("u1" to 30.0, "u2" to 30.0, "u3" to 30.0),
            createdAt = 0L,
            updatedAt = 0L
        )

        val result = useCase(listOf(expense), members)

        // u1: paid 90, consumed 30 => +60
        // u2: paid 0, consumed 30 => -30
        // u3: paid 0, consumed 30 => -30
        assertEquals(60.0, result["u1"] ?: 0.0, 0.001)
        assertEquals(-30.0, result["u2"] ?: 0.0, 0.001)
        assertEquals(-30.0, result["u3"] ?: 0.0, 0.001)
        assertEquals(0.0, result.values.sum(), 0.001)
    }

    @Test
    fun `multiple expenses accumulate balances across members`() {
        val expense1 = Expense(
            id = "e1",
            groupId = "g1",
            description = "Dinner",
            amount = 100.0,
            currency = Currency.USD,
            date = 0L,
            creatorId = "u1",
            payers = mapOf("u1" to 100.0),
            splits = mapOf("u1" to 50.0, "u2" to 50.0),
            createdAt = 0L,
            updatedAt = 0L
        )
        val expense2 = Expense(
            id = "e2",
            groupId = "g1",
            description = "Drinks",
            amount = 40.0,
            currency = Currency.USD,
            date = 0L,
            creatorId = "u2",
            payers = mapOf("u2" to 40.0),
            splits = mapOf("u1" to 20.0, "u2" to 20.0),
            createdAt = 0L,
            updatedAt = 0L
        )

        val result = useCase(listOf(expense1, expense2), members)

        // u1: (+100 - 50) + (-20) = +30
        // u2: (-50) + (+40 - 20) = -30
        assertEquals(30.0, result["u1"] ?: 0.0, 0.001)
        assertEquals(-30.0, result["u2"] ?: 0.0, 0.001)
    }

    @Test
    fun `deleted expense is ignored`() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Cancelled",
            amount = 100.0,
            currency = Currency.USD,
            date = 0L,
            creatorId = "u1",
            payers = mapOf("u1" to 100.0),
            splits = mapOf("u2" to 100.0),
            isDeleted = true,
            createdAt = 0L,
            updatedAt = 0L
        )

        val result = useCase(listOf(expense), members)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `confirmed payments offset balances`() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Hotel",
            amount = 100.0,
            currency = Currency.USD,
            date = 0L,
            creatorId = "u1",
            payers = mapOf("u1" to 100.0),
            splits = mapOf("u2" to 100.0),
            createdAt = 0L,
            updatedAt = 0L
        )
        val payment = Payment(
            id = "p1",
            groupId = "g1",
            payerId = "u2",
            receiverId = "u1",
            amount = 100.0,
            currency = Currency.USD,
            status = PaymentStatus.CONFIRMED,
            createdAt = 0L,
            updatedAt = 0L
        )

        val result = useCase(listOf(expense), members, listOf(payment))
        assertEquals(0.0, result["u1"] ?: 0.0, 0.001)
        assertEquals(0.0, result["u2"] ?: 0.0, 0.001)
    }

    @Test
    fun `pending payments do not modify balances`() {
        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Hotel",
            amount = 100.0,
            currency = Currency.USD,
            date = 0L,
            creatorId = "u1",
            payers = mapOf("u1" to 100.0),
            splits = mapOf("u2" to 100.0),
            createdAt = 0L,
            updatedAt = 0L
        )
        val pendingPayment = Payment(
            id = "p1",
            groupId = "g1",
            payerId = "u2",
            receiverId = "u1",
            amount = 100.0,
            currency = Currency.USD,
            status = PaymentStatus.PENDING,
            createdAt = 0L,
            updatedAt = 0L
        )

        val result = useCase(listOf(expense), members, listOf(pendingPayment))
        assertEquals(100.0, result["u1"] ?: 0.0, 0.001)
        assertEquals(-100.0, result["u2"] ?: 0.0, 0.001)
    }

    @Test
    fun `ghost members are resolved to merged user and excluded from keys`() {
        val ghost = Member(
            id = "ghost_1",
            groupId = "g1",
            name = "Ghost",
            isGhost = true,
            mergedWithUid = "u2",
            createdAt = 0L,
            updatedAt = 0L
        )
        val testMembers = listOf(members[0], members[1], ghost)

        val expense = Expense(
            id = "e1",
            groupId = "g1",
            description = "Food",
            amount = 50.0,
            currency = Currency.USD,
            date = 0L,
            creatorId = "u1",
            payers = mapOf("u1" to 50.0),
            splits = mapOf("ghost_1" to 50.0),
            createdAt = 0L,
            updatedAt = 0L
        )

        val result = useCase(listOf(expense), testMembers)

        assertFalse(result.containsKey("ghost_1"))
        assertEquals(50.0, result["u1"] ?: 0.0, 0.001)
        assertEquals(-50.0, result["u2"] ?: 0.0, 0.001)
    }
}
