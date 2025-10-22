package com.example.appui.data.repository

import android.content.Context
import com.example.appui.core.ai.elevenlabs.config.ElevenLabsConfig
import com.example.appui.core.ai.elevenlabs.session.ConnectionStatus
import com.example.appui.core.ai.elevenlabs.session.ConversationMode
import com.example.appui.core.ai.elevenlabs.session.ElevenLabsSessionManager
import com.example.appui.domain.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: ElevenLabsConfig,
    private val sessionManager: ElevenLabsSessionManager
) : VoiceRepository {

    override val status: Flow<VoiceStatus> =
        sessionManager.status.map { connectionStatus ->
            when (connectionStatus) {
                ConnectionStatus.CONNECTED -> VoiceStatus.CONNECTED
                ConnectionStatus.CONNECTING -> VoiceStatus.CONNECTING
                ConnectionStatus.DISCONNECTED -> VoiceStatus.DISCONNECTED
            }
        }

    override val mode: Flow<VoiceMode> =
        sessionManager.mode.map { conversationMode ->
            when (conversationMode) {
                ConversationMode.SPEAKING -> VoiceMode.SPEAKING
                ConversationMode.LISTENING -> VoiceMode.LISTENING
                ConversationMode.IDLE -> VoiceMode.IDLE
            }
        }

    override val transcript: Flow<TranscriptEvent> =
        sessionManager.transcript.map { transcriptUpdate ->
            TranscriptEvent(
                text = transcriptUpdate?.text ?: "",
                isFinal = transcriptUpdate?.isFinal ?: false
            )
        }

    override val vadScore: Flow<Float> = sessionManager.vadScore

    override suspend fun connect(agentId: String?, conversationToken: String?) {
        val aid = agentId ?: config.defaultAgentId
        val token = conversationToken ?: config.conversationTokenProvider?.invoke()
        sessionManager.connect(aid, token)
    }

    override suspend fun disconnect() {
        sessionManager.disconnect()
    }

    override fun sendText(text: String) {
        sessionManager.sendText(text)
    }

    override suspend fun setMicMuted(muted: Boolean) {
        sessionManager.setMicMuted(muted)
    }
}
