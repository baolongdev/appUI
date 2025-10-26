@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.appui.ui.screen.aiface

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import app.rive.runtime.kotlin.core.Fit
import com.example.appui.R
import com.example.appui.core.audio.capture.PcmData
import com.example.appui.data.model.ConversationMessage
import com.example.appui.data.model.Speaker
import com.example.appui.domain.repository.VoiceStatus
import com.example.appui.ui.components.RiveAnimation
import com.example.appui.ui.screen.voice.ConversationControlMode
import com.example.appui.ui.screen.voice.VoiceViewModel
import com.example.appui.ui.theme.extendedColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AIFaceScreen(
    agentId: String? = null,
    agentName: String = "AI Assistant",
    viewModel: VoiceViewModel = hiltViewModel(),
    onClose: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.conversationMessages.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showBottomSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.connect(agentId, agentName, enablePcmCapture = true)
        } else {
            Toast.makeText(context, "❌ Cần quyền Microphone để sử dụng", Toast.LENGTH_SHORT).show()
            onClose()
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

    fun handleClose() {
        scope.launch {
            if (viewModel.hasConversationToSave()) {
                showSaveDialog = true
            } else {
                viewModel.disconnect()
                onClose()
            }
        }
    }

    LaunchedEffect(agentId) {
        if (agentId != null && uiState.status == VoiceStatus.DISCONNECTED) {
            connectWithPermissionCheck()
        }
    }

    if (showSaveDialog) {
        SaveConversationDialog(
            messageCount = messages.size,
            agentName = uiState.agentName ?: agentName,
            onDismiss = { showSaveDialog = false },
            onSave = { title ->
                scope.launch {
                    val saved = viewModel.saveConversation(title)
                    if (saved) {
                        Toast.makeText(context, "✅ Đã lưu cuộc trò chuyện", Toast.LENGTH_SHORT).show()
                    }
                    viewModel.disconnect()
                    onClose()
                }
            },
            onDiscard = {
                scope.launch {
                    viewModel.disconnect()
                    onClose()
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            uiState.agentName ?: agentName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            when (uiState.mode) {
                                com.example.appui.domain.repository.VoiceMode.SPEAKING -> "Speaking..."
                                com.example.appui.domain.repository.VoiceMode.LISTENING -> "Listening..."
                                else -> when (uiState.status) {
                                    VoiceStatus.CONNECTED -> "Connected"
                                    VoiceStatus.CONNECTING -> "Connecting..."
                                    VoiceStatus.DISCONNECTED -> "Disconnected"
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (uiState.mode) {
                                com.example.appui.domain.repository.VoiceMode.SPEAKING -> MaterialTheme.extendedColors.success
                                com.example.appui.domain.repository.VoiceMode.LISTENING -> MaterialTheme.extendedColors.info
                                else -> Color.Gray
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { handleClose() }) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                },
                // ✅ 2 NÚT COMPACT + NÚT DETAILS
                actions = {
                    // ✅ MUTE BUTTON
                    AnimatedVisibility(
                        visible = uiState.status == VoiceStatus.CONNECTED,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        CompactMuteButton(
                            isMuted = uiState.micMuted,
                            onClick = { viewModel.toggleMic() }
                        )
                    }

                    // ✅ MODE SWITCH BUTTON
                    AnimatedVisibility(
                        visible = uiState.status == VoiceStatus.CONNECTED,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        CompactModeSwitchButton(
                            currentMode = uiState.conversationMode,
                            onClick = {
                                val newMode = when (uiState.conversationMode) {
                                    ConversationControlMode.FULL_DUPLEX -> ConversationControlMode.PTT
                                    ConversationControlMode.PTT -> ConversationControlMode.FULL_DUPLEX
                                }
                                viewModel.setConversationMode(newMode)
                            }
                        )
                    }

                    // ✅ DETAILS BUTTON
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(Icons.Default.ExpandLess, "Details", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                AIFaceWithRive(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    viewModel = viewModel
                )

                TypingTextDisplay(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF0A0A0A),
            dragHandle = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color.Gray
                    ) {}
                }
            }
        ) {
            BottomSheetContent(
                viewModel = viewModel,
                state = uiState,
                onClose = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                }
            )
        }
    }
}

// ==================== COMPACT BUTTONS (TopBar) ====================

@Composable
private fun CompactMuteButton(
    isMuted: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.extendedColors.success,
        animationSpec = tween(300),
        label = "compact_mute_bg"
    )

    val scale by animateFloatAsState(
        targetValue = if (isMuted) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "compact_mute_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale),
            shape = CircleShape,
            color = backgroundColor.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = backgroundColor
                )
            }
        }

        Text(
            text = if (isMuted) "Off" else "On",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = backgroundColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CompactModeSwitchButton(
    currentMode: ConversationControlMode,
    onClick: () -> Unit
) {
    val isFullDuplex = currentMode == ConversationControlMode.FULL_DUPLEX
    val backgroundColor = if (isFullDuplex) MaterialTheme.extendedColors.info else Color(0xFF6B4EFF)

    val rotation by animateFloatAsState(
        targetValue = if (isFullDuplex) 0f else 180f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "compact_mode_rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = backgroundColor.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isFullDuplex) Icons.Default.RecordVoiceOver else Icons.Default.TouchApp,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer(rotationZ = rotation),
                    tint = backgroundColor
                )
            }
        }

        Text(
            text = if (isFullDuplex) "FD" else "PTT",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = backgroundColor,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==================== AI FACE WITH RIVE ====================

@Composable
private fun AIFaceWithRive(
    modifier: Modifier = Modifier,
    viewModel: VoiceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val isUserSpeaking = uiState.mode == com.example.appui.domain.repository.VoiceMode.LISTENING
    var showListeningView by remember { mutableStateOf(false) }

    LaunchedEffect(isUserSpeaking) {
        if (isUserSpeaking) {
            delay(1000)
            if (uiState.mode == com.example.appui.domain.repository.VoiceMode.LISTENING) {
                showListeningView = true
            }
        } else {
            showListeningView = false
        }
    }

    val mouthScale by remember {
        derivedStateOf {
            calculateMouthScale(
                playbackPcm = uiState.playbackPcmData,
                isSpeaking = uiState.mode == com.example.appui.domain.repository.VoiceMode.SPEAKING
            )
        }
    }

    val animatedMouthScale by animateFloatAsState(
        targetValue = mouthScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mouth_scale"
    )

    Box(
        modifier = modifier
            .background(Color.Black)
            .padding(top = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = !showListeningView,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(500)
            ),
            exit = fadeOut(animationSpec = tween(500)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(500)
            )
        ) {
            AIFaceContent(animatedMouthScale)
        }

        AnimatedVisibility(
            visible = showListeningView,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(500)
            ),
            exit = fadeOut(animationSpec = tween(500)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(500)
            )
        ) {
            UserWaveformView(uiState.micPcmData)
        }
    }
}

@Composable
private fun AIFaceContent(mouthScale: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 120.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.92f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                ) {
                    RiveAnimation(
                        modifier = Modifier.fillMaxSize(),
                        resId = R.raw.eye_rig_example,
                        fit = Fit.CONTAIN,
                        autoplay = true
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .graphicsLayer(scaleX = -1f)
                ) {
                    RiveAnimation(
                        modifier = Modifier.fillMaxSize(),
                        resId = R.raw.eye_rig_example,
                        fit = Fit.CONTAIN,
                        autoplay = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height((-60).dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2.2f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .fillMaxHeight(1f)
                    .graphicsLayer(
                        scaleX = mouthScale * 1.5f,
                        scaleY = mouthScale * 1.5f
                    )
            ) {
                RiveAnimation(
                    modifier = Modifier.fillMaxSize(),
                    resId = R.raw.mouth,
                    fit = Fit.CONTAIN,
                    autoplay = true
                )
            }
        }
    }
}

@Composable
private fun UserWaveformView(micPcmData: PcmData?) {
    val infoColor = MaterialTheme.extendedColors.info

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(Color(0xFF0A0A0A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (micPcmData != null) {
                    CircularWaveformVisualization(
                        pcmData = micPcmData,
                        color = infoColor
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(infoColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = infoColor.copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        "Đang lắng nghe...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Light,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularWaveformVisualization(
    pcmData: PcmData,
    color: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val samples = pcmData.samples
        if (samples.isEmpty()) return@Canvas

        val centerX = size.width / 2
        val centerY = size.height / 2
        val baseRadius = minOf(centerX, centerY) * 0.6f
        val numBars = 80

        val step = maxOf(1, samples.size / numBars)

        for (i in 0 until numBars) {
            val sampleIndex = i * step
            if (sampleIndex >= samples.size) break

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
            val normalizedAmplitude = (rms / Short.MAX_VALUE).coerceIn(0f, 1f)

            val angle = (360f / numBars * i) * (Math.PI / 180f).toFloat()
            val barLength = baseRadius * 0.35f * normalizedAmplitude.coerceAtLeast(0.1f)

            val startX = centerX + kotlin.math.cos(angle) * baseRadius
            val startY = centerY + kotlin.math.sin(angle) * baseRadius
            val endX = centerX + kotlin.math.cos(angle) * (baseRadius + barLength)
            val endY = centerY + kotlin.math.sin(angle) * (baseRadius + barLength)

            val alpha = normalizedAmplitude.coerceAtLeast(0.25f) * 0.6f

            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun calculateMouthScale(
    playbackPcm: PcmData?,
    isSpeaking: Boolean
): Float {
    if (!isSpeaking || playbackPcm == null) return 1f

    val samples = playbackPcm.samples
    if (samples.isEmpty()) return 1f

    var sum = 0.0
    val sampleCount = minOf(samples.size, 256)

    for (i in 0 until sampleCount) {
        val sampleIndex = samples.size - sampleCount + i
        if (sampleIndex >= 0 && sampleIndex < samples.size) {
            val normalized = samples[sampleIndex].toFloat() / Short.MAX_VALUE
            sum += normalized * normalized
        }
    }

    val rms = kotlin.math.sqrt(sum / sampleCount).toFloat()
    val scale = 1f + (rms * 0.8f)
    return scale.coerceIn(1f, 1.3f)
}

// ==================== TYPING TEXT DISPLAY ====================

@Composable
private fun TypingTextDisplay(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.conversationMessages.collectAsState()
    val lastMessage = remember(messages) { messages.lastOrNull() }

    var isTyping by remember { mutableStateOf(false) }
    var visibleText by remember { mutableStateOf("") }

    LaunchedEffect(lastMessage?.text) {
        if (lastMessage != null) {
            isTyping = true
            val fullText = lastMessage.text
            val sentences = fullText.split(Regex("(?<=[.!?])\\s+"))

            for (sentenceIdx in sentences.indices) {
                val sentence = sentences[sentenceIdx]
                visibleText = ""

                for (i in sentence.indices) {
                    visibleText = sentence.substring(0, i + 1)
                    delay(50)
                }

                if (sentenceIdx < sentences.size - 1) {
                    delay(500)
                }
            }

            isTyping = false
        }
    }

    Surface(
        modifier = modifier,
        color = Color(0xFF1A1A1A),
        shape = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (lastMessage != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (lastMessage.speaker == Speaker.AGENT) "🤖 Agent" else "🧑 You",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (lastMessage.speaker == Speaker.AGENT) {
                            MaterialTheme.extendedColors.success
                        } else {
                            MaterialTheme.extendedColors.info
                        }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            visibleText + if (isTyping) "▌" else "",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Text(
                    "Waiting for conversation...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

// ==================== SAVE DIALOG & BOTTOM SHEET ====================
// (Giữ nguyên code cũ cho SaveConversationDialog, BottomSheetContent, v.v...)

@Composable
fun SaveConversationDialog(
    messageCount: Int,
    agentName: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
    onDiscard: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var showTitleInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.extendedColors.success.copy(alpha = 0.2f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Save,
                        null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.extendedColors.success
                    )
                }
            }
        },
        title = {
            Text(
                "Lưu cuộc hội thoại?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!agentName.isNullOrBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🤖", style = MaterialTheme.typography.titleMedium)
                            Text(
                                agentName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Số tin nhắn",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "$messageCount",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (showTitleInput) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tiêu đề (tùy chọn)") },
                        placeholder = { Text("Nhập tiêu đề...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    TextButton(
                        onClick = { showTitleInput = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Thêm tiêu đề")
                    }
                }

                Text(
                    "Cuộc hội thoại sẽ được lưu vào lịch sử",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.extendedColors.success
                )
            ) {
                Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Lưu")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Hủy")
                }

                OutlinedButton(
                    onClick = onDiscard,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bỏ qua")
                }
            }
        }
    )
}

@Composable
private fun BottomSheetContent(
    viewModel: VoiceViewModel,
    state: com.example.appui.ui.screen.voice.VoiceUiState,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ExpandMore, "Close", tint = Color.White)
            }
        }

        HorizontalDivider(color = Color(0xFF2A2A2A))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Audio Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            CompactWaveformCard(
                title = "🎤 You",
                pcmData = state.micPcmData,
                color = MaterialTheme.extendedColors.info,
                isMuted = state.isEffectiveMicMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )

            CompactWaveformCard(
                title = "🤖 Agent",
                pcmData = state.playbackPcmData,
                color = MaterialTheme.extendedColors.success,
                isMuted = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )
        }

        HorizontalDivider(color = Color(0xFF2A2A2A))

        ConversationSection(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
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
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF0A0A0A), MaterialTheme.shapes.small)
            ) {
                if (pcmData != null && !isMuted) {
                    WaveformVisualization(pcmData = pcmData, color = color)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isMuted) "🔇" else "—",
                            color = Color.Gray.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformVisualization(pcmData: PcmData, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val samples = pcmData.samples
        if (samples.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val numBars = 40
        val barWidth = (width / numBars) * 0.7f
        val barSpacing = (width / numBars) * 0.3f
        val step = maxOf(1, samples.size / numBars)

        for (i in 0 until numBars) {
            val sampleIndex = i * step
            if (sampleIndex >= samples.size) break

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
            val normalizedAmplitude = (rms / Short.MAX_VALUE).coerceIn(0f, 1f)
            val barHeight = height * normalizedAmplitude.coerceAtLeast(0.05f)

            val x = i * (barWidth + barSpacing)

            drawRoundRect(
                color = color,
                topLeft = Offset(x, (height - barHeight) / 2),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

@Composable
private fun ConversationSection(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.conversationMessages.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Conversation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "${messages.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.extendedColors.success
            )
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No messages yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
            modifier = Modifier.widthIn(max = 260.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color(0xFF2A2A2A)
                }
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    if (isUser) "🧑 You" else "🤖 Agent",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        Color.White
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        Color.White.copy(alpha = 0.9f)
                    }
                )
            }
        }
    }
}
