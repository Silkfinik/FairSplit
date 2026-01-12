package com.silkfinik.fairsplit.core.database.di

import android.content.Context
import androidx.room.Room
import com.silkfinik.fairsplit.core.database.AppDatabase
import com.silkfinik.fairsplit.core.database.dao.GroupDao
import com.silkfinik.fairsplit.core.database.dao.MemberDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fairsplit-db"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideGroupDao(database: AppDatabase): GroupDao {
        return database.groupDao()
    }

    @Provides
    fun provideMemberDao(database: AppDatabase): MemberDao {
        return database.memberDao()
    }
}