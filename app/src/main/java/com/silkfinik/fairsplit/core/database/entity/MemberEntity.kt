package com.silkfinik.fairsplit.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "members",
    primaryKeys = ["id", "group_id"],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("group_id")]
)
data class MemberEntity(
    val id: String,

    val group_id: String,
    val name: String,

    val is_ghost: Boolean = true,

    val created_at: Long,
    val updated_at: Long,
    val is_dirty: Boolean = true
)