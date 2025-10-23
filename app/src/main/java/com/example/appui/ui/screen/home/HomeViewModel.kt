package com.example.appui.ui.screen.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appui.BuildConfig
import com.example.appui.data.datastore.UpdatePreferences
import com.example.appui.domain.usecase.CheckAppUpdateUseCase
import com.example.appui.utils.Either
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase,
    private val updatePreferences: UpdatePreferences,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    // ✅ Persist state across configuration changes
    private var hasCheckedUpdate: Boolean
        get() = savedStateHandle.get<Boolean>("has_checked_update") ?: false
        set(value) = savedStateHandle.set("has_checked_update", value)

    init {
        loadCurrentVersion()
        checkForUpdatesOnce()
    }

    fun selectSection(section: HomeSection) {
        _ui.update { it.copy(section = section) }
    }

    fun toggleSidebar() {
        _ui.update { it.copy(sidebarOpen = !it.sidebarOpen) }
    }

    private fun loadCurrentVersion() {
        _ui.update { it.copy(currentVersion = BuildConfig.VERSION_NAME) }
    }

    // ✅ Chỉ check 1 lần duy nhất per session
    private fun checkForUpdatesOnce() {
        // Nếu đã check rồi thì skip
        if (hasCheckedUpdate) {
            return
        }

        viewModelScope.launch {
            // Check persistent state
            val hasVisitedUpdateScreen = updatePreferences.isUpdateScreenVisited()
            if (hasVisitedUpdateScreen) {
                hasCheckedUpdate = true
                return@launch
            }

            when (val result = checkAppUpdateUseCase()) {
                is Either.Left -> {
                    hasCheckedUpdate = true
                }
                is Either.Right -> {
                    val updateInfo = result.value
                    hasCheckedUpdate = true

                    if (updateInfo.isNewer) {
                        val isSnoozed = updatePreferences.isUpdateSnoozed(updateInfo.version)
                        val isDismissed = updatePreferences.isVersionDismissed(updateInfo.version)

                        if (!isSnoozed && !isDismissed) {
                            _ui.update {
                                it.copy(
                                    hasUpdate = true,
                                    updateVersion = updateInfo.version,
                                    showUpdateDialog = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun dismissUpdateNotification() {
        viewModelScope.launch {
            val version = _ui.value.updateVersion
            if (version.isNotEmpty()) {
                updatePreferences.dismissVersion(version)
            }

            _ui.update {
                it.copy(
                    showUpdateDialog = false
                )
            }
        }
    }

    fun showUpdateDialog() {
        if (_ui.value.hasUpdate) {
            _ui.update { it.copy(showUpdateDialog = true) }
        }
    }
}
