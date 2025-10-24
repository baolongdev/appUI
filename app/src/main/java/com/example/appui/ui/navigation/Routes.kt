package com.example.appui.ui.navigation

object Routes {
    const val HOME = "home"
    const val UPDATE = "update"
    const val VOICE = "voice"
    const val AI_FACE = "ai_face" // ✅ THÊM
    const val AGENTS = "agents"
    const val HISTORY = "history"

    fun voice(agentId: String?, agentName: String? = null): String {
        return if (agentId.isNullOrBlank()) {
            VOICE
        } else if (agentName.isNullOrBlank()) {
            "$VOICE?agentId=$agentId"
        } else {
            "$VOICE?agentId=$agentId&name=$agentName"
        }
    }

    // ✅ THÊM: Helper for AI Face route
    fun aiFace(agentId: String?, agentName: String? = null): String {
        return if (agentId.isNullOrBlank()) {
            AI_FACE
        } else if (agentName.isNullOrBlank()) {
            "$AI_FACE?agentId=$agentId"
        } else {
            "$AI_FACE?agentId=$agentId&name=$agentName"
        }
    }
}
