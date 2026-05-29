package com.astrodham.astroagent.planner

import com.astrodham.astroagent.api.CodemaxRepository
import com.astrodham.astroagent.state.MemoryManager
import com.astrodham.astroagent.util.Logger
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * AI-powered planner that converts user intent into executable action plans.
 *
 * Flow:
 * 1. Receives user intent + current screen state + memory context
 * 2. Sends structured prompt to Codemax.pro via CodemaxRepository
 * 3. Parses the JSON response into a list of PlannedActions
 * 4. Returns a Plan ready for the ActionExecutor
 *
 * Also handles replanning when actions fail (error recovery).
 */
class AgentPlanner(
    private val repository: CodemaxRepository,
    private val memoryManager: MemoryManager
) {
    private val gson = Gson()

    /**
     * Generates an action plan from a user's intent.
     *
     * @param userIntent What the user wants to accomplish
     * @param screenState Current visible screen content
     * @return A Plan with ordered actions, or failure
     */
    suspend fun createPlan(
        userIntent: String,
        screenState: String
    ): Result<PlanModels.Plan> {
        Logger.i("Creating plan for: $userIntent")

        val memoryContext = memoryManager.buildContextString()

        val result = repository.plan(
            userIntent = userIntent,
            screenState = screenState,
            memoryContext = memoryContext
        )

        return result.fold(
            onSuccess = { responseText ->
                try {
                    val actions = parseActions(responseText)
                    if (actions.isEmpty()) {
                        Logger.w("AI returned empty action plan")
                        Result.failure(PlanningException("AI returned no actions"))
                    } else {
                        val plan = PlanModels.Plan(
                            id = UUID.randomUUID().toString().take(8),
                            actions = actions,
                            goal = userIntent
                        )
                        Logger.i("Plan created: ${plan.actionCount} actions")
                        actions.forEachIndexed { index, action ->
                            Logger.d("  Step ${index + 1}: [${action.type}] ${action.description}")
                        }
                        Result.success(plan)
                    }
                } catch (e: Exception) {
                    Logger.e("Failed to parse action plan", e)
                    Result.failure(PlanningException("Failed to parse AI response: ${e.message}"))
                }
            },
            onFailure = { error ->
                Logger.e("Planning API call failed", error)
                Result.failure(error)
            }
        )
    }

    /**
     * Creates a recovery plan when an action fails.
     * Asks the AI for alternative approaches given the failure context.
     *
     * @param failedAction The action that failed
     * @param errorMessage Why it failed
     * @param screenState Current screen after failure
     * @return Recovery plan, or failure
     */
    suspend fun createRecoveryPlan(
        failedAction: PlanModels.PlannedAction,
        errorMessage: String,
        screenState: String
    ): Result<PlanModels.Plan> {
        Logger.i("Creating recovery plan for failed action: ${failedAction.type}")

        val result = repository.suggestRecovery(
            failedAction = "${failedAction.type}: ${failedAction.description}",
            errorMessage = errorMessage,
            screenState = screenState
        )

        return result.fold(
            onSuccess = { responseText ->
                try {
                    val actions = parseActions(responseText)
                    val plan = PlanModels.Plan(
                        id = "recovery-${UUID.randomUUID().toString().take(8)}",
                        actions = actions,
                        goal = "Recover from: ${failedAction.description}"
                    )
                    Logger.i("Recovery plan created: ${plan.actionCount} actions")
                    Result.success(plan)
                } catch (e: Exception) {
                    Logger.e("Failed to parse recovery plan", e)
                    Result.failure(PlanningException("Failed to parse recovery response"))
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    // ── Private ──

    /**
     * Parses the AI's JSON response into a list of PlannedActions.
     * Handles cases where the response may contain markdown code fences or extra text.
     */
    private fun parseActions(responseText: String): List<PlanModels.PlannedAction> {
        // Strip markdown code fences if present
        val cleanedJson = responseText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        // Find the JSON array in the response
        val jsonStart = cleanedJson.indexOf('[')
        val jsonEnd = cleanedJson.lastIndexOf(']')

        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
            throw PlanningException("No JSON array found in AI response: ${cleanedJson.take(200)}")
        }

        val jsonArray = cleanedJson.substring(jsonStart, jsonEnd + 1)

        return try {
            val type = object : TypeToken<List<PlanModels.PlannedAction>>() {}.type
            gson.fromJson<List<PlanModels.PlannedAction>>(jsonArray, type)
                ?: emptyList()
        } catch (e: JsonSyntaxException) {
            Logger.e("JSON parse error: ${e.message}")
            Logger.d("Raw JSON: $jsonArray")
            throw PlanningException("Invalid JSON in AI response: ${e.message}")
        }
    }
}

class PlanningException(message: String) : Exception(message)
