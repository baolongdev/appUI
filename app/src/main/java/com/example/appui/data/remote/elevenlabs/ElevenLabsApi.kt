// com/example/appui/data/remote/elevenlabs/ElevenLabsApi.kt
package com.example.appui.data.remote.elevenlabs

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ElevenLabsApi {
    @GET("v1/convai/agents")
    suspend fun listAgents(
        @Query("page_size") pageSize: Int? = 30,
        @Query("search") search: String? = null,
        @Query("archived") archived: Boolean? = null,
        @Query("sort_direction") sortDirection: String? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("cursor") cursor: String? = null
    ): GetAgentsPageResponseModel

    @GET("v1/convai/agents/{agent_id}")
    suspend fun getAgent(@Path("agent_id") agentId: String): GetAgentResponseModel
}
