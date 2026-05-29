package com.astrodham.astroagent.api.models

/**
 * Represents a single message in the conversation memory.
 * Used for maintaining context across multiple AI interactions.
 */
data class ConversationMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Converts to the API message format.
     */
    fun toMessageParam(): MessageParam = MessageParam(role = role, content = content)
}
