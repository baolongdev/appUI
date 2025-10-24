package com.example.appui

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.appui.kiosk.KioskManager
import com.example.appui.kiosk.gesture.TwoFingerDoubleTapDetector
import com.example.appui.kiosk.immersive.ImmersiveModeController
import com.example.appui.ui.navigation.AppNav
import com.example.appui.ui.theme.AppTheme
import com.example.appui.utils.VolumeKeyComboDetector
import com.example.appui.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity với Kiosk Mode & Lifecycle Auto-Management
 *
 * Features:
 * - Auto-enter kiosk mode on start
 * - Auto disable/enable on pause/resume
 * - Two-finger double-tap to toggle kiosk
 * - Volume key combo fallback
 * - Immersive fullscreen mode
 * - Back button prevention in kiosk mode
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Core managers
    private lateinit var kioskManager: KioskManager
    private lateinit var immersiveController: ImmersiveModeController

    // Gesture detectors
    private lateinit var twoFingerTapDetector: TwoFingerDoubleTapDetector
    private lateinit var volumeComboDetector: VolumeKeyComboDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        initializeManagers()

        // ✅ Register lifecycle observer for auto pause/resume
        lifecycle.addObserver(kioskManager)

        // Setup window
        setupWindow()

        // Setup back button handler
        setupBackButtonHandler()

        // Setup UI
        setContent {
            AppTheme {
                AppNav()
            }
        }

        // Enter kiosk mode on start
        kioskManager.enterKioskMode(autoStart = true)
    }

    override fun onResume() {
        super.onResume()
        immersiveController.enterImmersiveMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            immersiveController.enterImmersiveMode()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Handle two-finger double-tap gesture
        if (twoFingerTapDetector.onTouchEvent(event)) {
            kioskManager.toggleKioskMode()
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle volume key combo
        if (volumeComboDetector.onKeyDown(keyCode)) {
            kioskManager.toggleKioskMode()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ Cleanup lifecycle observer
        lifecycle.removeObserver(kioskManager)
    }

    // ==================== Private Methods ====================

    private fun initializeManagers() {
        kioskManager = KioskManager(
            activity = this,
            onKioskStateChanged = { isLocked ->
                val message = if (isLocked) {
                    "🔒 Chế độ Kiosk BẬT"
                } else {
                    "🟢 Chế độ Kiosk TẮT"
                }
                showToast(message)
            }
        )

        immersiveController = ImmersiveModeController(window)
        twoFingerTapDetector = TwoFingerDoubleTapDetector()
        volumeComboDetector = VolumeKeyComboDetector()
    }

    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        immersiveController.enterImmersiveMode()
    }

    private fun setupBackButtonHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (kioskManager.isKioskModeActive()) {
                    showToast("⚠️ Nút Back bị vô hiệu hóa trong chế độ Kiosk")
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }
}
