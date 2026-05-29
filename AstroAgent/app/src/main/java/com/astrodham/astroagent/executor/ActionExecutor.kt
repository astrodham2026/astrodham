package com.astrodham.astroagent.executor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.astrodham.astroagent.accessibility.AccessibilityActions
import com.astrodham.astroagent.accessibility.AstroAccessibilityService
import com.astrodham.astroagent.accessibility.NodeFinder
import com.astrodham.astroagent.ocr.OcrEngine
import com.astrodham.astroagent.planner.PlanModels
import com.astrodham.astroagent.util.Constants
import com.astrodham.astroagent.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Executes individual planned actions on the device.
 *
 * Maps each ActionType to real Android API calls through
 * the Accessibility and OCR modules.
 *
 * Each action is executed with:
 * - Timeout protection
 * - Result reporting
 * - Logging
 *
 * Runs on Dispatchers.Default for CPU-bound work,
 * switches to Main for accessibility operations that require it.
 */
class ActionExecutor(private val context: Context) {

    /**
     * Executes a single planned action.
     *
     * @param action The action to execute
     * @return ActionResult indicating success/failure
     */
    suspend fun execute(action: PlanModels.PlannedAction): ActionResult {
        val startTime = System.currentTimeMillis()
        Logger.i("Executing: [${action.type}] ${action.description}")

        return try {
            withTimeout(Constants.ACTION_TIMEOUT_MS) {
                val result = when (action.actionType) {
                    PlanModels.ActionType.OPEN_APP -> executeOpenApp(action)
                    PlanModels.ActionType.TAP -> executeTap(action)
                    PlanModels.ActionType.LONG_PRESS -> executeLongPress(action)
                    PlanModels.ActionType.SWIPE -> executeSwipe(action)
                    PlanModels.ActionType.TYPE_TEXT -> executeTypeText(action)
                    PlanModels.ActionType.WAIT -> executeWait(action)
                    PlanModels.ActionType.BACK -> executeBack()
                    PlanModels.ActionType.HOME -> executeHome()
                    PlanModels.ActionType.SCROLL -> executeScroll(action)
                    PlanModels.ActionType.READ_SCREEN -> executeReadScreen()
                    PlanModels.ActionType.RETRY_ACTION -> {
                        // Retry is handled by the WorkflowEngine, not here
                        ActionResult.success("Retry requested")
                    }
                    null -> ActionResult.failure(
                        "Unknown action type: ${action.type}",
                        error = "ActionType '${action.type}' not recognized"
                    )
                }
                val duration = System.currentTimeMillis() - startTime
                result.copy(durationMs = duration)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            val duration = System.currentTimeMillis() - startTime
            Logger.w("Action timed out after ${duration}ms: ${action.type}")
            ActionResult.failure(
                "Action timed out: ${action.description}",
                error = "Timeout after ${Constants.ACTION_TIMEOUT_MS}ms",
                durationMs = duration
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.e("Action failed: ${action.type}", e)
            ActionResult.failure(
                "Action failed: ${action.description}",
                error = e.message,
                durationMs = duration
            )
        }
    }

    // ── Action Implementations ──

    private suspend fun executeOpenApp(action: PlanModels.PlannedAction): ActionResult {
        val packageName = action.getParam("package")
        val appName = action.getParam("app_name")

        val targetPackage = packageName ?: resolvePackageName(appName)

        if (targetPackage == null) {
            return ActionResult.failure(
                "Cannot find app: ${appName ?: packageName}",
                error = "Package not found"
            )
        }

        return withContext(Dispatchers.Main) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    Logger.i("Launched app: $targetPackage")
                    ActionResult.success("Opened app: $targetPackage")
                } else {
                    ActionResult.failure(
                        "No launch intent for: $targetPackage",
                        error = "App may not have a launchable activity"
                    )
                }
            } catch (e: Exception) {
                ActionResult.failure("Failed to launch: $targetPackage", error = e.message)
            }
        }
    }

    private suspend fun executeTap(action: PlanModels.PlannedAction): ActionResult {
        ensureAccessibilityActive()

        val text = action.getParam("text")
        val contentDesc = action.getParam("content_description")
        val viewId = action.getParam("view_id")
        val xStr = action.getParam("x")
        val yStr = action.getParam("y")

        if (xStr != null && yStr != null) {
            val x = xStr.toFloatOrNull() ?: 50f
            val y = yStr.toFloatOrNull() ?: 50f
            val result = AccessibilityActions.tapAtScreenCoordinates(x, y)
            return if (result) {
                ActionResult.success("Tapped coordinates ($x%, $y%)")
            } else {
                ActionResult.failure("Failed to tap coordinates")
            }
        }

        val node = when {
            !text.isNullOrBlank() -> NodeFinder.findClickableByText(text)
            !contentDesc.isNullOrBlank() -> NodeFinder.findByContentDescription(contentDesc)
            !viewId.isNullOrBlank() -> NodeFinder.findByViewId(viewId)
            else -> return ActionResult.failure("Tap: no target specified", error = "Missing text/content_description/view_id/x/y param")
        }

        if (node == null) {
            return ActionResult.failure(
                "Cannot find element: ${text ?: contentDesc ?: viewId}",
                error = "Node not found in accessibility tree"
            )
        }

        // Try standard click first, fall back to gesture
        val clicked = AccessibilityActions.tapNode(node)
        return if (clicked) {
            ActionResult.success("Tapped: ${text ?: contentDesc ?: viewId}")
        } else {
            // Fallback: tap at node center via gesture
            val gestureResult = AccessibilityActions.tapAtNodeCenter(node)
            if (gestureResult) {
                ActionResult.success("Tapped (gesture): ${text ?: contentDesc ?: viewId}")
            } else {
                ActionResult.failure("Tap failed on: ${text ?: contentDesc ?: viewId}")
            }
        }
    }

    private suspend fun executeLongPress(action: PlanModels.PlannedAction): ActionResult {
        ensureAccessibilityActive()
        val text = action.getParam("text")
        val node = NodeFinder.findByText(text ?: "")
            ?: return ActionResult.failure("Cannot find element for long press: $text")

        val result = AccessibilityActions.longPressNode(node)
        return if (result) {
            ActionResult.success("Long pressed: $text")
        } else {
            ActionResult.failure("Long press failed on: $text")
        }
    }

    private suspend fun executeSwipe(action: PlanModels.PlannedAction): ActionResult {
        ensureAccessibilityActive()
        val direction = action.getParam("direction") ?: "up"
        val result = AccessibilityActions.swipeDirection(direction)
        return if (result) {
            ActionResult.success("Swiped $direction")
        } else {
            ActionResult.failure("Swipe $direction failed")
        }
    }

    private suspend fun executeTypeText(action: PlanModels.PlannedAction): ActionResult {
        ensureAccessibilityActive()
        val text = action.getParam("text")
            ?: return ActionResult.failure("Type: no text specified")

        val editNode = NodeFinder.findEditableNode()
            ?: return ActionResult.failure("No editable field found on screen")

        val result = AccessibilityActions.typeText(editNode, text)
        return if (result) {
            ActionResult.success("Typed: ${text.take(50)}${if (text.length > 50) "…" else ""}")
        } else {
            ActionResult.failure("Failed to type text")
        }
    }

    private suspend fun executeWait(action: PlanModels.PlannedAction): ActionResult {
        val ms = action.getParam("ms")?.toLongOrNull() ?: Constants.ACTION_WAIT_DEFAULT_MS
        val cappedMs = ms.coerceIn(100, 15_000) // Cap between 100ms and 15s
        Logger.d("Waiting ${cappedMs}ms")
        delay(cappedMs)
        return ActionResult.success("Waited ${cappedMs}ms")
    }

    private fun executeBack(): ActionResult {
        ensureAccessibilityActive()
        val service = AstroAccessibilityService.instance
            ?: return ActionResult.failure("Accessibility not active")
        val result = service.pressBack()
        return if (result) ActionResult.success("Pressed Back") else ActionResult.failure("Back press failed")
    }

    private fun executeHome(): ActionResult {
        ensureAccessibilityActive()
        val service = AstroAccessibilityService.instance
            ?: return ActionResult.failure("Accessibility not active")
        val result = service.pressHome()
        return if (result) ActionResult.success("Pressed Home") else ActionResult.failure("Home press failed")
    }

    private suspend fun executeScroll(action: PlanModels.PlannedAction): ActionResult {
        ensureAccessibilityActive()
        val direction = action.getParam("direction") ?: "down"
        val scrollNode = NodeFinder.findScrollableNode()

        if (scrollNode != null) {
            val result = when (direction.lowercase()) {
                "down", "forward" -> AccessibilityActions.scrollForward(scrollNode)
                "up", "backward" -> AccessibilityActions.scrollBackward(scrollNode)
                else -> false
            }
            return if (result) ActionResult.success("Scrolled $direction") else ActionResult.failure("Scroll $direction failed")
        }

        // Fallback: use swipe gesture for scrolling
        val swipeResult = AccessibilityActions.swipeDirection(
            if (direction == "down") "up" else "down" // Swipe opposite to scroll direction
        )
        return if (swipeResult) {
            ActionResult.success("Scrolled $direction (via swipe)")
        } else {
            ActionResult.failure("Scroll failed — no scrollable container found")
        }
    }

    private suspend fun executeReadScreen(): ActionResult {
        // Try accessibility tree first (faster, no permissions needed)
        val accessibilityText = NodeFinder.getScreenText()
        if (accessibilityText.isNotBlank()) {
            Logger.i("Screen read via accessibility: ${accessibilityText.length} chars")
            return ActionResult.success(
                "Screen read (accessibility)",
                data = accessibilityText
            )
        }

        // Fall back to OCR if accessibility didn't find much
        val ocrResult = OcrEngine.captureAndRecognize()
        return if (!ocrResult.isEmpty) {
            ActionResult.success(
                "Screen read (OCR): ${ocrResult.blocks.size} blocks",
                data = ocrResult.fullText
            )
        } else {
            ActionResult.failure("Could not read screen content")
        }
    }

    // ── Helpers ──

    private fun ensureAccessibilityActive() {
        if (!AstroAccessibilityService.isActive) {
            Logger.w("Accessibility service is not active — action may fail")
        }
    }

    /**
     * Resolves an app name to a package name by searching installed packages.
     */
    private fun resolvePackageName(appName: String?): String? {
        if (appName.isNullOrBlank()) return null

        val pm = context.packageManager
        
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = pm.queryIntentActivities(intent, 0)

        // Exact match first
        resolveInfoList.find { info ->
            info.loadLabel(pm).toString().equals(appName, ignoreCase = true)
        }?.let { return it.activityInfo.packageName }

        // Partial match
        resolveInfoList.find { info ->
            info.loadLabel(pm).toString().contains(appName, ignoreCase = true)
        }?.let { return it.activityInfo.packageName }

        // Common app name to package mappings
        val commonApps = mapOf(
            "chatgpt" to "com.openai.chatgpt",
            "chrome" to "com.android.chrome",
            "settings" to "com.android.settings",
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "gmail" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "camera" to "com.bbk.camera", // Vivo camera fallback
            "messages" to "com.google.android.apps.messaging",
            "phone" to "com.android.dialer",
            "calculator" to "com.google.android.calculator",
            "clock" to "com.google.android.deskclock",
            "calendar" to "com.google.android.calendar",
            "files" to "com.google.android.apps.nbu.files",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "instagram" to "com.instagram.android",
            "telegram" to "org.telegram.messenger",
            "spotify" to "com.spotify.music",
            "netflix" to "com.netflix.mediaclient"
        )

        return commonApps[appName.lowercase()]
    }
}
