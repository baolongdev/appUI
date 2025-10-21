// ui/screen/update/UpdateViewModel.kt
package com.example.appui.ui.screen.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appui.BuildConfig
import com.example.appui.domain.usecase.CheckAppUpdateUseCase
import com.example.appui.domain.usecase.GetAllReleasesUseCase
import com.example.appui.domain.repository.AppUpdateRepository
import com.example.appui.domain.repository.DownloadProgress
import com.example.appui.utils.Either
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase,
    private val getAllReleasesUseCase: GetAllReleasesUseCase,
    private val updateRepository: AppUpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        loadCurrentVersion()
        checkForUpdate()
        loadAllReleases()
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
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            latestUpdate = updateInfo,
                            updateAvailable = updateInfo.isNewer,
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

    fun downloadAndInstall(url: String) {
        viewModelScope.launch {
            updateRepository.downloadUpdate(url).collect { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }

                if (progress is DownloadProgress.Completed) {
                    installUpdate(progress.filePath)
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
                    // Installation intent launched
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
