package com.astrodham.astroagent.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.astrodham.astroagent.util.Constants
import com.astrodham.astroagent.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Core AccessibilityService for AstroAgent.
 *
 * Provides the ability to:
 * - Detect foreground applications
 * - Access the UI node tree for element discovery
 * - Perform global actions (BACK, HOME, RECENTS)
 * - Execute gestures (tap, swipe) via GestureDescription API
 *
 * SECURITY NOTE: AccessibilityService grants deep device control.
 * This service only activates when explicitly enabled by the user
 * in Android Settings → Accessibility. It does NOT perform any
 * hidden or background operations without user initiation.
 *
 * Lifecycle: Managed entirely by the Android system. The companion
 * object holds a reference to the active instance for other modules.
 */
class AstroAccessibilityService : AccessibilityService() {

    companion object {
        /** Reference to the active service instance, null when service is not running. */
        @Volatile
        var instance: AstroAccessibilityService? = null
            private set

        /** Check if the accessibility service is currently active. */
        val isActive: Boolean get() = instance != null

        private val _foregroundPackage = MutableStateFlow<String?>(null)
        /** Observable foreground package name */
        val foregroundPackage: StateFlow<String?> = _foregroundPackage.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Logger.i("AccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString()
                if (packageName != null && packageName != _foregroundPackage.value) {
                    _foregroundPackage.value = packageName
                    Logger.d("Foreground app changed: $packageName")
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // UI content changed — useful for detecting when a page loads
                Logger.d("Window content changed in: ${event.packageName}")
            }
            else -> { /* Other events we monitor but don't need to log */ }
        }
    }

    override fun onInterrupt() {
        Logger.w("AccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Logger.i("AccessibilityService destroyed")
    }

    // ── Public API ──

    /**
     * Gets the root AccessibilityNodeInfo for the current window.
     * Caller should NOT recycle this node (handled by the system on API 33+).
     *
     * @return Root node of the active window, or null if unavailable.
     */
    fun getRootNode(): AccessibilityNodeInfo? {
        return try {
            rootInActiveWindow
        } catch (e: Exception) {
            Logger.e("Failed to get root node", e)
            null
        }
    }

    /**
     * Performs the system BACK action.
     */
    fun pressBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK).also {
            Logger.d("Global action BACK: $it")
        }
    }

    /**
     * Performs the system HOME action.
     */
    fun pressHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME).also {
            Logger.d("Global action HOME: $it")
        }
    }

    /**
     * Opens the recent apps screen.
     */
    fun openRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS).also {
            Logger.d("Global action RECENTS: $it")
        }
    }

    /**
     * Opens the notification shade.
     */
    fun openNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS).also {
            Logger.d("Global action NOTIFICATIONS: $it")
        }
    }

    /**
     * Dispatches a gesture (tap or swipe) using the GestureDescription API.
     * This is the modern, non-root way to perform touch events.
     *
     * @param path The gesture path
     * @param startTime Delay before starting the gesture (ms)
     * @param duration Duration of the gesture (ms)
     * @param callback Optional callback for gesture result
     */
    fun dispatchGesture(
        path: Path,
        startTime: Long = 0,
        duration: Long = Constants.SWIPE_DURATION_MS,
        callback: GestureResultCallback? = null
    ): Boolean {
        val stroke = GestureDescription.StrokeDescription(path, startTime, duration)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        return try {
            dispatchGesture(gesture, callback, null)
            true
        } catch (e: Exception) {
            Logger.e("Failed to dispatch gesture", e)
            false
        }
    }
}
