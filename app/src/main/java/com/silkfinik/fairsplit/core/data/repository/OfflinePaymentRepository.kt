package com.silkfinik.fairsplit.core.data.repository

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.TimeProvider
import com.silkfinik.fairsplit.core.common.util.safeCall
import com.silkfinik.fairsplit.core.data.mapper.asDomainModel
import com.silkfinik.fairsplit.core.data.mapper.asEntity
import com.silkfinik.fairsplit.core.data.sync.listener.PaymentRealtimeListener
import com.silkfinik.fairsplit.core.data.util.GoogleTimeProvider
import com.silkfinik.fairsplit.core.data.worker.WorkManagerSyncManager
import com.silkfinik.fairsplit.core.database.dao.PaymentDao
import com.silkfinik.fairsplit.core.domain.repository.PaymentRepository
import com.silkfinik.fairsplit.core.model.Payment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflinePaymentRepository @Inject constructor(
    private val paymentDao: PaymentDao,
    private val workManagerSyncManager: WorkManagerSyncManager,
    private val paymentRealtimeListener: PaymentRealtimeListener,
    private val timeProvider: TimeProvider
) : PaymentRepository {

    override fun getPayments(groupId: String): Flow<List<Payment>> {
        return paymentDao.getPayments(groupId).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    override fun getPayment(paymentId: String): Flow<Payment?> {
        return paymentDao.getPayment(paymentId).map { it?.asDomainModel() }
    }

    override suspend fun createPayment(payment: Payment): Result<Unit> = safeCall {
        paymentDao.insertPayment(payment.asEntity(isDirty = true))
        workManagerSyncManager.scheduleSync()
    }

    override suspend fun updatePayment(payment: Payment): Result<Unit> = safeCall {
        val updatedPayment = payment.copy(updatedAt = timeProvider.now())
        paymentDao.updatePayment(updatedPayment.asEntity(isDirty = true))
        workManagerSyncManager.scheduleSync()
    }

    override fun startSync(groupId: String) {
        paymentRealtimeListener.startListening(groupId)
    }

    override fun stopSync(groupId: String) {
        paymentRealtimeListener.stopListening(groupId)
    }
}