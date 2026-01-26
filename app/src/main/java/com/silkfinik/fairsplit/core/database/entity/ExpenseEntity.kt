package com.silkfinik.fairsplit.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.silkfinik.fairsplit.core.model.Currency

@Entity(
    tableName = "expenses",
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
data class ExpenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "group_id") val groupId: String,
    val description: String,
    val amount: Double,
    val currency: Currency,
    val date: Long,
    @ColumnInfo(name = "creator_id") val creatorId: String,
    val payers: Map<String, Double>,
    val splits: Map<String, Double>,
    val category: String? = null,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_math_valid") val isMathValid: Boolean = true,

    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true
)
