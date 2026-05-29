package com.astrodham.astroagent.api

import com.astrodham.astroagent.api.models.ApiRequest
import com.astrodham.astroagent.api.models.ApiResponse
import com.astrodham.astroagent.api.models.ConversationMessage
import com.astrodham.astroagent.api.models.MessageParam
import com.astrodham.astroagent.util.Constants
import com.astrodham.astroagent.util.Logger
import com.astrodham.astroagent.util.retryWithBackoff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Repository layer for Codemax.pro API communication.
 *
 * Responsibilities:
 * - Construct API requests with conversation context
 * - Handle retries with exponential backoff
 * - Handle rate limiting (HTTP 429)
 * - Provide fallback behavior when API is unavailable
 * - Manage conversation history for context continuity
 */
class CodemaxRepository {

    private val apiService: CodemaxApiService by lazy { CodemaxClient.getService() }

    // Conversation history for context
    private val conversationHistory = mutableListOf<ConversationMessage>()

    /**
     * Send a user message to the AI and get a response.
     * Includes full conversation history for context.
     *
     * @param userMessage The user's input text
     * @param systemPrompt Optional system prompt override (defaults to planner prompt)
     * @return The AI's text response, or a fallback error message
     */
    suspend fun chat(
        userMessage: String,
        systemPrompt: String = Constants.SYSTEM_PROMPT
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Add user message to history
            addToHistory("user", userMessage)

            val result = retryWithBackoff(
                maxRetries = Constants.MAX_API_RETRIES,
                baseDelayMs = Constants.RETRY_BASE_DELAY_MS
            ) { attempt ->
                Logger.i("Sending message to Codemax.pro (attempt ${attempt + 1})")
                executeRequest(systemPrompt)
            }

            // Add assistant response to history
            val responseText = result.getTextContent()
            addToHistory("assistant", responseText)

            Logger.i("Codemax.pro response received (${result.usage?.outputTokens ?: 0} tokens)")
            Result.success(responseText)
        } catch (e: Exception) {
            Logger.e("Codemax.pro API call failed", e)
            // Remove the user message from history since we didn't get a response
            if (conversationHistory.isNotEmpty() && conversationHistory.last().role == "user") {
                conversationHistory.removeAt(conversationHistory.size - 1)
            }
            Result.failure(e)
        }
    }

    /**
     * Send a planning request — specifically for generating action plans.
     * Uses the system prompt configured for action planning.
     *
     * @param userIntent What the user wants to accomplish
     * @param screenState Current visible screen content (from OCR or accessibility)
     * @param memoryContext Additional context from state manager
     * @return JSON string of planned actions
     */
    suspend fun plan(
        userIntent: String,
        screenState: String,
        memoryContext: String
    ): Result<String> {
        val contextMessage = buildString {
            appendLine("USER INTENT: $userIntent")
            appendLine()
            if (screenState.isNotBlank()) {
                appendLine("CURRENT SCREEN STATE:")
                appendLine(screenState)
                appendLine()
            }
            if (memoryContext.isNotBlank()) {
                appendLine("MEMORY CONTEXT:")
                appendLine(memoryContext)
            }
        }

        return chat(contextMessage, Constants.SYSTEM_PROMPT)
    }

    /**
     * Interpret a user's natural language command to extract intent.
     *
     * @param userMessage Raw user input (voice or text)
     * @return Structured intent interpretation
     */
    suspend fun interpret(userMessage: String): Result<String> {
        val interpretPrompt = """
            You are an intent interpreter. Given the user's natural language command,
            extract and return a JSON object with:
            - "intent": A clear, concise description of what the user wants
            - "target_app": The app to use (if mentioned), or null
            - "action_summary": Brief summary of steps needed
            
            Respond ONLY with the JSON object.
        """.trimIndent()

        return chat(userMessage, interpretPrompt)
    }

    /**
     * Ask the AI for error recovery suggestions.
     */
    suspend fun suggestRecovery(
        failedAction: String,
        errorMessage: String,
        screenState: String
    ): Result<String> {
        val recoveryPrompt = """
            An action failed during automation. Suggest recovery steps as a JSON array of actions.
            
            Failed action: $failedAction
            Error: $errorMessage
            Current screen: $screenState
            
            Respond ONLY with a JSON array of recovery actions, same format as a regular plan.
        """.trimIndent()

        return chat(recoveryPrompt, Constants.SYSTEM_PROMPT)
    }

    /**
     * Clear conversation history (e.g., for a new workflow).
     */
    fun clearHistory() {
        conversationHistory.clear()
        Logger.i("Conversation history cleared")
    }

    /**
     * Get current conversation history size.
     */
    fun getHistorySize(): Int = conversationHistory.size

    // ── Private ──

    private suspend fun executeRequest(systemPrompt: String): ApiResponse {
        val request = ApiRequest(
            model = Constants.CODEMAX_MODEL,
            maxTokens = Constants.CODEMAX_MAX_TOKENS,
            system = systemPrompt,
            messages = conversationHistory.map { it.toMessageParam() }
        )

        val response = apiService.sendMessage(request)

        return when {
            response.isSuccessful -> {
                val body = response.body()
                    ?: throw ApiException("Empty response body from Codemax.pro")

                if (body.isError) {
                    throw ApiException("API error: ${body.error?.message ?: "Unknown"}")
                }
                body
            }
            response.code() == 429 -> {
                // Rate limited — throw to trigger retry with longer delay
                Logger.w("Rate limited by Codemax.pro (429). Will retry after delay.")
                delay(Constants.RATE_LIMIT_RETRY_DELAY_MS)
                throw RateLimitException("Rate limited by Codemax.pro")
            }
            response.code() == 401 || response.code() == 403 -> {
                throw AuthenticationException("Authentication failed. Check your Codemax.pro API key.")
            }
            response.code() >= 500 -> {
                throw ApiException("Codemax.pro server error (${response.code()})")
            }
            else -> {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                throw ApiException("API request failed (${response.code()}): $errorBody")
            }
        }
    }

    private fun addToHistory(role: String, content: String) {
        conversationHistory.add(ConversationMessage(role = role, content = content))

        // Cap history to prevent context window overflow
        while (conversationHistory.size > Constants.MAX_CONVERSATION_HISTORY) {
            conversationHistory.removeAt(0)
        }
    }
}

// ── Custom Exceptions ──

class ApiException(message: String) : Exception(message)
class RateLimitException(message: String) : Exception(message)
class AuthenticationException(message: String) : Exception(message)
