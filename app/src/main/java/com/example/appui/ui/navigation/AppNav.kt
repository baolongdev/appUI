package com.example.appui.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.appui.ui.screen.home.HomeScreen
import com.example.appui.ui.screen.voice.VoiceScreen
import com.example.appui.ui.screen.agents.AgentsScreen
import com.example.appui.ui.screen.update.UpdateScreen
import com.example.appui.ui.screen.history.ConversationHistoryScreen // ✅ NEW

@Composable
fun AppNav() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {
        // Home Screen
        composable(Routes.HOME) {
            HomeScreen(
                onVoiceClick = { agentId -> nav.navigate(Routes.voice(agentId)) },
                onAgentsClick = { nav.navigate(Routes.AGENTS) },
                onNavigateToUpdate = { nav.navigate(Routes.UPDATE) },
            )
        }

        // Voice Screen
        composable(
            route = Routes.VOICE + "?agentId={agentId}",
            arguments = listOf(
                navArgument("agentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStack ->
            val agentId = backStack.arguments?.getString("agentId")
            VoiceScreen(
                agentId = agentId,
                onNavigateBack = { nav.popBackStack() }
            )
        }

        // Agents Screen
        composable(Routes.AGENTS) {
            AgentsScreen(
                onPlayAgent = { id -> nav.navigate(Routes.voice(id)) },
                onNavigateBack = { nav.popBackStack() }
            )
        }

        // Conversation History Screen
        composable(Routes.HISTORY) {
            ConversationHistoryScreen(
                onNavigateBack = { nav.popBackStack() }
            )
        }

        // Update Screen
        composable(Routes.UPDATE) {
            UpdateScreen(
                onNavigateBack = { nav.popBackStack() }
            )
        }
    }
}
