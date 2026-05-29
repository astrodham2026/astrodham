package com.astrodham.astroagent.api.models

import com.google.gson.annotations.SerializedName

/**
 * Request model for the Codemax.pro API (Anthropic Messages API format).
 *
 * ASSUMPTION: Codemax.pro proxies the standard Anthropic /v1/messages endpoint.
 * If the request schema differs, this model will need adjustment.
 */
data class ApiRequest(
    @SerializedName("model")
    val model: String,

    @SerializedName("max_tokens")
    val maxTokens: Int,

    @SerializedName("system")
    val system: String? = null,

    @SerializedName("messages")
    val messages: List<MessageParam>
)

/**
 * A single message in the conversation.
 */
data class MessageParam(
    @SerializedName("role")
    val role: String, // "user" or "assistant"

    @SerializedName("content")
    val content: String
)

/**
 * Response model from the Codemax.pro API (Anthropic Messages API format).
 *
 * ASSUMPTION: Response follows standard Anthropic response schema.
 */
data class ApiResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("type")
    val type: String?, // "message"

    @SerializedName("role")
    val role: String?, // "assistant"

    @SerializedName("content")
    val content: List<ContentBlock>?,

    @SerializedName("model")
    val model: String?,

    @SerializedName("stop_reason")
    val stopReason: String?, // "end_turn", "max_tokens", etc.

    @SerializedName("usage")
    val usage: Usage?,

    // Error response fields
    @SerializedName("error")
    val error: ApiError?
) {
    /**
     * Extracts the text content from the first text block in the response.
     */
    fun getTextContent(): String {
        return content
            ?.filterIsInstance<ContentBlock>()
            ?.filter { it.type == "text" }
            ?.joinToString("") { it.text ?: "" }
            ?: ""
    }

    val isError: Boolean get() = error != null
}

data class ContentBlock(
    @SerializedName("type")
    val type: String, // "text"

    @SerializedName("text")
    val text: String?
)

data class Usage(
    @SerializedName("input_tokens")
    val inputTokens: Int?,

    @SerializedName("output_tokens")
    val outputTokens: Int?
)

data class ApiError(
    @SerializedName("type")
    val type: String?,

    @SerializedName("message")
    val message: String?
)
