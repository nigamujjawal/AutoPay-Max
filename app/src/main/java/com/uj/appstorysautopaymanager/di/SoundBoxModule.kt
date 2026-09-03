package com.uj.appstorysautopaymanager.di

import com.uj.appstorysautopaymanager.data.repository.PaymentRepositoryImpl
import com.uj.appstorysautopaymanager.data.repository.ProfileRepositoryImpl
import com.uj.appstorysautopaymanager.data.repository.SubscriptionRepositoryImpl
import com.uj.appstorysautopaymanager.domain.payment.repository.PaymentRepository
import com.uj.appstorysautopaymanager.domain.profile.repository.ProfileRepository
import com.uj.appstorysautopaymanager.domain.subscription.repository.SubscriptionRepository
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
