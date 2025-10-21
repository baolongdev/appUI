package com.example.appui.ui.screen.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appui.R
import com.example.appui.ui.screen.home.HomeSection
import com.example.appui.ui.theme.ComponentSize
import com.example.appui.ui.theme.Spacing

/**
 * Sidebar content với smooth animations và update badge
 */
@Composable
fun ColumnScope.SidebarContent(
    currentSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit,
    onToggleSidebar: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    isCollapsed: Boolean = false,
    hasUpdate: Boolean = false // ✅ Add hasUpdate parameter
) {
    // Header
    SidebarHeaderWithToggle(
        isCollapsed = isCollapsed,
        onToggle = onToggleSidebar
    )

    Spacer(Modifier.height(Spacing.Small))
    SidebarDivider()
    Spacer(Modifier.height(Spacing.Small))

    // Navigation section
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
    ) {
        // Section title với smooth height transition
        val titleAlpha by animateFloatAsState(
            targetValue = if (isCollapsed) 0f else 1f,
            animationSpec = tween(250, easing = FastOutSlowInEasing),
            label = "title-alpha"
        )

        val titleHeight by animateDpAsState(
            targetValue = if (isCollapsed) 0.dp else 32.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "title-height"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(titleHeight)
        ) {
            if (titleAlpha > 0f) {
                Text(
                    text = "Điều hướng",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall)
                        .alpha(titleAlpha)
                )
            }
        }

        // Navigation items
        HomeSection.entries.forEach { section ->
            SidebarNavigationItem(
                section = section,
                selected = section == currentSection,
                onClick = { onSectionSelected(section) },
                isCollapsed = isCollapsed
            )
        }
    }

    Spacer(Modifier.height(Spacing.Small))
    SidebarDivider()
    Spacer(Modifier.height(Spacing.Medium))

    // Footer
    SidebarFooterContent(
        onSettings = onSettings,
        onLogout = onLogout,
        isCollapsed = isCollapsed,
        hasUpdate = hasUpdate // ✅ Pass to footer
    )
}

/**
 * Header with logo and toggle
 */
@Composable
private fun SidebarHeaderWithToggle(
    isCollapsed: Boolean,
    onToggle: () -> Unit
) {
    val logoSize by animateDpAsState(
        targetValue = if (isCollapsed) 40.dp else ComponentSize.AvatarLarge,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "logo-size"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 180f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "chevron-rotation"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        // Logo
        Surface(
            modifier = Modifier.size(logoSize),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            tonalElevation = 0.dp
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = "App Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.ExtraSmall)
            )
        }

        // Toggle button
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Toggle sidebar",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

/**
 * Navigation item
 */
@Composable
private fun SidebarNavigationItem(
    section: HomeSection,
    selected: Boolean,
    onClick: () -> Unit,
    isCollapsed: Boolean
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    val textAlpha by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 1f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "text-alpha-${section.name}"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(ComponentSize.ButtonHeight)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.Small),
            contentAlignment = if (isCollapsed) Alignment.Center else Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = "${section.displayName} icon",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ComponentSize.IconMedium)
                )

                if (!isCollapsed) {
                    Spacer(Modifier.width(Spacing.Medium))

                    Text(
                        text = section.displayName,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        modifier = Modifier.alpha(textAlpha)
                    )
                }
            }
        }
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
 * Footer content with Settings and Logout
 */
@Composable
private fun SidebarFooterContent(
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    isCollapsed: Boolean,
    hasUpdate: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ExtraSmall)
    ) {
        // Settings button with update badge
        SidebarFooterItem(
            text = "Cài đặt",
            icon = Icons.Default.Settings,
            onClick = onSettings,
            isDestructive = false,
            isCollapsed = isCollapsed,
            showBadge = hasUpdate
        )

        // Logout button
        SidebarFooterItem(
            text = "Đăng xuất",
            icon = Icons.Default.Logout,
            onClick = onLogout,
            isDestructive = true,
            isCollapsed = isCollapsed,
            showBadge = false
        )
    }
}

/**
 * Footer item with optional badge
 */
@Composable
private fun SidebarFooterItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean,
    isCollapsed: Boolean,
    showBadge: Boolean = false
) {
    val textAlpha by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 1f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "footer-text-alpha-$text"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(ComponentSize.ButtonHeight)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.Small),
            contentAlignment = if (isCollapsed) Alignment.Center else Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Icon with badge
                Box {
                    Icon(
                        imageVector = icon,
                        contentDescription = "$text icon",
                        tint = if (isDestructive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(ComponentSize.IconMedium)
                    )

                    // Red dot badge (top-right corner)
                    if (showBadge) {
                        Box(
                            modifier = Modifier
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                                .align(Alignment.TopEnd)
                        )
                    }
                }

                if (!isCollapsed) {
                    Spacer(Modifier.width(Spacing.Medium))

                    Text(
                        text = text,
                        color = if (isDestructive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        modifier = Modifier.alpha(textAlpha)
                    )
                }
            }
        }
    }
}
