package com.example.appui.ui.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Home sections for navigation
 */
enum class HomeSection(
    val displayName: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Filled.Home),
    MY_AGENTS("My Agents", Icons.Filled.People),
}
