package com.silkfinik.fairsplit.core.data.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.silkfinik.fairsplit.core.data.mapper.asDto
import com.silkfinik.fairsplit.core.database.dao.ExpenseDao
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseUploader @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val expenseDao: ExpenseDao,
    private val authRepository: AuthRepository
) {

    suspend fun syncLocalChanges() {
        if (authRepository.getUserId() == null) return

        val dirtyExpenses = expenseDao.getDirtyExpenses()
        if (dirtyExpenses.isEmpty()) return

        val batch = firestore.batch()

        val expensesByGroup = dirtyExpenses.groupBy { it.groupId }

        expensesByGroup.forEach { (groupId, expenses) ->
            expenses.forEach { entity ->
                val docRef = firestore.collection("groups")
                    .document(groupId)
                    .collection("expenses")
                    .document(entity.id)
                
                val dto = entity.asDto()
                batch.set(docRef, dto)
            }
        }

        try {
            batch.commit().await()
            Log.d("Sync", "Successfully uploaded ${dirtyExpenses.size} expenses")

            dirtyExpenses.forEach { expense ->
                expenseDao.markExpenseAsSynced(expense.id, expense.updatedAt)
            }
        } catch (e: Exception) {
            Log.e("Sync", "Failed to upload expenses", e)
            throw e
        }
    }
}
