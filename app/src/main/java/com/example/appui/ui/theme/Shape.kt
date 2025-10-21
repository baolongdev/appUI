package com.example.appui.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * App shape system
 *
 * Follows Material 3 shape scale:
 * - None: 0dp (rectangles)
 * - Extra Small: 4dp
 * - Small: 8dp
 * - Medium: 12dp
 * - Large: 16dp
 * - Extra Large: 28dp
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
