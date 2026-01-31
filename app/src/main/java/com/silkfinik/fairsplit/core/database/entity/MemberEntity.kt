package com.silkfinik.fairsplit.core.database.entity

import androidx.room.ColumnInfo
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

    @ColumnInfo(name = "group_id") val groupId: String,
    val name: String,

    @ColumnInfo(name = "photo_url") val photoUrl: String? = null,

    @ColumnInfo(name = "is_ghost") val isGhost: Boolean = true,
    @ColumnInfo(name = "merged_with_uid") val mergedWithUid: String? = null,

    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true
)