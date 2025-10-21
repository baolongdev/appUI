package com.example.appui.domain.repository

import kotlinx.coroutines.flow.Flow

interface VoiceRepository {
    val status: Flow<VoiceStatus>       // CONNECTING/CONNECTED/DISCONNECTED
    val mode: Flow<VoiceMode>           // SPEAKING/LISTENING/IDLE
    val transcript: Flow<TranscriptEvent> // partial/final text
    val vadScore: Flow<Float>           // 0..1

    suspend fun connect(agentId: String? = null, conversationToken: String? = null)
    suspend fun disconnect()
    fun sendText(text: String)
    suspend fun setMicMuted(muted: Boolean)
}

enum class VoiceStatus { CONNECTING, CONNECTED, DISCONNECTED }
enum class VoiceMode { SPEAKING, LISTENING, IDLE }
data class TranscriptEvent(val text: String, val isFinal: Boolean)
