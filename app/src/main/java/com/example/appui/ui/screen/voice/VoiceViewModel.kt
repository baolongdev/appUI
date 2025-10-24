package com.example.appui.ui.screen.voice

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appui.core.audio.capture.AudioPcmCaptureManager
import com.example.appui.core.audio.session.AudioSessionManager
import com.example.appui.data.model.ConversationMessage
import com.example.appui.data.model.Speaker
import com.example.appui.data.repository.ElevenAgentsRepository
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
import javax.inject.Inject

/**
 * ViewModel for voice conversation screen with conversation tracking.
 */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: VoiceRepository,
    private val conversationRepository: ConversationRepository,
    private val agentsRepository: ElevenAgentsRepository
) : ViewModel() {

    private val audioSessionManager = AudioSessionManager(context)
    private val pcmCaptureManager = AudioPcmCaptureManager(context)

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    // ✅ FIXED: Use StateFlow thay vì mutableListOf để UI tự động update
    private val _conversationMessages = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val conversationMessages: StateFlow<List<ConversationMessage>> = _conversationMessages.asStateFlow()

    private var conversationStartTime = 0L
    private var currentAgentId: String? = null
    private var currentAgentName: String? = null

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
    fun connect(agentId: String? = null, agentName: String? = null, enablePcmCapture: Boolean = true) {
        viewModelScope.launch {
            val currentStatus = _uiState.value.status
            if (currentStatus in listOf(VoiceStatus.CONNECTED, VoiceStatus.CONNECTING)) {
                Log.w(TAG, "Connection already active: status=$currentStatus")
                return@launch
            }

            Log.i(TAG, "Connecting to agent: ${agentId ?: "default"} with mode: ${_uiState.value.conversationMode}")

            runCatching {
                currentAgentId = agentId ?: "default"
                currentAgentName = agentName ?: fetchAgentName(agentId)

                _uiState.update { it.copy(agentName = currentAgentName) }

                Log.d(TAG, "Agent info: id=$currentAgentId, name=$currentAgentName")

                // Start conversation tracking
                conversationStartTime = System.currentTimeMillis()
                _conversationMessages.value = emptyList() // ✅ Clear messages

                // Configure audio for voice chat
                audioSessionManager.enterVoiceSession(
                    maxBoostDb = VOICE_SESSION_BOOST_DB,
                    preferSpeakerForMax = false
                )

                repository.connect(agentId = agentId)

                if (enablePcmCapture) {
                    startPcmCapture()
                }

                updateMicActiveState(VoiceMode.IDLE)

                Log.i(TAG, "Conversation tracking started")
            }.onFailure { error ->
                Log.e(TAG, "Connection failed", error)
                audioSessionManager.exitVoiceSession()
                stopPcmCapture()
            }
        }
    }

    /**
     * Fetch agent name từ ElevenLabs API
     */
    private suspend fun fetchAgentName(agentId: String?): String {
        return try {
            if (agentId.isNullOrBlank()) {
                val agents = agentsRepository.listAgents()
                if (agents.isNotEmpty()) {
                    agents.first().name
                } else {
                    "Default Agent"
                }
            } else {
                val agentDetail = agentsRepository.getAgent(agentId)
                agentDetail.name
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch agent name for $agentId", e)
            agentId?.takeIf { it.isNotBlank() } ?: "Unknown Agent"
        }
    }

    /**
     * Terminates active voice connection.
     */
    fun disconnect() {
        viewModelScope.launch {
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
                        micActiveByMode = true,
                        agentName = null
                    )
                }

                Log.i(TAG, "Disconnection completed successfully")
            }.onFailure { error ->
                Log.e(TAG, "Disconnect failed", error)
            }
        }
    }

    /**
     * Save conversation to database.
     */
    suspend fun saveConversation(customTitle: String? = null): Boolean {
        return try {
            val messages = _conversationMessages.value
            if (messages.isEmpty()) {
                Log.w(TAG, "No messages to save")
                return false
            }

            val durationMs = System.currentTimeMillis() - conversationStartTime
            val agentName = currentAgentName ?: "Unknown Agent"

            conversationRepository.saveConversation(
                agentId = currentAgentId ?: "unknown",
                agentName = agentName,
                messages = messages,
                durationMs = durationMs,
                mode = _uiState.value.conversationMode.toString(),
                title = customTitle
            )

            Log.i(TAG, "Conversation saved: ${messages.size} messages, duration=${durationMs}ms, agentName=$agentName")
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
        return _conversationMessages.value.isNotEmpty()
    }

    /**
     * Toggles manual microphone mute state.
     */
    fun toggleMic() {
        viewModelScope.launch {
            val newMutedState = !_uiState.value.micMuted
            _uiState.update { it.copy(micMuted = newMutedState) }
            applyEffectiveMicMute()
            Log.i(TAG, "Manual mic mute: $newMutedState")
        }
    }

    /**
     * Applies effective mic mute state.
     */
    private fun applyEffectiveMicMute() {
        viewModelScope.launch {
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
    }

    /**
     * Sends text message to agent.
     */
    fun sendText(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "Cannot send blank text")
            return
        }

        if (_uiState.value.status != VoiceStatus.CONNECTED) {
            Log.w(TAG, "Cannot send text: not connected")
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.sendText(text)

                // ✅ Add to StateFlow
                val newMessage = ConversationMessage(
                    timestamp = System.currentTimeMillis(),
                    speaker = Speaker.USER,
                    text = text,
                    vadScore = 0f
                )
                _conversationMessages.value = _conversationMessages.value + newMessage

                Log.d(TAG, "Text sent: ${text.take(50)}${if (text.length > 50) "..." else ""} (total: ${_conversationMessages.value.size})")
            }.onFailure { error ->
                Log.e(TAG, "Failed to send text", error)
            }
        }
    }

    // ==================== Private Methods ====================

    private fun startPcmCapture() {
        viewModelScope.launch {
            runCatching {
                pcmCaptureManager.startMicCapture()
                pcmCaptureManager.startPlaybackCapture()
                Log.d(TAG, "PCM capture started")
            }.onFailure { error ->
                Log.e(TAG, "Failed to start PCM capture", error)
            }
        }
    }

    private fun stopPcmCapture() {
        viewModelScope.launch {
            runCatching {
                pcmCaptureManager.stopAllCapture()
                Log.d(TAG, "PCM capture stopped")
            }.onFailure { error ->
                Log.e(TAG, "Failed to stop PCM capture", error)
            }
        }
    }

    private fun updateMicActiveState(voiceMode: VoiceMode) {
        val newMicActiveByMode = when (_uiState.value.conversationMode) {
            ConversationControlMode.FULL_DUPLEX -> true
            ConversationControlMode.PTT -> voiceMode != VoiceMode.SPEAKING
        }

        _uiState.update { it.copy(micActiveByMode = newMicActiveByMode) }
        applyEffectiveMicMute()

        Log.d(TAG, "Mic active state updated: mode=$voiceMode, conversationMode=${_uiState.value.conversationMode}, micActiveByMode=$newMicActiveByMode")
    }

    private fun observeRepositoryStates() {
        viewModelScope.launch {
            repository.status.collect { status ->
                _uiState.update { it.copy(status = status) }
                Log.d(TAG, "Status updated: $status")
            }
        }

        viewModelScope.launch {
            repository.mode.collect { mode ->
                _uiState.update { it.copy(mode = mode) }
                updateMicActiveState(mode)

                if (mode == VoiceMode.SPEAKING) {
                    audioSessionManager.enterMediaLoudSession(maxBoostDb = MEDIA_SESSION_BOOST_DB)
                }
            }
        }

        // ✅ FIXED: Parse transcript and update StateFlow
        viewModelScope.launch {
            repository.transcript.collect { event ->
                val rawText = event.text
                if (rawText.isBlank()) return@collect

                Log.d(TAG, "📥 Raw: ${rawText.take(150)}")

                _uiState.update { it.copy(transcript = rawText) }

                try {
                    val jsonObject = JSONObject(rawText)
                    val eventType = jsonObject.optString("type", "")

                    when (eventType) {
                        "user_transcript" -> {
                            val userTranscript = jsonObject
                                .optJSONObject("user_transcription_event")
                                ?.optString("user_transcript", "")

                            if (!userTranscript.isNullOrBlank()) {
                                val newMessage = ConversationMessage(
                                    timestamp = System.currentTimeMillis(),
                                    speaker = Speaker.USER,
                                    text = userTranscript,
                                    vadScore = _uiState.value.vad
                                )

                                // ✅ Update StateFlow
                                _conversationMessages.value = _conversationMessages.value + newMessage

                                Log.d(TAG, "👤 USER: $userTranscript (total: ${_conversationMessages.value.size})")
                            }
                        }

                        "agent_response" -> {
                            val agentResponse = jsonObject
                                .optJSONObject("agent_response_event")
                                ?.optString("agent_response", "")

                            if (!agentResponse.isNullOrBlank()) {
                                val newMessage = ConversationMessage(
                                    timestamp = System.currentTimeMillis(),
                                    speaker = Speaker.AGENT,
                                    text = agentResponse.trim(),
                                    vadScore = 0f
                                )

                                // ✅ Update StateFlow
                                _conversationMessages.value = _conversationMessages.value + newMessage

                                Log.d(TAG, "🤖 AGENT: $agentResponse (total: ${_conversationMessages.value.size})")
                            }
                        }

                        else -> {
                            if (eventType.isNotEmpty()) {
                                Log.v(TAG, "ℹ️ Event: $eventType")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Parse failed", e)
                }
            }
        }

        viewModelScope.launch {
            repository.vadScore.collect { score ->
                _uiState.update { it.copy(vad = score) }
            }
        }
    }

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
        private const val VOICE_SESSION_BOOST_DB = 24
        private const val MEDIA_SESSION_BOOST_DB = 24
    }
}
