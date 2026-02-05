package com.silkfinik.fairsplit.core.data.sync.listener

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.silkfinik.fairsplit.core.common.di.ApplicationScope
import com.silkfinik.fairsplit.core.data.mapper.asEntity
import com.silkfinik.fairsplit.core.data.sync.FirestoreRoutes
import com.silkfinik.fairsplit.core.database.dao.PaymentDao
import com.silkfinik.fairsplit.core.network.model.PaymentDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRealtimeListener @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val paymentDao: PaymentDao,
    @ApplicationScope externalScope: CoroutineScope
) : BaseSubCollectionListener<PaymentDto>(externalScope) {

    override val logTag: String = "PaymentSync"

    override fun getCollectionReference(groupId: String): CollectionReference {
        return firestore.collection(FirestoreRoutes.GROUPS)
            .document(groupId)
            .collection(FirestoreRoutes.PAYMENTS)
    }

    override fun parseSnapshot(snapshot: QuerySnapshot): List<PaymentDto> {
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(PaymentDto::class.java)?.copy(id = doc.id)
        }
    }

    override suspend fun saveServerDataToLocal(groupId: String, dtos: List<PaymentDto>) {
        dtos.forEach { dto ->
            val localEntity = paymentDao.getPayment(dto.id).firstOrNull()

            val shouldUpdate = shouldUpdate(
                localEntityExists = localEntity != null,
                localIsDirty = localEntity?.isDirty == true,
                localUpdatedAt = localEntity?.updatedAt ?: 0L,
                serverUpdatedAt = dto.updatedAt
            )

            if (shouldUpdate) {
                paymentDao.insertPayment(dto.asEntity(groupId))
            }
        }
    }
}