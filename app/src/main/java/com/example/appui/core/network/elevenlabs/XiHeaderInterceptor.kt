package com.example.appui.core.network.elevenlabs

import com.example.appui.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class XiHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
            .apply {
                val apiKey = BuildConfig.ELEVENLABS_API_KEY.orEmpty().trim()
                if (apiKey.isNotEmpty()) addHeader("xi-api-key", apiKey)
            }
            .build()
        return chain.proceed(req)
    }
}