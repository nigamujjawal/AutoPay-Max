package com.autopaymax.di

import com.autopaymax.data.repository.PaymentRepositoryImpl
import com.autopaymax.data.repository.ProfileRepositoryImpl
import com.autopaymax.data.repository.SubscriptionRepositoryImpl
import com.autopaymax.domain.payment.repository.PaymentRepository
import com.autopaymax.domain.profile.repository.ProfileRepository
import com.autopaymax.domain.subscription.repository.SubscriptionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SoundBoxModule {

    @Provides
    @Singleton
    fun providePaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository = impl

    @Provides
    @Singleton
    fun provideProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository = impl

    @Provides
    @Singleton
    fun provideSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository = impl
}
