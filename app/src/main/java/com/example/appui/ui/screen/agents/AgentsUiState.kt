package com.example.appui.ui.screen.agents

import com.example.appui.data.remote.elevenlabs.models.AgentSummaryResponseModel
import com.example.appui.data.remote.elevenlabs.models.GetAgentDetailResponse

/**
 * UI State for Agents Screen
 */
data class AgentsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val agents: List<AgentSummaryResponseModel> = emptyList(),
    val filtered: List<AgentSummaryResponseModel> = emptyList(), // ✅ THÊM: filtered list
    val selected: GetAgentDetailResponse? = null,
    val favoriteIds: Set<String> = emptySet() // ✅ THÊM: favorite agent IDs
)

// ✅ Alias để tương thích với code cũ
val AgentsUiState.loading: Boolean get() = isLoading
