@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.appui.ui.screen.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.appui.domain.repository.VoiceMode
import com.example.appui.domain.repository.VoiceStatus
import com.example.appui.ui.theme.*
import com.example.appui.utils.showToast

/**
 * Voice conversation screen with ElevenLabs integration
 */
@Composable
fun VoiceScreen(
    agentId: String? = null,
    viewModel: VoiceViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {} // ✅ Added back navigation
) {
    val state by viewModel.ui.collectAsState()
    val context = LocalContext.current
    val extendedColors = MaterialTheme.extendedColors

    // Microphone permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.connect(agentId)
        } else {
            context.showToast("⚠️ Cần cấp quyền microphone để sử dụng voice chat")
        }
    }

    // Helper to check permission and connect
    fun connectWithPermissionCheck() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.connect(agentId)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Auto-connect when agentId is provided
    LaunchedEffect(agentId) {
        if (agentId != null && state.status == VoiceStatus.DISCONNECTED) {
            connectWithPermissionCheck()
        }
    }

    // Auto-disconnect on screen exit
    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnect()
        }
    }

    Scaffold(
        topBar = {
            VoiceTopBar(
                agentId = agentId,
                status = state.status,
                onNavigateBack = onNavigateBack // ✅ Pass callback
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.MediumLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.3f))

            // Voice Activity Indicator
            VoiceActivityIndicator(
                status = state.status,
                mode = state.mode,
                vadScore = state.vad
            )

            Spacer(Modifier.height(Spacing.Large))

            // Status Card
            VoiceStatusCard(
                status = state.status,
                mode = state.mode,
                vadScore = state.vad
            )

            // Transcript Display
            TranscriptCard(
                transcript = state.transcript
            )

            Spacer(Modifier.weight(1f))

            // Control Buttons
            VoiceControls(
                status = state.status,
                micMuted = state.micMuted,
                onConnect = ::connectWithPermissionCheck,
                onDisconnect = viewModel::disconnect,
                onToggleMic = viewModel::toggleMic
            )

            // Text Input
            TextMessageInput(
                enabled = state.status == VoiceStatus.CONNECTED,
                onSendMessage = viewModel::sendText
            )

            Spacer(Modifier.height(Spacing.Medium))
        }
    }
}

/**
 * Top app bar with back button
 */
@Composable
private fun VoiceTopBar(
    agentId: String?,
    status: VoiceStatus,
    onNavigateBack: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Voice Chat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (agentId != null) {
                    Text(
                        text = "Agent: ${agentId.take(12)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        // ✅ Back button
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        // ✅ Status indicator
        actions = {
            StatusIndicator(status = status)
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Status indicator badge in top bar
 */
@Composable
private fun StatusIndicator(status: VoiceStatus) {
    val extendedColors = MaterialTheme.extendedColors

    val color = when (status) {
        VoiceStatus.CONNECTED -> extendedColors.success
        VoiceStatus.CONNECTING -> extendedColors.warning
        VoiceStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = when (status) {
                VoiceStatus.CONNECTED -> "Connected"
                VoiceStatus.CONNECTING -> "Connecting"
                VoiceStatus.DISCONNECTED -> "Offline"
            },
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Animated voice activity indicator
 */
@Composable
private fun VoiceActivityIndicator(
    status: VoiceStatus,
    mode: VoiceMode,
    vadScore: Float
) {
    val extendedColors = MaterialTheme.extendedColors

    // Animate scale based on VAD
    val scale by animateFloatAsState(
        targetValue = 1f + (vadScore * 0.3f),
        animationSpec = tween(durationMillis = 100),
        label = "vad-scale"
    )

    // Determine color based on status and mode
    val indicatorColor = when {
        status == VoiceStatus.CONNECTED && mode == VoiceMode.SPEAKING -> extendedColors.success
        status == VoiceStatus.CONNECTED && mode == VoiceMode.LISTENING -> MaterialTheme.colorScheme.secondary
        status == VoiceStatus.CONNECTING -> extendedColors.warning
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(indicatorColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(indicatorColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (mode) {
                        VoiceMode.SPEAKING -> Icons.Filled.RecordVoiceOver
                        VoiceMode.LISTENING -> Icons.Filled.Hearing
                        else -> Icons.Filled.Mic
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Mode label
        Text(
            text = mode.toDisplayString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = indicatorColor
        )
    }
}

/**
 * Status information card
 */
@Composable
private fun VoiceStatusCard(
    status: VoiceStatus,
    mode: VoiceMode,
    vadScore: Float
) {
    val extendedColors = MaterialTheme.extendedColors

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = Elevation.Level2
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.MediumLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            StatusRow(
                label = "Trạng thái",
                value = status.toDisplayString(),
                icon = when (status) {
                    VoiceStatus.CONNECTED -> Icons.Filled.CheckCircle
                    VoiceStatus.CONNECTING -> Icons.Filled.Autorenew
                    VoiceStatus.DISCONNECTED -> Icons.Filled.Cancel
                },
                color = when (status) {
                    VoiceStatus.CONNECTED -> extendedColors.success
                    VoiceStatus.CONNECTING -> extendedColors.warning
                    VoiceStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.ExtraSmall),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            StatusRow(
                label = "Chế độ",
                value = mode.toDisplayString(),
                icon = when (mode) {
                    VoiceMode.SPEAKING -> Icons.Filled.RecordVoiceOver
                    VoiceMode.LISTENING -> Icons.Filled.Hearing
                    else -> Icons.Filled.Schedule
                },
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.ExtraSmall),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            StatusRow(
                label = "VAD Score",
                value = "%.2f".format(vadScore),
                icon = Icons.Filled.GraphicEq,
                color = if (vadScore > 0.5f) extendedColors.success else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Single status row with icon
 */
@Composable
private fun StatusRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(ComponentSize.IconSmall)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

/**
 * Transcript display card
 */
@Composable
private fun TranscriptCard(transcript: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.MediumLarge)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                Icon(
                    imageVector = Icons.Filled.Subtitles,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(ComponentSize.IconSmall)
                )
                Text(
                    text = "Transcript",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(Spacing.Small))

            Text(
                text = transcript.ifBlank { "Chờ bạn nói hoặc gửi tin nhắn..." },
                style = MaterialTheme.typography.bodyMedium,
                color = if (transcript.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Voice control buttons
 */
@Composable
private fun VoiceControls(
    status: VoiceStatus,
    micMuted: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleMic: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        // Connect/Disconnect Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Button(
                onClick = onConnect,
                enabled = status != VoiceStatus.CONNECTING && status != VoiceStatus.CONNECTED,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.PhoneEnabled, contentDescription = null)
                Spacer(Modifier.width(Spacing.Small))
                Text("Kết nối")
            }

            OutlinedButton(
                onClick = onDisconnect,
                enabled = status != VoiceStatus.DISCONNECTED,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Filled.PhoneDisabled, contentDescription = null)
                Spacer(Modifier.width(Spacing.Small))
                Text("Ngắt")
            }
        }

        // Mic Toggle Button
        FilledTonalButton(
            onClick = onToggleMic,
            enabled = status == VoiceStatus.CONNECTED,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (micMuted) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (micMuted) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        ) {
            Icon(
                imageVector = if (micMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = null
            )
            Spacer(Modifier.width(Spacing.Small))
            Text(if (micMuted) "Bật mic" else "Tắt mic")
        }
    }
}

/**
 * Text message input
 */
@Composable
private fun TextMessageInput(
    enabled: Boolean,
    onSendMessage: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Gửi tin nhắn văn bản...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                enabled = enabled,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            FilledIconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                enabled = enabled && text.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Gửi"
                )
            }
        }
    }
}

/**
 * Extension functions for display strings
 */
private fun VoiceStatus.toDisplayString(): String = when (this) {
    VoiceStatus.DISCONNECTED -> "Ngắt kết nối"
    VoiceStatus.CONNECTING -> "Đang kết nối..."
    VoiceStatus.CONNECTED -> "Đã kết nối"
}

private fun VoiceMode.toDisplayString(): String = when (this) {
    VoiceMode.IDLE -> "Chờ"
    VoiceMode.LISTENING -> "Đang nghe"
    VoiceMode.SPEAKING -> "Đang nói"
}
