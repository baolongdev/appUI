package com.example.appui.ui.screen.home.content

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort // ✅ AutoMirrored
import androidx.compose.material.icons.automirrored.filled.ViewList // ✅ AutoMirrored
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appui.ui.theme.Spacing

enum class AgentFilterTab {
    ALL, LAST_7_DAYS
}

enum class AgentSortBy {
    NEWEST, OLDEST, NAME
}

enum class AgentViewMode {
    CARD, LIST
}

@Composable
fun AgentsToolbar(
    searchText: String,
    onSearchChange: (String) -> Unit,
    viewMode: AgentViewMode,
    onViewModeChange: (AgentViewMode) -> Unit,
    filterTab: AgentFilterTab,
    onFilterTabChange: (AgentFilterTab) -> Unit,
    sortBy: AgentSortBy,
    onSortByChange: (AgentSortBy) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search agents...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = if (searchText.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                } else null,
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(Modifier.padding(4.dp)) {
                    ViewModeButton(
                        icon = Icons.Default.GridView,
                        selected = viewMode == AgentViewMode.CARD,
                        onClick = { onViewModeChange(AgentViewMode.CARD) }
                    )
                    ViewModeButton(
                        icon = Icons.AutoMirrored.Filled.ViewList, // ✅ FIXED
                        selected = viewMode == AgentViewMode.LIST,
                        onClick = { onViewModeChange(AgentViewMode.LIST) }
                    )
                }
            }

            SortMenuButton(
                currentSort = sortBy,
                onSortChange = onSortByChange
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            FilterTabButton(
                text = "All",
                selected = filterTab == AgentFilterTab.ALL,
                onClick = { onFilterTabChange(AgentFilterTab.ALL) }
            )
            FilterTabButton(
                text = "Last 7 Days",
                selected = filterTab == AgentFilterTab.LAST_7_DAYS,
                onClick = { onFilterTabChange(AgentFilterTab.LAST_7_DAYS) }
            )
        }
    }
}

@Composable
private fun ViewModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    val iconTint = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = backgroundColor,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = iconTint
                )
            }
        }
    }
}

@Composable
private fun SortMenuButton(
    currentSort: AgentSortBy,
    onSortChange: (AgentSortBy) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilledTonalIconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Sort, "Sort") // ✅ FIXED
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortMenuItem(
                text = "Newest First",
                icon = Icons.Default.Schedule,
                selected = currentSort == AgentSortBy.NEWEST,
                onClick = {
                    onSortChange(AgentSortBy.NEWEST)
                    expanded = false
                }
            )
            SortMenuItem(
                text = "Oldest First",
                icon = Icons.Default.History,
                selected = currentSort == AgentSortBy.OLDEST,
                onClick = {
                    onSortChange(AgentSortBy.OLDEST)
                    expanded = false
                }
            )
            SortMenuItem(
                text = "Name (A-Z)",
                icon = Icons.Default.SortByAlpha,
                selected = currentSort == AgentSortBy.NAME,
                onClick = {
                    onSortChange(AgentSortBy.NAME)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun SortMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                icon,
                null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        },
        trailingIcon = if (selected) {
            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
        } else null
    )
}

@Composable
private fun FilterTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
        } else null
    )
}
