package com.example.appui.ui.screen.home.content

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appui.data.remote.elevenlabs.models.AgentSummaryResponseModel
import com.example.appui.ui.theme.Spacing

@Composable
fun AgentsListView(
    agents: List<AgentSummaryResponseModel>,
    viewMode: AgentViewMode,
    favorites: List<String>,
    onOpenAgent: (String) -> Unit,
    onPlayAgent: (String) -> Unit,
    onAvatarView: (String, String?) -> Unit, // ✅ FIXED: (agentId, agentName)
    onToggleFavorite: (String, Boolean) -> Unit
) {
    if (agents.isEmpty()) {
        EmptyAgentsView()
        return
    }

    when (viewMode) {
        AgentViewMode.CARD -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                contentPadding = PaddingValues(Spacing.ExtraSmall),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                items(agents, key = { it.agentId }) { agent ->
                    AgentCard(
                        agent = agent,
                        isFavorite = favorites.contains(agent.agentId),
                        onClick = { onOpenAgent(agent.agentId) },
                        onPlay = {
                            Log.d("AgentsListView", "▶️ Play: ${agent.name}")
                            onPlayAgent(agent.agentId)
                        },
                        onAvatarView = { // ✅ FIXED: Pass both agentId and name
                            Log.d("AgentsListView", "🎭 Avatar: ${agent.name} (${agent.agentId})")
                            onAvatarView(agent.agentId, agent.name)
                        },
                        onToggleFavorite = {
                            onToggleFavorite(agent.agentId, !favorites.contains(agent.agentId))
                        }
                    )
                }
            }
        }
        AgentViewMode.LIST -> {
            LazyColumn(
                contentPadding = PaddingValues(Spacing.ExtraSmall),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                items(agents, key = { it.agentId }) { agent ->
                    AgentListItem(
                        agent = agent,
                        isFavorite = favorites.contains(agent.agentId),
                        onClick = { onOpenAgent(agent.agentId) },
                        onPlay = {
                            Log.d("AgentsListView", "▶️ Play: ${agent.name}")
                            onPlayAgent(agent.agentId)
                        },
                        onAvatarView = { // ✅ FIXED: Pass both agentId and name
                            Log.d("AgentsListView", "🎭 Avatar: ${agent.name} (${agent.agentId})")
                            onAvatarView(agent.agentId, agent.name)
                        },
                        onToggleFavorite = {
                            onToggleFavorite(agent.agentId, !favorites.contains(agent.agentId))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAgentsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.SearchOff,
                        null,
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                "No agents found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Try adjusting your search or filters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
