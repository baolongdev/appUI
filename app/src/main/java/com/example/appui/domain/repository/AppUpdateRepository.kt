// domain/repository/AppUpdateRepository.kt
package com.example.appui.domain.repository

import com.example.appui.domain.model.AppRelease
import com.example.appui.domain.model.UpdateInfo
import com.example.appui.utils.Either
import kotlinx.coroutines.flow.Flow

interface AppUpdateRepository {
    suspend fun checkForUpdate(): Either<String, UpdateInfo>
    suspend fun getAllReleases(): Either<String, List<AppRelease>>
    suspend fun downloadUpdate(url: String): Flow<DownloadProgress>
    suspend fun installUpdate(apkPath: String): Either<String, Unit>
}

sealed class DownloadProgress {
    data object Idle : DownloadProgress()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadProgress()
    data class Completed(val filePath: String) : DownloadProgress()
    data class Failed(val error: String) : DownloadProgress()
}
