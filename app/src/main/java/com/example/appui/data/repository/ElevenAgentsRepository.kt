package com.example.appui.data.repository

import com.example.appui.data.remote.elevenlabs.api.ElevenLabsApi
import com.example.appui.data.remote.elevenlabs.models.AgentSummary
import com.example.appui.data.remote.elevenlabs.models.GetAgentDetailResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for ElevenLabs agent management.
 */
@Singleton
class ElevenAgentsRepository @Inject constructor(
    private val api: ElevenLabsApi
) {
    /**
     * Lists available agents with optional search filter.
     *
     * @param search Optional search query
     * @return List of agent summaries
     */
    suspend fun listAgents(search: String? = null): List<AgentSummary> {
        val response = api.listAgents(pageSize = 50, search = search)
        return response.agents
    }

    /**
     * Gets detailed information about a specific agent.
     *
     * @param agentId Unique agent identifier
     * @return Agent detail model
     */
    suspend fun getAgent(agentId: String): GetAgentDetailResponse {
        return api.getAgentDetail(agentId)
    }
}
