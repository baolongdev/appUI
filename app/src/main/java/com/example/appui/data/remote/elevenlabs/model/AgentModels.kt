package com.example.appui.data.remote.elevenlabs.models

import com.squareup.moshi.Json

/**
 * Response for list agents API.
 */
data class GetAgentsPageResponse(
    val agents: List<AgentSummary>,
    @Json(name = "next_cursor") val nextCursor: String?,
    @Json(name = "has_more") val hasMore: Boolean
)

/**
 * Agent summary - KHÔNG ĐỔI TÊN
 */
data class AgentSummary(
    @Json(name = "agent_id") val agentId: String,
    val name: String,
    val tags: List<String> = emptyList(),
    @Json(name = "created_at_unix_secs") val createdAtUnixSecs: Long? = null,
    @Json(name = "last_call_time_unix_secs") val lastCallTimeUnixSecs: Long? = null,
    val archived: Boolean = false,
    @Json(name = "access_info") val accessInfo: ResourceAccessInfo? = null
)

/**
 * Alias for UI compatibility
 */
typealias AgentSummaryResponseModel = AgentSummary

/**
 * Resource access info.
 */
data class ResourceAccessInfo(
    @Json(name = "is_creator") val isCreator: Boolean,
    @Json(name = "creator_name") val creatorName: String? = null,
    @Json(name = "creator_email") val creatorEmail: String? = null,
    val role: String? = null
)

/**
 * Response for get agent detail API.
 */
data class GetAgentDetailResponse(
    @Json(name = "agent_id") val agentId: String,
    val name: String,
    @Json(name = "conversation_config") val conversationConfig: ConversationalConfig? = null,
    val tags: List<String> = emptyList(),
    @Json(name = "created_at_unix_secs") val createdAtUnixSecs: Long? = null,
    @Json(name = "last_call_time_unix_secs") val lastCallTimeUnixSecs: Long? = null
)

/**
 * ✅ ALIAS for UI compatibility (FIXED)
 */
typealias AgentDetailResponseModel = GetAgentDetailResponse

/**
 * Alias for backward compatibility
 */
typealias GetAgentResponseModel = GetAgentDetailResponse

/**
 * Conversational AI configuration.
 */
data class ConversationalConfig(
    val asr: AsrConfig? = null,
    val tts: TtsConfig? = null,
    val turn: TurnConfig? = null,
    val conversation: ConversationConfigDetail? = null,
    val agent: AgentConfig? = null
)

data class AsrConfig(
    val quality: String? = null,
    val provider: String? = null,
    @Json(name = "user_input_audio_format") val userInputAudioFormat: String? = null
)

data class TtsConfig(
    @Json(name = "model_id") val modelId: String? = null,
    @Json(name = "voice_id") val voiceId: String? = null,
    @Json(name = "agent_output_audio_format") val agentOutputAudioFormat: String? = null,
    @Json(name = "optimize_streaming_latency") val optimizeStreamingLatency: String? = null,
    val stability: Double? = null,
    val speed: Double? = null,
    @Json(name = "similarity_boost") val similarityBoost: Double? = null
)

data class TurnConfig(
    @Json(name = "turn_timeout") val turnTimeout: Double? = null,
    @Json(name = "silence_end_call_timeout") val silenceEndCallTimeout: Double? = null,
    val mode: String? = null
)

data class ConversationConfigDetail(
    @Json(name = "text_only") val textOnly: Boolean? = null,
    @Json(name = "max_duration_seconds") val maxDurationSeconds: Int? = null
)

data class AgentConfig(
    @Json(name = "first_message") val firstMessage: String? = null,
    val language: String? = null
)
