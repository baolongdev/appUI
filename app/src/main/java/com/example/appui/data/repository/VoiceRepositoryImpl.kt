package com.example.appui.data.repository

import android.content.Context
import com.example.appui.core.ai.cloud.elevenlabs.ElevenLabsConfig
import com.example.appui.core.ai.cloud.elevenlabs.ElevenLabsSessionManager
import com.example.appui.domain.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext   // 👈 import này
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,   // 👈 dùng ApplicationContext
    private val cfg: ElevenLabsConfig,
    private val session: ElevenLabsSessionManager
) : VoiceRepository {

    override val status: Flow<VoiceStatus> =
        session.status.map { when (it) {
            "connected" -> VoiceStatus.CONNECTED
            "connecting" -> VoiceStatus.CONNECTING
            else -> VoiceStatus.DISCONNECTED
        }}

    override val mode: Flow<VoiceMode> =
        session.mode.map { when (it) {
            "speaking" -> VoiceMode.SPEAKING
            "listening" -> VoiceMode.LISTENING
            else -> VoiceMode.IDLE
        }}

    override val transcript: Flow<TranscriptEvent> =
        session.transcript.map { pair -> (pair ?: ("" to false)).let { TranscriptEvent(it.first, it.second) } }

    override val vadScore: Flow<Float> = session.vadScore

    override suspend fun connect(agentId: String?, conversationToken: String?) {
        val aid = agentId ?: cfg.defaultAgentId
        val token = conversationToken ?: cfg.conversationTokenProvider?.invoke()
        session.connect(aid, token)
    }

    override suspend fun disconnect() = session.disconnect()
    override fun sendText(text: String) = session.sendText(text)
    override suspend fun setMicMuted(muted: Boolean) = session.setMicMuted(muted)
}
