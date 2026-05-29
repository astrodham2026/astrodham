package com.astrodham.astroagent.util

/**
 * App-wide constants for AstroAgent.
 * Centralized configuration to avoid magic strings/numbers throughout the codebase.
 */
object Constants {

    // ── Codemax.pro API Configuration ──
    // ASSUMPTION: Codemax.pro proxies the Anthropic Messages API at this base URL.
    // If the actual endpoint differs, update BASE_URL accordingly.
    const val CODEMAX_BASE_URL = "https://api.codemax.pro"
    const val CODEMAX_API_VERSION = "2023-06-01"
    const val CODEMAX_MODEL = "claude-sonnet-4-20250514" // Default model; configurable
    const val CODEMAX_MAX_TOKENS = 4096

    // ── Network Timeouts (seconds) ──
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 120L
    const val WRITE_TIMEOUT_SECONDS = 30L

    // ── Retry Configuration ──
    const val MAX_API_RETRIES = 3
    const val MAX_ACTION_RETRIES = 5
    const val RETRY_BASE_DELAY_MS = 1000L // Exponential backoff base: 1s → 2s → 4s
    const val RATE_LIMIT_RETRY_DELAY_MS = 5000L

    // ── Action Execution ──
    const val ACTION_TIMEOUT_MS = 10_000L
    const val ACTION_WAIT_DEFAULT_MS = 2000L
    const val SWIPE_DURATION_MS = 300L

    // ── Voice ──
    const val WAKE_PHRASE = "hey astro"

    // ── Memory ──
    const val MAX_CONVERSATION_HISTORY = 20
    const val MAX_SCREEN_STATE_HISTORY = 5

    // ── Screen Capture ──
    const val SCREEN_CAPTURE_NOTIFICATION_ID = 1001
    const val SCREEN_CAPTURE_CHANNEL_ID = "astro_screen_capture"

    // ── Logging ──
    const val LOG_TAG = "AstroAgent"
    const val MAX_LOG_BUFFER_SIZE = 500

    // ── System Prompt for AI Planner ──
    val SYSTEM_PROMPT = """
        You are AstroAgent, an Android AI automation assistant. You receive a user's intent 
        along with the current screen state (text content visible on screen) and conversation memory.
        
        Your job is to produce a JSON array of actions that the Android device should execute 
        to fulfill the user's intent.
        
        Available action types:
        - open_app: Launch an app. Params: {"package": "com.example.app"} or {"app_name": "App Name"}
        - tap: Tap a UI element. Params: {"text": "Button"} OR {"x": "50", "y": "90"} (x and y are percentages 0-100, e.g., 50, 90 is bottom-center, useful for Camera Shutter)
        - long_press: Long press a UI element. Same params as tap.
        - swipe: Swipe gesture. Params: {"direction": "up|down|left|right"}
        - type_text: Enter text into focused field. Params: {"text": "text to type"}
        - wait: Wait for UI to update. Params: {"ms": "2000"}
        - back: Press back button. No params.
        - home: Press home button. No params.
        - scroll: Scroll in a direction. Params: {"direction": "up|down"}
        - read_screen: Capture and OCR current screen. No params.
        - retry_action: Retry the last failed action. No params.
        
        Rules:
        1. Respond ONLY with a valid JSON array of action objects.
        2. Each action has: {"type": "action_type", "params": {...}, "description": "human-readable step"}
        3. Be precise about which UI elements to tap — use exact text when possible.
        4. Include wait actions after app launches and after sending messages.
        5. If uncertain about the UI, include a read_screen action first.
        6. Generate as many steps as required to complete the task. There is NO LIMIT on the number of steps.
    """.trimIndent()
}
