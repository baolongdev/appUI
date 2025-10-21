package com.example.appui.ui.screen.home.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appui.data.remote.elevenlabs.AgentSummaryResponseModel
import com.example.appui.ui.theme.Spacing
import com.example.appui.ui.theme.extendedColors
import java.time.Instant
import java.time.ZoneId

/**
 * Agent row item for list view
 */
@Composable
fun AgentRowItem(
    agent: AgentSummaryResponseModel,
    isFavorite: Boolean,
    onOpen: (String) -> Unit,
    onPlay: (String) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit
) {
    val (lastLabel, isRecent) = remember(agent.lastCallTimeUnixSecs) {
        formatLastCallTime(agent.lastCallTimeUnixSecs)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(agent.agentId) },
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            // Status dot
            StatusDot(isActive = isRecent)

            // Agent info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
            ) {
                Text(
                    text = agent.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Cuối: $lastLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Favorite button
            IconButton(
                onClick = { onToggleFavorite(agent.agentId, !isFavorite) }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = if (isFavorite) "Bỏ yêu thích" else "Yêu thích",
                    tint = if (isFavorite) {
                        MaterialTheme.extendedColors.warning
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Play button
            TextButton(onClick = { onPlay(agent.agentId) }) {
                Text("Chạy")
            }
        }
    }
}

/**
 * Agent card item for grid view
 */
@Composable
fun AgentCardItem(
    agent: AgentSummaryResponseModel,
    isFavorite: Boolean,
    onOpen: (String) -> Unit,
    onPlay: (String) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit
) {
    val (lastLabel, isRecent) = remember(agent.lastCallTimeUnixSecs) {
        formatLastCallTime(agent.lastCallTimeUnixSecs)
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = { onOpen(agent.agentId) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                StatusDot(isActive = isRecent)

                Text(
                    text = agent.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )

                IconButton(
                    onClick = { onToggleFavorite(agent.agentId, !isFavorite) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (isFavorite) "Bỏ yêu thích" else "Yêu thích",
                        tint = if (isFavorite) {
                            MaterialTheme.extendedColors.warning
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Last call time
            Text(
                text = "Cuối: $lastLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                OutlinedButton(
                    onClick = { onOpen(agent.agentId) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Chi tiết")
                }
                Button(
                    onClick = { onPlay(agent.agentId) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Chạy")
                }
            }
        }
    }
}

/**
 * Status indicator dot
 */
@Composable
private fun StatusDot(isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(
                if (isActive) {
                    MaterialTheme.extendedColors.success
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                }
            )
    )
}

/**
 * Format last call time with relative labels
 * @return Pair<label, isRecent>
 */
private fun formatLastCallTime(unixSecs: Long?): Pair<String, Boolean> {
    if (unixSecs == null || unixSecs <= 0L) {
        return "—" to false
    }

    val now = Instant.now().epochSecond
    val delta = now - unixSecs
    val sevenDays = 7L * 24 * 3600
    val isRecent = delta in 0..sevenDays

    val dateTime = Instant.ofEpochSecond(unixSecs)
        .atZone(ZoneId.systemDefault())

    val label = when {
        delta < 60 -> "vừa xong"
        delta < 3600 -> "${delta / 60} phút trước"
        delta < 86400 -> "${delta / 3600} giờ trước"
        delta < sevenDays -> "${delta / 86400} ngày trước"
        else -> dateTime.toLocalDate().toString()
    }

    return label to isRecent
}
