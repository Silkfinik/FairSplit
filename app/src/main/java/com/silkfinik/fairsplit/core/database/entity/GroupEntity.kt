package com.silkfinik.fairsplit.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.silkfinik.fairsplit.core.model.Currency

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val id: String,

    val name: String,
    val currency: Currency,

    @ColumnInfo(name = "owner_id") val ownerId: String,

    @ColumnInfo(name = "invite_code") val inviteCode: String? = null,

    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true
)