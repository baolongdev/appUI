@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.appui.ui.screen.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.appui.data.remote.elevenlabs.AgentSummaryResponseModel
import com.example.appui.data.remote.elevenlabs.GetAgentResponseModel
import java.time.Instant
import java.time.ZoneId

/* ---------- Palette ---------- */
private val Green = Color(0xFF22C55E)
private val AppBg = Color.Black
private val CardBg = Color(0xFF111111)
private val PanelBg = Color(0xFF181818)
private val OnDark = Color.White

/* ---------------- Screen ---------------- */

@Composable
fun AgentsScreen(
    vm: AgentsViewModel = hiltViewModel(),
    onPlayAgent: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {} // ✅ Added back navigation
) {
    val ui by vm.ui.collectAsState()
    var search by remember { mutableStateOf("") }

    LaunchedEffect(search) { vm.onSearchChange(search) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Agents Management",
                        fontWeight = FontWeight.SemiBold,
                        color = OnDark
                    )
                },
                // ✅ Back button
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = OnDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refresh(search.ifBlank { null }) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = OnDark)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBg
                )
            )
        },
        containerColor = AppBg,
        contentColor = OnDark
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
        ) {
            /* ---------- Header (Search) ---------- */
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = CardBg,
                    contentColor = OnDark
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Voice Agents",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnDark
                        )
                        Text(
                            "Manage and monitor your conversational AI agents",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnDark.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search agents…", color = OnDark.copy(alpha = 0.6f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnDark,
                            unfocusedTextColor = OnDark,
                            cursorColor = Green,
                            focusedBorderColor = Green,
                            unfocusedBorderColor = OnDark.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.widthIn(min = 220.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            /* ---------- Table / Detail ---------- */
            ElevatedCard(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = CardBg,
                    contentColor = OnDark
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        if (ui.selected == null) "All Agents" else "Agent Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OnDark
                    )
                    Spacer(Modifier.height(8.dp))

                    when {
                        ui.isLoading -> LoadingView()
                        ui.error != null -> ErrorView(ui.error!!)
                        ui.selected == null -> AgentsTable(
                            agents = ui.agents.sortedByDescending { it.lastCallTimeUnixSecs ?: 0L },
                            onRowClick = vm::loadDetail,
                            onPlay = onPlayAgent
                        )
                        else -> AgentDetail(
                            data = ui.selected!!,
                            onBack = vm::clearDetail,
                            onPlay = { onPlayAgent(ui.selected!!.agentId) }
                        )
                    }
                }
            }
        }
    }
}

/* ---------------- Loading & Error Views ---------------- */

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Green)
            Spacer(Modifier.height(12.dp))
            Text("Loading agents...", color = OnDark.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun ErrorView(error: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Error: $error", color = Color.Red)
        }
    }
}

/* ---------------- List & Row ---------------- */

@Composable
private fun AgentsTable(
    agents: List<AgentSummaryResponseModel>,
    onRowClick: (String) -> Unit,
    onPlay: (String) -> Unit
) {
    if (agents.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.SearchOff,
                    contentDescription = null,
                    tint = OnDark.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("No agents found", color = OnDark.copy(alpha = 0.7f))
            }
        }
        return
    }

    // Header
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Agent", Modifier.weight(0.50f), fontWeight = FontWeight.Medium, color = OnDark)
        Text("Last Call", Modifier.weight(0.30f), fontWeight = FontWeight.Medium, color = OnDark)
        Text("Actions", Modifier.weight(0.20f), fontWeight = FontWeight.Medium, color = OnDark)
    }
    HorizontalDivider(color = OnDark.copy(alpha = 0.15f))

    LazyColumn {
        items(agents, key = { it.agentId }) { a ->
            AgentRow(a, onRowClick = onRowClick, onPlay = onPlay)
            HorizontalDivider(color = OnDark.copy(alpha = 0.15f))
        }
    }
}

@Composable
private fun AgentRow(
    a: AgentSummaryResponseModel,
    onRowClick: (String) -> Unit,
    onPlay: (String) -> Unit
) {
    val (lastLabel, recent) = remember(a.lastCallTimeUnixSecs) {
        lastCallLabel(a.lastCallTimeUnixSecs)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRowClick(a.agentId) }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status dot + name
        Row(
            modifier = Modifier.weight(0.50f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(active = recent)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    a.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnDark
                )
                Text(
                    a.agentId,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnDark.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            lastLabel,
            modifier = Modifier.weight(0.30f),
            style = MaterialTheme.typography.bodyMedium,
            color = OnDark.copy(alpha = 0.8f)
        )

        Row(
            modifier = Modifier.weight(0.20f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledTonalIconButton(
                onClick = { onPlay(a.agentId) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Green,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }

            OutlinedIconButton(
                onClick = { onRowClick(a.agentId) },
                colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = OnDark)
            ) {
                Icon(Icons.Default.Info, contentDescription = "Details")
            }
        }
    }
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (active) Green else Color(0xFF2A2A2A))
    )
}

/* ---------------- Agent Detail ---------------- */

@Composable
fun AgentDetail(
    data: GetAgentResponseModel,
    onBack: () -> Unit,
    onPlay: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize()) {
        // Back button
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Green)
            Spacer(Modifier.width(4.dp))
            Text("Back to list", color = Green)
        }

        // Agent header card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = CardBg)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusDot(active = true)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            data.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnDark
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Green,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = Green
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(data.agentId)) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OnDark)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Copy ID")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play Agent")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Agent ID display
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PanelBg,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Agent ID:", fontWeight = FontWeight.SemiBold, color = OnDark)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            data.agentId,
                            color = OnDark.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Configuration details
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.elevatedCardColors(containerColor = CardBg)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SectionHeader(icon = Icons.Outlined.Audiotrack, title = "TTS Configuration")
                    Surface(color = PanelBg, shape = MaterialTheme.shapes.medium) {
                        val tts = data.conversationConfig.tts
                        InfoGrid {
                            InfoRow("Model", tts?.modelId)
                            InfoRow("Voice", tts?.voiceId)
                            InfoRow("Output Format", tts?.agentOutputAudioFormat)
                            InfoRow("Latency", tts?.optimizeStreamingLatency)
                            InfoRow("Stability", tts?.stability?.let { "%.2f".format(it) })
                            InfoRow("Speed", tts?.speed?.let { "%.2f".format(it) })
                            InfoRow("Similarity Boost", tts?.similarityBoost?.let { "%.2f".format(it) })
                        }
                    }
                }

                item {
                    SectionHeader(icon = Icons.Outlined.GraphicEq, title = "ASR Configuration")
                    Surface(color = PanelBg, shape = MaterialTheme.shapes.medium) {
                        val asr = data.conversationConfig.asr
                        InfoGrid {
                            InfoRow("Provider", asr?.provider)
                            InfoRow("Quality", asr?.quality)
                            InfoRow("Input Format", asr?.userInputAudioFormat)
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            SectionHeader(icon = Icons.Outlined.Timelapse, title = "Turn Settings")
                            Surface(color = PanelBg, shape = MaterialTheme.shapes.medium) {
                                val turn = data.conversationConfig.turn
                                InfoGrid {
                                    InfoRow("Mode", turn?.mode)
                                    InfoRow("Timeout (s)", turn?.turnTimeout?.let { "%.1f".format(it) })
                                    InfoRow("End Silence (s)", turn?.silenceEndCallTimeout?.let { "%.1f".format(it) })
                                }
                            }
                        }
                    }
                }

                item {
                    SectionHeader(icon = Icons.Outlined.RecordVoiceOver, title = "Agent Prompt")
                    Surface(color = PanelBg, shape = MaterialTheme.shapes.medium) {
                        val agent = data.conversationConfig.agent
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KeyValue("First Message", agent?.firstMessage ?: "-")
                            KeyValue("Language", agent?.language ?: "-")
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- UI Helpers ---------------- */

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Green, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = OnDark,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InfoGrid(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            modifier = Modifier
                .widthIn(min = 140.dp)
                .weight(0.4f),
            color = OnDark.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            value ?: "-",
            modifier = Modifier.weight(0.6f),
            color = OnDark,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun KeyValue(label: String, value: String) {
    Column {
        Text(
            label,
            color = OnDark.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = OnDark,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/* ---------------- Time Utils ---------------- */

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
