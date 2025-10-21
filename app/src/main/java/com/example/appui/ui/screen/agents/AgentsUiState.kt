package com.example.appui.ui.screen.agents

import com.example.appui.data.remote.elevenlabs.AgentSummaryResponseModel
import com.example.appui.data.remote.elevenlabs.GetAgentResponseModel

data class AgentsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val agents: List<AgentSummaryResponseModel> = emptyList(),
    val selected: GetAgentResponseModel? = null
)