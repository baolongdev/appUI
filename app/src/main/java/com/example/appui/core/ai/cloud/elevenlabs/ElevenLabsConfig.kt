package com.example.appui.core.ai.cloud.elevenlabs

/**
 * Cấu hình toàn cục cho ElevenLabs SDK.
 * - [baseUrl]: endpoint REST, mặc định https://api.elevenlabs.io
 * - [apiKey]: khoá XI-API-KEY cho REST client.
 * - [defaultAgentId]: Agent công khai (public agent ID).
 * - [conversationTokenProvider]: callback async để lấy token hội thoại cho agent riêng tư.
 */
data class ElevenLabsConfig(
    val baseUrl: String = "https://api.elevenlabs.io",
    val apiKey: String? = null,
    val defaultAgentId: String? = null,
    val conversationTokenProvider: (suspend () -> String)? = null
)
