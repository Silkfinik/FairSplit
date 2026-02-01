package com.silkfinik.fairsplit.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.data.mapper.asDomainModel
import com.silkfinik.fairsplit.core.data.mapper.asDto
import com.silkfinik.fairsplit.core.data.mapper.asEntity
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
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
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
    }

    override suspend fun updatePayment(payment: Payment) {
        paymentDao.updatePayment(payment.asEntity(isDirty = true))
    }

    override suspend fun syncPayments(groupId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return
        }

        try {
            val collectionRef = firestore.collection("groups").document(groupId).collection("payments")

            val dirtyPayments = paymentDao.getDirtyPayments().filter { it.groupId == groupId }
            
            for (paymentEntity in dirtyPayments) {
                val dto = paymentEntity.asDto()
                collectionRef.document(paymentEntity.id).set(dto).await()
                paymentDao.markAsSynced(paymentEntity.id)
            }

            val snapshot = collectionRef
                .orderBy("updated_at", Query.Direction.DESCENDING)
                .get()
                .await()

            val remotePayments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(PaymentDto::class.java)?.copy(id = doc.id)
            }

            val paymentEntities = remotePayments.map { it.asEntity(groupId) }
            paymentDao.insertPayments(paymentEntities)

        } catch (e: Exception) {
            throw e
        }
    }
}
