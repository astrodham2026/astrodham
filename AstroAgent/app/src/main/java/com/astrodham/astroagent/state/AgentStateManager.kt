package com.astrodham.astroagent.state

import com.astrodham.astroagent.executor.ActionResult
import com.astrodham.astroagent.ocr.OcrResult
import com.astrodham.astroagent.planner.PlanModels
import com.astrodham.astroagent.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central state manager for the AstroAgent system.
 *
 * Provides thread-safe state updates via Mutex and exposes
 * the state as a StateFlow for reactive UI observation.
 *
 * Single source of truth for:
 * - Current app and screen content
 * - Workflow progress
 * - Action results
 * - Error states
 */
class AgentStateManager {

    private val _state = MutableStateFlow(AgentState())
    val state: StateFlow<AgentState> = _state.asStateFlow()

    /** Thread-safety for state updates */
    private val mutex = Mutex()

    /** Current state snapshot (non-reactive) */
    val currentState: AgentState get() = _state.value

    // ── State Update Methods ──

    suspend fun updateCurrentApp(packageName: String?) = update {
        copy(currentApp = packageName)
    }

    suspend fun updateScreenText(text: String?) = update {
        copy(currentScreenText = text)
    }

    suspend fun updateOcrResult(result: OcrResult?) = update {
        copy(
            ocrResult = result,
            currentScreenText = result?.fullText
        )
    }

    suspend fun updateLastAction(action: PlanModels.PlannedAction?) = update {
        copy(lastAction = action)
    }

    suspend fun updateLastActionResult(result: ActionResult?) = update {
        copy(lastActionResult = result)
    }

    suspend fun updateWorkflowProgress(progress: Float) = update {
        copy(workflowProgress = progress.coerceIn(0f, 1f))
    }

    suspend fun updateRetryCount(count: Int) = update {
        copy(retryCount = count)
    }

    suspend fun setExecuting(executing: Boolean, goal: String? = null) = update {
        copy(
            isExecuting = executing,
            currentGoal = goal ?: currentGoal,
            statusMessage = if (executing) "⚡ Executing workflow…" else "Idle — Ready for commands",
            errorMessage = if (executing) null else errorMessage
        )
    }

    suspend fun setListening(listening: Boolean) = update {
        copy(
            isListening = listening,
            statusMessage = if (listening) "🎤 Listening…" else statusMessage
        )
    }

    suspend fun setPlanning(planning: Boolean) = update {
        copy(
            isPlanning = planning,
            statusMessage = if (planning) "🧠 Planning actions…" else statusMessage
        )
    }

    suspend fun setError(message: String) = update {
        copy(
            errorMessage = message,
            statusMessage = "❌ Error: $message",
            isExecuting = false,
            isPlanning = false
        )
    }

    suspend fun setDone(summary: String? = null) = update {
        copy(
            isExecuting = false,
            isPlanning = false,
            workflowProgress = 1f,
            statusMessage = "✅ ${summary ?: "Workflow complete"}",
            errorMessage = null
        )
    }

    suspend fun updateStatus(message: String) = update {
        copy(statusMessage = message)
    }

    /**
     * Resets the agent state for a new workflow.
     */
    suspend fun reset() = update {
        AgentState()
    }

    // ── Private ──

    /**
     * Thread-safe state update using Mutex.
     * Applies the transform function to the current state.
     */
    private suspend fun update(transform: AgentState.() -> AgentState) {
        mutex.withLock {
            val newState = _state.value.transform()
            _state.value = newState
            Logger.d("State updated: phase=${newState.phase}, app=${newState.currentApp}")
        }
    }
}
