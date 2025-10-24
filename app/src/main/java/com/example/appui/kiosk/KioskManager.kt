package com.example.appui.kiosk

import android.app.Activity
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.appui.utils.showToast

/**
 * Manager for Kiosk Mode operations with auto lifecycle handling
 *
 * Features:
 * - Enter/exit kiosk mode
 * - Auto disable when leaving app
 * - Auto re-enable when returning to app
 * - State persistence
 */
class KioskManager(
    private val activity: Activity,
    private val onKioskStateChanged: ((Boolean) -> Unit)? = null
) : DefaultLifecycleObserver {

    private var isLocked = false
    private var wasLockedBeforePause = false // ✅ Track state before leaving app

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
            Log.i(TAG, "✅ Kiosk mode enabled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to enter kiosk mode: ${e.message}", e)
            if (!autoStart) {
                activity.showToast(
                    "⚠️ Không thể bật chế độ Kiosk.\n" +
                            "Vui lòng bật 'Ghim màn hình' hoặc cấu hình Device Owner."
                )
            }
        }
    }

    /**
     * Exit kiosk mode
     * @param silent If true, don't show toast on error
     */
    fun exitKioskMode(silent: Boolean = false) {
        if (!isLocked) {
            Log.d(TAG, "Not in kiosk mode")
            return
        }

        try {
            activity.stopLockTask()
            isLocked = false
            onKioskStateChanged?.invoke(false)
            Log.i(TAG, "✅ Kiosk mode disabled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to exit kiosk mode: ${e.message}", e)
            if (!silent) {
                activity.showToast("❌ Không thể thoát chế độ Kiosk")
            }
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

    // ==================== LIFECYCLE CALLBACKS ====================

    /**
     * Called when app goes to background
     * Auto disable kiosk mode to allow external actions
     */
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.d(TAG, "📴 App paused - disabling kiosk mode")

        // Remember state before pause
        wasLockedBeforePause = isLocked

        // Disable kiosk mode if active
        if (isLocked) {
            exitKioskMode(silent = true)
        }
    }

    /**
     * Called when app returns to foreground
     * Auto re-enable kiosk mode if it was active before
     */
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d(TAG, "📱 App resumed")

        // Re-enable kiosk mode if it was active before pause
        if (wasLockedBeforePause && !isLocked) {
            Log.d(TAG, "🔄 Re-enabling kiosk mode")
            enterKioskMode(autoStart = true)
        }
    }

    /**
     * Called when app is destroyed
     * Clean up kiosk mode
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(TAG, "💥 App destroyed - cleaning up")

        if (isLocked) {
            exitKioskMode(silent = true)
        }
    }
}
