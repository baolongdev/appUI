package com.example.appui.kiosk.immersive

import android.view.Window
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Controller for immersive fullscreen mode
 *
 * Hides:
 * - Status bar
 * - Navigation bar
 * - System gestures
 */
class ImmersiveModeController(private val window: Window) {

    private val controller: WindowInsetsControllerCompat by lazy {
        WindowInsetsControllerCompat(window, window.decorView)
    }

    /**
     * Enter immersive fullscreen mode
     */
    fun enterImmersiveMode() {
        controller.hide(
            WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars() or
                    WindowInsetsCompat.Type.systemGestures()
        )

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /**
     * Exit immersive mode and show system bars
     */
    fun exitImmersiveMode() {
        controller.show(
            WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars()
        )
    }
}
