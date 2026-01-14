package com.silkfinik.fairsplit.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("SELECT * FROM groups ORDER BY created_at DESC")
    fun getGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :groupId")
    fun getGroup(groupId: String): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: String): GroupEntity?

    @Query("SELECT * FROM groups WHERE is_dirty = 1")
    suspend fun getDirtyGroups(): List<GroupEntity>

    @Query("UPDATE groups SET is_dirty = 0 WHERE id = :id AND updated_at = :lastUpdated")
    suspend fun markGroupAsSynced(id: String, lastUpdated: Long)
}