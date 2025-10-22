@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.appui.ui.screen.voice

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.appui.core.audio.capture.PcmData
import com.example.appui.data.model.ConversationMessage
import com.example.appui.data.model.Speaker
import com.example.appui.domain.repository.VoiceMode
import com.example.appui.domain.repository.VoiceStatus
import com.example.appui.ui.theme.Spacing
import com.example.appui.ui.theme.extendedColors
import com.example.appui.utils.showToast
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Voice Screen - Modern & Unified Design
 */
@Composable
fun VoiceScreen(
    agentId: String? = null,
    viewModel: VoiceViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.connect(agentId, enablePcmCapture = true)
        } else {
            val denied = permissions.filterValues { !it }.keys
            context.showToast("⚠️ Cần quyền: ${denied.joinToString()}")
        }
    }

    fun connectWithPermissionCheck() {
        val requiredPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

        val allGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            viewModel.connect(agentId, enablePcmCapture = true)
        } else {
            permissionsLauncher.launch(requiredPermissions)
        }
    }

    fun handleDisconnect() {
        if (viewModel.hasConversationToSave()) {
            showSaveDialog = true
        } else {
            viewModel.disconnect()
            onNavigateBack()
        }
    }

    LaunchedEffect(agentId) {
        if (agentId != null && state.status == VoiceStatus.DISCONNECTED) {
            connectWithPermissionCheck()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("VoiceScreen", "Screen disposing")
            viewModel.disconnect()
        }
    }

    BackHandler(enabled = state.status == VoiceStatus.CONNECTED) {
        handleDisconnect()
    }

    if (showSaveDialog) {
        SaveConversationDialog(
            messageCount = viewModel.conversationMessages.size,
            onDismiss = { showSaveDialog = false },
            onSave = { title ->
                scope.launch {
                    val saved = viewModel.saveConversation(title)
                    context.showToast(if (saved) "✅ Đã lưu" else "❌ Lỗi")
                    showSaveDialog = false
                    viewModel.disconnect()
                    onNavigateBack()
                }
            },
            onDiscard = {
                showSaveDialog = false
                viewModel.disconnect()
                onNavigateBack()
            }
        )
    }

    Scaffold(
        topBar = {
            ModernVoiceTopBar(
                agentId = agentId,
                status = state.status,
                onNavigateBack = {
                    if (state.status == VoiceStatus.CONNECTED) {
                        handleDisconnect()
                    } else {
                        onNavigateBack()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Control",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Waveform",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> ControlTab(
                    state = state,
                    messages = viewModel.conversationMessages,
                    isMicOn = !state.micMuted,
                    onConnect = ::connectWithPermissionCheck,
                    onDisconnect = ::handleDisconnect,
                    onToggleMic = viewModel::toggleMic,
                    onSetConversationMode = viewModel::setConversationMode,
                    onSendText = viewModel::sendText
                )
                1 -> WaveformTab(state = state)
            }
        }
    }
}

@Composable
private fun ModernVoiceTopBar(
    agentId: String?,
    status: VoiceStatus,
    onNavigateBack: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Voice Chat",
                    fontWeight = FontWeight.Bold
                )
                agentId?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
        },
        actions = {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (status) {
                    VoiceStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                    VoiceStatus.CONNECTING -> MaterialTheme.colorScheme.secondaryContainer
                    VoiceStatus.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (status) {
                                    VoiceStatus.CONNECTED -> MaterialTheme.extendedColors.success
                                    VoiceStatus.CONNECTING -> MaterialTheme.extendedColors.warning
                                    VoiceStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                    )
                    Text(
                        when (status) {
                            VoiceStatus.CONNECTED -> "Connected"
                            VoiceStatus.CONNECTING -> "Connecting..."
                            VoiceStatus.DISCONNECTED -> "Disconnected"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun ControlTab(
    state: VoiceUiState,
    messages: List<ConversationMessage>,
    isMicOn: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleMic: () -> Unit,
    onSetConversationMode: (ConversationControlMode) -> Unit,
    onSendText: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        ModernConnectionCard(
            status = state.status,
            onConnect = onConnect,
            onDisconnect = onDisconnect
        )

        if (state.status == VoiceStatus.CONNECTED) {
            ModernModeCard(
                currentMode = state.conversationMode,
                onModeChange = onSetConversationMode
            )

            ModernMicCard(
                isMicOn = isMicOn,
                onToggleMic = onToggleMic
            )

            ModernConversationCard(
                messages = messages
            )

            ModernTextInputCard(
                onSendText = onSendText
            )
        }
    }
}

@Composable
private fun ModernConnectionCard(
    status: VoiceStatus,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = when (status) {
                    VoiceStatus.CONNECTED -> MaterialTheme.extendedColors.success.copy(alpha = 0.2f)
                    VoiceStatus.CONNECTING -> MaterialTheme.extendedColors.warning.copy(alpha = 0.2f)
                    VoiceStatus.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when (status) {
                            VoiceStatus.CONNECTED -> Icons.Default.Link
                            VoiceStatus.CONNECTING -> Icons.Default.Sync
                            VoiceStatus.DISCONNECTED -> Icons.Default.LinkOff
                        },
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = when (status) {
                            VoiceStatus.CONNECTED -> MaterialTheme.extendedColors.success
                            VoiceStatus.CONNECTING -> MaterialTheme.extendedColors.warning
                            VoiceStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Text(
                when (status) {
                    VoiceStatus.CONNECTED -> "Connected"
                    VoiceStatus.CONNECTING -> "Connecting..."
                    VoiceStatus.DISCONNECTED -> "Not Connected"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            when (status) {
                VoiceStatus.DISCONNECTED -> {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Link, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Connect")
                    }
                }
                VoiceStatus.CONNECTING -> {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                }
                VoiceStatus.CONNECTED -> {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.LinkOff, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Disconnect")
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernModeCard(
    currentMode: ConversationControlMode,
    onModeChange: (ConversationControlMode) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Conversation Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConversationControlMode.entries.forEach { mode ->
                FilterChip(
                        selected = currentMode == mode,
                        onClick = { onModeChange(mode) },
                        label = { Text(mode.name) },
                        leadingIcon = if (currentMode == mode) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernMicCard(
    isMicOn: Boolean,
    onToggleMic: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggleMic
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isMicOn)
                        MaterialTheme.extendedColors.success.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                            null,
                            tint = if (isMicOn)
                                MaterialTheme.extendedColors.success
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column {
                    Text(
                        "Microphone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isMicOn) "Active" else "Muted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = isMicOn,
                onCheckedChange = { onToggleMic() }
            )
        }
    }
}

@Composable
private fun ModernConversationCard(
    messages: List<ConversationMessage> // ✅ Pass full objects to get speaker info
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Conversation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "${messages.size} messages",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (messages.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "No messages yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    messages.takeLast(5).forEach { message ->
                        MessageBubble(
                            message = message.text,
                            speaker = message.speaker,
                            timestamp = message.timestamp
                        )
                    }

                    if (messages.size > 5) {
                        Text(
                            "... and ${messages.size - 5} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: String,
    speaker: Speaker,
    timestamp: Long
) {
    val isUser = speaker == Speaker.USER

    // Alignment: User (right), Agent (left)
    val arrangement = if (isUser) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            color = if (isUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Speaker name with icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isUser)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isUser)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }

                    Text(
                        if (isUser) "You" else "Agent",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Timestamp
                    Text(
                        formatTimestamp(timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Message text
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModernTextInputCard(
    onSendText: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Send Text Message",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )

                FilledTonalIconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSendText(text)
                            text = ""
                        }
                    },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Send, "Send")
                }
            }
        }
    }
}

@Composable
private fun WaveformTab(state: VoiceUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        ModernWaveformCard(
            title = "Input Audio",
            icon = Icons.Default.Mic,
            pcmData = state.micPcmData,
            color = MaterialTheme.colorScheme.primary
        )

        ModernWaveformCard(
            title = "Output Audio",
            icon = Icons.Default.Speaker,
            pcmData = state.playbackPcmData,
            color = MaterialTheme.colorScheme.secondary
        )

        if (state.micPcmData != null || state.playbackPcmData != null) {
            ModernStatsCard(
                inputPcm = state.micPcmData,
                outputPcm = state.playbackPcmData
            )
        }
    }
}

@Composable
private fun ModernWaveformCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    pcmData: PcmData?,
    color: Color
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
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
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                if (pcmData != null) {
                    WaveformVisualization(pcmData = pcmData, color = color)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            pcmData?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip("${it.samples.size} samples")
                    InfoChip("${it.sampleRate} Hz")
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ModernStatsCard(
    inputPcm: PcmData?,
    outputPcm: PcmData?
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Audio Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                inputPcm?.let { pcm ->
                    val peak = pcm.samples.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0 // ✅ FIXED
                    val peakPercent = (peak.toFloat() / Short.MAX_VALUE) * 100f

                    StatItem(
                        label = "Input Peak",
                        value = String.format("%.1f%%", peakPercent),
                        modifier = Modifier.weight(1f)
                    )
                }

                outputPcm?.let { pcm ->
                    val peak = pcm.samples.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0 // ✅ FIXED
                    val peakPercent = (peak.toFloat() / Short.MAX_VALUE) * 100f

                    StatItem(
                        label = "Output Peak",
                        value = String.format("%.1f%%", peakPercent),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WaveformVisualization(
    pcmData: PcmData,
    color: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val samples = pcmData.samples
        if (samples.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val step = maxOf(1, samples.size / width.toInt())

        for (i in 0 until minOf(width.toInt(), samples.size / step)) {
            val sampleIndex = i * step
            if (sampleIndex < samples.size) {
                val amplitude = samples[sampleIndex] * centerY
                drawLine(
                    color = color,
                    start = Offset(i.toFloat(), centerY - amplitude),
                    end = Offset(i.toFloat(), centerY + amplitude),
                    strokeWidth = 2f
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        else -> {
            val date = java.util.Date(timestamp)
            val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            format.format(date)
        }
    }
}