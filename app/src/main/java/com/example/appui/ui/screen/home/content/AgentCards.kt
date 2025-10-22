package com.example.appui.ui.screen.home.content

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appui.data.remote.elevenlabs.models.AgentSummaryResponseModel
import com.example.appui.ui.theme.Spacing
import com.example.appui.ui.theme.extendedColors
import java.time.Instant

/**
 * Agent Card - Grid View
 */
@Composable
fun AgentCard(
    agent: AgentSummaryResponseModel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val (timeLabel, isRecent) = remember(agent.lastCallTimeUnixSecs) {
        formatLastCallTime(agent.lastCallTimeUnixSecs)
    }

    ElevatedCard(
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar with status
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.SmartToy,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Status dot
                        if (isRecent) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.extendedColors.success)
                            )
                        }
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            agent.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isFavorite) MaterialTheme.extendedColors.warning
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Agent ID
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    agent.agentId,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        timeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.extendedColors.success
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play")
                }
            }
        }
    }
}

/**
 * Agent List Item - List View
 */
@Composable
fun AgentListItem(
    agent: AgentSummaryResponseModel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val (timeLabel, isRecent) = remember(agent.lastCallTimeUnixSecs) {
        formatLastCallTime(agent.lastCallTimeUnixSecs)
    }

    ElevatedCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.SmartToy,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (isRecent) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(MaterialTheme.extendedColors.success)
                        )
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        agent.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        agent.agentId,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onToggleFavorite, Modifier.size(36.dp)) {
                    Icon(
                        if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isFavorite) MaterialTheme.extendedColors.warning
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalIconButton(
                    onClick = onPlay,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.extendedColors.success
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                }

                IconButton(onClick = onClick, Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

// Helper function
private fun formatLastCallTime(unixSecs: Long?): Pair<String, Boolean> {
    if (unixSecs == null || unixSecs <= 0) return "Never" to false

    val now = Instant.now().epochSecond
    val delta = now - unixSecs
    val sevenDays = 7L * 24 * 3600
    val isRecent = delta in 0..sevenDays

    val label = when {
        delta < 60 -> "Just now"
        delta < 3600 -> "${delta / 60}m ago"
        delta < 86400 -> "${delta / 3600}h ago"
        delta < sevenDays -> "${delta / 86400}d ago"
        else -> {
            val date = java.time.Instant.ofEpochSecond(unixSecs)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            date.toString()
        }
    }

    return label to isRecent
}
