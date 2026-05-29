package com.astrodham.astroagent.state

import com.astrodham.astroagent.executor.ActionResult
import com.astrodham.astroagent.ocr.OcrResult
import com.astrodham.astroagent.planner.PlanModels

/**
 * Immutable state data classes for the agent system.
 * Using data classes ensures clean state comparisons and copy-on-write updates.
 */

/**
 * Top-level state of the AstroAgent system.
 */
data class AgentState(
    /** Package name of the currently foreground app */
    val currentApp: String? = null,

    /** Text content currently visible on screen (from OCR or accessibility) */
    val currentScreenText: String? = null,

    /** The last action that was executed */
    val lastAction: PlanModels.PlannedAction? = null,

    /** Result of the last executed action */
    val lastActionResult: ActionResult? = null,

    /** Progress through the current workflow (0.0 to 1.0) */
    val workflowProgress: Float = 0f,

    /** Number of retries attempted for the current action */
    val retryCount: Int = 0,

    /** Whether the agent is currently executing a workflow */
    val isExecuting: Boolean = false,

    /** Whether the agent is currently listening for voice input */
    val isListening: Boolean = false,

    /** Whether the agent is planning (waiting for AI response) */
    val isPlanning: Boolean = false,

    /** The current workflow goal/description */
    val currentGoal: String? = null,

    /** Last OCR results */
    val ocrResult: OcrResult? = null,

    /** Human-readable status message for the UI */
    val statusMessage: String = "Idle — Ready for commands",

    /** Any error message to display */
    val errorMessage: String? = null
) {
    /**
     * Overall phase of the agent.
     */
    val phase: AgentPhase
        get() = when {
            errorMessage != null -> AgentPhase.ERROR
            isPlanning -> AgentPhase.PLANNING
            isExecuting -> AgentPhase.EXECUTING
            isListening -> AgentPhase.LISTENING
            currentGoal != null && workflowProgress >= 1f -> AgentPhase.DONE
            else -> AgentPhase.IDLE
        }
}

/**
 * Possible phases of the agent lifecycle.
 */
enum class AgentPhase {
    IDLE,
    LISTENING,
    PLANNING,
    EXECUTING,
    DONE,
    ERROR
}
