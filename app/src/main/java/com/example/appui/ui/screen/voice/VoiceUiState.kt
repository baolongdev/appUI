package com.example.appui.ui.screen.voice

import com.example.appui.core.audio.capture.PcmData
import com.example.appui.domain.repository.VoiceMode
import com.example.appui.domain.repository.VoiceStatus

data class VoiceUiState(
    val status: VoiceStatus = VoiceStatus.DISCONNECTED,
    val mode: VoiceMode = VoiceMode.IDLE,
    val transcript: String = "",
    val vad: Float = 0f,
    val micMuted: Boolean = false,
    val micPcmData: PcmData? = null,
    val playbackPcmData: PcmData? = null,

    // Conversation control mode
    val conversationMode: ConversationControlMode = ConversationControlMode.FULL_DUPLEX,
    val micActiveByMode: Boolean = true,

    // ✅ THÊM: Agent name
    val agentName: String? = null
) {
    /**
     * Effective mic mute state = manual mute OR not active by mode
     */
    val isEffectiveMicMuted: Boolean
        get() = micMuted || !micActiveByMode
}

/**
 * Conversation control modes
 */
enum class ConversationControlMode {
    /**
     * Full duplex: both mic and speaker active simultaneously
     */
    FULL_DUPLEX,

    /**
     * Push-to-talk: mic active only when agent is not speaking
     */
    PTT
}
