package com.silkfinik.fairsplit.core.data.di

import com.silkfinik.fairsplit.core.data.repository.AuthRepository
import com.silkfinik.fairsplit.core.data.repository.FirebaseAuthRepository
import com.silkfinik.fairsplit.core.data.repository.GroupRepository
import com.silkfinik.fairsplit.core.data.repository.OfflineGroupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindGroupRepository(
        impl: OfflineGroupRepository
    ): GroupRepository

    @Binds
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepository
    ): AuthRepository
}