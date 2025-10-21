package com.example.appui.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase // ✅ Inject use case
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    init {
        // ✅ Check for updates on startup
        checkForUpdates()
    }

    fun selectSection(section: HomeSection) {
        _ui.update { it.copy(section = section) }
    }

    fun toggleSidebar() {
        _ui.update { it.copy(sidebarOpen = !it.sidebarOpen) }
    }

    // ✅ Check for updates
    private fun checkForUpdates() {
        viewModelScope.launch {
            when (val result = checkAppUpdateUseCase()) {
                is Either.Left -> {
                    // Ignore error silently
                }
                is Either.Right -> {
                    val updateInfo = result.value
                    if (updateInfo.isNewer) {
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

    // ✅ Dismiss update notification
    fun dismissUpdateNotification() {
        _ui.update { it.copy(showUpdateDialog = false) }
    }

    // ✅ Show update dialog
    fun showUpdateDialog() {
        _ui.update { it.copy(showUpdateDialog = true) }
    }
}
