@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.appui.ui.screen.update

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.appui.domain.model.AppRelease
import com.example.appui.domain.repository.DownloadProgress
import com.example.appui.ui.theme.Spacing
import com.example.appui.ui.theme.extendedColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UpdateScreen(
    viewModel: UpdateViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val extendedColors = MaterialTheme.extendedColors

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.checkForUpdate() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                // Current version card
                item {
                    CurrentVersionCard(
                        version = uiState.currentVersion,
                        versionCode = uiState.currentVersionCode
                    )
                }

                // Update available card
                if (uiState.updateAvailable && uiState.latestUpdate != null) {
                    item {
                        UpdateAvailableCard(
                            updateInfo = uiState.latestUpdate!!,
                            downloadProgress = uiState.downloadProgress,
                            onDownload = { viewModel.downloadAndInstall(it) }
                        )
                    }
                }

                // All releases section
                item {
                    Text(
                        text = "Tất cả phiên bản",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = Spacing.Small)
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
                    items(uiState.allReleases) { release ->
                        ReleaseCard(
                            release = release,
                            currentVersionCode = uiState.currentVersionCode,
                            onDownload = { asset ->
                                viewModel.downloadAndInstall(asset.downloadUrl)
                            }
                        )
                    }
                }
            }

            // Error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(Spacing.Medium),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(
                                "OK",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                ) {
                    Text(error)
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.MediumLarge)
        ) {
            Row(
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
}

@Composable
private fun UpdateAvailableCard(
    updateInfo: com.example.appui.domain.model.UpdateInfo,
    downloadProgress: DownloadProgress,
    onDownload: (String) -> Unit
) {
    val extendedColors = MaterialTheme.extendedColors

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
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (updateInfo.releaseNotes.isNotBlank()) {
                Text(
                    updateInfo.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            when (downloadProgress) {
                is DownloadProgress.Idle -> {
                    Button(
                        onClick = { onDownload(updateInfo.downloadUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(Spacing.Small))
                        Text("Tải về và cài đặt")
                    }
                }
                is DownloadProgress.Downloading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        LinearProgressIndicator(
                            progress = { downloadProgress.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            "Đang tải: ${(downloadProgress.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                is DownloadProgress.Completed -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = extendedColors.success,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Đã tải xong! Đang mở trình cài đặt...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extendedColors.success
                        )
                    }
                }
                is DownloadProgress.Failed -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Lỗi: ${downloadProgress.error}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
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
    onDownload: (com.example.appui.domain.model.ReleaseAsset) -> Unit
) {
    val extendedColors = MaterialTheme.extendedColors

    val versionCode = release.tagName.removePrefix("v")
        .split(".")
        .let { parts ->
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            major * 10000 + minor * 100 + patch
        }

    val isCurrent = versionCode == currentVersionCode

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
                Column {
                    Text(
                        release.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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
                Text(
                    release.body.take(200) + if (release.body.length > 200) "..." else "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            release.assets.firstOrNull { it.name.endsWith(".apk") }?.let { apk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${apk.size / (1024 * 1024)} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isCurrent) {
                        Button(
                            onClick = { onDownload(apk) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(Spacing.ExtraSmall))
                            Text("Tải về")
                        }
                    }
                }
            }
        }
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
