package com.example.appui.ui.screen.home.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appui.data.remote.elevenlabs.GetAgentResponseModel
import com.example.appui.ui.screen.home.components.SidebarHeader
import com.example.appui.ui.screen.home.components.SidebarScrollableContent
import com.example.appui.ui.theme.Spacing
import com.example.appui.ui.theme.extendedColors

@Composable
fun ColumnScope.AgentDetailContent(
    agent: GetAgentResponseModel?,
    isLoading: Boolean,
    error: String?,
    onClose: () -> Unit,
    onPlay: (String) -> Unit
) {
    val extendedColors = MaterialTheme.extendedColors

    SidebarHeader(
        title = "Chi tiết Agent",
        onClose = onClose
    )

    // ✅ Use HorizontalDivider directly instead of SidebarDivider
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Small),
        color = MaterialTheme.colorScheme.outlineVariant
    )
    Spacer(Modifier.height(Spacing.Small))

    when {
        isLoading -> {
            LoadingState()
        }

        error != null -> {
            ErrorState(error = error)
        }

        agent != null -> {
            SidebarScrollableContent {
                AgentDetailInfo(
                    agent = agent,
                    onPlay = onPlay
                )
            }
        }

        else -> {
            EmptyState()
        }
    }
}

@Composable
private fun ColumnScope.LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.extendedColors.success
            )
            Spacer(Modifier.height(Spacing.Medium))
            Text(
                "Đang tải chi tiết agent...",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ColumnScope.ErrorState(error: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                "Lỗi tải agent",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                error,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ColumnScope.EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Chọn một agent để xem chi tiết",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AgentDetailInfo(
    agent: GetAgentResponseModel,
    onPlay: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val extendedColors = MaterialTheme.extendedColors

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        AgentHeaderCard(
            agent = agent,
            clipboard = clipboard,
            onPlay = onPlay
        )

        ConfigurationSection(
            icon = Icons.Outlined.Audiotrack,
            title = "Cấu hình TTS"
        ) {
            val tts = agent.conversationConfig.tts
            ConfigRow("Model", tts?.modelId)
            ConfigRow("Voice", tts?.voiceId)
            ConfigRow("Định dạng", tts?.agentOutputAudioFormat)
            ConfigRow("Tốc độ", tts?.speed?.let { "%.2f".format(it) })
            ConfigRow("Độ ổn định", tts?.stability?.let { "%.2f".format(it) })
        }

        ConfigurationSection(
            icon = Icons.Outlined.GraphicEq,
            title = "Cấu hình ASR"
        ) {
            val asr = agent.conversationConfig.asr
            ConfigRow("Provider", asr?.provider)
            ConfigRow("Chất lượng", asr?.quality)
            ConfigRow("Định dạng Input", asr?.userInputAudioFormat)
        }

        ConfigurationSection(
            icon = Icons.Outlined.Timelapse,
            title = "Cấu hình Turn"
        ) {
            val turn = agent.conversationConfig.turn
            ConfigRow("Chế độ", turn?.mode)
            ConfigRow("Timeout", turn?.turnTimeout?.let { "%.1fs".format(it) })
            ConfigRow("End-call Silence", turn?.silenceEndCallTimeout?.let { "%.1fs".format(it) })
        }

        ConfigurationSection(
            icon = Icons.Outlined.RecordVoiceOver,
            title = "Prompt Agent"
        ) {
            val agentConfig = agent.conversationConfig.agent
            ConfigRow("Tin nhắn đầu", agentConfig?.firstMessage)
            ConfigRow("Ngôn ngữ", agentConfig?.language)
        }
    }
}

@Composable
private fun AgentHeaderCard(
    agent: GetAgentResponseModel,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    onPlay: (String) -> Unit
) {
    val extendedColors = MaterialTheme.extendedColors

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.MediumLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(extendedColors.success)
                )
                Text(
                    agent.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    Text(
                        "ID: ",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        agent.agentId,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(agent.agentId))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "Copy ID",
                            tint = extendedColors.success,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Button(
                onClick = { onPlay(agent.agentId) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extendedColors.success,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    Icons.Outlined.RecordVoiceOver,
                    contentDescription = null
                )
                Spacer(Modifier.width(Spacing.Small))
                Text(
                    "Chạy Agent",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ConfigurationSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.extendedColors.success,
                modifier = Modifier.size(20.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ConfigRow(label: String, value: String?) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            value ?: "—",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
