package com.example.appui.ui.screen.voice

import com.example.appui.core.audio.capture.PcmData
import com.example.appui.domain.repository.VoiceMode
import com.example.appui.domain.repository.VoiceStatus

/**
 * Conversation control mode.
 */
enum class ConversationControlMode {
    FULL_DUPLEX,  // Mic always active (default)
    PTT           // Push-to-talk: mic only active after agent finishes speaking
}

/**
 * UI state for VoiceScreen with PCM data support and conversation mode control.
 */
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
    val micActiveByMode: Boolean = true // Whether mic is allowed to be active based on conversation mode
)

/**
 * Extension to check if mic should be truly muted.
 */
val VoiceUiState.isEffectiveMicMuted: Boolean
    get() = micMuted || !micActiveByMode
