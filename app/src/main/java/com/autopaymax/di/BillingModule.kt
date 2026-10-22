package com.autopaymax.di

import com.autopaymax.data.repository.BillingRepository
import com.autopaymax.data.repository.RevenueCatBillingRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindBillingRepository(
        impl: RevenueCatBillingRepositoryImpl
    ): BillingRepository
}
