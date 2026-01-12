package com.silkfinik.fairsplit.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.silkfinik.fairsplit.core.database.dao.GroupDao
import com.silkfinik.fairsplit.core.database.dao.MemberDao
import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import com.silkfinik.fairsplit.core.database.entity.MemberEntity
import com.silkfinik.fairsplit.core.database.util.Converters

@Database(
    entities = [GroupEntity::class, MemberEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun memberDao(): MemberDao
}