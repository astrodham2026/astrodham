package com.astrodham.astroagent.api

import com.astrodham.astroagent.api.models.ApiRequest
import com.astrodham.astroagent.api.models.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service interface for the Codemax.pro API.
 *
 * ASSUMPTION: Codemax.pro proxies the Anthropic Messages API at /v1/messages.
 * Authentication is handled via the OkHttp interceptor in CodemaxClient.
 */
interface CodemaxApiService {

    /**
     * Send a message to the AI model and receive a response.
     * Uses the Anthropic Messages API format.
     *
     * @param request The message request containing model, system prompt, and conversation messages.
     * @return Response wrapper containing the API response or error details.
     */
    @POST("/v1/messages")
    suspend fun sendMessage(@Body request: ApiRequest): Response<ApiResponse>
}
