package com.example.appui.core.ai.elevenlabs.session

import android.content.Context
import android.util.Log
import io.elevenlabs.ClientTool
import io.elevenlabs.ClientToolResult
import io.elevenlabs.ConversationClient
import io.elevenlabs.ConversationConfig
import io.elevenlabs.ConversationSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Connection status for ElevenLabs session.
 */
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/**
 * Conversation mode indicating agent state.
 */
enum class ConversationMode {
    IDLE,
    LISTENING,
    SPEAKING
}

/**
 * Transcript update with finality indicator.
 *
 * @property text Transcript text content
 * @property isFinal Whether this is the final transcript (true) or interim (false)
 */
data class TranscriptUpdate(
    val text: String,
    val isFinal: Boolean
)

/**
 * Manages ElevenLabs conversation sessions with state management.
 *
 * Responsibilities:
 * - Connection lifecycle management
 * - Session state tracking
 * - Message and transcript handling
 * - Voice Activity Detection (VAD) monitoring
 *
 * @property appContext Application context for SDK initialization
 */
class ElevenLabsSessionManager(
    private val appContext: Context
) {
    private var session: ConversationSession? = null

    // State flows for reactive UI updates
    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _mode = MutableStateFlow(ConversationMode.IDLE)
    val mode: StateFlow<ConversationMode> = _mode.asStateFlow()

    private val _transcript = MutableStateFlow<TranscriptUpdate?>(null)
    val transcript: StateFlow<TranscriptUpdate?> = _transcript.asStateFlow()

    private val _vadScore = MutableStateFlow(0f)
    val vadScore: StateFlow<Float> = _vadScore.asStateFlow()

    /**
     * Establishes connection to ElevenLabs agent.
     *
     * @param agentId Public agent identifier (null for token-based auth)
     * @param conversationToken Token for private agent access (null for public agents)
     */
    suspend fun connect(agentId: String?, conversationToken: String?) {
        if (session != null) {
            Log.w(TAG, "Already connected, ignoring connect request")
            return
        }

        _status.value = ConnectionStatus.CONNECTING

        val connectionType = when {
            agentId != null -> "Public agent: $agentId"
            conversationToken != null -> "Private agent (token-based)"
            else -> "Default configuration"
        }
        Log.i(TAG, "Initiating connection - $connectionType")

        val config = buildConversationConfig(agentId, conversationToken)

        session = withContext(Dispatchers.Main) {
            ConversationClient.startSession(config, appContext)
        }
    }

    /**
     * Terminates active conversation session.
     */
    suspend fun disconnect() {
        if (session == null) {
            Log.w(TAG, "No active session to disconnect")
            return
        }

        Log.i(TAG, "Disconnecting session")

        withContext(Dispatchers.Main) {
            session?.endSession()
        }

        session = null
        resetState()
    }

    /**
     * Sends text message to agent.
     *
     * @param text Message content
     */
    fun sendText(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "Attempted to send blank text")
            return
        }

        if (session == null) {
            Log.e(TAG, "Cannot send text: No active session")
            return
        }

        session?.sendUserMessage(text)
        Log.d(TAG, "Sent text message: ${text.take(100)}${if (text.length > 100) "..." else ""}")
    }

    /**
     * Controls microphone mute state.
     *
     * @param muted true to mute, false to unmute
     */
    suspend fun setMicMuted(muted: Boolean) {
        if (session == null) {
            Log.w(TAG, "Cannot set mic state: No active session")
            return
        }

        withContext(Dispatchers.Main) {
            session?.setMicMuted(muted)
        }
        Log.d(TAG, "Microphone ${if (muted) "muted" else "unmuted"}")
    }

    /**
     * Checks if currently connected to agent.
     *
     * @return true if session is active and connected
     */
    fun isConnected(): Boolean =
        session != null && _status.value == ConnectionStatus.CONNECTED

    // Private helper methods

    private fun buildConversationConfig(
        agentId: String?,
        conversationToken: String?
    ): ConversationConfig = ConversationConfig(
        agentId = agentId,
        conversationToken = conversationToken,

        // ✅ FIX: Connection callback
        onConnect = {
            _status.value = ConnectionStatus.CONNECTED
            Log.i(TAG, "Connection established successfully")
        },

        // ✅ FIX: State change callbacks
        onStatusChange = { status ->
            val newStatus = parseConnectionStatus(status)
            if (_status.value != newStatus) {
                _status.value = newStatus
                Log.d(TAG, "Status changed: $status -> $newStatus")

                // Handle disconnection here since there's no onDisconnect callback
                if (newStatus == ConnectionStatus.DISCONNECTED) {
                    Log.i(TAG, "Connection terminated")
                }
            }
        },

        onModeChange = { mode ->
            val newMode = parseConversationMode(mode)
            if (_mode.value != newMode) {
                _mode.value = newMode
                Log.d(TAG, "Mode changed: $mode -> $newMode")
            }
        },

        // Message handling
        onMessage = { role, messageJson ->
            messageJson?.let {
                _transcript.value = TranscriptUpdate(it, isFinal = true)
                Log.d(TAG, "Message from $role: ${it.take(100)}${if (it.length > 100) "..." else ""}")
            }
        },

        // Voice Activity Detection
        onVadScore = { score ->
            _vadScore.value = score ?: 0f
        },

        // Client-side tools
        clientTools = buildClientTools()
    )

    private fun buildClientTools(): Map<String, ClientTool> = mapOf(
        "logMessage" to object : ClientTool {
            override suspend fun execute(parameters: Map<String, Any>): ClientToolResult {
                val message = parameters["message"] as? String ?: "no-message"
                Log.d(TAG, "Client tool executed - logMessage: $message")
                return ClientToolResult.success("logged:$message")
            }
        }
    )

    private fun resetState() {
        _status.value = ConnectionStatus.DISCONNECTED
        _mode.value = ConversationMode.IDLE
        _vadScore.value = 0f
        _transcript.value = null
        Log.d(TAG, "Session state reset")
    }

    private fun parseConnectionStatus(status: String?): ConnectionStatus =
        when (status?.lowercase()) {
            "connected" -> ConnectionStatus.CONNECTED
            "connecting" -> ConnectionStatus.CONNECTING
            "disconnected" -> ConnectionStatus.DISCONNECTED
            else -> {
                Log.w(TAG, "Unknown connection status: $status, defaulting to DISCONNECTED")
                ConnectionStatus.DISCONNECTED
            }
        }

    private fun parseConversationMode(mode: String?): ConversationMode =
        when (mode?.lowercase()) {
            "speaking" -> ConversationMode.SPEAKING
            "listening" -> ConversationMode.LISTENING
            "idle" -> ConversationMode.IDLE
            else -> {
                Log.w(TAG, "Unknown conversation mode: $mode, defaulting to IDLE")
                ConversationMode.IDLE
            }
        }

    companion object {
        private const val TAG = "ElevenLabsSessionManager"
    }
}
