package com.example.appui.data.remote.elevenlabs

import com.squareup.moshi.Json

data class GetAgentsPageResponseModel(
    val agents: List<AgentSummaryResponseModel>,
    @Json(name = "next_cursor") val nextCursor: String?,
    @Json(name = "has_more") val hasMore: Boolean
)

data class AgentSummaryResponseModel(
    @Json(name = "agent_id") val agentId: String,
    val name: String,
    val tags: List<String>,
    @Json(name = "created_at_unix_secs") val createdAtUnixSecs: Long,
    @Json(name = "last_call_time_unix_secs") val lastCallTimeUnixSecs: Long? = null,
    val archived: Boolean,
    val access_info: ResourceAccessInfo?
)

data class ResourceAccessInfo(
    val is_creator: Boolean,
    val creator_name: String,
    val creator_email: String,
    val role: String // admin | editor | commenter | viewer
)

// ---------------- Detail ----------------

data class GetAgentResponseModel(
    @Json(name = "agent_id") val agentId: String,
    val name: String,
    @Json(name = "conversation_config") val conversationConfig: ConversationalConfig,
    val tags: List<String> = emptyList()
)

data class ConversationalConfig(
    val asr: ASRConfig? = null,
    val tts: TTSConfigOutput? = null,
    val turn: TurnConfig? = null,
    val conversation: ConversationConfig? = null,
    val agent: AgentConfig? = null
)

data class ASRConfig(
    val quality: String? = null, // "high"
    val provider: String? = null, // "elevenlabs"
    @Json(name = "user_input_audio_format") val userInputAudioFormat: String? = null
)

data class TTSConfigOutput(
    @Json(name = "model_id") val modelId: String?,
    @Json(name = "voice_id") val voiceId: String?,
    @Json(name = "agent_output_audio_format") val agentOutputAudioFormat: String?,
    @Json(name = "optimize_streaming_latency") val optimizeStreamingLatency: String? = null,
    val stability: Double? = null,
    val speed: Double? = null,
    @Json(name = "similarity_boost") val similarityBoost: Double? = null
)

data class TurnConfig(
    @Json(name = "turn_timeout") val turnTimeout: Double? = null,
    @Json(name = "silence_end_call_timeout") val silenceEndCallTimeout: Double? = null,
    val mode: String? = null // "silence" | "turn"
)

data class ConversationConfig(
    @Json(name = "text_only") val textOnly: Boolean? = null,
    @Json(name = "max_duration_seconds") val maxDurationSeconds: Int? = null
)

data class AgentConfig(
    @Json(name = "first_message") val firstMessage: String? = null,
    val language: String? = null
)
