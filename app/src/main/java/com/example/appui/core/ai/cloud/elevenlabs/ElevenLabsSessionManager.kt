package com.example.appui.core.ai.cloud.elevenlabs

import android.content.Context
import io.elevenlabs.ClientTool
import io.elevenlabs.ClientToolResult
import io.elevenlabs.ConversationClient
import io.elevenlabs.ConversationConfig
import io.elevenlabs.ConversationSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ElevenLabsSessionManager(
    private val appContext: Context,
) {
    private var session: ConversationSession? = null

    private val _status = MutableStateFlow("disconnected")
    val status = _status.asStateFlow()

    private val _mode = MutableStateFlow("idle")
    val mode = _mode.asStateFlow()

    private val _transcript = MutableStateFlow<Pair<String, Boolean>?>(null)
    val transcript = _transcript.asStateFlow()

    private val _vadScore = MutableStateFlow(0f)
    val vadScore = _vadScore.asStateFlow()

    /** startSession là suspend → connect cũng suspend */
    suspend fun connect(agentId: String?, conversationToken: String?) {
        if (session != null) return
        _status.value = "connecting"

        val config = ConversationConfig(
            agentId = agentId,
            conversationToken = conversationToken,
            onConnect = { _status.value = "connected" },
            onStatusChange = { s -> _status.value = s },
            onModeChange = { m -> _mode.value = m ?: "idle" },
            onMessage = { _, messageJson ->
                _transcript.value = messageJson?.let { it to true }
            },
            onVadScore = { score -> _vadScore.value = score ?: 0f },
            clientTools = mapOf(
                "logMessage" to object : ClientTool {
                    override suspend fun execute(parameters: Map<String, Any>): ClientToolResult {
                        val msg = parameters["message"] as? String ?: "no-message"
                        return ClientToolResult.success("logged:$msg")
                    }
                }
            )
        )

        // đa số SDK ưu tiên chạy trên Main
        session = withContext(Dispatchers.Main) {
            ConversationClient.startSession(config, appContext)
        }
    }

    /** endSession là suspend → disconnect cũng suspend */
    suspend fun disconnect() {
        withContext(Dispatchers.Main) {
            session?.endSession()
        }
        session = null
        _status.value = "disconnected"
        _mode.value = "idle"
        _vadScore.value = 0f
    }

    fun sendText(text: String) {
        session?.sendUserMessage(text)
    }

    /** setMicMuted là suspend → bọc trong suspend */
    suspend fun setMicMuted(muted: Boolean) {
        withContext(Dispatchers.Main) {
            session?.setMicMuted(muted)
        }
    }

    fun isConnected(): Boolean = session != null && _status.value == "connected"
}
