package com.example.appui.core.update

import java.util.Locale

/**
 * Helper để tính toán thông tin download cho notification
 */
object DownloadProgressMapper {

    /**
     * Chuyển bytes thành MB (trả về Float, không format)
     */
    fun bytesToMb(bytes: Long): Float {
        return bytes / (1024f * 1024f)
    }

    /**
     * Chuyển bytes thành MB string đã format (dùng Locale.US để tránh lỗi)
     */
    fun formatMb(bytes: Long): String {
        return "%.1f".format(Locale.US, bytes / (1024f * 1024f))
    }

    /**
     * Tính phần trăm download
     */
    fun calculateProgress(downloaded: Long, total: Long): Int {
        if (total <= 0) return 0
        return ((downloaded.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Format size với đơn vị phù hợp
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(Locale.US, bytes / (1024f * 1024f))
            bytes >= 1024 -> "%.1f KB".format(Locale.US, bytes / 1024f)
            else -> "$bytes B"
        }
    }

    /**
     * Format tốc độ download
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1024 * 1024 -> {
                "%.1f MB/s".format(Locale.US, bytesPerSecond / (1024f * 1024f))
            }
            bytesPerSecond >= 1024 -> {
                "%.1f KB/s".format(Locale.US, bytesPerSecond / 1024f)
            }
            else -> {
                "$bytesPerSecond B/s"
            }
        }
    }

    /**
     * Tính tốc độ download trung bình
     */
    fun calculateSpeed(downloadedBytes: Long, elapsedMillis: Long): Long {
        if (elapsedMillis <= 0) return 0L
        return (downloadedBytes * 1000) / elapsedMillis
    }

    /**
     * Ước tính thời gian còn lại
     */
    fun estimateRemainingTime(
        downloadedBytes: Long,
        totalBytes: Long,
        elapsedMillis: Long
    ): String {
        if (downloadedBytes <= 0 || elapsedMillis <= 0 || totalBytes <= downloadedBytes) {
            return "Đang tính..."
        }

        val speed = calculateSpeed(downloadedBytes, elapsedMillis)
        if (speed <= 0) return "Đang tính..."

        val remainingBytes = totalBytes - downloadedBytes
        val remainingSeconds = remainingBytes / speed

        return when {
            remainingSeconds >= 3600 -> {
                val hours = remainingSeconds / 3600
                "${hours}h"
            }
            remainingSeconds >= 60 -> {
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                "${minutes}m ${seconds}s"
            }
            else -> "${remainingSeconds}s"
        }
    }
}
