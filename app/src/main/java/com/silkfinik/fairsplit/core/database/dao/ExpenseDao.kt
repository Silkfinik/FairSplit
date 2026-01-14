package com.silkfinik.fairsplit.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.silkfinik.fairsplit.core.database.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE group_id = :groupId AND is_deleted = 0 ORDER BY date DESC")
    fun getExpensesForGroup(groupId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    fun getExpense(expenseId: String): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE is_dirty = 1")
    suspend fun getDirtyExpenses(): List<ExpenseEntity>

    @Query("UPDATE expenses SET is_dirty = 0, updated_at = :syncedAt WHERE id = :id")
    suspend fun markExpenseAsSynced(id: String, syncedAt: Long)
}
