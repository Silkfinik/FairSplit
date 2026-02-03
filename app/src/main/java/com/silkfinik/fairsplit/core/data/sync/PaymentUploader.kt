package com.silkfinik.fairsplit.core.data.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.silkfinik.fairsplit.core.data.mapper.asDto
import com.silkfinik.fairsplit.core.database.dao.PaymentDao
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentUploader @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val paymentDao: PaymentDao
) {

    suspend fun syncLocalChanges() {
        val dirtyPayments = paymentDao.getDirtyPayments()

        if (dirtyPayments.isEmpty()) return

        val batch = firestore.batch()

        dirtyPayments.forEach { paymentEntity ->
            val docRef = firestore.collection("groups")
                .document(paymentEntity.groupId)
                .collection("payments")
                .document(paymentEntity.id)

            val dto = paymentEntity.asDto()
            batch.set(docRef, dto, SetOptions.merge())
        }

        try {
            batch.commit().await()
            Log.d("PaymentSync", "Successfully uploaded ${dirtyPayments.size} payments")

            dirtyPayments.forEach {
                paymentDao.markAsSynced(it.id)
            }
        } catch (e: Exception) {
            Log.e("PaymentSync", "Failed to upload payments", e)
            throw e
        }
    }
}