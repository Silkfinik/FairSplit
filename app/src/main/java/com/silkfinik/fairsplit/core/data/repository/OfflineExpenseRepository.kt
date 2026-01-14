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

class OfflineExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val expenseRealtimeListener: ExpenseRealtimeListener,
    private val workManagerSyncManager: WorkManagerSyncManager
) : ExpenseRepository {

    override fun getExpensesForGroup(groupId: String): Flow<List<Expense>> {
        // Start listening when this flow is collected (actually, usually controlled by UI lifecycle, 
        // but for now let's assume UI calls start/stop or we rely on explicit calls)
        // For best practice, we'll expose explicit start/stop methods in the interface.
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
    
    // Extensions for sync control (can be added to interface if needed, or cast)
    override fun startSync(groupId: String) {
        expenseRealtimeListener.startListening(groupId)
    }

    override fun stopSync(groupId: String) {
        expenseRealtimeListener.stopListening(groupId)
    }
}

