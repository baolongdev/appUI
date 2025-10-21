// ui/screen/update/UpdateUiState.kt
package com.example.appui.ui.screen.update

import com.example.appui.domain.model.AppRelease
import com.example.appui.domain.model.UpdateInfo
import com.example.appui.domain.repository.DownloadProgress

data class UpdateUiState(
    val currentVersion: String = "",
    val currentVersionCode: Int = 0,
    val latestUpdate: UpdateInfo? = null,
    val allReleases: List<AppRelease> = emptyList(),
    val isCheckingUpdate: Boolean = false,
    val isLoadingReleases: Boolean = false,
    val downloadProgress: DownloadProgress = DownloadProgress.Idle,
    val error: String? = null,
    val updateAvailable: Boolean = false
)
