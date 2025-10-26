@file:OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)

package com.example.appui.ui.screen.home.content

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.appui.data.remote.elevenlabs.models.AgentSummaryResponseModel
import com.example.appui.data.remote.elevenlabs.models.GetAgentResponseModel
import com.example.appui.ui.screen.home.components.Sidebar
import com.example.appui.ui.screen.home.components.SidebarPosition
import com.example.appui.ui.theme.Spacing
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.Locale

@Composable
fun MyAgentsContent(
    agents: List<AgentSummaryResponseModel>,
    selectedAgent: GetAgentResponseModel?,
    isLoadingDetail: Boolean,
    detailError: String?,
    onOpenAgent: (String) -> Unit,
    onPlayAgent: (String) -> Unit,
    onAvatarView: (String, String?) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onCloseDetail: () -> Unit
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    var filterTab by rememberSaveable { mutableStateOf(AgentFilterTab.ALL) }
    var sortBy by rememberSaveable { mutableStateOf(AgentSortBy.NEWEST) }
    var viewMode by rememberSaveable { mutableStateOf(AgentViewMode.CARD) }

    val favoritesLocal = rememberSaveable(
        saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })
    ) { mutableStateListOf<String>() }

    LaunchedEffect(searchText) {
        snapshotFlow { searchText }
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce(300)
            .collect { debouncedQuery = it }
    }

    val filteredAgents by remember {
        derivedStateOf {
            agents
                .asSequence()
                .filter { agent ->
                    if (debouncedQuery.isNotBlank()) {
                        val query = debouncedQuery.lowercase()
                        agent.name.lowercase().contains(query) ||
                                agent.agentId.lowercase().contains(query)
                    } else true
                }
                .filter { agent ->
                    when (filterTab) {
                        AgentFilterTab.LAST_7_DAYS -> isWithinLast7Days(agent.lastCallTimeUnixSecs)
                        AgentFilterTab.ALL -> true
                    }
                }
                .sortedWith(
                    when (sortBy) {
                        AgentSortBy.NEWEST -> compareByDescending { it.lastCallTimeUnixSecs ?: 0L }
                        AgentSortBy.OLDEST -> compareBy { it.lastCallTimeUnixSecs ?: Long.MAX_VALUE }
                        AgentSortBy.NAME -> compareBy { it.name.lowercase(Locale.getDefault()) }
                    }
                )
                .toList()
        }
    }

    val isPanelOpen = selectedAgent != null || isLoadingDetail || detailError != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            AgentsToolbar(
                searchText = searchText,
                onSearchChange = { searchText = it },
                viewMode = viewMode,
                onViewModeChange = { viewMode = it },
                filterTab = filterTab,
                onFilterTabChange = { filterTab = it },
                sortBy = sortBy,
                onSortByChange = { sortBy = it }
            )

            AgentsListView(
                agents = filteredAgents,
                viewMode = viewMode,
                favorites = favoritesLocal,
                onOpenAgent = onOpenAgent,
                onPlayAgent = onPlayAgent,
                onAvatarView = { agentId, agentName ->
                    Log.d("MyAgentsContent", "🎭 Avatar: id=$agentId, name=$agentName")
                    onAvatarView(agentId, agentName)
                },
                onToggleFavorite = { id, isFavorite ->
                    if (isFavorite) favoritesLocal.add(id) else favoritesLocal.remove(id)
                    onToggleFavorite(id, isFavorite)
                }
            )
        }

        // Scrim overlay
        AnimatedVisibility(
            visible = isPanelOpen,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                    .clickable(
                        onClick = onCloseDetail,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
            )
        }

        // Agent detail panel (right sidebar)
        val panelWidth = 420.dp
        val density = LocalDensity.current

        AnimatedVisibility(
            visible = isPanelOpen,
            enter = slideInHorizontally(
                initialOffsetX = { with(density) { panelWidth.toPx().toInt() } },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { with(density) { panelWidth.toPx().toInt() } },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier
                .fillMaxHeight()
                .width(panelWidth)
                .align(Alignment.TopEnd)
        ) {
            Sidebar(
                position = SidebarPosition.RIGHT,
                modifier = Modifier.fillMaxHeight()
            ) {
                AgentDetailContent(
                    agent = selectedAgent,
                    isLoading = isLoadingDetail,
                    error = detailError,
                    onClose = onCloseDetail,
                    onPlay = onPlayAgent
                )
            }
        }
    }
}

private fun isWithinLast7Days(unixSecs: Long?): Boolean {
    if (unixSecs == null || unixSecs <= 0) return false
    val delta = Instant.now().epochSecond - unixSecs
    return delta in 0..(7L * 24 * 3600)
}
