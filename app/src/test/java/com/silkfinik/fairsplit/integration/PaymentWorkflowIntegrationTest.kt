package com.silkfinik.fairsplit.integration

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.TimeProvider
import com.silkfinik.fairsplit.core.data.repository.OfflinePaymentRepository
import com.silkfinik.fairsplit.core.data.sync.listener.PaymentRealtimeListener
import com.silkfinik.fairsplit.core.data.worker.WorkManagerSyncManager
import com.silkfinik.fairsplit.core.database.dao.PaymentDao
import com.silkfinik.fairsplit.core.database.entity.PaymentEntity
import com.silkfinik.fairsplit.core.domain.model.AppError
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.usecase.payment.CreatePaymentUseCase
import com.silkfinik.fairsplit.core.domain.usecase.payment.UpdatePaymentStatusUseCase
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus
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

class PaymentWorkflowIntegrationTest {

    private lateinit var paymentDao: InMemoryPaymentDao
    private lateinit var paymentRepository: OfflinePaymentRepository
    private lateinit var createPaymentUseCase: CreatePaymentUseCase
    private lateinit var updatePaymentStatusUseCase: UpdatePaymentStatusUseCase

    private val paymentRealtimeListener: PaymentRealtimeListener = mockk(relaxed = true)
    private val workManagerSyncManager: WorkManagerSyncManager = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()
    private val groupRepository: GroupRepository = mockk()

    private var currentTime = 1000L
    private val testTimeProvider = object : TimeProvider {
        override fun now(): Long = currentTime
        override suspend fun initialize() {}
    }

    private val testGroupId = "group_split_456"
    private val testGroup = Group(id = testGroupId, name = "Flatmates", currency = Currency.EUR)

    @Before
    fun setUp() {
        paymentDao = InMemoryPaymentDao()

        every { authRepository.hasSession() } returns true
        every { groupRepository.getGroup(testGroupId) } returns flowOf(testGroup)

        paymentRepository = OfflinePaymentRepository(
            paymentDao = paymentDao,
            workManagerSyncManager = workManagerSyncManager,
            paymentRealtimeListener = paymentRealtimeListener,
            timeProvider = testTimeProvider
        )

        createPaymentUseCase = CreatePaymentUseCase(
            paymentRepository = paymentRepository,
            groupRepository = groupRepository,
            authRepository = authRepository,
            timeProvider = testTimeProvider
        )

        updatePaymentStatusUseCase = UpdatePaymentStatusUseCase(
            paymentRepository = paymentRepository,
            timeProvider = testTimeProvider
        )
    }

    @Test
    fun `createPayment via CreatePaymentUseCase inserts entity in DAO with PENDING status, triggers sync, and emits via flow`() = runTest {
        currentTime = 1500L

        val params = CreatePaymentUseCase.Params(
            groupId = testGroupId,
            payerId = "user_bob",
            receiverId = "user_alice",
            amount = 75.0
        )

        val result = createPaymentUseCase(params)
        assertTrue(result is Result.Success)

        val allPayments = paymentRepository.getPayments(testGroupId).first()
        assertEquals(1, allPayments.size)
        val payment = allPayments[0]
        assertEquals(testGroupId, payment.groupId)
        assertEquals("user_bob", payment.payerId)
        assertEquals("user_alice", payment.receiverId)
        assertEquals(75.0, payment.amount, 0.001)
        assertEquals(Currency.EUR, payment.currency)
        assertEquals(PaymentStatus.PENDING, payment.status)
        assertEquals(1500L, payment.createdAt)
        assertEquals(1500L, payment.updatedAt)

        coVerify(atLeast = 1) { workManagerSyncManager.scheduleSync() }
    }

    @Test
    fun `updatePaymentStatus transitions payment from PENDING to CONFIRMED and updates reactive flow`() = runTest {
        currentTime = 1000L
        createPaymentUseCase(
            CreatePaymentUseCase.Params(
                groupId = testGroupId,
                payerId = "user_bob",
                receiverId = "user_alice",
                amount = 120.0
            )
        )

        val createdPayment = paymentRepository.getPayments(testGroupId).first()[0]
        assertEquals(PaymentStatus.PENDING, createdPayment.status)

        currentTime = 2000L
        val updateResult = updatePaymentStatusUseCase(createdPayment.id, PaymentStatus.CONFIRMED)
        assertTrue(updateResult is Result.Success)

        val updatedPayment = paymentRepository.getPayment(createdPayment.id).first()
        assertNotNull(updatedPayment)
        assertEquals(PaymentStatus.CONFIRMED, updatedPayment!!.status)
        assertEquals(2000L, updatedPayment.updatedAt)

        coVerify(atLeast = 2) { workManagerSyncManager.scheduleSync() }
    }

    @Test
    fun `updatePaymentStatus rejects status change if payment is already finalized`() = runTest {
        currentTime = 1000L
        createPaymentUseCase(
            CreatePaymentUseCase.Params(
                groupId = testGroupId,
                payerId = "user_bob",
                receiverId = "user_alice",
                amount = 50.0
            )
        )
        val createdPayment = paymentRepository.getPayments(testGroupId).first()[0]

        updatePaymentStatusUseCase(createdPayment.id, PaymentStatus.CONFIRMED)

        val secondUpdateResult = updatePaymentStatusUseCase(createdPayment.id, PaymentStatus.REJECTED)
        assertTrue(secondUpdateResult is Result.Error)
        assertEquals(AppError.Payment.StatusAlreadyChanged, (secondUpdateResult as Result.Error).error)

        val finalPayment = paymentRepository.getPayment(createdPayment.id).first()
        assertEquals(PaymentStatus.CONFIRMED, finalPayment!!.status)
    }

    @Test
    fun `createPayment with invalid arguments fails and does not write to DAO`() = runTest {
        val selfTransferResult = createPaymentUseCase(
            CreatePaymentUseCase.Params(
                groupId = testGroupId,
                payerId = "user_alice",
                receiverId = "user_alice",
                amount = 100.0
            )
        )
        assertTrue(selfTransferResult is Result.Error)
        assertEquals(AppError.Payment.SelfTransferForbidden, (selfTransferResult as Result.Error).error)

        val zeroAmountResult = createPaymentUseCase(
            CreatePaymentUseCase.Params(
                groupId = testGroupId,
                payerId = "user_bob",
                receiverId = "user_alice",
                amount = -10.0
            )
        )
        assertTrue(zeroAmountResult is Result.Error)
        assertEquals(AppError.Payment.AmountTooLow, (zeroAmountResult as Result.Error).error)

        val payments = paymentRepository.getPayments(testGroupId).first()
        assertTrue(payments.isEmpty())
    }

    private class InMemoryPaymentDao : PaymentDao {
        private val payments = MutableStateFlow<Map<String, PaymentEntity>>(emptyMap())

        override fun getPayments(groupId: String): Flow<List<PaymentEntity>> {
            return payments.map { map ->
                map.values
                    .filter { it.groupId == groupId }
                    .sortedByDescending { it.createdAt }
            }
        }

        override fun getPayment(id: String): Flow<PaymentEntity?> {
            return payments.map { it[id] }
        }

        override suspend fun insertPayment(payment: PaymentEntity) {
            payments.value += (payment.id to payment)
        }

        override suspend fun insertPayments(payments: List<PaymentEntity>) {
            this.payments.value += payments.associateBy { it.id }
        }

        override suspend fun updatePayment(payment: PaymentEntity) {
            payments.value += (payment.id to payment)
        }

        override suspend fun getDirtyPayments(): List<PaymentEntity> {
            return payments.value.values.filter { it.isDirty }
        }

        override suspend fun markPaymentAsSynced(id: String) {
            val entity = payments.value[id]
            if (entity != null) {
                payments.value += (id to entity.copy(isDirty = false))
            }
        }
    }
}
