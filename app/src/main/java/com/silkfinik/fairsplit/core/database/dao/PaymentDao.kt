package com.silkfinik.fairsplit.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.silkfinik.fairsplit.core.database.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE group_id = :groupId ORDER BY created_at DESC")
    fun getPayments(groupId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id")
    fun getPayment(id: String): Flow<PaymentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>)
    
    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE is_dirty = 1")
    suspend fun getDirtyPayments(): List<PaymentEntity>

    @Query("UPDATE payments SET is_dirty = 0 WHERE id = :id")
    suspend fun markPaymentAsSynced(id: String)
}
