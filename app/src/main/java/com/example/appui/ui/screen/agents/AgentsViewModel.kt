package com.example.appui.ui.screen.agents

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appui.data.repository.ElevenAgentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ✅ DataStore for favorite agents
private val Context.agentPrefs by preferencesDataStore(name = "agent_preferences")

@HiltViewModel
class AgentsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: ElevenAgentsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AgentsUiState(isLoading = true))
    val ui: StateFlow<AgentsUiState> = _ui

    // Search query với debounce
    private val query = MutableStateFlow<String?>(null)

    // ✅ Key cho DataStore
    private val FAVORITES_KEY = stringSetPreferencesKey("favorite_agents")

    init {
        // Load favorites từ DataStore
        loadFavorites()

        // Auto load + debounce khi search thay đổi
        viewModelScope.launch {
            query
                .debounce(350)
                .distinctUntilChanged()
                .onEach { q ->
                    _ui.update { it.copy(isLoading = true, error = null) }
                    runCatching { repo.listAgents(q) }
                        .onSuccess { list ->
                            val sorted = list.sortedByDescending { it.lastCallTimeUnixSecs ?: 0L }
                            _ui.update {
                                it.copy(
                                    isLoading = false,
                                    agents = sorted,
                                    filtered = filterAgents(sorted, q),
                                    selected = null
                                )
                            }
                        }
                        .onFailure { e ->
                            _ui.update {
                                it.copy(
                                    isLoading = false,
                                    error = e.message ?: "Load failed"
                                )
                            }
                        }
                }
                .launchIn(this)
        }

        refresh() // initial load
    }

    /**
     * ✅ Load favorite IDs từ DataStore
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            context.agentPrefs.data
                .map { prefs ->
                    prefs[FAVORITES_KEY] ?: emptySet()
                }
                .collect { favoriteIds ->
                    _ui.update { it.copy(favoriteIds = favoriteIds) }
                }
        }
    }

    /**
     * ✅ Toggle favorite agent
     */
    fun toggleFavorite(agentId: String) {
        viewModelScope.launch {
            context.agentPrefs.edit { prefs ->
                val current = prefs[FAVORITES_KEY]?.toMutableSet() ?: mutableSetOf()
                if (current.contains(agentId)) {
                    current.remove(agentId)
                } else {
                    current.add(agentId)
                }
                prefs[FAVORITES_KEY] = current
            }
        }
    }

    /**
     * Search query changed
     */
    fun onSearchChange(text: String) {
        val searchQuery = text.ifBlank { null }
        query.value = searchQuery

        // ✅ Update filtered list immediately for UI responsiveness
        _ui.update {
            it.copy(filtered = filterAgents(it.agents, searchQuery))
        }
    }

    /**
     * ✅ Filter agents based on search query
     */
    private fun filterAgents(
        agents: List<com.example.appui.data.remote.elevenlabs.models.AgentSummaryResponseModel>,
        searchQuery: String?
    ): List<com.example.appui.data.remote.elevenlabs.models.AgentSummaryResponseModel> {
        if (searchQuery.isNullOrBlank()) {
            return agents
        }

        val query = searchQuery.lowercase()
        return agents.filter { agent ->
            agent.name.lowercase().contains(query) ||
                    agent.agentId.lowercase().contains(query) ||
                    agent.tags.any { it.lowercase().contains(query) }
        }
    }

    /**
     * Refresh agents list
     */
    fun refresh(search: String? = query.value) {
        query.value = search // reuse pipeline
    }

    /**
     * Load agent detail
     */
    fun loadDetail(agentId: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true, error = null) }
        runCatching { repo.getAgent(agentId) }
            .onSuccess { detail ->
                _ui.update {
                    it.copy(
                        isLoading = false,
                        selected = detail
                    )
                }
            }
            .onFailure { e ->
                _ui.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Load detail failed"
                    )
                }
            }
    }

    /**
     * Clear selected agent detail
     */
    fun clearDetail() {
        _ui.update { it.copy(selected = null) }
    }
}
