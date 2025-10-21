package com.example.appui.utils

import android.os.SystemClock
import android.view.KeyEvent
import kotlin.math.abs

/**
 * Detects Volume Up + Volume Down combo for quick unlock
 *
 * Both keys must be pressed within 700ms of each other
 */
class VolumeKeyComboDetector(
    private val comboWindowMs: Long = 700L,
    private val cooldownMs: Long = 1000L
) {
    private var lastVolumeUpTimeMs = 0L
    private var lastVolumeDownTimeMs = 0L
    private var lastToggleTimeMs = 0L

    /**
     * Process key down event and return true if combo detected
     */
    fun onKeyDown(keyCode: Int): Boolean {
        val now = SystemClock.elapsedRealtime()

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> lastVolumeUpTimeMs = now
            KeyEvent.KEYCODE_VOLUME_DOWN -> lastVolumeDownTimeMs = now
            else -> return false
        }

        // Check if both keys were pressed recently
        if (lastVolumeUpTimeMs == 0L || lastVolumeDownTimeMs == 0L) {
            return false
        }

        val timeDiff = abs(lastVolumeUpTimeMs - lastVolumeDownTimeMs)
        val timeSinceLastToggle = now - lastToggleTimeMs

        return if (timeDiff < comboWindowMs && timeSinceLastToggle > cooldownMs) {
            lastToggleTimeMs = now
            resetCombo()
            true
        } else {
            false
        }
    }

    private fun resetCombo() {
        lastVolumeUpTimeMs = 0L
        lastVolumeDownTimeMs = 0L
    }
}
