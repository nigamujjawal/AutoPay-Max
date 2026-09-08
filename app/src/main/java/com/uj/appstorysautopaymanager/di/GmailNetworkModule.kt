package com.uj.appstorysautopaymanager.di

import com.uj.appstorysautopaymanager.BuildConfig
import com.uj.appstorysautopaymanager.data.remote.GmailApi
import com.uj.appstorysautopaymanager.data.remote.GmailAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

// Second network graph, separate from NetworkModule (SoundBox backend). Both would otherwise
// provide an unqualified OkHttpClient/Retrofit - @GmailHttp disambiguates.
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GmailHttp

@Module
@InstallIn(SingletonComponent::class)
object GmailNetworkModule {

    @Provides
    @Singleton
    @GmailHttp
    fun provideGmailOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(GmailAuthInterceptor())
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @GmailHttp
    fun provideGmailRetrofit(@GmailHttp okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://gmail.googleapis.com/gmail/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideGmailApi(@GmailHttp retrofit: Retrofit): GmailApi = retrofit.create(GmailApi::class.java)
}
