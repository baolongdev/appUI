package com.example.appui.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.appui.ui.theme.extendedColors
import kotlin.math.max

// ==================== DATA MODELS ====================

data class TutorialStep(
    val targetKey: String,
    val title: String,
    val description: String,
    val icon: ImageVector? = null,
    val shape: SpotlightShape = SpotlightShape.Circle
)

enum class SpotlightShape {
    Circle,
    Rectangle,
    RoundedRectangle
}

// ==================== SPOTLIGHT TUTORIAL ====================

@Composable
fun SpotlightTutorial(
    steps: List<TutorialStep>,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStepIndex by remember { mutableStateOf(0) }
    var targetBounds by remember { mutableStateOf<Rect?>(null) }
    val currentStep = steps.getOrNull(currentStepIndex)

    // Animation
    val spotlightAlpha by animateFloatAsState(
        targetValue = if (targetBounds != null) 1f else 0f,
        animationSpec = tween(300),
        label = "spotlight_alpha"
    )

    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    DisposableEffect(Unit) {
        onDispose {
            SpotlightRegistry.clearTargets()
        }
    }

    if (currentStep != null) {
        LaunchedEffect(currentStepIndex) {
            targetBounds = SpotlightRegistry.getTargetBounds(currentStep.targetKey)
        }

        Box(modifier = modifier.fillMaxSize()) {
            // Dark overlay with spotlight
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f * spotlightAlpha))
            ) {
                targetBounds?.let { bounds ->
                    val path = Path().apply {
                        addRect(
                            androidx.compose.ui.geometry.Rect(
                                0f, 0f, size.width, size.height
                            )
                        )
                    }

                    // Create hole in overlay
                    when (currentStep.shape) {
                        SpotlightShape.Circle -> {
                            val centerX = bounds.center.x
                            val centerY = bounds.center.y
                            val radius = max(bounds.width, bounds.height) / 2 + 24.dp.toPx()

                            path.addOval(
                                androidx.compose.ui.geometry.Rect(
                                    centerX - radius * pulseScale,
                                    centerY - radius * pulseScale,
                                    centerX + radius * pulseScale,
                                    centerY + radius * pulseScale
                                )
                            )
                        }
                        SpotlightShape.Rectangle -> {
                            path.addRect(
                                androidx.compose.ui.geometry.Rect(
                                    bounds.left - 12.dp.toPx(),
                                    bounds.top - 12.dp.toPx(),
                                    bounds.right + 12.dp.toPx(),
                                    bounds.bottom + 12.dp.toPx()
                                )
                            )
                        }
                        SpotlightShape.RoundedRectangle -> {
                            path.addRoundRect(
                                androidx.compose.ui.geometry.RoundRect(
                                    bounds.left - 12.dp.toPx(),
                                    bounds.top - 12.dp.toPx(),
                                    bounds.right + 12.dp.toPx(),
                                    bounds.bottom + 12.dp.toPx(),
                                    20.dp.toPx(),
                                    20.dp.toPx()
                                )
                            )
                        }
                    }

                    drawPath(
                        path = path,
                        color = Color.Transparent
                    )
                }
            }

            // Highlight border - ✅ Use AccentOrange
            targetBounds?.let { bounds ->
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val highlightColor = Color(0xFFFF6B35) // AccentOrange

                    when (currentStep.shape) {
                        SpotlightShape.Circle -> {
                            val centerX = bounds.center.x
                            val centerY = bounds.center.y
                            val radius = max(bounds.width, bounds.height) / 2 + 24.dp.toPx()

                            drawCircle(
                                color = highlightColor.copy(alpha = 0.9f),
                                radius = radius * pulseScale,
                                center = Offset(centerX, centerY),
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }
                        SpotlightShape.Rectangle, SpotlightShape.RoundedRectangle -> {
                            drawRect(
                                color = highlightColor.copy(alpha = 0.9f),
                                topLeft = Offset(bounds.left - 12.dp.toPx(), bounds.top - 12.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(
                                    bounds.width + 24.dp.toPx(),
                                    bounds.height + 24.dp.toPx()
                                ),
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Tutorial card
            targetBounds?.let { bounds ->
                TutorialCard(
                    step = currentStep,
                    currentIndex = currentStepIndex,
                    totalSteps = steps.size,
                    targetBounds = bounds,
                    onNext = {
                        if (currentStepIndex < steps.size - 1) {
                            currentStepIndex++
                        } else {
                            onComplete()
                        }
                    },
                    onPrevious = {
                        if (currentStepIndex > 0) {
                            currentStepIndex--
                        }
                    },
                    onSkip = onComplete,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ==================== TUTORIAL CARD ====================

@Composable
private fun TutorialCard(
    step: TutorialStep,
    currentIndex: Int,
    totalSteps: Int,
    targetBounds: Rect,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val targetCenterY = with(LocalDensity.current) {
        targetBounds.center.y.toDp()
    }
    val isTopHalf = targetCenterY < screenHeight / 2

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(if (isTopHalf) Alignment.BottomCenter else Alignment.TopCenter)
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Progress indicator - ✅ Use AccentOrange
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (index <= currentIndex) {
                                    Color(0xFFFF6B35) // AccentOrange
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Step counter - ✅ Use AccentOrange
            Text(
                "Bước ${currentIndex + 1} của $totalSteps",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFFF6B35), // AccentOrange
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            // Icon + Title
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                step.icon?.let { icon ->
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF6B35).copy(alpha = 0.15f), // AccentOrange light
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color(0xFFFF6B35) // AccentOrange
                            )
                        }
                    }
                }

                Text(
                    step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(16.dp))

            // Description
            Text(
                step.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight.times(1.3f)
            )

            Spacer(Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip button
                TextButton(
                    onClick = onSkip,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Bỏ qua", fontWeight = FontWeight.Medium)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Previous button
                    if (currentIndex > 0) {
                        OutlinedButton(
                            onClick = onPrevious,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Quay lại", fontWeight = FontWeight.Medium)
                        }
                    }

                    // Next/Finish button - ✅ Use AccentOrange
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B35), // AccentOrange
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            if (currentIndex < totalSteps - 1) "Tiếp theo" else "Hoàn thành",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            if (currentIndex < totalSteps - 1) Icons.Default.ArrowForward else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==================== SPOTLIGHT TARGET MODIFIER ====================

fun Modifier.spotlightTarget(key: String): Modifier = this.then(
    Modifier.onGloballyPositioned { coordinates ->
        SpotlightRegistry.registerTarget(key, coordinates.boundsInWindow())
    }
)

// ==================== REGISTRY ====================

object SpotlightRegistry {
    private val targets = mutableMapOf<String, Rect>()

    fun registerTarget(key: String, bounds: Rect) {
        targets[key] = bounds
    }

    fun getTargetBounds(key: String): Rect? = targets[key]

    fun clearTargets() {
        targets.clear()
    }
}
