package com.astrodham.astroagent.api

import com.astrodham.astroagent.BuildConfig
import com.astrodham.astroagent.util.ApiKeyManager
import com.astrodham.astroagent.util.Constants
import com.astrodham.astroagent.util.Logger
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds and provides the Retrofit client for Codemax.pro API communication.
 *
 * Handles:
 * - Authentication via x-api-key header (Anthropic format)
 * - API version header
 * - Timeouts configuration
 * - Debug logging
 *
 * ASSUMPTION: Codemax.pro accepts the standard Anthropic auth header format.
 * If it uses Bearer token or a different header name, update the authInterceptor.
 */
object CodemaxClient {

    private var retrofit: Retrofit? = null
    private var apiService: CodemaxApiService? = null

    /**
     * Get or create the API service instance.
     */
    fun getService(): CodemaxApiService {
        if (apiService == null) {
            apiService = buildRetrofit().create(CodemaxApiService::class.java)
        }
        return apiService!!
    }

    /**
     * Force rebuild the client (e.g., after API key change).
     */
    fun reset() {
        retrofit = null
        apiService = null
        Logger.i("CodemaxClient reset")
    }

    private fun buildRetrofit(): Retrofit {
        if (retrofit != null) return retrofit!!

        val client = OkHttpClient.Builder()
            .addInterceptor(createAuthInterceptor())
            .addInterceptor(createLoggingInterceptor())
            .connectTimeout(Constants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        val baseUrl = ApiKeyManager.getBaseUrl()
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        Logger.i("CodemaxClient initialized with base URL: ${Constants.CODEMAX_BASE_URL}")
        return retrofit!!
    }

    /**
     * Authentication interceptor.
     * Adds the API key and version headers to every request.
     *
     * ASSUMPTION: Codemax.pro uses x-api-key header (Anthropic standard).
     * API keys are expected to be prefixed with "sk-cm-".
     */
    private fun createAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val apiKey = ApiKeyManager.getApiKey()
            val request = chain.request().newBuilder()
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", Constants.CODEMAX_API_VERSION)
                .addHeader("content-type", "application/json")
                .build()
            chain.proceed(request)
        }
    }

    /**
     * Logging interceptor — full body in debug, headers only in release.
     */
    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Logger.d(message, "OkHttp")
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.HEADERS
            }
        }
    }
}
