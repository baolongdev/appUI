package com.example.appui.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Dark color scheme
 */
private val DarkColorScheme = darkColorScheme(
    // Primary
    primary = AppColors.White,
    onPrimary = AppColors.Black,
    primaryContainer = AppColors.Dark.SurfaceVariant,
    onPrimaryContainer = AppColors.White,

    // Secondary (Accent)
    secondary = AppColors.AccentOrange,
    onSecondary = AppColors.White,
    secondaryContainer = AppColors.AccentOrange.copy(alpha = 0.2f),
    onSecondaryContainer = AppColors.AccentOrange,

    // Tertiary
    tertiary = AppColors.AccentGreen,
    onTertiary = AppColors.White,
    tertiaryContainer = AppColors.AccentGreen.copy(alpha = 0.2f),
    onTertiaryContainer = AppColors.AccentGreen,

    // Error
    error = AppColors.Error,
    onError = AppColors.White,
    errorContainer = AppColors.Error.copy(alpha = 0.2f),
    onErrorContainer = AppColors.Error,

    // Background
    background = AppColors.Black,
    onBackground = AppColors.White,

    // Surface
    surface = AppColors.Dark.Surface,
    onSurface = AppColors.White,
    surfaceVariant = AppColors.Dark.SurfaceVariant,
    onSurfaceVariant = AppColors.White.copy(alpha = AppColors.Alpha.High),
    surfaceTint = AppColors.AccentOrange,

    // Inverse
    inverseSurface = AppColors.White,
    inverseOnSurface = AppColors.Black,
    inversePrimary = AppColors.Black,

    // Outline
    outline = AppColors.Dark.Outline,
    outlineVariant = AppColors.Dark.OutlineVariant,

    // Scrim
    scrim = AppColors.Dark.Scrim
)

/**
 * Light color scheme
 */
private val LightColorScheme = lightColorScheme(
    // Primary
    primary = AppColors.Black,
    onPrimary = AppColors.White,
    primaryContainer = AppColors.Light.SurfaceVariant,
    onPrimaryContainer = AppColors.Black,

    // Secondary (Accent)
    secondary = AppColors.AccentOrange,
    onSecondary = AppColors.White,
    secondaryContainer = AppColors.AccentOrange.copy(alpha = 0.15f),
    onSecondaryContainer = AppColors.AccentOrange,

    // Tertiary
    tertiary = AppColors.AccentGreen,
    onTertiary = AppColors.White,
    tertiaryContainer = AppColors.AccentGreen.copy(alpha = 0.15f),
    onTertiaryContainer = AppColors.AccentGreen,

    // Error
    error = AppColors.Error,
    onError = AppColors.White,
    errorContainer = AppColors.Error.copy(alpha = 0.15f),
    onErrorContainer = AppColors.Error,

    // Background
    background = AppColors.White,
    onBackground = AppColors.Black,

    // Surface
    surface = AppColors.Light.Surface,
    onSurface = AppColors.Black,
    surfaceVariant = AppColors.Light.SurfaceVariant,
    onSurfaceVariant = AppColors.Black.copy(alpha = AppColors.Alpha.High),
    surfaceTint = AppColors.AccentOrange,

    // Inverse
    inverseSurface = AppColors.Black,
    inverseOnSurface = AppColors.White,
    inversePrimary = AppColors.White,

    // Outline
    outline = AppColors.Light.Outline,
    outlineVariant = AppColors.Light.OutlineVariant,

    // Scrim
    scrim = AppColors.Light.Scrim
)

/**
 * Extended colors for app-specific use cases
 */
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val onInfo: Color
)

private val DarkExtendedColors = ExtendedColors(
    success = AppColors.Success,
    onSuccess = AppColors.White,
    warning = AppColors.Warning,
    onWarning = AppColors.Black,
    info = AppColors.Info,
    onInfo = AppColors.White
)

private val LightExtendedColors = ExtendedColors(
    success = AppColors.Success,
    onSuccess = AppColors.White,
    warning = AppColors.Warning,
    onWarning = AppColors.Black,
    info = AppColors.Info,
    onInfo = AppColors.White
)

/**
 * CompositionLocal for extended colors
 */
val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }

/**
 * App theme with Material 3 support
 *
 * Usage:
 * ```
 * AppTheme {
 *     // Your content
 * }
 * ```
 *
 * Access extended colors:
 * ```
 * val extendedColors = MaterialTheme.extendedColors
 * Icon(tint = extendedColors.success)
 * ```
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Future: support dynamic colors on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/**
 * Extension property to access extended colors
 */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current
