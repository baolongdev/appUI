package com.example.appui.kiosk.gesture

import android.os.SystemClock
import android.view.MotionEvent
import kotlin.math.hypot
import kotlin.math.max

/**
 * Detects two-finger double-tap gesture
 *
 * Configuration:
 * - Max tap duration: 250ms
 * - Double-tap window: 500ms
 * - Movement tolerance: 24px
 */
class TwoFingerDoubleTapDetector(
    private val tapMaxDurationMs: Long = 250L,
    private val doubleTapWindowMs: Long = 500L,
    private val movementTolerancePx: Float = 24f
) {
    // Gesture state
    private var pointer1Id = -1
    private var pointer2Id = -1
    private var gestureStartTimeMs = 0L
    private var hadTwoPointersDown = false
    private var isTapValid = false

    // Initial positions
    private var startX1 = 0f
    private var startY1 = 0f
    private var startX2 = 0f
    private var startY2 = 0f

    // Movement tracking
    private var maxMovement1 = 0f
    private var maxMovement2 = 0f

    // Double-tap tracking
    private var lastTwoFingerTapTimeMs = 0L

    /**
     * Process touch event and return true if double-tap detected
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resetGesture()
                pointer1Id = event.getPointerId(0)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    initializeTwoFingerGesture(event)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (hadTwoPointersDown && pointer1Id != -1 && pointer2Id != -1) {
                    updateMovement(event)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // Handle when one finger is lifted
                if (event.pointerCount - 1 < 1) {
                    val detected = evaluateDoubleTap()
                    resetGesture()
                    return detected
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val detected = evaluateDoubleTap()
                resetGesture()
                return detected
            }
        }

        return false
    }

    private fun initializeTwoFingerGesture(event: MotionEvent) {
        hadTwoPointersDown = true
        gestureStartTimeMs = SystemClock.elapsedRealtime()
        isTapValid = true

        val index1 = 0
        val index2 = event.actionIndex

        pointer1Id = event.getPointerId(index1)
        pointer2Id = event.getPointerId(index2)

        startX1 = event.getX(index1)
        startY1 = event.getY(index1)
        startX2 = event.getX(index2)
        startY2 = event.getY(index2)

        maxMovement1 = 0f
        maxMovement2 = 0f
    }

    private fun updateMovement(event: MotionEvent) {
        val index1 = event.findPointerIndex(pointer1Id)
        val index2 = event.findPointerIndex(pointer2Id)

        if (index1 == -1 || index2 == -1) {
            isTapValid = false
            return
        }

        val dx1 = event.getX(index1) - startX1
        val dy1 = event.getY(index1) - startY1
        val dx2 = event.getX(index2) - startX2
        val dy2 = event.getY(index2) - startY2

        maxMovement1 = max(maxMovement1, hypot(dx1, dy1))
        maxMovement2 = max(maxMovement2, hypot(dx2, dy2))

        // Invalidate tap if movement exceeds tolerance
        if (max(maxMovement1, maxMovement2) > movementTolerancePx) {
            isTapValid = false
        }
    }

    private fun evaluateDoubleTap(): Boolean {
        if (!hadTwoPointersDown || !isTapValid) {
            return false
        }

        val duration = SystemClock.elapsedRealtime() - gestureStartTimeMs
        if (duration > tapMaxDurationMs) {
            return false
        }

        val now = SystemClock.elapsedRealtime()
        val timeSinceLastTap = now - lastTwoFingerTapTimeMs

        return if (timeSinceLastTap <= doubleTapWindowMs && lastTwoFingerTapTimeMs > 0) {
            // Double-tap detected!
            lastTwoFingerTapTimeMs = 0L
            true
        } else {
            // First tap
            lastTwoFingerTapTimeMs = now
            false
        }
    }

    private fun resetGesture() {
        pointer1Id = -1
        pointer2Id = -1
        gestureStartTimeMs = 0L
        hadTwoPointersDown = false
        isTapValid = false
        maxMovement1 = 0f
        maxMovement2 = 0f
    }
}
