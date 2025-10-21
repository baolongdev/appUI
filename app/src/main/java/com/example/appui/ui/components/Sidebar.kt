package com.example.appui.ui.screen.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.appui.ui.theme.Elevation
import com.example.appui.ui.theme.Spacing

/**
 * Sidebar position enum
 */
enum class SidebarPosition {
    LEFT,
    RIGHT
}

/**
 * Sidebar container - hỗ trợ collapsed mode
 *
 * @param position LEFT or RIGHT side of screen
 * @param isCollapsed True nếu chỉ hiển thị icons
 * @param content Sidebar content
 */
@Composable
fun Sidebar(
    modifier: Modifier = Modifier,
    position: SidebarPosition = SidebarPosition.LEFT,
    isCollapsed: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = when (position) {
        SidebarPosition.LEFT -> MaterialTheme.shapes.large
        SidebarPosition.RIGHT -> MaterialTheme.shapes.large
    }

    Surface(
        modifier = modifier
            .padding(Spacing.Small)
            .clip(shape),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = Elevation.Level3,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.Small)
        ) {
            content()
        }
    }
}

/**
 * Scrollable content section
 */
@Composable
fun ColumnScope.SidebarScrollableContent(
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = Spacing.ExtraSmall),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        content()
    }
}

/**
 * Sidebar divider
 */
@Composable
private fun SidebarDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Small),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp
    )
}

/**
 * Sidebar header (generic)
 */
@Composable
fun ColumnScope.SidebarHeader(
    title: String,
    onClose: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (onClose != null) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Đóng",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
