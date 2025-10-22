package com.example.appui.ui.screen.voice

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appui.core.audio.capture.AudioPcmCaptureManager
import com.example.appui.core.audio.session.AudioSessionManager
import com.example.appui.data.model.ConversationMessage
import com.example.appui.data.model.Speaker
import com.example.appui.domain.repository.ConversationRepository
import com.example.appui.domain.repository.VoiceMode
import com.example.appui.domain.repository.VoiceRepository
import com.example.appui.domain.repository.VoiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for voice conversation screen with conversation tracking.
 */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: VoiceRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val audioSessionManager = AudioSessionManager(context)
    private val pcmCaptureManager = AudioPcmCaptureManager(context)

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    // Conversation tracking
    val conversationMessages = mutableListOf<ConversationMessage>()
    private var conversationStartTime = 0L
    private var currentAgentId: String? = null

    init {
        observeRepositoryStates()
        observePcmData()
        Log.d(TAG, "VoiceViewModel initialized")
    }

    /**
     * Sets conversation mode.
     */
    fun setConversationMode(mode: ConversationControlMode) {
        if (_uiState.value.conversationMode == mode) {
            Log.d(TAG, "Conversation mode already set to $mode")
            return
        }

        _uiState.update { it.copy(conversationMode = mode) }

        if (_uiState.value.status == VoiceStatus.CONNECTED) {
            updateMicActiveState(_uiState.value.mode)
        }

        Log.i(TAG, "Conversation mode set: $mode")
    }

    /**
     * Initiates connection to voice agent.
     */
    fun connect(agentId: String? = null, enablePcmCapture: Boolean = true) = viewModelScope.launch {
        val currentStatus = _uiState.value.status

        if (currentStatus in listOf(VoiceStatus.CONNECTED, VoiceStatus.CONNECTING)) {
            Log.w(TAG, "Connection already active (status=$currentStatus)")
            return@launch
        }

        Log.i(TAG, "Connecting to agent: ${agentId ?: "default"} with mode: ${_uiState.value.conversationMode}")

        runCatching {
            // Start conversation tracking
            currentAgentId = agentId ?: "default"
            conversationStartTime = System.currentTimeMillis()
            conversationMessages.clear()

            // Configure audio for voice chat
            audioSessionManager.enterVoiceSession(
                maxBoostDb = VOICE_SESSION_BOOST_DB,
                preferSpeakerForMax = false
            )

            // Connect to ElevenLabs
            repository.connect(agentId = agentId)

            // Start PCM capture if enabled
            if (enablePcmCapture) {
                startPcmCapture()
            }

            // Apply initial mic state based on mode
            updateMicActiveState(VoiceMode.IDLE)

            Log.i(TAG, "Conversation tracking started")
        }.onFailure { error ->
            Log.e(TAG, "Connection failed", error)
            audioSessionManager.exitVoiceSession()
            stopPcmCapture()
        }
    }

    /**
     * Terminates active voice connection.
     */
    fun disconnect() = viewModelScope.launch {
        val currentStatus = _uiState.value.status

        if (currentStatus == VoiceStatus.DISCONNECTED) {
            Log.d(TAG, "Already disconnected")
            return@launch
        }

        Log.i(TAG, "Disconnecting voice session")

        runCatching {
            stopPcmCapture()
            repository.disconnect()
            audioSessionManager.exitVoiceSession()

            _uiState.update {
                VoiceUiState(
                    status = VoiceStatus.DISCONNECTED,
                    mode = VoiceMode.IDLE,
                    transcript = "",
                    vad = 0f,
                    micMuted = false,
                    micPcmData = null,
                    playbackPcmData = null,
                    conversationMode = it.conversationMode,
                    micActiveByMode = true
                )
            }

            Log.i(TAG, "Disconnection completed successfully")
        }.onFailure { error ->
            Log.e(TAG, "Disconnect failed", error)
        }
    }

    /**
     * Save conversation to database.
     */
    suspend fun saveConversation(customTitle: String? = null): Boolean {
        return try {
            if (conversationMessages.isEmpty()) {
                Log.w(TAG, "No messages to save")
                return false
            }

            // Calculate duration
            val durationMs = System.currentTimeMillis() - conversationStartTime

            // Get agent name from agentId (or use agentId as fallback)
            val agentName = when (currentAgentId) {
                "default" -> "Default Agent"
                else -> currentAgentId // You can add a mapping here if needed
            }

            conversationRepository.saveConversation(
                agentId = currentAgentId ?: "unknown",
                agentName = agentName,
                messages = conversationMessages.toList(),
                durationMs = durationMs,
                mode = _uiState.value.conversationMode.toString(), // ✅ Use conversationMode
                title = customTitle
            )

            Log.i(TAG, "Conversation saved: ${conversationMessages.size} messages, duration: ${durationMs}ms")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save conversation", e)
            false
        }
    }

    /**
     * Check if conversation has content to save.
     */
    fun hasConversationToSave(): Boolean {
        return conversationMessages.isNotEmpty()
    }

    /**
     * Toggles manual microphone mute state.
     */
    fun toggleMic() = viewModelScope.launch {
        val newMutedState = !_uiState.value.micMuted
        _uiState.update { it.copy(micMuted = newMutedState) }
        applyEffectiveMicMute()
        Log.i(TAG, "Manual mic mute: $newMutedState")
    }

    /**
     * Applies effective mic mute state.
     */
    private fun applyEffectiveMicMute() = viewModelScope.launch {
        val effectiveMute = _uiState.value.isEffectiveMicMuted

        runCatching {
            repository.setMicMuted(effectiveMute)

            if (effectiveMute) {
                pcmCaptureManager.pauseMicCapture()
            } else {
                pcmCaptureManager.resumeMicCapture()
            }

            Log.d(TAG, "Effective mic mute applied: $effectiveMute")
        }.onFailure { error ->
            Log.e(TAG, "Failed to apply mic mute", error)
        }
    }

    /**
     * Updates mic active state based on conversation mode.
     */
    private fun updateMicActiveState(voiceMode: VoiceMode) {
        val currentConvMode = _uiState.value.conversationMode

        when (currentConvMode) {
            ConversationControlMode.FULL_DUPLEX -> {
                _uiState.update { it.copy(micActiveByMode = true) }
            }
            ConversationControlMode.PTT -> {
                val micShouldBeActive = voiceMode != VoiceMode.SPEAKING
                _uiState.update { it.copy(micActiveByMode = micShouldBeActive) }
                Log.d(TAG, "PTT Mode: Voice=$voiceMode, Mic allowed=$micShouldBeActive")
            }
        }

        applyEffectiveMicMute()
    }

    /**
     * Starts PCM capture.
     */
    fun startPcmCapture(recordToFile: Boolean = false) {
        val micFile = if (recordToFile) {
            File(context.cacheDir, "mic_${System.currentTimeMillis()}.wav")
        } else null

        val playbackFile = if (recordToFile) {
            File(context.cacheDir, "playback_${System.currentTimeMillis()}.wav")
        } else null

        pcmCaptureManager.startMicCapture(
            sampleRate = 16000,
            recordToFile = micFile
        )

        pcmCaptureManager.startPlaybackCapture(
            captureSize = 1024,
            recordToFile = playbackFile
        )

        Log.i(TAG, "PCM capture started (recordToFile=$recordToFile)")
    }

    /**
     * Stops PCM capture.
     */
    fun stopPcmCapture() {
        pcmCaptureManager.stopAllCapture()
        Log.i(TAG, "PCM capture stopped")
    }

    /**
     * Sends text message to agent.
     */
    fun sendText(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "Ignoring blank text message")
            return
        }

        runCatching {
            repository.sendText(text)

            // Add user message to conversation
            conversationMessages.add(
                ConversationMessage(
                    timestamp = System.currentTimeMillis(),
                    speaker = Speaker.USER,
                    text = text,
                    vadScore = 0f
                )
            )

            Log.d(TAG, "Text sent and logged: ${text.take(50)}...")
        }.onFailure { error ->
            Log.e(TAG, "Failed to send text", error)
        }
    }

    /**
     * Observes repository state flows.
     */
    private fun observeRepositoryStates() {
        // Connection status
        viewModelScope.launch {
            repository.status.collect { status ->
                _uiState.update { it.copy(status = status) }
                Log.d(TAG, "Status updated: $status")
            }
        }

        // Conversation mode - Update mic state
        viewModelScope.launch {
            repository.mode.collect { mode ->
                _uiState.update { it.copy(mode = mode) }
                updateMicActiveState(mode)

                if (mode == VoiceMode.SPEAKING) {
                    audioSessionManager.enterMediaLoudSession(
                        maxBoostDb = MEDIA_SESSION_BOOST_DB
                    )
                }
            }
        }

        // Transcript updates with proper parsing
        viewModelScope.launch {
            repository.transcript.collect { event ->
                if (event.text.isNotBlank()) {
                    _uiState.update { it.copy(transcript = event.text) }

                    // ✅ Parse JSON events to extract actual conversation
                    try {
                        val jsonObject = JSONObject(event.text)
                        val eventType = jsonObject.optString("type")

                        when (eventType) {
                            "user_transcript" -> {
                                val userTranscript = jsonObject
                                    .optJSONObject("user_transcription_event")
                                    ?.optString("user_transcript", "")

                                if (!userTranscript.isNullOrBlank()) {
                                    conversationMessages.add(
                                        ConversationMessage(
                                            timestamp = System.currentTimeMillis(),
                                            speaker = Speaker.USER,
                                            text = userTranscript,
                                            vadScore = _uiState.value.vad
                                        )
                                    )
                                    Log.d(TAG, "USER: $userTranscript")
                                }
                            }

                            "agent_response" -> {
                                val agentResponse = jsonObject
                                    .optJSONObject("agent_response_event")
                                    ?.optString("agent_response", "")

                                if (!agentResponse.isNullOrBlank()) {
                                    conversationMessages.add(
                                        ConversationMessage(
                                            timestamp = System.currentTimeMillis(),
                                            speaker = Speaker.AGENT,
                                            text = agentResponse.trim(),
                                            vadScore = 0f
                                        )
                                    )
                                    Log.d(TAG, "AGENT: $agentResponse")
                                }
                            }

                            // Ignore other events: vad_score, ping, etc.
                            else -> {
                                Log.v(TAG, "Ignored event: $eventType")
                            }
                        }
                    } catch (e: Exception) {
                        // Fallback: treat as plain text
                        Log.w(TAG, "Failed to parse event, using raw text", e)

                        val speaker = if (_uiState.value.mode == VoiceMode.SPEAKING) {
                            Speaker.AGENT
                        } else {
                            Speaker.USER
                        }

                        conversationMessages.add(
                            ConversationMessage(
                                timestamp = System.currentTimeMillis(),
                                speaker = speaker,
                                text = event.text,
                                vadScore = _uiState.value.vad
                            )
                        )
                    }
                }
            }
        }


        // VAD score
        viewModelScope.launch {
            repository.vadScore.collect { score ->
                _uiState.update { it.copy(vad = score) }
            }
        }
    }

    /**
     * Observes PCM data flows.
     */
    private fun observePcmData() {
        viewModelScope.launch {
            pcmCaptureManager.micPcmFlow.collect { pcmData ->
                _uiState.update { it.copy(micPcmData = pcmData) }
            }
        }

        viewModelScope.launch {
            pcmCaptureManager.playbackPcmFlow.collect { pcmData ->
                _uiState.update { it.copy(playbackPcmData = pcmData) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "VoiceViewModel clearing")

        pcmCaptureManager.stopAllCapture()
        audioSessionManager.exitVoiceSession()

        if (_uiState.value.status != VoiceStatus.DISCONNECTED) {
            viewModelScope.launch {
                try {
                    repository.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during forced disconnect", e)
                }
            }
        }

        Log.d(TAG, "VoiceViewModel cleared")
    }

    companion object {
        private const val TAG = "VoiceViewModel"
        private const val VOICE_SESSION_BOOST_DB = 0
        private const val MEDIA_SESSION_BOOST_DB = 0
    }
}
