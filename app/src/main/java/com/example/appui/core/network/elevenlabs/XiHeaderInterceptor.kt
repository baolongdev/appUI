package com.example.appui.core.network.elevenlabs

import com.example.appui.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor to add XI-API-KEY header to ElevenLabs API requests.
 */
class XiHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val apiKey = BuildConfig.ELEVENLABS_API_KEY

        // Skip adding header if API key is empty
        if (apiKey.isBlank()) {
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
            .header("xi-api-key", apiKey)
            .header("Content-Type", "application/json")
            .method(originalRequest.method, originalRequest.body)
            .build()

        return chain.proceed(newRequest)
    }
}
