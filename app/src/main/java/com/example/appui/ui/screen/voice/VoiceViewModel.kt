package com.example.appui.ui.screen.voice

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appui.core.audio.session.AudioSessionManager
import com.example.appui.domain.repository.TranscriptEvent
import com.example.appui.domain.repository.VoiceRepository
import com.example.appui.domain.repository.VoiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for VoiceScreen
 *
 * Manages:
 * - Voice connection lifecycle
 * - Audio session management
 * - Microphone control
 * - Transcript updates
 */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: VoiceRepository
) : ViewModel() {

    companion object {
        private const val TAG = "VoiceViewModel"
        private const val VOICE_SESSION_MAX_BOOST_DB = 22
        private const val MEDIA_SESSION_MAX_BOOST_DB = 24
    }

    private val audioManager = AudioSessionManager(context)

    private val _ui = MutableStateFlow(VoiceUiState())
    val ui: StateFlow<VoiceUiState> = _ui.asStateFlow()

    init {
        observeRepositoryStates()
    }

    /**
     * Connect to voice service
     */
    fun connect(agentId: String? = null) = viewModelScope.launch {
        if (_ui.value.status in listOf(VoiceStatus.CONNECTED, VoiceStatus.CONNECTING)) {
            Log.w(TAG, "Already connected or connecting")
            return@launch
        }

        try {
            // Setup audio session for voice chat
            audioManager.enterVoiceSession(
                maxBoostDb = VOICE_SESSION_MAX_BOOST_DB,
                preferSpeakerForMax = false
            )

            // Connect to repository
            repository.connect(agentId = agentId)

            Log.i(TAG, "Voice connection initiated${if (agentId != null) " with agent $agentId" else ""}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect: ${e.message}", e)
            audioManager.exitVoiceSession()
        }
    }

    /**
     * Disconnect from voice service
     */
    fun disconnect() = viewModelScope.launch {
        try {
            repository.disconnect()
            audioManager.exitVoiceSession()

            // Reset state
            _ui.update {
                it.copy(
                    transcript = "",
                    vad = 0f,
                    micMuted = false
                )
            }

            Log.i(TAG, "Voice disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect: ${e.message}", e)
        }
    }

    /**
     * Toggle microphone mute
     */
    fun toggleMic() = viewModelScope.launch {
        val newMuted = !_ui.value.micMuted

        try {
            repository.setMicMuted(newMuted)
            _ui.update { it.copy(micMuted = newMuted) }

            Log.i(TAG, "Microphone ${if (newMuted) "muted" else "unmuted"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle mic: ${e.message}", e)
        }
    }

    /**
     * Send text message
     */
    fun sendText(text: String) {
        if (text.isBlank()) return

        try {
            repository.sendText(text)
            Log.i(TAG, "Text message sent: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send text: ${e.message}", e)
        }
    }

    /**
     * Observe repository state flows
     */
    private fun observeRepositoryStates() {
        // Status updates
        viewModelScope.launch {
            repository.status.collect { status ->
                _ui.update { it.copy(status = status) }
            }
        }

        // Mode updates
        viewModelScope.launch {
            repository.mode.collect { mode ->
                _ui.update { it.copy(mode = mode) }

                // Boost media when in speaking mode
                if (mode == com.example.appui.domain.repository.VoiceMode.SPEAKING) {
                    audioManager.enterMediaLoudSession(maxBoostDb = MEDIA_SESSION_MAX_BOOST_DB)
                }
            }
        }

        // Transcript updates
        viewModelScope.launch {
            repository.transcript.collect { event ->
                if (event.text.isNotBlank()) {
                    _ui.update { it.copy(transcript = event.text) }
                }
            }
        }

        // VAD score updates
        viewModelScope.launch {
            repository.vadScore.collect { score ->
                _ui.update { it.copy(vad = score) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.exitVoiceSession()
        Log.d(TAG, "ViewModel cleared")
    }
}
