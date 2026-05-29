package com.astrodham.astroagent.executor

import com.astrodham.astroagent.util.Constants
import com.astrodham.astroagent.util.Logger

/**
 * Manages retry logic for failed actions with exponential backoff.
 *
 * Tracks per-action retry counts and determines whether
 * an action should be retried or escalated to replanning.
 */
class RetryManager {

    /** Retry counts keyed by action description/index */
    private val retryCounts = mutableMapOf<String, Int>()

    /** Max retries per action before escalating */
    val maxRetries: Int = Constants.MAX_ACTION_RETRIES

    /**
     * Records a retry attempt for an action.
     *
     * @param actionKey Unique key for the action (e.g., "step_3" or action description)
     * @return The new retry count
     */
    fun recordRetry(actionKey: String): Int {
        val count = (retryCounts[actionKey] ?: 0) + 1
        retryCounts[actionKey] = count
        Logger.d("Retry recorded for '$actionKey': attempt $count/$maxRetries")
        return count
    }

    /**
     * Checks if more retries are available for an action.
     *
     * @param actionKey Unique key for the action
     * @return true if the action can be retried
     */
    fun canRetry(actionKey: String): Boolean {
        val count = retryCounts[actionKey] ?: 0
        return count < maxRetries
    }

    /**
     * Gets the current retry count for an action.
     */
    fun getRetryCount(actionKey: String): Int {
        return retryCounts[actionKey] ?: 0
    }

    /**
     * Calculates the delay before the next retry using exponential backoff.
     *
     * @param actionKey Unique key for the action
     * @return Delay in milliseconds
     */
    fun getRetryDelay(actionKey: String): Long {
        val count = retryCounts[actionKey] ?: 0
        return Constants.RETRY_BASE_DELAY_MS * (1L shl count) // 1s, 2s, 4s, 8s...
    }

    /**
     * Resets retry count for a specific action (e.g., after success).
     */
    fun resetAction(actionKey: String) {
        retryCounts.remove(actionKey)
    }

    /**
     * Resets all retry counts (e.g., for a new workflow).
     */
    fun resetAll() {
        retryCounts.clear()
        Logger.d("All retry counts reset")
    }

    /**
     * Gets total retries across all actions.
     */
    fun getTotalRetries(): Int = retryCounts.values.sum()
}
