@file:OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)

package com.example.appui.ui.screen.home.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.appui.data.remote.elevenlabs.AgentSummaryResponseModel
import com.example.appui.data.remote.elevenlabs.GetAgentResponseModel
import com.example.appui.ui.screen.home.components.Sidebar
import com.example.appui.ui.screen.home.components.SidebarPosition
import com.example.appui.ui.theme.Spacing
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.Locale

/**
 * My Agents Content - Optimized with animations
 *
 * Performance optimizations:
 * - derivedStateOf for filtering (tránh recompute không cần thiết)
 * - key() for isolated recomposition
 * - Flow debouncing for search (300ms)
 * - AnimatedVisibility cho smooth transitions
 */
@Composable
fun MyAgentsContent(
    agents: List<AgentSummaryResponseModel>,
    selectedAgent: GetAgentResponseModel?,
    isLoadingDetail: Boolean,
    detailError: String?,
    onOpenAgent: (String) -> Unit,
    onPlayAgent: (String) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onCloseDetail: () -> Unit
) {
    // State
    var search by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(FilterTab.ALL) }
    var sortBy by rememberSaveable { mutableStateOf(SortBy.Newest) }
    var viewMode by rememberSaveable { mutableStateOf(ViewMode.Card) }

    // Favorites (local state)
    val favoritesLocal = rememberSaveable(
        saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })
    ) { mutableStateListOf<String>() }

    // ⚡ Debounce search (300ms delay)
    LaunchedEffect(search) {
        snapshotFlow { search }
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce(300)
            .collect { query = it }
    }

    // ⚡ Optimized filtering with derivedStateOf
    // Chỉ recompute khi dependencies thực sự thay đổi
    val filtered by remember {
        derivedStateOf {
            agents
                .asSequence()
                .filter { agent ->
                    // Search filter
                    if (query.isNotBlank()) {
                        val lowerQuery = query.lowercase()
                        agent.name.lowercase().contains(lowerQuery) ||
                                agent.agentId.lowercase().contains(lowerQuery)
                    } else {
                        true
                    }
                }
                .filter { agent ->
                    // Time filter
                    when (tab) {
                        FilterTab.LAST7D -> isRecent7d(agent.lastCallTimeUnixSecs)
                        FilterTab.ALL -> true
                    }
                }
                .sortedWith(
                    when (sortBy) {
                        SortBy.Newest -> compareByDescending { it.lastCallTimeUnixSecs ?: 0L }
                        SortBy.Oldest -> compareBy { it.lastCallTimeUnixSecs ?: Long.MAX_VALUE }
                        SortBy.Name -> compareBy { it.name.lowercase(Locale.getDefault()) }
                    }
                )
                .toList()
        }
    }

    val panelOpen = selectedAgent != null || isLoadingDetail || detailError != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            // ⚡ Toolbar (isolated recomposition)
            key("toolbar") {
                AgentsToolbar(
                    search = search,
                    onSearchChange = { search = it },
                    viewMode = viewMode,
                    onViewModeChange = { viewMode = it },
                    filterTab = tab,
                    onFilterTabChange = { tab = it },
                    sortBy = sortBy,
                    onSortByChange = { sortBy = it }
                )
            }

            // ⚡ List/Grid (only recomposes when filtered list changes)
            key("content", viewMode, tab, sortBy) {
                AgentsListView(
                    agents = filtered,
                    viewMode = viewMode,
                    favorites = favoritesLocal,
                    onOpenAgent = onOpenAgent,
                    onPlayAgent = onPlayAgent,
                    onToggleFavorite = { id, fav ->
                        if (fav) favoritesLocal.add(id) else favoritesLocal.remove(id)
                        onToggleFavorite(id, fav)
                    }
                )
            }
        }

        // Scrim overlay
        AnimatedVisibility(
            visible = panelOpen,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim)
                    .clickable { onCloseDetail() }
            )
        }

        // Right sidebar (detail panel) - Slide from RIGHT
        val panelWidth = 420.dp
        val density = LocalDensity.current
        AnimatedVisibility(
            visible = panelOpen,
            enter = slideInHorizontally(
                initialOffsetX = { with(density) { panelWidth.toPx().toInt() } }, // From right
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { with(density) { panelWidth.toPx().toInt() } }, // To right
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier
                .fillMaxHeight()
                .width(panelWidth)
                .align(Alignment.TopEnd) // ✅ Align to right
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

// Utils
private fun isRecent7d(unixSecs: Long?): Boolean {
    if (unixSecs == null || unixSecs <= 0) return false
    val delta = java.time.Instant.now().epochSecond - unixSecs
    return delta in 0..(7L * 24 * 3600)
}
