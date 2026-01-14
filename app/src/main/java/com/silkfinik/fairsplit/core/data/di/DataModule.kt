package com.silkfinik.fairsplit.core.data.di

import com.silkfinik.fairsplit.core.data.repository.FirebaseAuthRepository
import com.silkfinik.fairsplit.core.data.repository.OfflineExpenseRepository
import com.silkfinik.fairsplit.core.data.repository.OfflineGroupRepository
import com.silkfinik.fairsplit.core.data.repository.OfflineMemberRepository
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
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

    @Binds
    abstract fun bindExpenseRepository(
        impl: OfflineExpenseRepository
    ): ExpenseRepository

    @Binds
    abstract fun bindMemberRepository(
        impl: OfflineMemberRepository
    ): MemberRepository
}