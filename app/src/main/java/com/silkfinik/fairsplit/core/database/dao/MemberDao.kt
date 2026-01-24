package com.silkfinik.fairsplit.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.silkfinik.fairsplit.core.database.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Delete
    suspend fun deleteMember(member: MemberEntity)

    @Query("SELECT * FROM members WHERE group_id = :groupId ORDER BY name ASC")
    fun getMembers(groupId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE group_id = :groupId")
    suspend fun getMembersSync(groupId: String): List<MemberEntity>

    @Query("SELECT * FROM members WHERE group_id = :groupId AND id = :memberId")
    suspend fun getMember(groupId: String, memberId: String): MemberEntity?
}