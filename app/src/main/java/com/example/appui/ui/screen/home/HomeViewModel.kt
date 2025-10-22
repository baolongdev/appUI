package com.example.appui.ui.screen.home

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
    private val updatePreferences: UpdatePreferences
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    init {
        loadCurrentVersion()
        checkForUpdates()
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

    private fun checkForUpdates() {
        viewModelScope.launch {
            val isScreenVisited = updatePreferences.isUpdateScreenVisited()
            if (isScreenVisited) return@launch

            when (val result = checkAppUpdateUseCase()) {
                is Either.Left -> {}
                is Either.Right -> {
                    val updateInfo = result.value
                    if (updateInfo.isNewer) {
                        val isSnoozed = updatePreferences.isUpdateSnoozed(updateInfo.version)
                        if (!isSnoozed) {
                            _ui.update {
                                it.copy(
                                    hasUpdate = true,
                                    updateVersion = updateInfo.version
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun dismissUpdateNotification() {
        _ui.update { it.copy(showUpdateDialog = false) }
    }

    fun showUpdateDialog() {
        _ui.update { it.copy(showUpdateDialog = true) }
    }
}
