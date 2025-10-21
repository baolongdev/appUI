package com.example.appui.ui.screen.home

/**
 * UI state for HomeScreen
 */
data class HomeUiState(
    val section: HomeSection = HomeSection.HOME,
    val sidebarOpen: Boolean = true,

    // ✅ Update notification fields
    val hasUpdate: Boolean = false,
    val updateVersion: String = "",
    val showUpdateDialog: Boolean = false
)
