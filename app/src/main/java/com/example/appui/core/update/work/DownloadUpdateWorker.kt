package com.example.appui.core.update.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.appui.core.notification.DownloadNotificationManager
import com.example.appui.core.update.DownloadProgressMapper
import com.example.appui.domain.repository.AppUpdateRepository
import com.example.appui.domain.repository.DownloadProgress
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest

@HiltWorker
class DownloadUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val updateRepository: AppUpdateRepository,
    private val notificationManager: DownloadNotificationManager
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val KEY_APK_PATH = "apk_path"
        const val NOTIFICATION_ID = 2001
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_DOWNLOAD_URL)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing URL"))

        return try {
            var apkPath: String? = null

            updateRepository.downloadUpdate(url).collectLatest { progress ->
                when (progress) {
                    is DownloadProgress.Downloading -> {
                        setProgress(workDataOf(KEY_PROGRESS to (progress.progress * 100).toInt()))
                    }
                    is DownloadProgress.Completed -> {
                        apkPath = progress.filePath
                    }
                    is DownloadProgress.Failed -> {
                        return@collectLatest
                    }
                    else -> {}
                }
            }

            if (apkPath != null) {
                Result.success(workDataOf(KEY_APK_PATH to apkPath))
            } else {
                Result.failure(workDataOf(KEY_ERROR to "Download failed"))
            }
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to e.localizedMessage))
        }
    }
}
