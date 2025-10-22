package com.example.appui.ui.navigation

object Routes {
    const val HOME = "home"
    const val UPDATE = "update"
    const val VOICE = "voice"
    const val AGENTS = "agents"
    const val HISTORY = "history"

    fun voice(agentId: String?): String =
        if (agentId.isNullOrBlank()) VOICE else "$VOICE?agentId=$agentId"
}
