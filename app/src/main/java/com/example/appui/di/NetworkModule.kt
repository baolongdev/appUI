package com.example.appui.di

import android.util.Log
import com.example.appui.BuildConfig
import com.example.appui.core.network.elevenlabs.XiHeaderInterceptor
import com.example.appui.data.remote.elevenlabs.api.ElevenLabsApi
import com.example.appui.data.remote.github.GitHubApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TAG = "NetworkModule"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

    // ==================== OK HTTP CLIENTS ====================

    @ElevenLabsClient
    @Provides
    @Singleton
    fun provideElevenLabsOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(XiHeaderInterceptor())
            .addInterceptor(createLoggingInterceptor(HttpLoggingInterceptor.Level.BODY))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .build()
    }

    // ✅ THÊM: GitHub Token Interceptor
    private fun createGitHubTokenInterceptor(): Interceptor {
        return Interceptor { chain ->
            val token = BuildConfig.GITHUB_TOKEN

            // Debug logs
            Log.d(TAG, "═══════════════════════════════")
            Log.d(TAG, "GitHub Token Info:")
            Log.d(TAG, "  Length: ${token.length}")
            Log.d(TAG, "  Is blank: ${token.isBlank()}")

            val request = if (token.isNotBlank()) {
                Log.d(TAG, "  Preview: ${token.take(15)}...")
                Log.d(TAG, "  ✅ Adding Authorization header")

                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                Log.e(TAG, "  ⚠️ Token is BLANK! GitHub API will return 401")
                chain.request()
            }

            Log.d(TAG, "═══════════════════════════════")
            chain.proceed(request)
        }
    }

    @UpdateClient
    @Provides
    @Singleton
    fun provideUpdateOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(createGitHubTokenInterceptor())  // ✅ THÊM TOKEN INTERCEPTOR
            .addInterceptor(createLoggingInterceptor(HttpLoggingInterceptor.Level.HEADERS))  // ✅ Đổi sang HEADERS để xem Authorization
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // ==================== RETROFIT INSTANCES ====================

    @Named("elevenlabs")
    @Provides
    @Singleton
    fun provideElevenLabsRetrofit(
        moshi: Moshi,
        @ElevenLabsClient client: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.ELEVENLABS_BASE_URL.ifBlank { "https://api.elevenlabs.io/" })
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()

    @Named("github")
    @Provides
    @Singleton
    fun provideGitHubRetrofit(
        @UpdateClient client: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

    // ==================== API SERVICES ====================

    @Provides
    @Singleton
    fun provideElevenLabsApi(
        @Named("elevenlabs") retrofit: Retrofit
    ): ElevenLabsApi =
        retrofit.create(ElevenLabsApi::class.java)

    @Provides
    @Singleton
    fun provideGitHubApiService(
        @Named("github") retrofit: Retrofit
    ): GitHubApiService =
        retrofit.create(GitHubApiService::class.java)

    // ==================== HELPER ====================

    private fun createLoggingInterceptor(level: HttpLoggingInterceptor.Level): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }.apply {
            this.level = if (BuildConfig.DEBUG) level else HttpLoggingInterceptor.Level.NONE
        }
    }
}
