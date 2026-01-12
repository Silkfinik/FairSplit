package com.silkfinik.fairsplit.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.silkfinik.fairsplit.core.model.Currency

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val id: String,

    val name: String,
    val currency: Currency,

    val owner_id: String,

    val created_at: Long,
    val updated_at: Long,
    val is_dirty: Boolean = true
)