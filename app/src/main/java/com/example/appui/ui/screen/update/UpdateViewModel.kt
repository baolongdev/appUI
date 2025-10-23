package com.example.appui.ui.screen.update

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appui.BuildConfig
import com.example.appui.data.datastore.UpdatePreferences
import com.example.appui.data.repository.AppUpdateRepositoryImpl
import com.example.appui.domain.usecase.CheckAppUpdateUseCase
import com.example.appui.domain.usecase.GetAllReleasesUseCase
import com.example.appui.domain.repository.DownloadProgress
import com.example.appui.utils.Either
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase,
    private val getAllReleasesUseCase: GetAllReleasesUseCase,
    private val updateRepository: AppUpdateRepositoryImpl,
    private val updatePreferences: UpdatePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    init {
        loadCurrentVersion()
        restoreDownloadState()
        checkForUpdate()
        loadAllReleases()
        observeSnoozedVersion()
        markUpdateScreenVisited()
    }

    private fun loadCurrentVersion() {
        try {
            val versionName = BuildConfig.VERSION_NAME
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }

            _uiState.update {
                it.copy(
                    currentVersion = versionName,
                    currentVersionCode = versionCode
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    currentVersion = BuildConfig.VERSION_NAME,
                    currentVersionCode = BuildConfig.VERSION_CODE
                )
            }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true, error = null) }

            when (val result = checkAppUpdateUseCase()) {
                is Either.Left -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            error = result.value
                        )
                    }
                }
                is Either.Right -> {
                    val updateInfo = result.value

                    // ✅ Nếu có phiên bản mới và không bị snoozed
                    val isSnoozed = updatePreferences.isUpdateSnoozed(updateInfo.version)

                    if (updateInfo.isNewer && !isSnoozed) {
                        // ✅ Clear dismissed và screen visited để popup hiện lại với version mới
                        updatePreferences.clearDismissed()
                        updatePreferences.clearUpdateScreenVisited()
                        updatePreferences.clearSnooze()
                    }

                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            latestUpdate = updateInfo,
                            updateAvailable = updateInfo.isNewer && !it.isDismissed,
                            error = null
                        )
                    }
                }
            }
        }
    }

    fun loadAllReleases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReleases = true, error = null) }

            when (val result = getAllReleasesUseCase()) {
                is Either.Left -> {
                    _uiState.update {
                        it.copy(
                            isLoadingReleases = false,
                            error = result.value
                        )
                    }
                }
                is Either.Right -> {
                    _uiState.update {
                        it.copy(
                            isLoadingReleases = false,
                            allReleases = result.value,
                            error = null
                        )
                    }
                }
            }
        }
    }

    private fun restoreDownloadState() {
        viewModelScope.launch {
            updatePreferences.getDownloadState()?.let { state ->
                _uiState.update {
                    it.copy(
                        isDownloading = true,
                        currentDownloadUrl = state.url,
                        downloadProgress = DownloadProgress.Downloading(state.progress)
                    )
                }
                continueDownload(state.url)
            }
        }
    }

    private fun markUpdateScreenVisited() {
        viewModelScope.launch {
            updatePreferences.markUpdateScreenVisited()
        }
    }

    private fun observeSnoozedVersion() {
        viewModelScope.launch {
            val latestVersion = _uiState.value.latestUpdate?.version ?: return@launch
            val isSnoozed = updatePreferences.isUpdateSnoozed(latestVersion)

            _uiState.update {
                it.copy(isDismissed = isSnoozed)
            }
        }
    }

    fun downloadAndInstall(url: String) {
        if (_uiState.value.isDownloading) {
            _uiState.update {
                it.copy(error = "Đã có file đang tải. Vui lòng hủy trước khi tải file khác.")
            }
            return
        }

        downloadJob?.cancel()
        continueDownload(url)
    }

    private fun continueDownload(url: String) {
        downloadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloading = true,
                    currentDownloadUrl = url,
                    error = null
                )
            }

            updateRepository.downloadUpdate(url).collect { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }

                when (progress) {
                    is DownloadProgress.Downloading -> {
                        updatePreferences.saveDownloadState(url, progress.progress)
                    }
                    is DownloadProgress.Completed -> {
                        updatePreferences.clearDownloadState()
                        installUpdate(progress.filePath)
                        _uiState.update {
                            it.copy(
                                isDownloading = false,
                                currentDownloadUrl = null
                            )
                        }
                    }
                    is DownloadProgress.Failed -> {
                        updatePreferences.clearDownloadState()
                        _uiState.update {
                            it.copy(
                                isDownloading = false,
                                currentDownloadUrl = null
                            )
                        }
                    }
                    else -> { }
                }
            }
        }
    }

    fun cancelDownload() {
        updateRepository.cancelDownload()
        downloadJob?.cancel()
        downloadJob = null

        viewModelScope.launch {
            updatePreferences.clearDownloadState()
        }

        _uiState.update {
            it.copy(
                isDownloading = false,
                currentDownloadUrl = null,
                downloadProgress = DownloadProgress.Idle
            )
        }
    }

    fun snoozeUpdate() {
        viewModelScope.launch {
            _uiState.value.latestUpdate?.version?.let { version ->
                updatePreferences.snoozeUpdate(version, days = 7)
                _uiState.update {
                    it.copy(
                        updateAvailable = false,
                        isDismissed = true
                    )
                }
            }
        }
    }

    private fun installUpdate(apkPath: String) {
        viewModelScope.launch {
            when (val result = updateRepository.installUpdate(apkPath)) {
                is Either.Left -> {
                    _uiState.update { it.copy(error = result.value) }
                }
                is Either.Right -> {
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // ✅ Thêm function này
    fun hasInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }
}
