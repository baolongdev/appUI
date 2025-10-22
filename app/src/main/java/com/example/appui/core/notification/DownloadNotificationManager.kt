package com.example.appui.core.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.appui.core.update.DownloadProgressMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quản lý notification cho việc tải xuống cập nhật.
 * Permission được check trước khi gọi notify().
 */
@Singleton
class DownloadNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        private const val CHANNEL_ID = "app_update_download"
        private const val CHANNEL_NAME = "Tải xuống cập nhật"
        private const val NOTIFICATION_ID = 2001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị tiến trình tải xuống bản cập nhật ứng dụng"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Hiển thị notification đang download với progress bar
     */
    @SuppressLint("MissingPermission")
    fun showDownloadProgress(progress: Int, downloadedBytes: Long, totalBytes: Long) {
        if (!hasNotificationPermission()) return

        val downloadedMb = DownloadProgressMapper.formatMb(downloadedBytes)
        val totalMb = DownloadProgressMapper.formatMb(totalBytes)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Đang tải bản cập nhật")
            .setContentText("$downloadedMb MB / $totalMb MB")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Hiển thị notification download hoàn tất
     */
    @SuppressLint("MissingPermission")
    fun showDownloadComplete(totalBytes: Long) {
        if (!hasNotificationPermission()) return

        val fileSizeMb = DownloadProgressMapper.formatMb(totalBytes)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Tải xuống hoàn tất")
            .setContentText("Đã tải $fileSizeMb MB • Nhấn để cài đặt")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Hiển thị notification download thất bại
     */
    @SuppressLint("MissingPermission")
    fun showDownloadError(error: String) {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Lỗi tải xuống")
            .setContentText(error)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Xóa notification
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Kiểm tra quyền notification (Android 13+)
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
