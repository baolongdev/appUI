@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.appui.ui.screen.update

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.appui.domain.model.AppRelease
import com.example.appui.domain.model.ReleaseAsset
import com.example.appui.domain.model.UpdateInfo
import com.example.appui.domain.repository.DownloadProgress
import com.example.appui.ui.permission.InstallPermissionHandler
import com.example.appui.ui.permission.NotificationPermissionHandler
import com.example.appui.ui.theme.Spacing
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
    val extendedColors = MaterialTheme.extendedColors
    val snackbarHostState = remember { SnackbarHostState() }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showInstallPermissionDialog by remember { mutableStateOf(false) }
    var pendingDownloadUrl by remember { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isDownloading) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Đang tải xuống")
            },
            text = {
                Text("Bạn có chắc muốn hủy tải xuống và thoát?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelDownload()
                        showExitDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Hủy và thoát", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Button(onClick = { showExitDialog = false }) {
                    Text("Tiếp tục tải")
                }
            }
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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Cập nhật ứng dụng",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.isDownloading) {
                                showExitDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.checkForUpdate() },
                        enabled = !uiState.isCheckingUpdate && !uiState.isDownloading
                    ) {
                        if (uiState.isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Làm mới")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            item {
                CurrentVersionCard(
                    version = uiState.currentVersion,
                    versionCode = uiState.currentVersionCode
                )
            }

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
                        onSnooze = { viewModel.snoozeUpdate() },
                    )
                }
            }

            item {
                Text(
                    text = "Tất cả phiên bản",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Spacing.Medium, bottom = Spacing.Small)
                )
            }

            if (uiState.isLoadingReleases) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.Large),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = extendedColors.success
                        )
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
                        downloadProgress = uiState.downloadProgress,
                        isDownloading = uiState.isDownloading,
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
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentVersionCard(
    version: String,
    versionCode: Int
) {
    val extendedColors = MaterialTheme.extendedColors

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.MediumLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = extendedColors.info,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    "Phiên bản hiện tại",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "v$version ($versionCode)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
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
    onSnooze: () -> Unit,
) {
    val extendedColors = MaterialTheme.extendedColors

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(downloadProgress) {
        if (downloadProgress is DownloadProgress.Downloading) {
            animatedProgress.animateTo(
                targetValue = downloadProgress.progress,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    val isThisFileDownloading = isDownloading && currentDownloadUrl == updateInfo.downloadUrl

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.MediumLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                Icon(
                    Icons.Default.Upgrade,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        "Phiên bản mới có sẵn!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "v${updateInfo.version}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (updateInfo.releaseNotes.isNotBlank()) {
                Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))

                MarkdownText(
                    markdown = updateInfo.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    maxLines = 8,
                    modifier = Modifier.padding(vertical = Spacing.Small)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
            ) {
                when {
                    isThisFileDownloading && downloadProgress is DownloadProgress.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Đang tải xuống...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "${(animatedProgress.value * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { animatedProgress.value },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Không được thoát trong khi tải",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                                TextButton(onClick = onCancelDownload) {
                                    Text("Hủy", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    isThisFileDownloading && downloadProgress is DownloadProgress.Completed -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = extendedColors.success.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.Medium),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = extendedColors.success,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        "Đã tải xong!",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = extendedColors.success
                                    )
                                    Text(
                                        "Đang mở trình cài đặt...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    isThisFileDownloading && downloadProgress is DownloadProgress.Failed -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.Medium),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        downloadProgress.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Button(
                                onClick = { onDownload(updateInfo.downloadUrl) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isDownloading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(Spacing.Small))
                                Text("Thử lại")
                            }
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                        ) {
                            Button(
                                onClick = { onDownload(updateInfo.downloadUrl) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isDownloading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(Spacing.Small))
                                Text(if (isDownloading) "Đang tải file khác..." else "Tải về và cài đặt")
                            }

                            TextButton(
                                onClick = onSnooze,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(Spacing.ExtraSmall))
                                Text("Bỏ qua (nhắc lại sau 7 ngày)")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseCard(
    release: AppRelease,
    currentVersionCode: Int,
    downloadProgress: DownloadProgress,
    isDownloading: Boolean,
    onDownload: (ReleaseAsset) -> Unit
) {
    val extendedColors = MaterialTheme.extendedColors
    var isExpanded by remember { mutableStateOf(false) }

    val versionCode = parseVersionCode(release.tagName)
    val isCurrent = versionCode == currentVersionCode
    val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.MediumLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        Text(
                            release.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (release.isPrerelease) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Beta", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(24.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = extendedColors.warning.copy(alpha = 0.2f),
                                    labelColor = extendedColors.warning
                                )
                            )
                        }
                    }
                    Text(
                        formatDate(release.publishedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isCurrent) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Hiện tại") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = extendedColors.success,
                            labelColor = extendedColors.onSuccess,
                            leadingIconContentColor = extendedColors.onSuccess
                        )
                    )
                }
            }

            if (release.body.isNotBlank()) {
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                MarkdownText(
                    markdown = release.body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 5,
                    modifier = Modifier.padding(vertical = Spacing.Small)
                )

                if (release.body.lines().size > 5) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (isExpanded) "Thu gọn" else "Xem thêm")
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            apkAsset?.let { apk ->
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "%.1f MB".format(Locale.US, apk.size / (1024f * 1024f)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!isCurrent) {
                        Button(
                            onClick = { onDownload(apk) },
                            enabled = !isDownloading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(Spacing.ExtraSmall))
                            Text(if (isDownloading) "Đang tải..." else "Tải về")
                        }
                    }
                }
            }
        }
    }
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
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = parser.parse(dateString)
        formatter.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}
