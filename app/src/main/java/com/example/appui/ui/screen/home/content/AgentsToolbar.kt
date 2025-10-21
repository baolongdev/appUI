package com.example.appui.ui.screen.home.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appui.ui.theme.Spacing

/**
 * Toolbar with search, view mode, filter, and sort
 */
@Composable
fun AgentsToolbar(
    search: String,
    onSearchChange: (String) -> Unit,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    filterTab: FilterTab,
    onFilterTabChange: (FilterTab) -> Unit,
    sortBy: SortBy,
    onSortByChange: (SortBy) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        // Search + View mode toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            SearchField(
                value = search,
                onValueChange = onSearchChange,
                placeholder = "Tìm agents…",
                modifier = Modifier.weight(1f)
            )

            ViewModeToggle(
                currentMode = viewMode,
                onModeChange = onViewModeChange
            )
        }

        // Filter tabs + Sort menu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            FilterTabs(
                selected = filterTab,
                onSelected = onFilterTabChange
            )
            Spacer(Modifier.weight(1f))
            SortByMenu(
                selected = sortBy,
                onSelected = onSortByChange
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        placeholder = {
            Text(
                placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        singleLine = true,
        modifier = modifier,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun ViewModeToggle(
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit
) {
    FilledTonalIconButton(
        onClick = {
            onModeChange(
                if (currentMode == ViewMode.Card) ViewMode.List else ViewMode.Card
            )
        },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            imageVector = if (currentMode == ViewMode.Card) {
                Icons.Filled.List
            } else {
                Icons.Filled.GridView
            },
            contentDescription = "Toggle view mode"
        )
    }
}

@Composable
private fun FilterTabs(
    selected: FilterTab,
    onSelected: (FilterTab) -> Unit
) {
    val items = listOf(
        FilterTab.ALL to "Tất cả",
        FilterTab.LAST7D to "7 ngày"
    )

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(2.dp)
    ) {
        items.forEachIndexed { index, (tab, label) ->
            SegmentedButton(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                shape = SegmentedButtonDefaults.itemShape(index, items.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    activeContentColor = MaterialTheme.colorScheme.onSurface,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected == tab) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SortByMenu(
    selected: SortBy,
    onSelected: (SortBy) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilledTonalButton(
            onClick = { expanded = true },
            modifier = Modifier.heightIn(min = 36.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("Sắp xếp: ${selected.displayName}")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortBy.values().forEach { sortOption ->
                DropdownMenuItem(
                    text = { Text(sortOption.displayName) },
                    onClick = {
                        onSelected(sortOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Extension for display names
private val SortBy.displayName: String
    get() = when (this) {
        SortBy.Newest -> "Mới nhất"
        SortBy.Oldest -> "Cũ nhất"
        SortBy.Name -> "Tên"
    }
