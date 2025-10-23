package com.example.appui.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.appui.BuildConfig
import com.example.appui.core.notification.DownloadNotificationManager
import com.example.appui.core.update.DownloadProgressMapper
import com.example.appui.data.mapper.GitHubMapper
import com.example.appui.data.remote.github.GitHubApiService
import com.example.appui.di.UpdateClient
import com.example.appui.domain.model.AppRelease
import com.example.appui.domain.model.UpdateInfo
import com.example.appui.domain.repository.AppUpdateRepository
import com.example.appui.domain.repository.DownloadProgress
import com.example.appui.utils.Either
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class AppUpdateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @UpdateClient private val okHttpClient: OkHttpClient,
    private val notificationManager: DownloadNotificationManager,
    private val gitHubApiService: GitHubApiService
) : AppUpdateRepository {

    companion object {
        private const val GITHUB_OWNER = "baolongdev"
        private const val GITHUB_REPO = "appUI"
    }

    // ✅ NEW: Store current download call for cancellation
    @Volatile
    private var currentDownloadCall: Call? = null

    override suspend fun checkForUpdate(): Either<String, UpdateInfo> {
        return try {
            val currentVersionCode = getCurrentVersionCode()

            val response = withContext(Dispatchers.IO) {
                gitHubApiService.getLatestRelease(GITHUB_OWNER, GITHUB_REPO)
            }

            when {
                response.isSuccessful -> {
                    val releaseDto = response.body()
                    if (releaseDto != null) {
                        val updateInfo = GitHubMapper.toUpdateInfo(releaseDto, currentVersionCode)
                        Either.Right(updateInfo)
                    } else {
                        Either.Left("Không có dữ liệu từ GitHub")
                    }
                }

                response.code() == 401 -> {
                    Either.Left("Token GitHub không hợp lệ.\nVui lòng kiểm tra GITHUB_TOKEN trong local.properties")
                }

                response.code() == 403 -> {
                    Either.Left("Đã vượt giới hạn API GitHub.\nVui lòng thử lại sau 1 giờ.")
                }

                response.code() == 404 -> {
                    Either.Left("Không tìm thấy repository")
                }

                else -> {
                    Either.Left("Lỗi kiểm tra cập nhật: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            val errorMsg = when (e) {
                is java.net.UnknownHostException -> "Không có kết nối internet"
                is java.net.SocketTimeoutException -> "Kết nối quá chậm"
                else -> "Lỗi: ${e.localizedMessage ?: "Không xác định"}"
            }
            Either.Left(errorMsg)
        }
    }

    override suspend fun getAllReleases(): Either<String, List<AppRelease>> {
        return try {
            val response = withContext(Dispatchers.IO) {
                gitHubApiService.getReleases(GITHUB_OWNER, GITHUB_REPO)
            }

            when {
                response.isSuccessful -> {
                    val releasesDto = response.body()
                    if (releasesDto != null) {
                        val releases = releasesDto
                            .filter { !it.draft }
                            .map { GitHubMapper.toAppRelease(it) }
                        Either.Right(releases)
                    } else {
                        Either.Left("Không có dữ liệu từ GitHub")
                    }
                }

                response.code() == 401 -> {
                    Either.Left("Token GitHub không hợp lệ")
                }

                response.code() == 403 -> {
                    Either.Left("Đã vượt giới hạn API GitHub")
                }

                else -> {
                    Either.Left("Lỗi tải danh sách: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            val errorMsg = when (e) {
                is java.net.UnknownHostException -> "Không có kết nối internet"
                is java.net.SocketTimeoutException -> "Kết nối quá chậm"
                else -> "Lỗi: ${e.localizedMessage ?: "Không xác định"}"
            }
            Either.Left(errorMsg)
        }
    }

    override suspend fun downloadUpdate(url: String): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Downloading(0f))

        try {
            val request = Request.Builder().url(url).build()

            // ✅ Store call reference for cancellation
            val call = okHttpClient.newCall(request)
            currentDownloadCall = call

            val response = call.execute()

            // ✅ Check if cancelled
            if (!response.isSuccessful) {
                currentDownloadCall = null

                if (call.isCanceled()) {
                    notificationManager.cancelNotification()
                    emit(DownloadProgress.Failed("Đã hủy tải xuống"))
                    return@flow
                }

                val errorMsg = "Lỗi tải xuống: ${response.code}"
                notificationManager.showDownloadError(errorMsg)
                emit(DownloadProgress.Failed(errorMsg))
                return@flow
            }

            val body = response.body ?: run {
                currentDownloadCall = null
                val errorMsg = "Không có dữ liệu"
                notificationManager.showDownloadError(errorMsg)
                emit(DownloadProgress.Failed(errorMsg))
                return@flow
            }

            val contentLength = body.contentLength()
            if (contentLength <= 0) {
                currentDownloadCall = null
                val errorMsg = "Không xác định được kích thước file"
                notificationManager.showDownloadError(errorMsg)
                emit(DownloadProgress.Failed(errorMsg))
                return@flow
            }

            val apkFile = File(context.cacheDir, "updates/update.apk")

            apkFile.parentFile?.mkdirs()

            if (apkFile.exists()) {
                apkFile.delete()
            }

            notificationManager.showDownloadProgress(0, 0L, contentLength)

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    var lastProgress = 0
                    var lastNotificationTime = System.currentTimeMillis()
                    var lastEmitTime = System.currentTimeMillis()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // ✅ Check if cancelled during download
                        if (call.isCanceled()) {
                            apkFile.delete()
                            currentDownloadCall = null
                            notificationManager.cancelNotification()
                            emit(DownloadProgress.Failed("Đã hủy tải xuống"))
                            return@flow
                        }

                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        val progress = DownloadProgressMapper.calculateProgress(
                            totalBytesRead,
                            contentLength
                        )
                        val progressFloat = totalBytesRead.toFloat() / contentLength
                        val currentTime = System.currentTimeMillis()

                        val timeSinceLastEmit = currentTime - lastEmitTime
                        val progressDiff = progress - lastProgress

                        if (timeSinceLastEmit >= 200 || progressDiff >= 2) {
                            lastEmitTime = currentTime
                            lastProgress = progress
                            emit(DownloadProgress.Downloading(progressFloat))
                        }

                        if (progress % 5 == 0 || (currentTime - lastNotificationTime) >= 500) {
                            lastNotificationTime = currentTime
                            notificationManager.showDownloadProgress(
                                progress,
                                totalBytesRead,
                                contentLength
                            )
                        }
                    }
                }
            }

            currentDownloadCall = null

            if (apkFile.length() != contentLength) {
                apkFile.delete()
                val errorMsg = "File tải xuống không hoàn chỉnh"
                notificationManager.showDownloadError(errorMsg)
                emit(DownloadProgress.Failed(errorMsg))
                return@flow
            }

            notificationManager.showDownloadComplete(contentLength)
            emit(DownloadProgress.Completed(apkFile.absolutePath))

        } catch (e: Exception) {
            currentDownloadCall = null

            // ✅ Better error handling
            val errorMsg = when {
                e is java.io.IOException && e.message?.contains("Canceled") == true -> {
                    notificationManager.cancelNotification()
                    "Đã hủy tải xuống"
                }
                e is java.net.UnknownHostException -> "Không có kết nối mạng"
                e is java.net.SocketTimeoutException -> "Quá thời gian chờ"
                e is java.io.IOException -> "Lỗi đọc/ghi dữ liệu"
                else -> "Lỗi: ${e.localizedMessage ?: "Không xác định"}"
            }

            if (!errorMsg.contains("Đã hủy")) {
                notificationManager.showDownloadError(errorMsg)
            }

            emit(DownloadProgress.Failed(errorMsg))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * ✅ NEW: Cancel current download
     */
    fun cancelDownload() {
        currentDownloadCall?.cancel()
        currentDownloadCall = null
    }

    override suspend fun installUpdate(apkPath: String): Either<String, Unit> {
        return try {
            val apkFile = File(apkPath)

            if (!apkFile.exists()) {
                return Either.Left("File không tồn tại")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    return Either.Left("Vui lòng cho phép cài đặt từ nguồn không xác định")
                }
            }

            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                    apkFile
                )

                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                val apkUri = Uri.fromFile(apkFile)
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            context.startActivity(intent)

            kotlinx.coroutines.delay(2000)
            notificationManager.cancelNotification()

            Either.Right(Unit)

        } catch (e: Exception) {
            Either.Left("Lỗi cài đặt: ${e.localizedMessage ?: "Không xác định"}")
        }
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: Exception) {
            BuildConfig.VERSION_CODE
        }
    }
}
