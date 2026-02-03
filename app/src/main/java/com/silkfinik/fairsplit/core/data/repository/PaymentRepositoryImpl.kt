package com.silkfinik.fairsplit.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.data.mapper.asDomainModel
import com.silkfinik.fairsplit.core.data.mapper.asDto
import com.silkfinik.fairsplit.core.data.mapper.asEntity
import com.silkfinik.fairsplit.core.data.worker.WorkManagerSyncManager
import com.silkfinik.fairsplit.core.database.dao.PaymentDao
import com.silkfinik.fairsplit.core.domain.repository.PaymentRepository
import com.silkfinik.fairsplit.core.model.Payment
import com.silkfinik.fairsplit.core.network.model.PaymentDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val paymentDao: PaymentDao,
    private val workManagerSyncManager: WorkManagerSyncManager
) : PaymentRepository {

    override fun getPayments(groupId: String): Flow<List<Payment>> {
        return paymentDao.getPayments(groupId).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    override fun getPayment(paymentId: String): Flow<Payment?> {
        return paymentDao.getPayment(paymentId).map { it?.asDomainModel() }
    }

    override suspend fun createPayment(payment: Payment) {
        paymentDao.insertPayment(payment.asEntity(isDirty = true))
        workManagerSyncManager.scheduleSync()
    }

    override suspend fun updatePayment(payment: Payment) {
        paymentDao.updatePayment(payment.asEntity(isDirty = true))
        workManagerSyncManager.scheduleSync()
    }

    override suspend fun syncPayments(groupId: String) {
        workManagerSyncManager.scheduleSync()
    }
}
