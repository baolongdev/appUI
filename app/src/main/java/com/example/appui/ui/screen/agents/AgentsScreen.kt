@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.appui.ui.screen.agents

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.appui.data.remote.elevenlabs.models.AgentSummary
import com.example.appui.data.remote.elevenlabs.models.GetAgentDetailResponse
import com.example.appui.ui.theme.extendedColors
import java.time.Instant
import java.time.ZoneId

@Composable
fun AgentsScreen(
    vm: AgentsViewModel = hiltViewModel(),
    onPlayAgent: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val ui by vm.ui.collectAsState()
    var search by remember { mutableStateOf("") }

    LaunchedEffect(search) { vm.onSearchChange(search) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Modern Header với gradient
        Surface(
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.SmartToy,
                                    null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Column {
                            Text(
                                "Agents Management",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.extendedColors.success)
                                    )
                                    Text(
                                        "${ui.agents.size} agents",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { vm.refresh(search.ifBlank { null }) }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Modern Search Bar
                TextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Tìm kiếm agents...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = if (search.isNotEmpty()) {
                        {
                            IconButton(onClick = { search = "" }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when {
                ui.isLoading -> ModernLoadingView()
                ui.error != null -> ModernErrorView(ui.error!!)
                ui.selected == null -> ModernAgentsTable(
                    agents = ui.agents,
                    onRowClick = vm::loadDetail,
                    onPlay = onPlayAgent
                )
                else -> ModernAgentDetail(
                    data = ui.selected!!,
                    onBack = vm::clearDetail,
                    onPlay = { onPlayAgent(ui.selected!!.agentId) }
                )
            }
        }
    }
}

// ============= MODERN LOADING VIEW =============
@Composable
private fun ModernLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Đang tải agents...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============= MODERN ERROR VIEW =============
@Composable
private fun ModernErrorView(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                "Đã xảy ra lỗi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// ============= MODERN AGENTS TABLE =============
@Composable
private fun ModernAgentsTable(
    agents: List<AgentSummary>,
    onRowClick: (String) -> Unit,
    onPlay: (String) -> Unit
) {
    if (agents.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.SearchOff,
                            null,
                            modifier = Modifier.size(50.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    "Không tìm thấy agents",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Thử tìm với từ khóa khác",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(agents, key = { it.agentId }) { agent ->
            ModernAgentCard(
                agent = agent,
                onClick = { onRowClick(agent.agentId) },
                onPlay = { onPlay(agent.agentId) }
            )
        }
    }
}

// ============= MODERN AGENT CARD =============
@Composable
private fun ModernAgentCard(
    agent: AgentSummary,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    val (lastLabel, recent) = remember(agent.lastCallTimeUnixSecs) {
        lastCallLabel(agent.lastCallTimeUnixSecs)
    }
    val successColor = MaterialTheme.extendedColors.success

    ElevatedCard(
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with status
                Box {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.SmartToy,
                                null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(
                                if (recent) successColor
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .then(
                                if (recent) Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                                else Modifier
                            )
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        agent.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                agent.agentId,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        if (recent) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = successColor.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(successColor)
                                    )
                                    Text(
                                        lastLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = successColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Text(
                                lastLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = onPlay,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = successColor,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        null,
                        Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Play")
                }

                OutlinedIconButton(onClick = onClick) {
                    Icon(Icons.Default.Info, "Details")
                }
            }
        }
    }
}

// ============= MODERN AGENT DETAIL =============
@Composable
private fun ModernAgentDetail(
    data: GetAgentDetailResponse,
    onBack: () -> Unit,
    onPlay: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val successColor = MaterialTheme.extendedColors.success

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back button
        item {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null)
                Spacer(Modifier.width(8.dp))
                Text("Quay lại danh sách")
            }
        }

        // Agent header card
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.SmartToy,
                                    null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                data.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = successColor.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(successColor)
                                    )
                                    Text(
                                        "Active",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = successColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Agent ID
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Agent ID",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    data.agentId,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            FilledTonalIconButton(
                                onClick = { clipboard.setText(AnnotatedString(data.agentId)) }
                            ) {
                                Icon(Icons.Outlined.ContentCopy, "Copy")
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onPlay,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = successColor
                            )
                        ) {
                            Icon(Icons.Outlined.RecordVoiceOver, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Play Agent")
                        }
                    }
                }
            }
        }

        // Configuration sections
        item {
            ModernConfigSection(
                icon = Icons.Outlined.Audiotrack,
                title = "TTS Configuration",
                color = MaterialTheme.colorScheme.primary
            ) {
                val tts = data.conversationConfig?.tts
                ConfigGrid {
                    ConfigItem("Model", tts?.modelId)
                    ConfigItem("Voice", tts?.voiceId)
                    ConfigItem("Output Format", tts?.agentOutputAudioFormat)
                    ConfigItem("Latency", tts?.optimizeStreamingLatency)
                    ConfigItem("Stability", tts?.stability?.let { "%.2f".format(it) })
                    ConfigItem("Speed", tts?.speed?.let { "%.2f".format(it) })
                    ConfigItem("Similarity", tts?.similarityBoost?.let { "%.2f".format(it) })
                }
            }
        }

        item {
            ModernConfigSection(
                icon = Icons.Outlined.GraphicEq,
                title = "ASR Configuration",
                color = MaterialTheme.colorScheme.secondary
            ) {
                val asr = data.conversationConfig?.asr
                ConfigGrid {
                    ConfigItem("Provider", asr?.provider)
                    ConfigItem("Quality", asr?.quality)
                    ConfigItem("Input Format", asr?.userInputAudioFormat)
                }
            }
        }

        item {
            ModernConfigSection(
                icon = Icons.Outlined.Timelapse,
                title = "Turn Settings",
                color = MaterialTheme.colorScheme.tertiary
            ) {
                val turn = data.conversationConfig?.turn
                ConfigGrid {
                    ConfigItem("Mode", turn?.mode)
                    ConfigItem("Timeout", turn?.turnTimeout?.let { "%.1f s".format(it) })
                    ConfigItem("Silence End", turn?.silenceEndCallTimeout?.let { "%.1f s".format(it) })
                }
            }
        }

        item {
            ModernConfigSection(
                icon = Icons.Outlined.RecordVoiceOver,
                title = "Agent Prompt",
                color = MaterialTheme.colorScheme.primary
            ) {
                val agent = data.conversationConfig?.agent
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    agent?.firstMessage?.let {
                        ConfigDetail("First Message", it)
                    }
                    agent?.language?.let {
                        ConfigDetail("Language", it)
                    }
                }
            }
        }
    }
}

// ============= MODERN CONFIG SECTION =============
@Composable
private fun ModernConfigSection(
    icon: ImageVector,
    title: String,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = color
                        )
                    }
                }

                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Content
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    content = content
                )
            }
        }
    }
}

// ============= CONFIG GRID =============
@Composable
private fun ConfigGrid(content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

// ============= CONFIG ITEM =============
@Composable
private fun ConfigItem(label: String, value: String?) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface
        ) {
            Text(
                value ?: "-",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = if (value?.contains("_") == true) FontFamily.Monospace else FontFamily.Default,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// ============= CONFIG DETAIL =============
@Composable
private fun ConfigDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

// ============= TIME UTILS (unchanged but included for completeness) =============
private fun lastCallLabel(unixSecs: Long?): Pair<String, Boolean> {
    if (unixSecs == null || unixSecs <= 0L) return "Never" to false
    val now = Instant.now().epochSecond
    val delta = now - unixSecs
    val sevenDays = 7L * 24 * 3600
    val recent = delta in 0..sevenDays
    val label = when {
        delta < 60 -> "Just now"
        delta < 3600 -> "${delta / 60}m ago"
        delta < 86400 -> "${delta / 3600}h ago"
        delta < sevenDays -> "${delta / 86400}d ago"
        else -> {
            val dt = Instant.ofEpochSecond(unixSecs).atZone(ZoneId.systemDefault())
            dt.toLocalDate().toString()
        }
    }
    return label to recent
}
