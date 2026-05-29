package com.astrodham.astroagent

import android.app.Application
import com.astrodham.astroagent.util.ApiKeyManager
import com.astrodham.astroagent.util.Logger

/**
 * Application class for AstroAgent.
 *
 * Initializes global resources that need to live for the entire app lifecycle.
 * Dependency injection frameworks (Hilt/Dagger) would be configured here
 * in a production app with more complex dependency graphs.
 */
class AstroAgentApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Logger.i("AstroAgent application started")
        Logger.i("Build: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Logger.i("Debug: ${BuildConfig.DEBUG}")

        // Initialize API Key Manager
        ApiKeyManager.init(this)
        
        if (ApiKeyManager.hasApiKey()) {
            Logger.i("Codemax.pro API key is configured.")
        } else {
            Logger.w("⚠ Codemax.pro API key not configured! User must enter it in UI.")
        }
    }
}
