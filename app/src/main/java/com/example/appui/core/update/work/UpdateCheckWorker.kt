package com.example.appui.core.update.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.appui.MainActivity
import com.example.appui.R
import com.example.appui.data.datastore.UpdatePreferences
import com.example.appui.domain.usecase.CheckAppUpdateUseCase
import com.example.appui.utils.Either
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase,
    private val updatePreferences: UpdatePreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (val result = checkAppUpdateUseCase()) {
            is Either.Left -> Result.failure()
            is Either.Right -> {
                val updateInfo = result.value

                // ✅ Only show if update available AND not snoozed/visited
                if (updateInfo.isNewer && shouldShowNotification(updateInfo.version)) {
                    showUpdateNotification(updateInfo.version)
                }
                Result.success()
            }
        }
    }

    /**
     * ✅ UPDATED: Check if should show notification
     */
    private suspend fun shouldShowNotification(version: String): Boolean {
        // Check if user visited update screen
        val isScreenVisited = updatePreferences.isUpdateScreenVisited()
        if (isScreenVisited) return false

        // Check if snoozed for this version
        val isSnoozed = updatePreferences.isUpdateSnoozed(version)
        if (isSnoozed) return false

        return true
    }

    private fun showUpdateNotification(version: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cập nhật ứng dụng",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo khi có phiên bản mới"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "update")
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Phiên bản mới có sẵn")
            .setContentText("Version $version đã sẵn sàng để cài đặt")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Nhấn để xem chi tiết và tải xuống phiên bản $version")
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "update_channel"
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "update_check_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                24, TimeUnit.HOURS,
                30, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
