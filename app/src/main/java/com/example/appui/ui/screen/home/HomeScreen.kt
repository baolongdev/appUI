package com.example.appui.ui.screen.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.appui.ui.screen.agents.AgentsViewModel
import com.example.appui.ui.screen.home.components.Sidebar
import com.example.appui.ui.screen.home.components.SidebarContent
import com.example.appui.ui.screen.home.components.SidebarPosition
import com.example.appui.ui.screen.home.content.HomeContent
import com.example.appui.ui.screen.home.content.MyAgentsContent
import com.example.appui.ui.theme.Spacing
import com.example.appui.ui.theme.extendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    agentsViewModel: AgentsViewModel = hiltViewModel(),
    onVoiceClick: (String?) -> Unit = {},
    onAgentsClick: () -> Unit = {},
    onNavigateToUpdate: () -> Unit = {}
) {
    val homeState by viewModel.ui.collectAsState()
    val agentsState by agentsViewModel.ui.collectAsState()

    // ✅ Show popup on first update detection
    LaunchedEffect(homeState.hasUpdate) {
        if (homeState.hasUpdate && !homeState.showUpdateDialog) {
            viewModel.showUpdateDialog()
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Sidebar
        HomeSidebar(
            isOpen = homeState.sidebarOpen,
            currentSection = homeState.section,
            onSectionSelected = viewModel::selectSection,
            onToggleSidebar = viewModel::toggleSidebar,
            onSettings = onNavigateToUpdate,
            onLogout = { /* TODO */ },
            hasUpdate = homeState.hasUpdate // ✅ Pass update status
        )

        // Main content
        HomeScaffold(
            sidebarOpen = homeState.sidebarOpen,
            currentSection = homeState.section,
            onVoiceClick = onVoiceClick,
            onAgentsClick = onAgentsClick,
            agentsState = agentsState,
            onLoadAgentDetail = agentsViewModel::loadDetail,
            onClearAgentDetail = agentsViewModel::clearDetail
        )

        // ✅ Update notification popup
        if (homeState.showUpdateDialog) {
            UpdateNotificationDialog(
                version = homeState.updateVersion,
                onDismiss = viewModel::dismissUpdateNotification,
                onUpdate = {
                    viewModel.dismissUpdateNotification()
                    onNavigateToUpdate()
                }
            )
        }
    }
}

/**
 * Sidebar với animated width và update badge
 */
@Composable
private fun HomeSidebar(
    isOpen: Boolean,
    currentSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit,
    onToggleSidebar: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    hasUpdate: Boolean // ✅ Update badge flag
) {
    val sidebarWidth = 192.dp
    val collapsedWidth = 84.dp

    val animatedWidth by animateDpAsState(
        targetValue = if (isOpen) sidebarWidth else collapsedWidth,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "sidebar-width"
    )

    Sidebar(
        modifier = Modifier
            .width(animatedWidth)
            .fillMaxHeight(),
        position = SidebarPosition.LEFT,
        isCollapsed = !isOpen
    ) {
        SidebarContent(
            currentSection = currentSection,
            onSectionSelected = onSectionSelected,
            onToggleSidebar = onToggleSidebar,
            onSettings = onSettings,
            onLogout = onLogout,
            isCollapsed = !isOpen,
            hasUpdate = hasUpdate // ✅ Pass to SidebarContent
        )
    }
}

/**
 * Update notification dialog
 */
@Composable
private fun UpdateNotificationDialog(
    version: String,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val extendedColors = MaterialTheme.extendedColors

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Upgrade,
                contentDescription = null,
                tint = extendedColors.success,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                "Phiên bản mới có sẵn!",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Version $version đã sẵn sàng để cài đặt.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Bạn có muốn cập nhật ngay bây giờ?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = extendedColors.success
                )
            ) {
                Text("Cập nhật")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Để sau")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * Main scaffold
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScaffold(
    sidebarOpen: Boolean,
    currentSection: HomeSection,
    onVoiceClick: (String?) -> Unit,
    onAgentsClick: () -> Unit,
    agentsState: com.example.appui.ui.screen.agents.AgentsUiState,
    onLoadAgentDetail: (String) -> Unit,
    onClearAgentDetail: () -> Unit
) {
    val sidebarWidth = 192.dp
    val collapsedWidth = 84.dp
    val sidebarGap = Spacing.Small

    val contentPadding by animateDpAsState(
        targetValue = if (sidebarOpen) {
            sidebarWidth + sidebarGap
        } else {
            collapsedWidth + sidebarGap
        },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "content-padding"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = contentPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (currentSection) {
            HomeSection.HOME -> HomeContent(
                onVoiceClick = { onVoiceClick(null) },
                onAgentsClick = onAgentsClick
            )

            HomeSection.MY_AGENTS -> MyAgentsContent(
                agents = agentsState.agents,
                selectedAgent = agentsState.selected,
                isLoadingDetail = agentsState.isLoading && agentsState.selected != null,
                detailError = agentsState.error,
                onOpenAgent = onLoadAgentDetail,
                onPlayAgent = onVoiceClick,
                onToggleFavorite = { _, _ -> /* TODO */ },
                onCloseDetail = onClearAgentDetail
            )
        }
    }
}
