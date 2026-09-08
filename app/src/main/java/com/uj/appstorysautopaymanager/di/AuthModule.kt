package com.uj.appstorysautopaymanager.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.uj.appstorysautopaymanager.data.local.dao.AuthTokenDao
import com.uj.appstorysautopaymanager.data.local.database.AppDatabase
import com.uj.appstorysautopaymanager.data.repository.AuthRepositoryImpl
import com.uj.appstorysautopaymanager.domain.auth.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager =
        CredentialManager.create(context)

    @Provides
    fun provideAuthTokenDao(database: AppDatabase): AuthTokenDao = database.authTokenDao()

    @Provides
    @Singleton
    fun provideAuthRepository(impl: AuthRepositoryImpl): AuthRepository = impl
}
