package com.example.appui.ui.screen.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.appui.ui.components.spotlightTarget
import com.example.appui.ui.screen.home.HomeSection
import com.example.appui.ui.theme.ComponentSize
import com.example.appui.ui.theme.Spacing

@Composable
fun ColumnScope.SidebarContent(
    currentSection: HomeSection,
    currentVersion: String,
    onSectionSelected: (HomeSection) -> Unit,
    onToggleSidebar: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    isCollapsed: Boolean = false,
    hasUpdate: Boolean = false,
    showTutorial: Boolean = false,
    onShowTutorial: () -> Unit = {}
) {
    SidebarHeaderWithToggle(
        isCollapsed = isCollapsed,
        onToggle = onToggleSidebar
    )

    Spacer(Modifier.height(Spacing.Small))
    SidebarDivider()
    Spacer(Modifier.height(Spacing.Small))

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
    ) {
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

        // ✅ HOME SECTION
        SidebarNavigationItem(
            section = HomeSection.HOME,
            selected = HomeSection.HOME == currentSection,
            onClick = { onSectionSelected(HomeSection.HOME) },
            isCollapsed = isCollapsed,
            modifier = Modifier.spotlightTarget("home_section")
        )

        // ✅ MY AGENTS SECTION
        SidebarNavigationItem(
            section = HomeSection.MY_AGENTS,
            selected = HomeSection.MY_AGENTS == currentSection,
            onClick = { onSectionSelected(HomeSection.MY_AGENTS) },
            isCollapsed = isCollapsed,
            modifier = Modifier.spotlightTarget("my_agents_section")
        )

        // ✅ HISTORY SECTION
        SidebarNavigationItem(
            section = HomeSection.HISTORY,
            selected = HomeSection.HISTORY == currentSection,
            onClick = { onSectionSelected(HomeSection.HISTORY) },
            isCollapsed = isCollapsed,
            modifier = Modifier.spotlightTarget("history_section")
        )
    }

    Spacer(Modifier.height(Spacing.Small))
    SidebarDivider()
    Spacer(Modifier.height(Spacing.Medium))

    SidebarFooterContent(
        currentVersion = currentVersion,
        onSettings = onSettings,
        onLogout = onLogout,
        isCollapsed = isCollapsed,
        hasUpdate = hasUpdate,
        showTutorial = showTutorial,
        onShowTutorial = onShowTutorial
    )
}

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
        Surface(
            modifier = Modifier.size(logoSize),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 0.dp
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo ứng dụng",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.ExtraSmall)
            )
        }

        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Thu gọn/Mở rộng sidebar",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
private fun SidebarNavigationItem(
    section: HomeSection,
    selected: Boolean,
    onClick: () -> Unit,
    isCollapsed: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            Color.Transparent
        },
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "nav-bg-${section.name}"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            Color.Transparent
        },
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "nav-border-${section.name}"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 1f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "text-alpha-${section.name}"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentSize.ButtonHeight)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            ),
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
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        modifier = Modifier.alpha(textAlpha)
                    )
                }
            }
        }
    }
}

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

@Composable
private fun SidebarFooterContent(
    currentVersion: String,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    isCollapsed: Boolean,
    hasUpdate: Boolean,
    showTutorial: Boolean,
    onShowTutorial: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ExtraSmall)
    ) {
        // ✅ NÚT HƯỚNG DẪN (chỉ hiện sau khi tutorial xong)
        if (!showTutorial) {
            SidebarFooterItem(
                text = "Hướng dẫn",
                icon = Icons.Default.Help,
                onClick = onShowTutorial,
                isDestructive = false,
                isCollapsed = isCollapsed,
                showBadge = false
            )
        }

        // ✅ CÀI ĐẶT
        SidebarFooterItem(
            text = "Cài đặt",
            icon = Icons.Default.Settings,
            onClick = onSettings,
            isDestructive = false,
            isCollapsed = isCollapsed,
            showBadge = hasUpdate,
            modifier = Modifier.spotlightTarget("settings_button")
        )

        // ✅ THOÁT
        SidebarFooterItem(
            text = "Thoát",
            icon = Icons.Default.Logout,
            onClick = onLogout,
            isDestructive = true,
            isCollapsed = isCollapsed,
            showBadge = false
        )

        if (!isCollapsed && currentVersion.isNotBlank()) {
            Spacer(Modifier.height(Spacing.Small))
            Text(
                text = "Phiên bản $currentVersion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = Spacing.Small)
            )
        }
    }
}

@Composable
private fun SidebarFooterItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean,
    isCollapsed: Boolean,
    showBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textAlpha by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 1f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "footer-text-alpha-$text"
    )

    Surface(
        modifier = modifier
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

                    if (showBadge) {
                        Box(
                            modifier = Modifier
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
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
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        modifier = Modifier.alpha(textAlpha)
                    )
                }
            }
        }
    }
}
