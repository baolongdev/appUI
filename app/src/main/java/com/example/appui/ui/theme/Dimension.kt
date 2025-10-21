package com.example.appui.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale for consistent padding and margins
 *
 * Based on 4dp grid system
 */
object Spacing {
    val None = 0.dp
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val MediumLarge = 16.dp
    val Large = 20.dp
    val ExtraLarge = 24.dp
    val XXLarge = 32.dp
    val XXXLarge = 48.dp
}

/**
 * Elevation scale for shadows and depth
 */
object Elevation {
    val None = 0.dp
    val Level1 = 1.dp
    val Level2 = 3.dp
    val Level3 = 6.dp
    val Level4 = 8.dp
    val Level5 = 12.dp
}

/**
 * Component-specific sizes
 */
object ComponentSize {
    val IconSmall = 16.dp
    val IconMedium = 24.dp
    val IconLarge = 32.dp

    val AvatarSmall = 32.dp
    val AvatarMedium = 40.dp
    val AvatarLarge = 56.dp

    val ButtonHeight = 40.dp
    val ButtonMinWidth = 64.dp

    val TextFieldHeight = 56.dp
}
