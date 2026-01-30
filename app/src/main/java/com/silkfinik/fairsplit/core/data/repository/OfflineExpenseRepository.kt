package com.silkfinik.fairsplit.core.data.repository

import com.silkfinik.fairsplit.core.data.mapper.asDomainModel
import com.silkfinik.fairsplit.core.data.mapper.asEntity
import com.silkfinik.fairsplit.core.database.dao.ExpenseDao
import com.silkfinik.fairsplit.core.data.sync.ExpenseRealtimeListener
import com.silkfinik.fairsplit.core.data.worker.WorkManagerSyncManager
import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.silkfinik.fairsplit.core.model.HistoryItem
import com.silkfinik.fairsplit.core.network.model.HistoryDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OfflineExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val expenseRealtimeListener: ExpenseRealtimeListener,
    private val workManagerSyncManager: WorkManagerSyncManager,
    private val firestore: FirebaseFirestore
) : ExpenseRepository {

    override fun getExpenseHistory(groupId: String, expenseId: String): Flow<List<HistoryItem>> = callbackFlow {
        val listenerRegistration = firestore.collection("groups")
            .document(groupId)
            .collection("expenses")
            .document(expenseId)
            .collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            val dto = doc.toObject(HistoryDto::class.java)!!
                            val timestamp = (dto.timestamp as? Timestamp)?.toDate()?.time ?: 0L
                            
                            HistoryItem(
                                id = doc.id,
                                action = dto.action,
                                changes = dto.changes,
                                timestamp = timestamp,
                                isMathValid = dto.isMathValid
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(items)
                }
            }
        
        awaitClose { listenerRegistration.remove() }
    }

    override fun getExpensesForGroup(groupId: String): Flow<List<Expense>> {
        return expenseDao.getExpensesForGroup(groupId).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    override fun getExpense(expenseId: String): Flow<Expense?> {
        return expenseDao.getExpense(expenseId).map { it?.asDomainModel() }
    }

    override suspend fun createExpense(expense: Expense): String {
        expenseDao.insertExpense(expense.asEntity(isDirty = true))
        workManagerSyncManager.scheduleSync()
        return expense.id
    }

    override suspend fun updateExpense(expense: Expense) {
        val updatedExpense = expense.copy(updatedAt = System.currentTimeMillis())
        expenseDao.updateExpense(updatedExpense.asEntity(isDirty = true))
        workManagerSyncManager.scheduleSync()
    }

    override suspend fun deleteExpense(expenseId: String) {
        val expenseEntity = expenseDao.getExpenseById(expenseId) ?: return
        val deletedExpense = expenseEntity.copy(
            isDeleted = true,
            updatedAt = System.currentTimeMillis(),
            isDirty = true
        )
        expenseDao.updateExpense(deletedExpense)
        workManagerSyncManager.scheduleSync()
    }

    override fun startSync(groupId: String) {
        expenseRealtimeListener.startListening(groupId)
    }

    override fun stopSync(groupId: String) {
        expenseRealtimeListener.stopListening(groupId)
    }
}

