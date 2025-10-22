package com.example.appui.core.ai.elevenlabs.config

/**
 * Configuration for ElevenLabs SDK integration.
 *
 * @property baseUrl REST API endpoint (default: https://api.elevenlabs.io)
 * @property apiKey XI-API-KEY for REST client authentication
 * @property defaultAgentId Public agent ID for default connections
 * @property conversationTokenProvider Async callback for private agent token retrieval
 *
 * @see <a href="https://elevenlabs.io/docs">ElevenLabs Documentation</a>
 */
data class ElevenLabsConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val apiKey: String? = null,
    val defaultAgentId: String? = null,
    val conversationTokenProvider: (suspend () -> String)? = null
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.elevenlabs.io"

        /**
         * Validates the configuration for production use.
         *
         * @return List of validation errors, empty if valid
         */
        fun validate(config: ElevenLabsConfig): List<String> = buildList {
            if (config.apiKey.isNullOrBlank()) {
                add("API key is required for production use")
            }
            if (config.defaultAgentId.isNullOrBlank() && config.conversationTokenProvider == null) {
                add("Either defaultAgentId or conversationTokenProvider must be provided")
            }
        }
    }
}
