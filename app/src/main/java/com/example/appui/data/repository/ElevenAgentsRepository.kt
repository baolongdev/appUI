package com.example.appui.data.repository

import com.example.appui.data.remote.elevenlabs.ElevenLabsApi
import com.example.appui.data.remote.elevenlabs.AgentSummaryResponseModel
import com.example.appui.data.remote.elevenlabs.GetAgentResponseModel
import javax.inject.Inject

class ElevenAgentsRepository @Inject constructor(
    private val api: ElevenLabsApi
) {
    suspend fun listAgents(search: String? = null): List<AgentSummaryResponseModel> {
        val res = api.listAgents(pageSize = 50, search = search)
        return res.agents
    }

    suspend fun getAgent(agentId: String): GetAgentResponseModel =
        api.getAgent(agentId)
}
