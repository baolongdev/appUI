package com.example.appui.ui.screen.home

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.appui.ui.screen.agents.AgentsViewModel
import com.example.appui.ui.screen.history.ConversationHistoryContent
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
    onAvatarView: (String, String?) -> Unit = { _, _ -> },
    onNavigateToUpdate: () -> Unit = {},
    onExit: () -> Unit = {}
) {
    val homeState by viewModel.ui.collectAsState()
    val agentsState by agentsViewModel.ui.collectAsState()

    // ✅ THÊM: Exit confirmation dialog state
    var showExitDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        HomeSidebar(
            isOpen = homeState.sidebarOpen,
            currentSection = homeState.section,
            currentVersion = homeState.currentVersion,
            onSectionSelected = viewModel::selectSection,
            onToggleSidebar = viewModel::toggleSidebar,
            onSettings = onNavigateToUpdate,
            onLogout = { showExitDialog = true }, // ✅ Show dialog instead
            hasUpdate = homeState.hasUpdate
        )

        HomeScaffold(
            sidebarOpen = homeState.sidebarOpen,
            currentSection = homeState.section,
            onVoiceClick = onVoiceClick,
            onAgentsClick = onAgentsClick,
            onAvatarView = onAvatarView,
            agentsState = agentsState,
            onLoadAgentDetail = agentsViewModel::loadDetail,
            onClearAgentDetail = agentsViewModel::clearDetail
        )

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

        // ✅ THÊM: Exit confirmation dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        "Thoát ứng dụng?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("Bạn có chắc muốn thoát khỏi ứng dụng không?")
                },
                confirmButton = {
                    Button(
                        onClick = onExit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Thoát")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}

// ✅ THÊM: HomeSidebar function
@Composable
private fun HomeSidebar(
    isOpen: Boolean,
    currentSection: HomeSection,
    currentVersion: String,
    onSectionSelected: (HomeSection) -> Unit,
    onToggleSidebar: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    hasUpdate: Boolean
) {
    val sidebarWidth = 192.dp
    val collapsedWidth = 84.dp

    val animatedWidth by animateDpAsState(
        targetValue = if (isOpen) sidebarWidth else collapsedWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
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
            currentVersion = currentVersion,
            onSectionSelected = onSectionSelected,
            onToggleSidebar = onToggleSidebar,
            onSettings = onSettings,
            onLogout = onLogout,
            isCollapsed = !isOpen,
            hasUpdate = hasUpdate
        )
    }
}

// ✅ THÊM: UpdateNotificationDialog function
@Composable
private fun UpdateNotificationDialog(
    version: String,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val successColor = MaterialTheme.extendedColors.success

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = successColor.copy(alpha = 0.2f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Icon(
                        Icons.Default.Upgrade,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = successColor
                    )
                }
            }
        },
        title = {
            Text(
                "Phiên bản mới có sẵn!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Version $version đã sẵn sàng để cài đặt.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Cập nhật ngay để trải nghiệm tính năng mới",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = successColor
                )
            ) {
                Icon(Icons.Default.Upgrade, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cập nhật ngay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Để sau")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScaffold(
    sidebarOpen: Boolean,
    currentSection: HomeSection,
    onVoiceClick: (String?) -> Unit,
    onAgentsClick: () -> Unit,
    onAvatarView: (String, String?) -> Unit,
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
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
                agents = agentsState.agents,
                onVoiceClick = { agentId -> onVoiceClick(agentId) },
                onAgentsClick = onAgentsClick
            )

            HomeSection.MY_AGENTS -> {
                MyAgentsContent(
                    agents = agentsState.agents,
                    selectedAgent = agentsState.selected,
                    isLoadingDetail = agentsState.isLoading && agentsState.selected != null,
                    detailError = agentsState.error,
                    onOpenAgent = onLoadAgentDetail,
                    onPlayAgent = onVoiceClick,
                    onAvatarView = onAvatarView,
                    onToggleFavorite = { _, _ -> },
                    onCloseDetail = onClearAgentDetail
                )
            }

            HomeSection.HISTORY -> ConversationHistoryContent()
        }
    }
}
