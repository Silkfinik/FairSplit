package com.silkfinik.fairsplit.integration

import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.domain.repository.PaymentRepository
import com.silkfinik.fairsplit.core.domain.service.MemberResolutionService
import com.silkfinik.fairsplit.core.domain.usecase.group.CalculateGroupBalanceUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupDetailsScreenDataUseCase
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.Payment
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus
import com.silkfinik.fairsplit.core.model.enums.SplitType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GroupBalanceIntegrationTest {

    private lateinit var memberResolutionService: MemberResolutionService
    private lateinit var calculateGroupBalanceUseCase: CalculateGroupBalanceUseCase
    private lateinit var getGroupDetailsScreenDataUseCase: GetGroupDetailsScreenDataUseCase

    private val groupRepository: GroupRepository = mockk()
    private val expenseRepository: ExpenseRepository = mockk()
    private val paymentRepository: PaymentRepository = mockk()
    private val memberRepository: MemberRepository = mockk()
    private val authRepository: AuthRepository = mockk()

    private val groupFlow = MutableStateFlow<Group?>(null)
    private val expensesFlow = MutableStateFlow<List<Expense>>(emptyList())
    private val paymentsFlow = MutableStateFlow<List<Payment>>(emptyList())
    private val membersFlow = MutableStateFlow<List<Member>>(emptyList())
    private val currentUserIdFlow = MutableStateFlow<String?>("user_alice")

    private val testGroupId = "group_trip_123"

    @Before
    fun setUp() {
        memberResolutionService = MemberResolutionService()
        calculateGroupBalanceUseCase = CalculateGroupBalanceUseCase(memberResolutionService)

        every { groupRepository.getGroup(testGroupId) } returns groupFlow
        every { expenseRepository.getExpensesForGroup(testGroupId) } returns expensesFlow
        every { paymentRepository.getPayments(testGroupId) } returns paymentsFlow
        every { memberRepository.getMembers(testGroupId) } returns membersFlow
        every { authRepository.currentUserId } returns currentUserIdFlow

        getGroupDetailsScreenDataUseCase = GetGroupDetailsScreenDataUseCase(
            groupRepository = groupRepository,
            expenseRepository = expenseRepository,
            paymentRepository = paymentRepository,
            memberRepository = memberRepository,
            authRepository = authRepository,
            calculateGroupBalanceUseCase = calculateGroupBalanceUseCase
        )

        groupFlow.value = Group(id = testGroupId, name = "Trip to Altai", currency = Currency.RUB)
    }

    @Test
    fun `multi-payer expenses and unequal splits correctly calculate net balances and preserve zero-sum invariant`() = runTest {
        val alice = Member(id = "alice", groupId = testGroupId, name = "Alice", isGhost = false, createdAt = 1000L, updatedAt = 1000L)
        val bob = Member(id = "bob", groupId = testGroupId, name = "Bob", isGhost = false, createdAt = 1000L, updatedAt = 1000L)
        val charlie = Member(id = "charlie", groupId = testGroupId, name = "Charlie", isGhost = false, createdAt = 1000L, updatedAt = 1000L)
        membersFlow.value = listOf(alice, bob, charlie)

        val expense1 = Expense(
            id = "exp_1",
            groupId = testGroupId,
            description = "Hotel",
            amount = 3000.0,
            currency = Currency.RUB,
            date = 1000L,
            creatorId = "alice",
            payers = mapOf("alice" to 3000.0),
            splits = mapOf("alice" to 1000.0, "bob" to 1000.0, "charlie" to 1000.0),
            splitType = SplitType.EQUAL,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val expense2 = Expense(
            id = "exp_2",
            groupId = testGroupId,
            description = "Taxi for Charlie",
            amount = 600.0,
            currency = Currency.RUB,
            date = 2000L,
            creatorId = "bob",
            payers = mapOf("bob" to 600.0),
            splits = mapOf("charlie" to 600.0),
            splitType = SplitType.EXACT,
            createdAt = 2000L,
            updatedAt = 2000L
        )

        expensesFlow.value = listOf(expense1, expense2)

        val screenData = getGroupDetailsScreenDataUseCase(testGroupId).first()

        assertNotNull(screenData.group)
        assertEquals(3, screenData.members.size)
        assertEquals(2, screenData.expenses.size)

        assertEquals(2000.0, screenData.balances["alice"] ?: 0.0, 0.001)
        assertEquals(-400.0, screenData.balances["bob"] ?: 0.0, 0.001)
        assertEquals(-1600.0, screenData.balances["charlie"] ?: 0.0, 0.001)

        val totalNet = screenData.balances.values.sum()
        assertEquals(0.0, totalNet, 0.001)
    }

    @Test
    fun `ghost member resolution and payment settlement integrates seamlessly into screen data`() = runTest {
        val alice = Member(id = "user_alice", groupId = testGroupId, name = "Alice", isGhost = false, createdAt = 1000L, updatedAt = 1000L)
        val ghostDave = Member(
            id = "ghost_dave",
            groupId = testGroupId,
            name = "Dave (Ghost)",
            isGhost = true,
            mergedWithUid = "user_dave",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        val realDave = Member(
            id = "user_dave",
            groupId = testGroupId,
            name = "Dave",
            isGhost = false,
            createdAt = 2000L,
            updatedAt = 2000L
        )
        membersFlow.value = listOf(alice, ghostDave, realDave)

        val expense = Expense(
            id = "exp_ghost",
            groupId = testGroupId,
            description = "Dinner with Dave",
            amount = 1500.0,
            currency = Currency.RUB,
            date = 1500L,
            creatorId = "user_alice",
            payers = mapOf("user_alice" to 1500.0),
            splits = mapOf("user_alice" to 750.0, "ghost_dave" to 750.0),
            createdAt = 1500L,
            updatedAt = 1500L
        )
        expensesFlow.value = listOf(expense)

        var screenData = getGroupDetailsScreenDataUseCase(testGroupId).first()
        assertFalse(screenData.balances.containsKey("ghost_dave"))
        assertEquals(750.0, screenData.balances["user_alice"] ?: 0.0, 0.001)
        assertEquals(-750.0, screenData.balances["user_dave"] ?: 0.0, 0.001)

        val payment = Payment(
            id = "pay_1",
            groupId = testGroupId,
            payerId = "user_dave",
            receiverId = "user_alice",
            amount = 750.0,
            currency = Currency.RUB,
            status = PaymentStatus.CONFIRMED,
            createdAt = 3000L,
            updatedAt = 3000L
        )
        paymentsFlow.value = listOf(payment)

        screenData = getGroupDetailsScreenDataUseCase(testGroupId).first()
        assertEquals(0.0, screenData.balances["user_alice"] ?: 0.0, 0.001)
        assertEquals(0.0, screenData.balances["user_dave"] ?: 0.0, 0.001)
        assertEquals(0.0, screenData.balances.values.sum(), 0.001)
    }

    @Test
    fun `deleted expenses and unconfirmed payments are excluded from balance calculations`() = runTest {
        val alice = Member(id = "alice", groupId = testGroupId, name = "Alice", isGhost = false, createdAt = 1000L, updatedAt = 1000L)
        val bob = Member(id = "bob", groupId = testGroupId, name = "Bob", isGhost = false, createdAt = 1000L, updatedAt = 1000L)
        membersFlow.value = listOf(alice, bob)

        val activeExpense = Expense(
            id = "exp_active",
            groupId = testGroupId,
            description = "Lunch",
            amount = 1000.0,
            currency = Currency.RUB,
            date = 1000L,
            creatorId = "alice",
            payers = mapOf("alice" to 1000.0),
            splits = mapOf("alice" to 500.0, "bob" to 500.0),
            isDeleted = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val deletedExpense = Expense(
            id = "exp_deleted",
            groupId = testGroupId,
            description = "Cancelled Activity",
            amount = 2000.0,
            currency = Currency.RUB,
            date = 2000L,
            creatorId = "bob",
            payers = mapOf("bob" to 2000.0),
            splits = mapOf("alice" to 1000.0, "bob" to 1000.0),
            isDeleted = true,
            createdAt = 2000L,
            updatedAt = 2000L
        )

        val pendingPayment = Payment(
            id = "pay_pending",
            groupId = testGroupId,
            payerId = "bob",
            receiverId = "alice",
            amount = 500.0,
            currency = Currency.RUB,
            status = PaymentStatus.PENDING,
            createdAt = 3000L,
            updatedAt = 3000L
        )

        expensesFlow.value = listOf(activeExpense, deletedExpense)
        paymentsFlow.value = listOf(pendingPayment)

        val screenData = getGroupDetailsScreenDataUseCase(testGroupId).first()

        assertEquals(500.0, screenData.balances["alice"] ?: 0.0, 0.001)
        assertEquals(-500.0, screenData.balances["bob"] ?: 0.0, 0.001)
    }
}
