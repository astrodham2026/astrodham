package com.astrodham.astroagent.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.astrodham.astroagent.util.Constants
import com.astrodham.astroagent.util.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Performs UI actions through the AccessibilityService.
 *
 * All methods are safe to call even if the service is not active
 * (they will return false and log warnings).
 *
 * Actions include:
 * - Tap (by node or coordinates)
 * - Long press
 * - Type text
 * - Swipe (directional)
 * - Scroll
 * - Clear text field
 */
object AccessibilityActions {

    /**
     * Taps on an accessibility node using ACTION_CLICK.
     *
     * @param node The node to click
     * @return true if the action was performed successfully
     */
    fun tapNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Logger.d("Tap node '${node.text ?: node.contentDescription}': $result")
            result
        } catch (e: Exception) {
            Logger.e("Failed to tap node", e)
            false
        }
    }

    /**
     * Long presses on an accessibility node.
     */
    fun longPressNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            Logger.d("Long press node '${node.text ?: node.contentDescription}': $result")
            result
        } catch (e: Exception) {
            Logger.e("Failed to long press node", e)
            false
        }
    }

    /**
     * Types text into an editable node using ACTION_SET_TEXT.
     * The node must be an EditText or similar editable field.
     *
     * @param node The editable node to type into
     * @param text The text to enter
     * @return true if the text was set successfully
     */
    fun typeText(node: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            // First focus the node
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

            // Then set the text
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            Logger.d("Type text '${text.take(30)}...' into node: $result")
            result
        } catch (e: Exception) {
            Logger.e("Failed to type text", e)
            false
        }
    }

    /**
     * Clears the text content of an editable node.
     */
    fun clearText(node: AccessibilityNodeInfo): Boolean {
        return typeText(node, "")
    }

    /**
     * Scrolls a scrollable node forward (down or right).
     *
     * @param node The scrollable node
     * @return true if scroll was performed
     */
    fun scrollForward(node: AccessibilityNodeInfo): Boolean {
        return try {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            Logger.d("Scroll forward: $result")
            result
        } catch (e: Exception) {
            Logger.e("Failed to scroll forward", e)
            false
        }
    }

    /**
     * Scrolls a scrollable node backward (up or left).
     */
    fun scrollBackward(node: AccessibilityNodeInfo): Boolean {
        return try {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            Logger.d("Scroll backward: $result")
            result
        } catch (e: Exception) {
            Logger.e("Failed to scroll backward", e)
            false
        }
    }

    /**
     * Taps at specific screen coordinates using a gesture.
     * Requires the AccessibilityService to have canPerformGestures=true.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if the gesture was dispatched
     */
    suspend fun tapAtCoordinates(x: Float, y: Float): Boolean {
        val service = AstroAccessibilityService.instance ?: run {
            Logger.w("Cannot tap: AccessibilityService not active")
            return false
        }

        val path = Path().apply {
            moveTo(x, y)
        }

        val result = dispatchGestureAsync(service, path, duration = 50L)
        Logger.d("Tap at ($x, $y): $result")
        return result
    }

    /**
     * Taps at specific screen coordinates given as percentages (0-100).
     */
    suspend fun tapAtScreenCoordinates(xPercent: Float, yPercent: Float): Boolean {
        val metrics = android.content.res.Resources.getSystem().displayMetrics
        val x = metrics.widthPixels * (xPercent / 100f)
        val y = metrics.heightPixels * (yPercent / 100f)
        return tapAtCoordinates(x, y)
    }

    /**
     * Taps at the center of a node's bounds using a gesture.
     * Useful when ACTION_CLICK doesn't work on certain custom views.
     *
     * @param node The node to tap on
     * @return true if the gesture was dispatched
     */
    suspend fun tapAtNodeCenter(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val centerX = rect.centerX().toFloat()
        val centerY = rect.centerY().toFloat()
        return tapAtCoordinates(centerX, centerY)
    }

    /**
     * Performs a swipe gesture between two points.
     *
     * @param startX Start X coordinate
     * @param startY Start Y coordinate
     * @param endX End X coordinate
     * @param endY End Y coordinate
     * @param duration Duration of the swipe in milliseconds
     * @return true if the gesture was dispatched
     */
    suspend fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long = Constants.SWIPE_DURATION_MS
    ): Boolean {
        val service = AstroAccessibilityService.instance ?: run {
            Logger.w("Cannot swipe: AccessibilityService not active")
            return false
        }

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val result = dispatchGestureAsync(service, path, duration)
        Logger.d("Swipe ($startX,$startY) → ($endX,$endY): $result")
        return result
    }

    /**
     * Performs a directional swipe (up, down, left, right).
     * Uses screen center as the reference point with a reasonable swipe distance.
     *
     * @param direction "up", "down", "left", or "right"
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     * @return true if the gesture was dispatched
     */
    suspend fun swipeDirection(
        direction: String,
        screenWidth: Int = 1080,
        screenHeight: Int = 2400
    ): Boolean {
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f
        val swipeDistance = screenHeight / 3f

        return when (direction.lowercase()) {
            "up" -> swipe(centerX, centerY + swipeDistance, centerX, centerY - swipeDistance)
            "down" -> swipe(centerX, centerY - swipeDistance, centerX, centerY + swipeDistance)
            "left" -> swipe(centerX + swipeDistance, centerY, centerX - swipeDistance, centerY)
            "right" -> swipe(centerX - swipeDistance, centerY, centerX + swipeDistance, centerY)
            else -> {
                Logger.w("Unknown swipe direction: $direction")
                false
            }
        }
    }

    // ── Private Helpers ──

    /**
     * Dispatches a gesture asynchronously and waits for the result.
     * Uses CompletableDeferred to bridge the callback to coroutines.
     */
    private suspend fun dispatchGestureAsync(
        service: AstroAccessibilityService,
        path: Path,
        duration: Long
    ): Boolean {
        val deferred = CompletableDeferred<Boolean>()

        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                deferred.complete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                deferred.complete(false)
            }
        }

        service.dispatchGesture(gesture, callback, null)

        // Wait for result with timeout
        return withTimeoutOrNull(5000L) {
            deferred.await()
        } ?: false
    }
}
