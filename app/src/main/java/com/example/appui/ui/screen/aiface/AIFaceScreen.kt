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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.example.appui.ui.components.SpotlightTutorial
import com.example.appui.ui.components.TutorialStep
import com.example.appui.ui.components.spotlightTarget
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
    var showTutorial by remember { mutableStateOf(false) }
    var tutorialCompleted by remember { mutableStateOf(false) } // ✅ THÊM
    var isFirstTime by remember { mutableStateOf(true) } // ✅ THÊM

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // ✅ CHỈ connect khi tutorial đã hoàn thành
            if (tutorialCompleted) {
                viewModel.connect(agentId, agentName, enablePcmCapture = true)
            }
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

    // ✅ LOGIC MỚI: Hiện tutorial trước, connect sau
    LaunchedEffect(agentId) {
        if (agentId != null && uiState.status == VoiceStatus.DISCONNECTED) {
            if (isFirstTime) {
                // Hiện tutorial trước
                delay(500)
                showTutorial = true
                isFirstTime = false
            } else {
                // Không phải lần đầu → connect ngay
                connectWithPermissionCheck()
            }
        }
    }

    // ✅ Connect sau khi tutorial hoàn thành
    LaunchedEffect(tutorialCompleted) {
        if (tutorialCompleted && agentId != null && uiState.status == VoiceStatus.DISCONNECTED) {
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

    Box(modifier = Modifier.fillMaxSize()) {
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
                                    com.example.appui.domain.repository.VoiceMode.SPEAKING -> "Đang nói..."
                                    com.example.appui.domain.repository.VoiceMode.LISTENING -> "Đang nghe..."
                                    else -> when (uiState.status) {
                                        VoiceStatus.CONNECTED -> "Đã kết nối"
                                        VoiceStatus.CONNECTING -> "Đang kết nối..."
                                        VoiceStatus.DISCONNECTED -> if (showTutorial) "Hướng dẫn sử dụng" else "Ngắt kết nối"
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when (uiState.mode) {
                                    com.example.appui.domain.repository.VoiceMode.SPEAKING -> MaterialTheme.extendedColors.success
                                    com.example.appui.domain.repository.VoiceMode.LISTENING -> MaterialTheme.extendedColors.info
                                    else -> if (showTutorial) MaterialTheme.extendedColors.info else Color.Gray
                                }
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (showTutorial) {
                                // Nếu đang tutorial → skip tutorial và connect
                                showTutorial = false
                                tutorialCompleted = true
                            } else {
                                handleClose()
                            }
                        }) {
                            Icon(
                                if (showTutorial) Icons.Default.Close else Icons.Default.ArrowBack,
                                if (showTutorial) "Bỏ qua" else "Đóng",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // ✅ CHỈ hiện khi đã connect
                        if (uiState.status == VoiceStatus.CONNECTED) {
                            // Mute Button with Spotlight
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                CompactMuteButton(
                                    isMuted = uiState.micMuted,
                                    isEffectiveMuted = uiState.isEffectiveMicMuted,
                                    onClick = { viewModel.toggleMic() },
                                    modifier = Modifier.spotlightTarget("mic_button")
                                )
                            }

                            // Mode Switch Button with Spotlight
                            AnimatedVisibility(
                                visible = true,
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
                                    },
                                    modifier = Modifier.spotlightTarget("mode_button")
                                )
                            }

                            // Details Button with Spotlight
                            IconButton(
                                onClick = { showBottomSheet = true },
                                modifier = Modifier.spotlightTarget("details_button")
                            ) {
                                Icon(Icons.Default.MoreVert, "Chi tiết", tint = Color.White)
                            }
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
                    // AI Face with Spotlight
                    AIFaceWithRive(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .spotlightTarget("ai_face"),
                        viewModel = viewModel
                    )

                    // Text Display with Spotlight
                    TypingTextDisplay(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .spotlightTarget("text_display")
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

        // ✅ SPOTLIGHT TUTORIAL với callback
        if (showTutorial) {
            SpotlightTutorial(
                steps = getAIFaceTutorialSteps(),
                onComplete = {
                    showTutorial = false
                    tutorialCompleted = true // ✅ Đánh dấu hoàn thành
                }
            )
        }

        // ✅ Loading overlay khi đang connecting
        AnimatedVisibility(
            visible = uiState.status == VoiceStatus.CONNECTING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 5.dp,
                        color = MaterialTheme.extendedColors.info
                    )
                    Text(
                        "Đang kết nối với Agent...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Vui lòng chờ trong giây lát",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ==================== TUTORIAL STEPS ====================

private fun getAIFaceTutorialSteps() = listOf(
    TutorialStep(
        targetKey = "ai_face",
        title = "Giao diện AI Face",
        description = "Đây là khuôn mặt AI của bạn. Miệng sẽ chuyển động khi Agent nói, và bạn sẽ thấy hiệu ứng sóng âm thanh khi bạn nói.",
        icon = Icons.Default.Face
    ),
    TutorialStep(
        targetKey = "text_display",
        title = "Hiển thị văn bản",
        description = "Phần này hiển thị nội dung cuộc trò chuyện theo thời gian thực. Văn bản sẽ được hiển thị từng ký tự với hiệu ứng typing đẹp mắt.",
        icon = Icons.Default.ChatBubble
    ),
    TutorialStep(
        targetKey = "mic_button",
        title = "Nút Microphone",
        description = "Bật/tắt microphone để điều khiển AI có nghe bạn hay không. Badge bên dưới hiển thị trạng thái mic hiện tại.",
        icon = Icons.Default.Mic
    ),
    TutorialStep(
        targetKey = "mode_button",
        title = "Chế độ hội thoại",
        description = "Chuyển đổi giữa 2 chế độ:\n• Tự động: AI luôn lắng nghe bạn\n• PTT: Nhấn giữ để nói",
        icon = Icons.Default.RecordVoiceOver
    ),
    TutorialStep(
        targetKey = "details_button",
        title = "Chi tiết cuộc trò chuyện",
        description = "Xem lịch sử tin nhắn đầy đủ, biểu đồ âm thanh trực quan, và các thông tin chi tiết về cuộc trò chuyện.",
        icon = Icons.Default.MoreVert
    )
)

// ==================== COMPACT BUTTONS ====================

@Composable
private fun CompactMuteButton(
    isMuted: Boolean,
    isEffectiveMuted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isEffectiveMuted) MaterialTheme.colorScheme.error
        else MaterialTheme.extendedColors.success,
        animationSpec = tween(300),
        label = "compact_mute_bg"
    )

    val scale by animateFloatAsState(
        targetValue = if (isEffectiveMuted) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "compact_mute_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "mute_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale),
            shape = CircleShape,
            color = backgroundColor.copy(alpha = if (isEffectiveMuted) pulseAlpha else 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isEffectiveMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = backgroundColor
                )
            }
        }

        Text(
            text = if (isEffectiveMuted) "Tắt" else "Bật",
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier
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
            text = if (isFullDuplex) "Tự động" else "PTT",
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
    val uiState by viewModel.uiState.collectAsState()
    val lastMessage = remember(messages) { messages.lastOrNull() }

    var isTyping by remember { mutableStateOf(false) }
    var visibleText by remember { mutableStateOf("") }
    var showMicPrompt by remember { mutableStateOf(false) }

    // PTT Auto-unmute
    LaunchedEffect(lastMessage?.text, uiState.conversationMode) {
        if (lastMessage != null &&
            lastMessage.speaker == Speaker.AGENT &&
            uiState.conversationMode == ConversationControlMode.PTT) {

            val textLength = lastMessage.text.length
            val waitDuration = when {
                textLength < 50 -> 1000L
                textLength < 150 -> 1500L
                else -> 2000L
            }

            delay(waitDuration)

            if (uiState.conversationMode == ConversationControlMode.PTT &&
                (uiState.micMuted || uiState.isEffectiveMicMuted)) {
                viewModel.toggleMic()
            }
        }
    }

    // Mic Prompt for both modes
    LaunchedEffect(uiState.mode, uiState.isEffectiveMicMuted) {
        if (uiState.mode == com.example.appui.domain.repository.VoiceMode.LISTENING &&
            uiState.isEffectiveMicMuted) {

            showMicPrompt = false
            delay(2500L)

            if (uiState.mode == com.example.appui.domain.repository.VoiceMode.LISTENING &&
                uiState.isEffectiveMicMuted) {
                showMicPrompt = true
            }
        } else {
            showMicPrompt = false
        }
    }

    // Typing animation
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
                    delay(40)
                }

                if (sentenceIdx < sentences.size - 1) {
                    delay(300)
                }
            }

            isTyping = false
        }
    }

    Box(modifier = modifier) {
        Surface(
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedVisibility(
                                visible = isTyping ||
                                        uiState.mode == com.example.appui.domain.repository.VoiceMode.LISTENING ||
                                        !uiState.isEffectiveMicMuted,
                                enter = scaleIn() + fadeIn(),
                                exit = scaleOut() + fadeOut()
                            ) {
                                val dotColor = when {
                                    isTyping && lastMessage.speaker == Speaker.AGENT ->
                                        MaterialTheme.extendedColors.success
                                    uiState.mode == com.example.appui.domain.repository.VoiceMode.LISTENING &&
                                            uiState.isEffectiveMicMuted ->
                                        MaterialTheme.colorScheme.error
                                    !uiState.isEffectiveMicMuted ->
                                        MaterialTheme.extendedColors.info
                                    else -> Color.Gray
                                }

                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }

                            Text(
                                if (lastMessage.speaker == Speaker.AGENT) "🤖 Agent" else "🧑 Bạn",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (lastMessage.speaker == Speaker.AGENT) {
                                    MaterialTheme.extendedColors.success
                                } else {
                                    MaterialTheme.extendedColors.info
                                }
                            )

                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                val isReady = !uiState.isEffectiveMicMuted &&
                                        uiState.mode == com.example.appui.domain.repository.VoiceMode.LISTENING
                                val isListeningMuted = uiState.mode == com.example.appui.domain.repository.VoiceMode.LISTENING &&
                                        uiState.isEffectiveMicMuted

                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = when {
                                        isReady -> MaterialTheme.extendedColors.info.copy(alpha = 0.2f)
                                        isListeningMuted -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                        else -> Color.Gray.copy(alpha = 0.2f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (uiState.isEffectiveMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                            null,
                                            modifier = Modifier.size(12.dp),
                                            tint = when {
                                                isReady -> MaterialTheme.extendedColors.info
                                                isListeningMuted -> MaterialTheme.colorScheme.error
                                                else -> Color.Gray
                                            }
                                        )
                                        Text(
                                            when {
                                                isReady -> "Sẵn sàng"
                                                isListeningMuted -> "Mic tắt"
                                                else -> when (uiState.conversationMode) {
                                                    ConversationControlMode.PTT -> "Chờ..."
                                                    ConversationControlMode.FULL_DUPLEX -> "Đang nghe"
                                                }
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isReady -> MaterialTheme.extendedColors.info
                                                isListeningMuted -> MaterialTheme.colorScheme.error
                                                else -> Color.Gray
                                            }
                                        )
                                    }
                                }
                            }
                        }

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
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.extendedColors.info
                        )
                        Text(
                            "Đang chờ cuộc trò chuyện...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }

        // Mic Prompt Snackbar
        AnimatedVisibility(
            visible = showMicPrompt,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.inverseSurface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .widthIn(max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MicOff,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Mic đang tắt",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                        Text(
                            when (uiState.conversationMode) {
                                ConversationControlMode.PTT -> "Bật mic để bắt đầu nói?"
                                ConversationControlMode.FULL_DUPLEX -> "Bật mic để AI nghe bạn?"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { showMicPrompt = false },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface
                            )
                        ) {
                            Text("Bỏ qua", fontSize = 13.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.toggleMic()
                                showMicPrompt = false
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.extendedColors.info,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Bật", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==================== SAVE DIALOG ====================

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

// ==================== BOTTOM SHEET CONTENT ====================

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
                "Chi tiết",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ExpandMore, "Đóng", tint = Color.White)
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
                "Hoạt động âm thanh",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            CompactWaveformCard(
                title = "🎤 Bạn",
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
                "Cuộc trò chuyện",
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
                Text("Chưa có tin nhắn", color = Color.Gray)
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
                    if (isUser) "🧑 Bạn" else "🤖 Agent",
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
