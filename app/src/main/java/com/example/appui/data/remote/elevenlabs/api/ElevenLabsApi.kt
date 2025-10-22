package com.example.appui.data.remote.elevenlabs.api

import com.example.appui.data.remote.elevenlabs.models.GetAgentDetailResponse
import com.example.appui.data.remote.elevenlabs.models.GetAgentsPageResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ElevenLabs REST API interface.
 *
 * @see <a href="https://elevenlabs.io/docs/api-reference">API Reference</a>
 */
interface ElevenLabsApi {

    /**
     * Lists conversational AI agents with pagination.
     *
     * @param pageSize Number of results per page (max 100)
     * @param search Search query for agent names
     * @param archived Filter archived agents
     * @param sortDirection "asc" or "desc"
     * @param sortBy Sort field (e.g., "created_at", "name")
     * @param cursor Pagination cursor from previous response
     */
    @GET("v1/convai/agents")
    suspend fun listAgents(
        @Query("page_size") pageSize: Int? = 30,
        @Query("search") search: String? = null,
        @Query("archived") archived: Boolean? = null,
        @Query("sort_direction") sortDirection: String? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("cursor") cursor: String? = null
    ): GetAgentsPageResponse

    /**
     * Retrieves detailed agent configuration.
     *
     * @param agentId Unique agent identifier
     */
    @GET("v1/convai/agents/{agent_id}")
    suspend fun getAgentDetail(
        @Path("agent_id") agentId: String
    ): GetAgentDetailResponse
}
