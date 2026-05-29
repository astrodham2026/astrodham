package com.astrodham.astroagent.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astrodham.astroagent.accessibility.AstroAccessibilityService
import com.astrodham.astroagent.accessibility.NodeFinder
import com.astrodham.astroagent.api.CodemaxRepository
import com.astrodham.astroagent.executor.ActionExecutor
import com.astrodham.astroagent.executor.RetryManager
import com.astrodham.astroagent.planner.AgentPlanner
import com.astrodham.astroagent.planner.WorkflowEngine
import com.astrodham.astroagent.planner.WorkflowResult
import com.astrodham.astroagent.state.AgentState
import com.astrodham.astroagent.state.AgentStateManager
import com.astrodham.astroagent.state.MemoryManager
import com.astrodham.astroagent.util.Logger
import com.astrodham.astroagent.voice.TextToSpeechManager
import com.astrodham.astroagent.voice.VoiceCommandManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Primary ViewModel coordinating all AstroAgent modules.
 *
 * Responsibilities:
 * - Accept user commands (text or voice)
 * - Orchestrate: interpret → plan → execute pipeline
 * - Expose state for UI observation
 * - Manage lifecycle-scoped coroutines
 * - Handle cancellation
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // ── Dependencies (manual DI — production apps should use Hilt/Dagger) ──
    private val repository = CodemaxRepository()
    private val memoryManager = MemoryManager()
    private val stateManager = AgentStateManager()
    private val retryManager = RetryManager()
    private val planner = AgentPlanner(repository, memoryManager)
    private val executor = ActionExecutor(application.applicationContext)
    private val workflowEngine = WorkflowEngine(executor, planner, stateManager, memoryManager, retryManager)

    val voiceManager = VoiceCommandManager(application.applicationContext)
    val ttsManager = TextToSpeechManager(application.applicationContext)

    // ── State Flows ──

    val agentState: StateFlow<AgentState> = stateManager.state.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AgentState()
    )

    private val _logs = MutableStateFlow<List<Logger.LogEntry>>(emptyList())
    val logs: StateFlow<List<Logger.LogEntry>> = _logs.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    /** Current executing workflow job — used for cancellation */
    private var workflowJob: Job? = null

    init {
        // Collect log entries for UI display
        viewModelScope.launch {
            Logger.logFlow.collect { entry ->
                val current = _logs.value.toMutableList()
                current.add(entry)
                // Cap at 200 entries in UI
                if (current.size > 200) {
                    _logs.value = current.takeLast(200)
                } else {
                    _logs.value = current
                }
            }
        }

        // Collect voice recognition results
        viewModelScope.launch {
            voiceManager.recognizedText.collect { text ->
                Logger.i("Voice command received: $text")
                executeCommand(text)
            }
        }

        // Monitor foreground app changes
        viewModelScope.launch {
            AstroAccessibilityService.foregroundPackage.collect { packageName ->
                if (packageName != null) {
                    stateManager.updateCurrentApp(packageName)
                }
            }
        }

        // Collect Workflow Events for Chat UI updates
        viewModelScope.launch {
            workflowEngine.events.collect { event ->
                when (event) {
                    is com.astrodham.astroagent.planner.WorkflowEvent.ActionStarted -> {
                        // Optional: Too noisy for chat? Maybe only log errors or important steps.
                        // addChatMessage("Executing step ${event.index + 1}: ${event.action.description}", false)
                    }
                    is com.astrodham.astroagent.planner.WorkflowEvent.ActionFailed -> {
                        addChatMessage("❌ Failed step ${event.index + 1}: ${event.result.message}", false)
                    }
                    is com.astrodham.astroagent.planner.WorkflowEvent.Replanned -> {
                        addChatMessage("🧠 Replanning to recover from failure...", false)
                    }
                    is com.astrodham.astroagent.planner.WorkflowEvent.Error -> {
                        addChatMessage("❌ Error: ${event.exception.message}", false)
                    }
                    else -> {}
                }
            }
        }

        Logger.i("MainViewModel initialized — AstroAgent ready")
    }

    /**
     * Executes a user command through the full pipeline:
     * interpret → plan → execute → summarize
     *
     * @param command The user's natural language command
     */
    fun executeCommand(command: String) {
        if (command.isBlank()) {
            Logger.w("Empty command, ignoring")
            return
        }

        // Cancel any existing workflow
        workflowJob?.cancel()

        addChatMessage(command, isUser = true)

        workflowJob = viewModelScope.launch(Dispatchers.Default) {
            Logger.i("═══ New Command: $command ═══")
            addChatMessage("Thinking...", isUser = false)

            try {
                // Phase 1: Planning
                stateManager.setPlanning(true)

                // Get current screen state for context
                val screenState = getScreenState()

                val planResult = planner.createPlan(
                    userIntent = command,
                    screenState = screenState
                )

                stateManager.setPlanning(false)

                if (planResult.isFailure) {
                    val error = planResult.exceptionOrNull()?.message ?: "Planning failed"
                    stateManager.setError(error)
                    ttsManager.speak("Sorry, I couldn't plan that action. $error")
                    return@launch
                }

                val plan = planResult.getOrThrow()
                addChatMessage("Created a plan with ${plan.actionCount} steps. Executing...", isUser = false)

                // Phase 2: Execution
                val workflowResult = workflowEngine.execute(plan)

                // Phase 3: Summary
                handleWorkflowResult(command, workflowResult)

            } catch (e: kotlinx.coroutines.CancellationException) {
                Logger.i("Command execution cancelled")
                addChatMessage("Workflow cancelled.", isUser = false)
            } catch (e: Exception) {
                Logger.e("Command execution failed", e)
                stateManager.setError(e.message ?: "Unknown error")
                addChatMessage("An error occurred: ${e.message}", isUser = false)
                ttsManager.speak("An error occurred: ${e.message}")
            }
        }
    }

    /**
     * Cancels any running workflow.
     */
    fun cancelWorkflow() {
        workflowJob?.cancel()
        workflowJob = null
        viewModelScope.launch {
            stateManager.setError("Cancelled by user")
        }
        Logger.i("Workflow cancelled by user")
    }

    /**
     * Clears all logs from the UI.
     */
    fun clearLogs() {
        _logs.value = emptyList()
        _chatMessages.value = emptyList()
        Logger.clear()
    }

    private fun addChatMessage(text: String, isUser: Boolean) {
        val msg = ChatMessage(text = text, isUser = isUser)
        _chatMessages.value = _chatMessages.value + msg
    }

    /**
     * Resets the agent state for a fresh start.
     */
    fun resetAgent() {
        cancelWorkflow()
        viewModelScope.launch {
            stateManager.reset()
            memoryManager.clear()
            repository.clearHistory()
            retryManager.resetAll()
        }
        Logger.i("Agent reset to initial state")
    }

    override fun onCleared() {
        super.onCleared()
        workflowJob?.cancel()
        voiceManager.destroy()
        ttsManager.shutdown()
        Logger.i("MainViewModel cleared")
    }

    // ── Private Helpers ──

    /**
     * Gets the current screen state for planning context.
     * Prefers accessibility tree text; falls back to OCR.
     */
    private fun getScreenState(): String {
        return try {
            val accessibilityText = NodeFinder.getScreenText()
            if (accessibilityText.isNotBlank()) {
                accessibilityText
            } else {
                "[Screen content not available — accessibility service may not be active]"
            }
        } catch (e: Exception) {
            Logger.w("Failed to get screen state: ${e.message}")
            "[Screen state unavailable]"
        }
    }

    /**
     * Handles the result of a completed workflow.
     */
    private fun handleWorkflowResult(command: String, result: WorkflowResult) {
        if (result.success) {
            val summary = "Completed: ${result.successCount} actions succeeded"
            Logger.i("═══ Workflow Complete: $summary ═══")

            // If we have screen data from the last read_screen, offer to summarize
            if (result.lastScreenData != null) {
                Logger.i("Final screen content available (${result.lastScreenData.length} chars)")
                ttsManager.speak("Done. $summary")
            } else {
                ttsManager.speak("Done. $summary")
            }
            addChatMessage("✅ $summary", isUser = false)
        } else {
            Logger.w("═══ Workflow Failed: ${result.message} ═══")
            ttsManager.speak("The workflow encountered issues. ${result.message}")
            addChatMessage("❌ Workflow Failed: ${result.message}", isUser = false)
        }
    }
}
