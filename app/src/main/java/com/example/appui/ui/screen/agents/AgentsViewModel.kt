package com.example.appui.ui.screen.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appui.data.repository.ElevenAgentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AgentsViewModel @Inject constructor(
    private val repo: ElevenAgentsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AgentsUiState(isLoading = true))
    val ui: StateFlow<AgentsUiState> = _ui

    // search query với debounce
    private val query = MutableStateFlow<String?>(null)

    init {
        // khi khởi tạo, auto load + debounce khi search thay đổi
        viewModelScope.launch {
            query
                .debounce(350)
                .distinctUntilChanged()
                .onEach { q ->
                    _ui.update { it.copy(isLoading = true, error = null) }
                    runCatching { repo.listAgents(q) }
                        .onSuccess { list ->
                            val sorted = list.sortedByDescending { it.lastCallTimeUnixSecs ?: 0L }
                            _ui.update { it.copy(isLoading = false, agents = sorted, selected = null) }
                        }
                        .onFailure { e ->
                            _ui.update { it.copy(isLoading = false, error = e.message ?: "Load failed") }
                        }
                }
                .launchIn(this)
        }
        refresh() // initial
    }

    fun onSearchChange(text: String) {
        query.value = text.ifBlank { null }
    }

    fun refresh(search: String? = query.value) {
        query.value = search // reuse pipeline ở trên
    }

    fun loadDetail(agentId: String) = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true, error = null) }
        runCatching { repo.getAgent(agentId) }
            .onSuccess { detail -> _ui.update { it.copy(isLoading = false, selected = detail) } }
            .onFailure { e -> _ui.update { it.copy(isLoading = false, error = e.message ?: "Load detail failed") } }
    }

    fun clearDetail() { _ui.update { it.copy(selected = null) } }
}
