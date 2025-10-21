package com.example.appui.kiosk

import android.app.Activity
import android.util.Log
import com.example.appui.utils.showToast

/**
 * Manager for Kiosk Mode operations
 *
 * Handles:
 * - Enter/exit kiosk mode (lock task)
 * - State management
 * - Error handling
 */
class KioskManager(
    private val activity: Activity,
    private val onKioskStateChanged: ((Boolean) -> Unit)? = null
) {
    private var isLocked = false

    companion object {
        private const val TAG = "KioskManager"
    }

    /**
     * Check if kiosk mode is currently active
     */
    fun isKioskModeActive(): Boolean = isLocked

    /**
     * Enter kiosk mode
     * @param autoStart If true, suppress error toast on failure
     */
    fun enterKioskMode(autoStart: Boolean = false) {
        if (isLocked) {
            Log.d(TAG, "Already in kiosk mode")
            return
        }

        try {
            activity.startLockTask()
            isLocked = true
            onKioskStateChanged?.invoke(true)
            Log.i(TAG, "Kiosk mode enabled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enter kiosk mode: ${e.message}", e)
            if (!autoStart) {
                activity.showToast(
                    "⚠️ Không thể bật chế độ Kiosk.\n" +
                            "Vui lòng bật 'Ghim màn hình' hoặc cấu hình Device Owner."
                )
            } else {
                activity.showToast(
                    "⚠️ Kiosk mode không khả dụng.\n" +
                            "Cần cấu hình Screen Pinning hoặc Device Owner."
                )
            }
        }
    }

    /**
     * Exit kiosk mode
     */
    fun exitKioskMode() {
        if (!isLocked) {
            Log.d(TAG, "Not in kiosk mode")
            return
        }

        try {
            activity.stopLockTask()
            isLocked = false
            onKioskStateChanged?.invoke(false)
            Log.i(TAG, "Kiosk mode disabled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to exit kiosk mode: ${e.message}", e)
            activity.showToast("❌ Không thể thoát chế độ Kiosk")
        }
    }

    /**
     * Toggle kiosk mode on/off
     */
    fun toggleKioskMode() {
        if (isLocked) {
            exitKioskMode()
        } else {
            enterKioskMode()
        }
    }
}
