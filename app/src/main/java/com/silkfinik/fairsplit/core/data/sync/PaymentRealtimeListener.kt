package com.silkfinik.fairsplit.core.data.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.silkfinik.fairsplit.core.common.di.ApplicationScope
import com.silkfinik.fairsplit.core.data.mapper.asEntity
import com.silkfinik.fairsplit.core.database.dao.PaymentDao
import com.silkfinik.fairsplit.core.database.entity.PaymentEntity
import com.silkfinik.fairsplit.core.network.model.PaymentDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRealtimeListener @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val paymentDao: PaymentDao,
    @param:ApplicationScope private val externalScope: CoroutineScope
) {

    private val listeners = mutableMapOf<String, ListenerRegistration>()

    fun startListening(groupId: String) {
        if (listeners.containsKey(groupId)) return

        Log.d("Sync", "Starting payment listener for group: $groupId")

        val query = firestore.collection("groups")
            .document(groupId)
            .collection("payments")

        val registration = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("Sync", "Payment listen failed for group $groupId", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val dtos = snapshot.toObjects(PaymentDto::class.java).map {
                    it 
                }

                val paymentDtos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PaymentDto::class.java)?.copy(id = doc.id)
                }

                externalScope.launch {
                    saveServerDataToLocal(groupId, paymentDtos)
                }
            }
        }
        
        listeners[groupId] = registration
    }

    fun stopListening(groupId: String) {
        listeners[groupId]?.remove()
        listeners.remove(groupId)
        Log.d("Sync", "Stopped payment listener for group: $groupId")
    }

    private suspend fun saveServerDataToLocal(groupId: String, dtos: List<PaymentDto>) {
        dtos.forEach { dto ->
            val localEntity = paymentDao.getPayment(dto.id).firstOrNull()

            if (shouldUpdateLocal(localEntity, dto)) {
                paymentDao.insertPayment(dto.asEntity(groupId))
            }
        }
    }

    private fun shouldUpdateLocal(localEntity: PaymentEntity?, dto: PaymentDto): Boolean {
        if (localEntity == null) return true
        if (localEntity.isDirty) return false
        return dto.updatedAt > localEntity.updatedAt
    }
}
