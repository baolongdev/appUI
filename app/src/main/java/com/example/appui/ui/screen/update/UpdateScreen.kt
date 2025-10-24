@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.appui.ui.screen.update

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.appui.domain.model.AppRelease
import com.example.appui.domain.model.ReleaseAsset
import com.example.appui.domain.model.UpdateInfo
import com.example.appui.domain.repository.DownloadProgress
import com.example.appui.ui.permission.InstallPermissionHandler
import com.example.appui.ui.permission.NotificationPermissionHandler
import com.example.appui.ui.theme.extendedColors
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UpdateScreen(
    viewModel: UpdateViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showInstallPermissionDialog by remember { mutableStateOf(false) }
    var pendingDownloadUrl by remember { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isDownloading) {
        showExitDialog = true
    }

    if (showExitDialog) {
        SimpleAlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = Icons.Outlined.Warning,
            iconColor = MaterialTheme.colorScheme.error,
            title = "Hủy tải xuống?",
            message = "Bạn có chắc muốn hủy và thoát?",
            confirmText = "Hủy",
            dismissText = "Tiếp tục",
            onConfirm = {
                viewModel.cancelDownload()
                showExitDialog = false
                onNavigateBack()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    if (showInstallPermissionDialog) {
        InstallPermissionHandler(
            onPermissionGranted = {
                showInstallPermissionDialog = false
                if (viewModel.hasNotificationPermission()) {
                    pendingDownloadUrl?.let { url ->
                        viewModel.downloadAndInstall(url)
                        pendingDownloadUrl = null
                    }
                } else {
                    showPermissionDialog = true
                }
            },
            onPermissionDenied = {
                showInstallPermissionDialog = false
                pendingDownloadUrl = null
            }
        )
    }

    if (showPermissionDialog) {
        NotificationPermissionHandler(
            onPermissionGranted = {
                showPermissionDialog = false
                pendingDownloadUrl?.let { url ->
                    viewModel.downloadAndInstall(url)
                    pendingDownloadUrl = null
                }
            },
            onPermissionDenied = {
                showPermissionDialog = false
                pendingDownloadUrl = null
            }
        )
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            CleanTopBar(
                isDownloading = uiState.isDownloading,
                isCheckingUpdate = uiState.isCheckingUpdate,
                onNavigateBack = {
                    if (uiState.isDownloading) {
                        showExitDialog = true
                    } else {
                        onNavigateBack()
                    }
                },
                onRefresh = { viewModel.checkForUpdate() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Version
            item {
                CurrentVersionCard(
                    version = uiState.currentVersion,
                    versionCode = uiState.currentVersionCode
                )
            }

            // Update Available
            if ((uiState.updateAvailable || uiState.isDownloading) && uiState.latestUpdate != null) {
                item {
                    UpdateAvailableCard(
                        updateInfo = uiState.latestUpdate!!,
                        downloadProgress = uiState.downloadProgress,
                        isDownloading = uiState.isDownloading,
                        currentDownloadUrl = uiState.currentDownloadUrl,
                        onDownload = { url ->
                            if (!viewModel.hasInstallPermission()) {
                                pendingDownloadUrl = url
                                showInstallPermissionDialog = true
                            } else if (!viewModel.hasNotificationPermission()) {
                                pendingDownloadUrl = url
                                showPermissionDialog = true
                            } else {
                                viewModel.downloadAndInstall(url)
                            }
                        },
                        onCancelDownload = { viewModel.cancelDownload() },
                        onSnooze = { viewModel.snoozeUpdate() }
                    )
                }
            }

            // Section Header
            item {
                SectionDivider(title = "Lịch sử phiên bản")
            }

            // Loading
            if (uiState.isLoadingReleases) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(
                    items = uiState.allReleases,
                    key = { it.id }
                ) { release ->
                    ReleaseCard(
                        release = release,
                        currentVersionCode = uiState.currentVersionCode,
                        isDownloading = uiState.isDownloading,
                        currentDownloadUrl = uiState.currentDownloadUrl,
                        downloadProgress = uiState.downloadProgress,
                        onDownload = { asset ->
                            if (!viewModel.hasInstallPermission()) {
                                pendingDownloadUrl = asset.downloadUrl
                                showInstallPermissionDialog = true
                            } else if (!viewModel.hasNotificationPermission()) {
                                pendingDownloadUrl = asset.downloadUrl
                                showPermissionDialog = true
                            } else {
                                viewModel.downloadAndInstall(asset.downloadUrl)
                            }
                        },
                        onCancelDownload = { viewModel.cancelDownload() } // ✅ Add callback
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CleanTopBar(
    isDownloading: Boolean,
    isCheckingUpdate: Boolean,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Cập nhật",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Outlined.ArrowBack, "Quay lại")
            }
        },
        actions = {
            IconButton(
                onClick = onRefresh,
                enabled = !isCheckingUpdate && !isDownloading
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Outlined.Refresh, "Làm mới")
                }
            }
        }
    )
}

@Composable
private fun CurrentVersionCard(
    version: String,
    versionCode: Int
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Phiên bản hiện tại",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "v$version",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Build $versionCode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UpdateAvailableCard(
    updateInfo: UpdateInfo,
    downloadProgress: DownloadProgress,
    isDownloading: Boolean,
    currentDownloadUrl: String?,
    onDownload: (String) -> Unit,
    onCancelDownload: () -> Unit,
    onSnooze: () -> Unit
) {
    val animatedProgress = remember { Animatable(0f) }
    val isThisFileDownloading = isDownloading && currentDownloadUrl == updateInfo.downloadUrl

    LaunchedEffect(downloadProgress) {
        if (downloadProgress is DownloadProgress.Downloading) {
            animatedProgress.animateTo(
                targetValue = downloadProgress.progress,
                animationSpec = tween(300)
            )
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Upgrade,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Phiên bản mới",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "v${updateInfo.version}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Release Notes
            if (updateInfo.releaseNotes.isNotBlank()) {
                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Có gì mới",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    MarkdownText(
                        markdown = updateInfo.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4
                    )
                }
            }

            HorizontalDivider()

            // Download State
            AnimatedContent(
                targetState = when {
                    isThisFileDownloading && downloadProgress is DownloadProgress.Downloading -> "downloading"
                    isThisFileDownloading && downloadProgress is DownloadProgress.Completed -> "completed"
                    isThisFileDownloading && downloadProgress is DownloadProgress.Failed -> "failed"
                    else -> "idle"
                },
                label = "download_state"
            ) { state ->
                when (state) {
                    "downloading" -> DownloadingState(
                        progress = animatedProgress.value,
                        onCancel = onCancelDownload
                    )
                    "completed" -> CompletedState()
                    "failed" -> FailedState(
                        error = (downloadProgress as DownloadProgress.Failed).error,
                        onRetry = { onDownload(updateInfo.downloadUrl) },
                        isDownloading = isDownloading
                    )
                    else -> IdleState(
                        fileSize = updateInfo.fileSize,
                        isDownloading = isDownloading,
                        onDownload = { onDownload(updateInfo.downloadUrl) },
                        onSnooze = onSnooze
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadingState(
    progress: Float,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Đang tải xuống",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Close, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Hủy")
        }
    }
}

@Composable
private fun CompletedState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.extendedColors.success.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.extendedColors.success,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    "Hoàn tất!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.extendedColors.success
                )
                Text(
                    "Đang mở trình cài đặt...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun FailedState(
    error: String,
    onRetry: () -> Unit,
    isDownloading: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isDownloading
        ) {
            Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Thử lại")
        }
    }
}

@Composable
private fun IdleState(
    fileSize: Long,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onSnooze: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // File Size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "%.1f MB".format(Locale.US, fileSize / (1024f * 1024f)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Button(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isDownloading
        ) {
            Icon(Icons.Outlined.Download, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (isDownloading) "Đang tải..." else "Tải về & Cài đặt",
                style = MaterialTheme.typography.labelLarge
            )
        }

        TextButton(
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Nhắc lại sau 7 ngày")
        }
    }
}

@Composable
private fun SectionDivider(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ReleaseCard(
    release: AppRelease,
    currentVersionCode: Int,
    isDownloading: Boolean,
    currentDownloadUrl: String?,
    downloadProgress: DownloadProgress,
    onDownload: (ReleaseAsset) -> Unit,
    onCancelDownload: () -> Unit // ✅ Add callback
) {
    var isExpanded by remember { mutableStateOf(false) }
    val versionCode = parseVersionCode(release.tagName)
    val isCurrent = versionCode == currentVersionCode
    val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }

    val isThisReleaseDownloading = isDownloading && currentDownloadUrl == apkAsset?.downloadUrl

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = if (isCurrent) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            release.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (release.isPrerelease) {
                            SimpleBadge(text = "Beta", color = MaterialTheme.extendedColors.warning)
                        }
                    }
                    Text(
                        formatDate(release.publishedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isCurrent) {
                    SimpleBadge(text = "Hiện tại", color = MaterialTheme.extendedColors.success)
                }
            }

            // Release Notes
            if (release.body.isNotBlank()) {
                HorizontalDivider()

                MarkdownText(
                    markdown = release.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3
                )

                if (release.body.lines().size > 3) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (isExpanded) "Thu gọn" else "Xem thêm")
                        Icon(
                            if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Download Section
            apkAsset?.let { apk ->
                HorizontalDivider()

                // ✅ Show download progress if downloading
                if (isThisReleaseDownloading) {
                    when (downloadProgress) {
                        is DownloadProgress.Downloading -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Progress header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Đang tải xuống",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${(downloadProgress.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Progress bar
                                LinearProgressIndicator(
                                    progress = { downloadProgress.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                )

                                // ✅ Cancel button
                                OutlinedButton(
                                    onClick = onCancelDownload,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Hủy tải")
                                }
                            }
                        }
                        is DownloadProgress.Completed -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.extendedColors.success.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.extendedColors.success,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Hoàn tất!",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.extendedColors.success
                                    )
                                }
                            }
                        }
                        is DownloadProgress.Failed -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            "Lỗi tải xuống",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }

                                // ✅ Retry button
                                FilledTonalButton(
                                    onClick = { onDownload(apk) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Thử lại")
                                }
                            }
                        }
                        else -> {}
                    }
                } else {
                    // Normal state
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "%.1f MB".format(Locale.US, apk.size / (1024f * 1024f)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!isCurrent) {
                            FilledTonalButton(
                                onClick = { onDownload(apk) },
                                enabled = !isDownloading
                            ) {
                                Icon(Icons.Outlined.Download, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (isDownloading) "Đang tải..." else "Tải về")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleBadge(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SimpleAlertDialog(
    onDismissRequest: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

private fun parseVersionCode(tagName: String): Int {
    val version = tagName.removePrefix("v")
    val parts = version.split(".")
    return try {
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        major * 10000 + minor * 100 + patch
    } catch (e: Exception) {
        0
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = parser.parse(dateString)
        formatter.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}
