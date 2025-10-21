package com.example.appui.ui.screen.voice

import com.example.appui.domain.repository.VoiceMode
import com.example.appui.domain.repository.VoiceStatus

/**
 * UI state for VoiceScreen
 */
data class VoiceUiState(
    val status: VoiceStatus = VoiceStatus.DISCONNECTED,
    val mode: VoiceMode = VoiceMode.IDLE,
    val transcript: String = "",
    val vad: Float = 0f,
    val micMuted: Boolean = false
)
