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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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

/**
 * Modern voice conversation screen - Single page layout
 */
@Composable
fun VoiceScreen(
    agentId: String? = null,
    agentName: String? = null,
    viewModel: VoiceViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val messages by viewModel.conversationMessages.collectAsState() // ✅ Collect StateFlow
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSaveDialog by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.connect(agentId, agentName, enablePcmCapture = true)
        } else {
            context.showToast("❌ Cần quyền Mic để sử dụng Voice Chat")
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
            viewModel.connect(agentId, agentName, enablePcmCapture = true)
        } else {
            permissionsLauncher.launch(requiredPermissions)
        }
    }

    fun handleDisconnect() {
        scope.launch {
            if (viewModel.hasConversationToSave()) {
                showSaveDialog = true
            } else {
                viewModel.disconnect()
                onNavigateBack()
            }
        }
    }

    LaunchedEffect(agentId) {
        if (agentId != null && state.status == VoiceStatus.DISCONNECTED) {
            connectWithPermissionCheck()
        }
    }

    BackHandler(enabled = state.status == VoiceStatus.CONNECTED) {
        handleDisconnect()
    }

    // ✅ FIXED: Dùng messages.size thay vì viewModel.conversationMessages.size
    if (showSaveDialog) {
        SaveConversationDialog(
            messageCount = messages.size,
            agentName = state.agentName ?: agentName, // ✅ Thêm dòng này
            onDismiss = { showSaveDialog = false },
            onSave = { title ->
                scope.launch {
                    val saved = viewModel.saveConversation(title)
                    if (saved) {
                        context.showToast("✅ Đã lưu cuộc trò chuyện")
                    }
                    viewModel.disconnect()
                    onNavigateBack()
                }
            },
            onDiscard = {
                scope.launch {
                    viewModel.disconnect()
                    onNavigateBack()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            ModernVoiceTopBar(
                agentId = agentId,
                agentName = state.agentName ?: agentName,
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
        when (state.status) {
            VoiceStatus.DISCONNECTED -> DisconnectedView(
                onConnect = { connectWithPermissionCheck() },
                modifier = Modifier.padding(paddingValues)
            )
            VoiceStatus.CONNECTING -> ConnectingView(
                modifier = Modifier.padding(paddingValues)
            )
            VoiceStatus.CONNECTED -> ConnectedSinglePageView(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

// ==================== TOP BAR ====================

@Composable
private fun ModernVoiceTopBar(
    agentId: String?,
    agentName: String?,
    status: VoiceStatus,
    onNavigateBack: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Voice Chat",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                agentName?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

// ==================== DISCONNECTED VIEW ====================

@Composable
private fun DisconnectedView(
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PhoneDisabled,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(Spacing.Medium))

        Text(
            "Not Connected",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(Spacing.Small))

        Text(
            "Tap the button below to start voice chat",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.Large))

        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Phone, contentDescription = null)
            Spacer(modifier = Modifier.width(Spacing.Small))
            Text("Connect", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ==================== CONNECTING VIEW ====================

@Composable
private fun ConnectingView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(60.dp),
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(Spacing.Large))

        Text(
            "Connecting...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(Spacing.Small))

        Text(
            "Please wait while we establish connection",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== CONNECTED SINGLE PAGE VIEW ====================

@Composable
private fun ConnectedSinglePageView(
    state: VoiceUiState,
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        AudioVisualizationSection(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
        )

        // ✅ Truyền viewModel thay vì messages list
        ConversationSection(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        ControlsSection(
            state = state,
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


// ==================== 1️⃣ AUDIO VISUALIZATION SECTION ====================

@Composable
private fun AudioVisualizationSection(
    state: VoiceUiState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            // Mic Waveform
            CompactWaveformCard(
                title = "🎤 You",
                pcmData = state.micPcmData,
                color = MaterialTheme.colorScheme.primary,
                isMuted = state.isEffectiveMicMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Agent Waveform
            CompactWaveformCard(
                title = "🤖 Agent",
                pcmData = state.playbackPcmData,
                color = MaterialTheme.colorScheme.secondary,
                isMuted = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // VAD Score Bar
            CompactVadBar(vad = state.vad)
        }
    }
}

@Composable
private fun CompactWaveformCard(
    title: String,
    pcmData: PcmData?,
    color: Color,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isMuted) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.extendedColors.warning.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "MUTED",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.extendedColors.warning,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.small
                    )
            ) {
                if (pcmData != null && !isMuted) {
                    WaveformVisualization(pcmData = pcmData, color = color)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isMuted) "🔇" else "○",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactVadBar(vad: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "🎙️",
            style = MaterialTheme.typography.bodyMedium
        )
        LinearProgressIndicator(
            progress = { vad },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(MaterialTheme.shapes.small),
        )
        Text(
            "${(vad * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )
    }
}

@Composable
private fun WaveformVisualization(
    pcmData: PcmData,
    color: Color
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 100),
        label = "waveform_alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val samples = pcmData.samples
        if (samples.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height

        // ✅ Fixed number of bars
        val numBars = 60
        val barWidth = (width / numBars) * 0.7f
        val barSpacing = (width / numBars) * 0.3f
        val step = maxOf(1, samples.size / numBars)

        for (i in 0 until numBars) {
            val sampleIndex = i * step
            if (sampleIndex >= samples.size) break

            // Calculate RMS for this bar
            var sum = 0f
            val windowSize = step.coerceAtMost(20)
            for (j in 0 until windowSize) {
                val idx = sampleIndex + j
                if (idx < samples.size) {
                    val sample = samples[idx].toFloat()
                    sum += sample * sample
                }
            }
            val rms = kotlin.math.sqrt(sum / windowSize)

            // Normalize to 0-1
            val normalizedAmplitude = (rms / Short.MAX_VALUE).coerceIn(0f, 1f)

            // Bar height (minimum 5% for visual feedback)
            val barHeight = height * normalizedAmplitude.coerceAtLeast(0.05f)

            // X position
            val x = i * (barWidth + barSpacing)

            // Draw bar (rounded)
            drawRoundRect(
                color = color.copy(alpha = animatedAlpha),
                topLeft = Offset(x, (height - barHeight) / 2),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}


// ==================== 2️⃣ CONVERSATION SECTION ====================

@Composable
private fun ConversationSection(
    viewModel: VoiceViewModel, // ✅ Nhận ViewModel thay vì List
    modifier: Modifier = Modifier
) {
    // ✅ Collect StateFlow
    val messages by viewModel.conversationMessages.collectAsState()
    val listState = rememberLazyListState()

    // ✅ Debug log
    LaunchedEffect(messages.size) {
        Log.d("VoiceScreen", "📊 Messages: ${messages.size}")
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        "No conversation yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        "Start talking to begin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                items(messages) { message ->
                    ConversationMessageBubble(message)
                }
            }
        }
    }
}


@Composable
private fun ConversationMessageBubble(message: ConversationMessage) {
    val isUser = message.speaker == Speaker.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ),
            shape = if (isUser) {
                androidx.compose.foundation.shape.RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 4.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                )
            } else {
                androidx.compose.foundation.shape.RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                )
            }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (isUser) "🧑" else "🤖",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        if (isUser) "You" else "Agent",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }
        }
    }
}

// ==================== 3️⃣ CONTROLS SECTION ====================

@Composable
private fun ControlsSection(
    state: VoiceUiState,
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    var showModeDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    if (showModeDialog) {
        ConversationModeDialog(
            currentMode = state.conversationMode,
            onDismiss = { showModeDialog = false },
            onModeSelected = {
                viewModel.setConversationMode(it)
                showModeDialog = false
            }
        )
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            // Row 1: Mic & Mode buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                // Mic button
                FilledTonalButton(
                    onClick = { viewModel.toggleMic() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (state.micMuted) {
                            MaterialTheme.extendedColors.warning
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Icon(
                        if (state.micMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (state.micMuted) "Unmute" else "Mute")
                }

                // Mode button
                OutlinedButton(
                    onClick = { showModeDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        when (state.conversationMode) {
                            ConversationControlMode.FULL_DUPLEX -> "Duplex"
                            ConversationControlMode.PTT -> "PTT"
                        },
                        maxLines = 1
                    )
                }
            }

            // Row 2: Text input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type message...") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendText(textInput)
                            textInput = ""
                        }
                    },
                    enabled = textInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

// ==================== MODE DIALOG ====================

@Composable
private fun ConversationModeDialog(
    currentMode: ConversationControlMode,
    onDismiss: () -> Unit,
    onModeSelected: (ConversationControlMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Settings, contentDescription = null)
        },
        title = {
            Text("Conversation Mode")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ConversationControlMode.entries.forEach { mode ->
                    Card(
                        onClick = { onModeSelected(mode) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentMode == mode) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentMode == mode,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    when (mode) {
                                        ConversationControlMode.FULL_DUPLEX -> "Full Duplex"
                                        ConversationControlMode.PTT -> "Push-to-Talk"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    when (mode) {
                                        ConversationControlMode.FULL_DUPLEX -> "Both can talk anytime"
                                        ConversationControlMode.PTT -> "Mic mutes when agent speaks"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
