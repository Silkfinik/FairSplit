package com.silkfinik.fairsplit.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // This module is currently empty as dependencies are provided by:
    // - core/database/di/DatabaseModule (DB, Daos)
    // - app/di/FirebaseModule (Firebase)
    // - core/data/di/DataModule (Repositories)
}
