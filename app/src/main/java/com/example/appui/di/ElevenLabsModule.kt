package com.example.appui.di

import android.content.Context
import android.util.Log
import com.example.appui.BuildConfig
import com.example.appui.core.ai.elevenlabs.config.ElevenLabsConfig
import com.example.appui.core.ai.elevenlabs.session.ElevenLabsSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ElevenLabsModule {

    private const val TAG = "ElevenLabsModule"

    @Provides @Singleton
    fun provideElevenLabsConfig(): ElevenLabsConfig {
        val base = BuildConfig.ELEVENLABS_BASE_URL.ifBlank { "https://api.elevenlabs.io" }
        val apiKey = BuildConfig.ELEVENLABS_API_KEY.trim().ifBlank { null }
        val agentId = BuildConfig.ELEVENLABS_AGENT_ID.trim().ifBlank { null }

        if (apiKey == null) Log.w(TAG, "ELEVENLABS_API_KEY is blank – REST calls may fail.")
        if (agentId == null) Log.w(TAG, "ELEVENLABS_AGENT_ID is blank – connect() should pass agentId manually.")

        return ElevenLabsConfig(
            baseUrl = base,
            apiKey = apiKey,
            defaultAgentId = agentId,
            conversationTokenProvider = null // nếu dùng private agent: cung cấp provider của bạn
        )
    }

    @Provides @Singleton
    fun provideElevenLabsSessionManager(@ApplicationContext app: Context): ElevenLabsSessionManager =
        ElevenLabsSessionManager(app)
}
