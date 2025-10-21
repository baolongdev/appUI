package com.example.appui.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design System Color Tokens
 *
 * Monochrome base with accent colors for specific use cases
 */
object AppColors {

    // ==================== Core Colors ====================

    /** Pure black - primary background in dark theme */
    val Black = Color(0xFF000000)

    /** Pure white - primary background in light theme */
    val White = Color(0xFFFFFFFF)


    // ==================== Accent Colors ====================

    /** Primary accent - Orange for CTAs and highlights */
    val AccentOrange = Color(0xFFFF5722)

    /** Success/Active state - Green */
    val AccentGreen = Color(0xFF22C55E)

    /** Error/Danger state - Red */
    val AccentRed = Color(0xFFFF4D4F)

    /** Warning state - Yellow */
    val AccentYellow = Color(0xFFFACC15)

    /** Secondary accent - Lighter orange */
    val AccentOrangeLight = Color(0xFFF97316)


    // ==================== Semantic Colors ====================

    /** Success indicator */
    val Success = AccentGreen

    /** Error indicator */
    val Error = AccentRed

    /** Warning indicator */
    val Warning = AccentYellow

    /** Info indicator */
    val Info = Color(0xFF3B82F6)


    // ==================== Surface Colors ====================

    /**
     * Dark theme surfaces
     */
    object Dark {
        val Surface = Color(0xFF121212)
        val SurfaceVariant = Color(0xFF1E1E1E)
        val SurfaceDim = Color(0xFF0A0A0A)
        val SurfaceBright = Color(0xFF2A2A2A)

        val Outline = White.copy(alpha = 0.12f)
        val OutlineVariant = White.copy(alpha = 0.22f)

        val Scrim = Black.copy(alpha = 0.32f)
    }

    /**
     * Light theme surfaces
     */
    object Light {
        val Surface = Color(0xFFFAFAFA)
        val SurfaceVariant = Color(0xFFF5F5F5)
        val SurfaceDim = Color(0xFFEEEEEE)
        val SurfaceBright = White

        val Outline = Black.copy(alpha = 0.12f)
        val OutlineVariant = Black.copy(alpha = 0.22f)

        val Scrim = Black.copy(alpha = 0.32f)
    }


    // ==================== Alpha Variants ====================

    /**
     * Standard alpha values for consistency
     */
    object Alpha {
        const val Disabled = 0.38f
        const val Medium = 0.60f
        const val High = 0.87f
        const val Full = 1.0f
    }
}
