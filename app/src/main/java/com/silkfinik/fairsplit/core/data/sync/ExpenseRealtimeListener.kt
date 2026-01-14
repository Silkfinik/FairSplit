package com.silkfinik.fairsplit.core.data.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.silkfinik.fairsplit.core.common.di.ApplicationScope
import com.silkfinik.fairsplit.core.data.mapper.asEntity
import com.silkfinik.fairsplit.core.database.dao.ExpenseDao
import com.silkfinik.fairsplit.core.database.entity.ExpenseEntity
import com.silkfinik.fairsplit.core.network.model.ExpenseDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRealtimeListener @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val expenseDao: ExpenseDao,
    @param:ApplicationScope private val externalScope: CoroutineScope
) {

    private val listeners = mutableMapOf<String, ListenerRegistration>()

    fun startListening(groupId: String) {
        if (listeners.containsKey(groupId)) return

        Log.d("Sync", "Starting expense listener for group: $groupId")

        val query = firestore.collection("groups")
            .document(groupId)
            .collection("expenses")
            // We can add sorting here if needed, but for sync just getting all is safer.
            // If the collection is huge, we might need 'updated_at' > lastSyncTime.
            // For now, full sync of the collection.

        val registration = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("Sync", "Expense listen failed for group $groupId", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val dtos = snapshot.toObjects(ExpenseDto::class.java)
                externalScope.launch {
                    saveServerDataToLocal(groupId, dtos)
                }
            }
        }
        
        listeners[groupId] = registration
    }

    fun stopListening(groupId: String) {
        listeners[groupId]?.remove()
        listeners.remove(groupId)
        Log.d("Sync", "Stopped expense listener for group: $groupId")
    }

    private suspend fun saveServerDataToLocal(groupId: String, dtos: List<ExpenseDto>) {
        dtos.forEach { dto ->
            val localEntity = expenseDao.getExpenseById(dto.id)

            if (shouldUpdateLocal(localEntity, dto)) {
                expenseDao.insertExpense(dto.asEntity(groupId))
            }
        }
    }

    private fun shouldUpdateLocal(localEntity: ExpenseEntity?, dto: ExpenseDto): Boolean {
        if (localEntity == null) return true // New expense
        
        if (!localEntity.isDirty) return true // Local is clean, accept server version (even if same, to be safe)

        // Conflict resolution: Last Write Wins (LWW) based on updatedAt
        return dto.updatedAt > localEntity.updatedAt
    }
}
