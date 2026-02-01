package com.silkfinik.fairsplit.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["group_id"])]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "group_id") val groupId: String,
    @ColumnInfo(name = "payer_id") val payerId: String,
    @ColumnInfo(name = "receiver_id") val receiverId: String,
    val amount: Double,
    val currency: Currency,
    val status: PaymentStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true
)
