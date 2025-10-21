package com.example.appui.ui.screen.home.content

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.appui.data.remote.elevenlabs.AgentSummaryResponseModel
import com.example.appui.ui.theme.Spacing

/**
 * List or Grid view with smooth animations
 *
 * Animations trigger khi:
 * - Đổi view mode (List <-> Grid)
 * - Đổi filter tab
 * - Search results update
 */
@Composable
fun AgentsListView(
    agents: List<AgentSummaryResponseModel>,
    viewMode: ViewMode,
    favorites: SnapshotStateList<String>,
    onOpenAgent: (String) -> Unit,
    onPlayAgent: (String) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit
) {
    // Crossfade giữa empty state và content
    Crossfade(
        targetState = agents.isEmpty(),
        animationSpec = tween(durationMillis = 300),
        label = "content-crossfade"
    ) { isEmpty ->
        if (isEmpty) {
            EmptyState()
        } else {
            // Crossfade giữa List/Grid view modes
            Crossfade(
                targetState = viewMode,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "view-mode-crossfade"
            ) { mode ->
                when (mode) {
                    ViewMode.List -> AgentsListMode(
                        agents = agents,
                        favorites = favorites,
                        onOpenAgent = onOpenAgent,
                        onPlayAgent = onPlayAgent,
                        onToggleFavorite = onToggleFavorite
                    )

                    ViewMode.Card -> AgentsGridMode(
                        agents = agents,
                        favorites = favorites,
                        onOpenAgent = onOpenAgent,
                        onPlayAgent = onPlayAgent,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Không có agent nào.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

/**
 * List mode với item animations
 * - Fade in + Slide up khi xuất hiện
 * - Fade out + Slide up khi biến mất
 */
@Composable
private fun AgentsListMode(
    agents: List<AgentSummaryResponseModel>,
    favorites: SnapshotStateList<String>,
    onOpenAgent: (String) -> Unit,
    onPlayAgent: (String) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        contentPadding = PaddingValues(bottom = Spacing.Medium)
    ) {
        items(
            items = agents,
            key = { it.agentId }
        ) { agent ->
            key(agent.agentId) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + expandVertically(
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    ) + slideOutVertically(
                        targetOffsetY = { -it / 3 },
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    ) + shrinkVertically(
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    )
                ) {
                    val isFavorite = favorites.contains(agent.agentId)
                    AgentRowItem(
                        agent = agent,
                        isFavorite = isFavorite,
                        onOpen = onOpenAgent,
                        onPlay = onPlayAgent,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }
        }
    }
}

/**
 * Grid mode với item animations
 * - Fade in + Scale in khi xuất hiện
 * - Fade out + Scale out khi biến mất
 */
@Composable
private fun AgentsGridMode(
    agents: List<AgentSummaryResponseModel>,
    favorites: SnapshotStateList<String>,
    onOpenAgent: (String) -> Unit,
    onPlayAgent: (String) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 240.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        contentPadding = PaddingValues(bottom = Spacing.Medium)
    ) {
        items(
            items = agents,
            key = { it.agentId }
        ) { agent ->
            key(agent.agentId) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + scaleIn(
                        initialScale = 0.85f,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    ) + scaleOut(
                        targetScale = 0.85f,
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    )
                ) {
                    val isFavorite = favorites.contains(agent.agentId)
                    AgentCardItem(
                        agent = agent,
                        isFavorite = isFavorite,
                        onOpen = onOpenAgent,
                        onPlay = onPlayAgent,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }
        }
    }
}
