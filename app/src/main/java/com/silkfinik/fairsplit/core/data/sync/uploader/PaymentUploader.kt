package com.silkfinik.fairsplit.core.data.sync.uploader

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.silkfinik.fairsplit.core.data.mapper.asDto
import com.silkfinik.fairsplit.core.data.sync.FirestoreRoutes
import com.silkfinik.fairsplit.core.database.dao.PaymentDao
import com.silkfinik.fairsplit.core.database.entity.PaymentEntity
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentUploader @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val paymentDao: PaymentDao,
    authRepository: AuthRepository
) : BaseFirestoreUploader<PaymentEntity>(firestore, authRepository) {

    override val logTag: String = "PaymentSync"

    override suspend fun getDirtyItems(): List<PaymentEntity> {
        return paymentDao.getDirtyPayments()
    }

    override suspend fun addToBatch(batch: WriteBatch, item: PaymentEntity, userId: String) {
        val docRef = firestore.collection(FirestoreRoutes.GROUPS)
            .document(item.groupId)
            .collection(FirestoreRoutes.PAYMENTS)
            .document(item.id)

        val dto = item.asDto()
        batch.set(docRef, dto)
    }

    override suspend fun markAsSynced(items: List<PaymentEntity>) {
        items.forEach { payment ->
            paymentDao.markPaymentAsSynced(payment.id)
        }
    }
}