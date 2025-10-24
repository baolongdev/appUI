package com.example.appui.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.appui.ui.screen.agents.AgentsScreen
import com.example.appui.ui.screen.aiface.AIFaceScreen
import com.example.appui.ui.screen.history.ConversationHistoryScreen
import com.example.appui.ui.screen.home.HomeScreen
import com.example.appui.ui.screen.update.UpdateScreen
import com.example.appui.ui.screen.voice.VoiceScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {
        // ==================== HOME SCREEN ====================
        composable(Routes.HOME) {
            Log.d("AppNav", "🏠 Rendering HomeScreen")
            HomeScreen(
                onVoiceClick = { agentId ->
                    Log.d("AppNav", "▶️ Voice click: $agentId")
                    nav.navigate(Routes.voice(agentId))
                },
                onAgentsClick = {
                    Log.d("AppNav", "👥 Agents click")
                    nav.navigate(Routes.AGENTS)
                },
                onAvatarView = { agentId, agentName -> // ✅ FIXED: Nhận cả 2 params
                    Log.d("AppNav", "🎭 Avatar view: id=$agentId, name=$agentName")
                    nav.navigate(Routes.aiFace(agentId, agentName))
                },
                onNavigateToUpdate = {
                    Log.d("AppNav", "⬆️ Update click")
                    nav.navigate(Routes.UPDATE)
                }
            )
        }

        // ==================== VOICE SCREEN ====================
        composable(
            route = Routes.VOICE + "?agentId={agentId}&name={name}",
            arguments = listOf(
                navArgument("agentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("name") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStack ->
            val agentId = backStack.arguments?.getString("agentId")
            val agentName = backStack.arguments?.getString("name")
            Log.d("AppNav", "🎙️ VoiceScreen: agentId=$agentId, name=$agentName")
            VoiceScreen(
                agentId = agentId,
                agentName = agentName,
                onNavigateBack = { nav.popBackStack() }
            )
        }

        // ==================== AI FACE SCREEN ====================
        composable(
            route = Routes.AI_FACE + "?agentId={agentId}&name={name}",
            arguments = listOf(
                navArgument("agentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("name") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStack ->
            val agentId = backStack.arguments?.getString("agentId")
            val agentName = backStack.arguments?.getString("name")
            Log.d("AppNav", "🤖 AIFaceScreen: agentId=$agentId, name=$agentName")
            AIFaceScreen(
                agentId = agentId,
                agentName = agentName ?: "AI Assistant",
                onClose = {
                    Log.d("AppNav", "❌ AIFaceScreen closed")
                    nav.popBackStack()
                }
            )
        }

        // ==================== AGENTS SCREEN ====================
        composable(Routes.AGENTS) {
            Log.d("AppNav", "👥 Rendering AgentsScreen")
            AgentsScreen(
                onPlayAgent = { id, name ->
                    Log.d("AppNav", "▶️ Play agent: $id, $name")
                    nav.navigate(Routes.voice(id, name))
                },
                onAvatarView = { id, name ->
                    Log.d("AppNav", "🎭 Avatar view from AgentsScreen: $id, $name")
                    nav.navigate(Routes.aiFace(id, name))
                },
                onNavigateBack = {
                    Log.d("AppNav", "⬅️ Back from AgentsScreen")
                    nav.popBackStack()
                }
            )
        }

        // ==================== CONVERSATION HISTORY SCREEN ====================
        composable(Routes.HISTORY) {
            Log.d("AppNav", "📜 Rendering ConversationHistoryScreen")
            ConversationHistoryScreen(
                onNavigateBack = { nav.popBackStack() }
            )
        }

        // ==================== UPDATE SCREEN ====================
        composable(Routes.UPDATE) {
            Log.d("AppNav", "⬆️ Rendering UpdateScreen")
            UpdateScreen(
                onNavigateBack = { nav.popBackStack() }
            )
        }
    }
}
