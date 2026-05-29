package com.astrodham.astroagent.executor

/**
 * Result of executing a single action.
 */
data class ActionResult(
    /** Whether the action succeeded */
    val success: Boolean,

    /** Human-readable description of what happened */
    val message: String,

    /** Any data produced by the action (e.g., OCR text from read_screen) */
    val data: String? = null,

    /** Error details if the action failed */
    val error: String? = null,

    /** Time taken to execute in milliseconds */
    val durationMs: Long = 0
) {
    companion object {
        fun success(message: String, data: String? = null, durationMs: Long = 0) =
            ActionResult(true, message, data, durationMs = durationMs)

        fun failure(message: String, error: String? = null, durationMs: Long = 0) =
            ActionResult(false, message, error = error, durationMs = durationMs)
    }
}
