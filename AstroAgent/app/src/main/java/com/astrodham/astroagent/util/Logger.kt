package com.astrodham.astroagent.util

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Centralized logging utility for AstroAgent.
 *
 * Provides dual output:
 * 1. Standard Android Logcat
 * 2. In-memory buffer exposed via SharedFlow for real-time UI display
 *
 * Thread-safe via CopyOnWriteArrayList for the buffer.
 */
object Logger {

    enum class Level { DEBUG, INFO, WARN, ERROR }

    data class LogEntry(
        val level: Level,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
        val tag: String = Constants.LOG_TAG
    ) {
        val formattedTime: String
            get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))

        override fun toString(): String = "[$formattedTime] ${level.name}: $message"
    }

    private val buffer = CopyOnWriteArrayList<LogEntry>()

    private val _logFlow = MutableSharedFlow<LogEntry>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val logFlow: SharedFlow<LogEntry> = _logFlow.asSharedFlow()

    /** Get a snapshot of all buffered logs */
    val logs: List<LogEntry> get() = buffer.toList()

    fun d(message: String, tag: String = Constants.LOG_TAG) {
        emit(Level.DEBUG, message, tag)
        Log.d(tag, message)
    }

    fun i(message: String, tag: String = Constants.LOG_TAG) {
        emit(Level.INFO, message, tag)
        Log.i(tag, message)
    }

    fun w(message: String, tag: String = Constants.LOG_TAG) {
        emit(Level.WARN, message, tag)
        Log.w(tag, message)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = Constants.LOG_TAG) {
        val fullMessage = if (throwable != null) "$message: ${throwable.message}" else message
        emit(Level.ERROR, fullMessage, tag)
        Log.e(tag, message, throwable)
    }

    fun clear() {
        buffer.clear()
    }

    private fun emit(level: Level, message: String, tag: String) {
        val entry = LogEntry(level, message, tag = tag)

        // Cap buffer size to prevent memory issues
        if (buffer.size >= Constants.MAX_LOG_BUFFER_SIZE) {
            // Remove oldest entries (first 50)
            val toRemove = buffer.take(50)
            buffer.removeAll(toRemove.toSet())
        }
        buffer.add(entry)

        // Emit to flow (non-suspending tryEmit for thread safety)
        _logFlow.tryEmit(entry)
    }
}
