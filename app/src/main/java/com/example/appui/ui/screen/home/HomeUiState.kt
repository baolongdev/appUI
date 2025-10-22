package com.example.appui.ui.screen.home

data class HomeUiState(
    val section: HomeSection = HomeSection.HOME,
    val sidebarOpen: Boolean = true,
    val hasUpdate: Boolean = false,
    val updateVersion: String = "",
    val showUpdateDialog: Boolean = false,
    val currentVersion: String = ""
)
