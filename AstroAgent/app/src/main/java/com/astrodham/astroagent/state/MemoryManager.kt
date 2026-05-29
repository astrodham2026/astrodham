package com.astrodham.astroagent.state

import com.astrodham.astroagent.ocr.OcrResult
import com.astrodham.astroagent.util.Constants
import com.astrodham.astroagent.util.Logger

/**
 * Manages conversation and context memory for the AI planner.
 *
 * Tracks:
 * - Recent screen states for temporal context
 * - Completed actions for workflow awareness
 * - Custom key-value context pairs
 * - Provides serialized context strings for API calls
 */
class MemoryManager {

    /** Recent screen states (most recent last) */
    private val screenHistory = mutableListOf<ScreenSnapshot>()

    /** Completed actions in the current workflow */
    private val completedActions = mutableListOf<String>()

    /** Custom context key-value store */
    private val contextStore = mutableMapOf<String, String>()

    /**
     * Records a screen state snapshot.
     */
    fun recordScreenState(text: String, appPackage: String?) {
        screenHistory.add(ScreenSnapshot(
            text = text.take(1000), // Cap text length to avoid huge context
            appPackage = appPackage,
            timestamp = System.currentTimeMillis()
        ))

        // Cap history size
        while (screenHistory.size > Constants.MAX_SCREEN_STATE_HISTORY) {
            screenHistory.removeAt(0)
        }
    }

    /**
     * Records a screen state from an OCR result.
     */
    fun recordOcrResult(ocrResult: OcrResult, appPackage: String?) {
        recordScreenState(ocrResult.fullText, appPackage)
    }

    /**
     * Records a completed action description.
     */
    fun recordCompletedAction(description: String) {
        completedActions.add(description)
        Logger.d("Memory: recorded action — $description")
    }

    /**
     * Stores a custom context value.
     */
    fun setContext(key: String, value: String) {
        contextStore[key] = value
    }

    /**
     * Retrieves a custom context value.
     */
    fun getContext(key: String): String? = contextStore[key]

    /**
     * Builds a serialized context string for inclusion in AI API calls.
     * Includes recent screen history and completed actions.
     */
    fun buildContextString(): String {
        return buildString {
            // Completed actions
            if (completedActions.isNotEmpty()) {
                appendLine("COMPLETED ACTIONS:")
                completedActions.forEachIndexed { index, action ->
                    appendLine("  ${index + 1}. $action")
                }
                appendLine()
            }

            // Recent screen history
            if (screenHistory.isNotEmpty()) {
                appendLine("RECENT SCREEN HISTORY:")
                screenHistory.takeLast(3).forEachIndexed { index, snapshot ->
                    appendLine("  Screen ${index + 1} (${snapshot.appPackage ?: "unknown"}):")
                    appendLine("  ${snapshot.text.take(300)}")
                    appendLine()
                }
            }

            // Custom context
            if (contextStore.isNotEmpty()) {
                appendLine("ADDITIONAL CONTEXT:")
                contextStore.forEach { (key, value) ->
                    appendLine("  $key: $value")
                }
            }
        }.trim()
    }

    /**
     * Returns the most recent screen text.
     */
    fun getLastScreenText(): String? {
        return screenHistory.lastOrNull()?.text
    }

    /**
     * Clears all memory for a new workflow.
     */
    fun clear() {
        screenHistory.clear()
        completedActions.clear()
        contextStore.clear()
        Logger.i("Memory cleared")
    }

    /**
     * Returns the number of actions completed in the current workflow.
     */
    fun getCompletedActionCount(): Int = completedActions.size
}

/**
 * A snapshot of the screen state at a point in time.
 */
data class ScreenSnapshot(
    val text: String,
    val appPackage: String?,
    val timestamp: Long
)
