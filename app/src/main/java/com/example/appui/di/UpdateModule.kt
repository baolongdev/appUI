package com.example.appui.di

import android.content.Context
import com.example.appui.core.notification.DownloadNotificationManager
import com.example.appui.data.repository.AppUpdateRepositoryImpl
import com.example.appui.domain.repository.AppUpdateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UpdateOkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object UpdateModule {

    /**
     * Provide OkHttpClient tối ưu cho download file lớn
     */
    @Provides
    @Singleton
    @UpdateOkHttpClient
    fun provideUpdateOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (com.example.appui.BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .build()
    }

    /**
     * Provide DownloadNotificationManager singleton
     */
    @Provides
    @Singleton
    fun provideDownloadNotificationManager(
        @ApplicationContext context: Context
    ): DownloadNotificationManager {
        return DownloadNotificationManager(context)
    }
}