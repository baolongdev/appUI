// data/repository/AppUpdateRepositoryImpl.kt
package com.example.appui.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import com.example.appui.BuildConfig
import com.example.appui.data.remote.github.GitHubApiService
import com.example.appui.domain.model.AppRelease
import com.example.appui.domain.model.ReleaseAsset
import com.example.appui.domain.model.UpdateInfo
import com.example.appui.domain.repository.AppUpdateRepository
import com.example.appui.domain.repository.DownloadProgress
import com.example.appui.utils.Either
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.time.Instant
import javax.inject.Inject

class AppUpdateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: GitHubApiService,
    private val httpClient: OkHttpClient
) : AppUpdateRepository {

    private val currentVersionCode: Int by lazy {
        try {
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

    override suspend fun checkForUpdate(): Either<String, UpdateInfo> {
        return try {
            val response = apiService.getUpdateJson(BuildConfig.UPDATE_JSON_URL)

            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val updateInfo = UpdateInfo(
                    version = dto.latestVersion,
                    versionCode = dto.latestVersionCode,
                    downloadUrl = dto.downloadUrl,
                    releaseNotes = dto.releaseNotes,
                    releasedAt = Instant.parse(dto.releasedAt),
                    mandatory = dto.mandatory,
                    isNewer = dto.latestVersionCode > currentVersionCode
                )
                Either.Right(updateInfo)
            } else {
                Either.Left("Failed to check for updates: ${response.code()}")
            }
        } catch (e: Exception) {
            Either.Left("Error checking for updates: ${e.message}")
        }
    }

    override suspend fun getAllReleases(): Either<String, List<AppRelease>> {
        return try {
            val response = apiService.getReleases(
                owner = BuildConfig.GITHUB_OWNER,
                repo = BuildConfig.GITHUB_REPO
            )

            if (response.isSuccessful && response.body() != null) {
                val releases = response.body()!!
                    .filter { !it.prerelease } // Chỉ lấy stable releases
                    .map { dto ->
                        AppRelease(
                            tagName = dto.tagName,
                            name = dto.name ?: dto.tagName,
                            body = dto.body ?: "",
                            publishedAt = dto.publishedAt,
                            assets = dto.assets.map { asset ->
                                ReleaseAsset(
                                    name = asset.name,
                                    downloadUrl = asset.browserDownloadUrl,
                                    size = asset.size,
                                    contentType = asset.contentType
                                )
                            }
                        )
                    }
                Either.Right(releases)
            } else {
                Either.Left("Failed to fetch releases: ${response.code()}")
            }
        } catch (e: Exception) {
            Either.Left("Error fetching releases: ${e.message}")
        }
    }

    override suspend fun downloadUpdate(url: String): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Idle)

        try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(DownloadProgress.Failed("Download failed: ${response.code}"))
                return@flow
            }

            val body = response.body ?: run {
                emit(DownloadProgress.Failed("Empty response body"))
                return@flow
            }

            val totalBytes = body.contentLength()
            val downloadDir = File(context.cacheDir, "updates")
            downloadDir.mkdirs()

            val apkFile = File(downloadDir, "app-update.apk")
            val inputStream = body.byteStream()
            val outputStream = apkFile.outputStream()

            var downloadedBytes = 0L
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                val progress = if (totalBytes > 0) {
                    downloadedBytes.toFloat() / totalBytes.toFloat()
                } else 0f

                emit(DownloadProgress.Downloading(progress, downloadedBytes, totalBytes))
            }

            outputStream.close()
            inputStream.close()

            emit(DownloadProgress.Completed(apkFile.absolutePath))

        } catch (e: Exception) {
            emit(DownloadProgress.Failed("Download error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun installUpdate(apkPath: String): Either<String, Unit> {
        return try {
            val apkFile = File(apkPath)
            if (!apkFile.exists()) {
                return Either.Left("APK file not found")
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
            Either.Right(Unit)

        } catch (e: Exception) {
            Either.Left("Installation error: ${e.message}")
        }
    }
}
