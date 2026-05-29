package com.astrodham.astroagent.planner

import com.astrodham.astroagent.executor.ActionExecutor
import com.astrodham.astroagent.executor.ActionResult
import com.astrodham.astroagent.executor.RetryManager
import com.astrodham.astroagent.state.AgentStateManager
import com.astrodham.astroagent.state.MemoryManager
import com.astrodham.astroagent.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Orchestrates the execution of a complete workflow (plan).
 *
 * Flow:
 * 1. Receives a Plan from AgentPlanner
 * 2. Iterates through actions sequentially
 * 3. For each action: execute → check result → update state → continue or retry/replan
 * 4. Emits workflow events for UI observation
 * 5. Handles cancellation gracefully
 *
 * Supports:
 * - Sequential action execution
 * - Per-action retry with exponential backoff
 * - Replanning via AgentPlanner when retries exhausted
 * - Progress tracking
 * - Cancellation
 */
class WorkflowEngine(
    private val executor: ActionExecutor,
    private val planner: AgentPlanner,
    private val stateManager: AgentStateManager,
    private val memoryManager: MemoryManager,
    private val retryManager: RetryManager
) {
    /** Workflow execution events */
    private val _events = MutableSharedFlow<WorkflowEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<WorkflowEvent> = _events.asSharedFlow()

    /**
     * Executes a complete workflow plan.
     *
     * @param plan The plan to execute
     * @return Final result summary
     */
    suspend fun execute(plan: PlanModels.Plan): WorkflowResult {
        Logger.i("▶ Starting workflow: ${plan.goal} (${plan.actionCount} actions)")
        retryManager.resetAll()

        stateManager.setExecuting(true, plan.goal)
        emitEvent(WorkflowEvent.Started(plan))

        var currentPlan = plan
        var actionIndex = 0
        val results = mutableListOf<ActionResult>()
        var lastScreenData: String? = null

        try {
            while (actionIndex < currentPlan.actions.size) {
                // Check for cancellation
                if (!coroutineContext.isActive) {
                    throw CancellationException("Workflow cancelled")
                }

                val action = currentPlan.actions[actionIndex]
                val actionKey = "step_${actionIndex}"
                val progress = actionIndex.toFloat() / currentPlan.actions.size

                stateManager.updateWorkflowProgress(progress)
                stateManager.updateLastAction(action)
                stateManager.updateStatus("⚡ Step ${actionIndex + 1}/${currentPlan.actionCount}: ${action.description}")
                emitEvent(WorkflowEvent.ActionStarted(actionIndex, action))

                // Execute the action
                val result = executor.execute(action)
                results.add(result)

                stateManager.updateLastActionResult(result)

                if (result.success) {
                    // Success — record and move to next action
                    Logger.i("✓ Step ${actionIndex + 1} succeeded: ${result.message}")
                    memoryManager.recordCompletedAction(action.description)
                    retryManager.resetAction(actionKey)

                    // If the action produced data (e.g., read_screen), store it
                    if (result.data != null) {
                        lastScreenData = result.data
                        memoryManager.recordScreenState(result.data, stateManager.currentState.currentApp)
                    }

                    emitEvent(WorkflowEvent.ActionCompleted(actionIndex, action, result))
                    actionIndex++
                } else {
                    // Failure — try retry
                    Logger.w("✗ Step ${actionIndex + 1} failed: ${result.message}")
                    emitEvent(WorkflowEvent.ActionFailed(actionIndex, action, result))

                    if (retryManager.canRetry(actionKey)) {
                        val retryCount = retryManager.recordRetry(actionKey)
                        val retryDelay = retryManager.getRetryDelay(actionKey)

                        Logger.i("↻ Retrying step ${actionIndex + 1} (attempt $retryCount/${retryManager.maxRetries}) after ${retryDelay}ms")
                        stateManager.updateRetryCount(retryCount)
                        emitEvent(WorkflowEvent.Retrying(actionIndex, retryCount))

                        delay(retryDelay)
                        // Don't increment actionIndex — retry same action
                    } else {
                        // Max retries exhausted — attempt replanning
                        Logger.w("Max retries exhausted for step ${actionIndex + 1}. Attempting recovery plan.")

                        val screenState = lastScreenData
                            ?: com.astrodham.astroagent.accessibility.NodeFinder.getScreenText()

                        val recoveryResult = planner.createRecoveryPlan(
                            failedAction = action,
                            errorMessage = result.error ?: "Unknown error",
                            screenState = screenState
                        )

                        if (recoveryResult.isSuccess) {
                            val recoveryPlan = recoveryResult.getOrThrow()
                            if (!recoveryPlan.isEmpty) {
                                Logger.i("Recovery plan received: ${recoveryPlan.actionCount} actions")
                                // Replace remaining actions with recovery plan
                                val completedActions = currentPlan.actions.take(actionIndex)
                                currentPlan = currentPlan.copy(
                                    actions = completedActions + recoveryPlan.actions
                                )
                                retryManager.resetAction(actionKey)
                                emitEvent(WorkflowEvent.Replanned(recoveryPlan))
                                // Continue from the current index (first recovery action)
                            } else {
                                // Recovery plan is empty — skip and continue
                                Logger.w("Empty recovery plan, skipping failed action")
                                actionIndex++
                            }
                        } else {
                            // Recovery planning failed — skip action
                            Logger.e("Recovery planning failed, skipping action")
                            actionIndex++
                        }
                    }
                }
            }

            // Workflow complete
            stateManager.updateWorkflowProgress(1f)
            stateManager.setDone("Workflow complete: ${plan.goal}")

            val workflowResult = WorkflowResult(
                success = true,
                message = "Completed ${results.count { it.success }}/${results.size} actions",
                results = results,
                lastScreenData = lastScreenData
            )
            emitEvent(WorkflowEvent.Completed(workflowResult))
            Logger.i("✅ Workflow complete: ${workflowResult.message}")
            return workflowResult

        } catch (e: CancellationException) {
            Logger.i("Workflow cancelled")
            stateManager.setError("Workflow cancelled")
            val workflowResult = WorkflowResult(false, "Cancelled", results)
            emitEvent(WorkflowEvent.Cancelled)
            return workflowResult

        } catch (e: Exception) {
            Logger.e("Workflow failed with exception", e)
            stateManager.setError(e.message ?: "Unknown error")
            val workflowResult = WorkflowResult(false, "Error: ${e.message}", results)
            emitEvent(WorkflowEvent.Error(e))
            return workflowResult
        }
    }

    private fun emitEvent(event: WorkflowEvent) {
        _events.tryEmit(event)
    }
}

// ── Result and Event Models ──

data class WorkflowResult(
    val success: Boolean,
    val message: String,
    val results: List<ActionResult>,
    val lastScreenData: String? = null
) {
    val successCount: Int get() = results.count { it.success }
    val failureCount: Int get() = results.count { !it.success }
}

sealed class WorkflowEvent {
    data class Started(val plan: PlanModels.Plan) : WorkflowEvent()
    data class ActionStarted(val index: Int, val action: PlanModels.PlannedAction) : WorkflowEvent()
    data class ActionCompleted(val index: Int, val action: PlanModels.PlannedAction, val result: ActionResult) : WorkflowEvent()
    data class ActionFailed(val index: Int, val action: PlanModels.PlannedAction, val result: ActionResult) : WorkflowEvent()
    data class Retrying(val index: Int, val attempt: Int) : WorkflowEvent()
    data class Replanned(val newPlan: PlanModels.Plan) : WorkflowEvent()
    data class Completed(val result: WorkflowResult) : WorkflowEvent()
    data object Cancelled : WorkflowEvent()
    data class Error(val exception: Exception) : WorkflowEvent()
}
