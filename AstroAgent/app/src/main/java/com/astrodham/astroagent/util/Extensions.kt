package com.astrodham.astroagent.util

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.withTimeout

/**
 * Kotlin extension functions used across the AstroAgent project.
 */

// ── AccessibilityNodeInfo Extensions ──

/**
 * Recursively collects all nodes in the accessibility tree rooted at this node.
 * Caller is responsible for recycling nodes when done.
 */
fun AccessibilityNodeInfo.allNodes(): List<AccessibilityNodeInfo> {
    val result = mutableListOf<AccessibilityNodeInfo>()
    collectNodes(this, result)
    return result
}

private fun collectNodes(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
    out.add(node)
    for (i in 0 until node.childCount) {
        val child = node.getChild(i) ?: continue
        collectNodes(child, out)
    }
}

/**
 * Finds the first node matching the predicate via depth-first search.
 */
fun AccessibilityNodeInfo.findFirst(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
    if (predicate(this)) return this
    for (i in 0 until childCount) {
        val child = getChild(i) ?: continue
        val found = child.findFirst(predicate)
        if (found != null) return found
    }
    return null
}

/**
 * Gets the text content of this node, checking both text and contentDescription.
 */
fun AccessibilityNodeInfo.getDisplayText(): String {
    return text?.toString()
        ?: contentDescription?.toString()
        ?: ""
}

/**
 * Checks if a node is clickable (directly or via ancestors).
 */
fun AccessibilityNodeInfo.isEffectivelyClickable(): Boolean {
    return isClickable || isCheckable || isFocusable
}

/**
 * Dumps the accessibility tree as a readable string for debugging.
 */
fun AccessibilityNodeInfo.dumpTree(indent: Int = 0): String {
    val sb = StringBuilder()
    val prefix = " ".repeat(indent)
    sb.appendLine("${prefix}[${className}] text='${text}' desc='${contentDescription}' click=${isClickable} id=${viewIdResourceName}")
    for (i in 0 until childCount) {
        val child = getChild(i) ?: continue
        sb.append(child.dumpTree(indent + 2))
    }
    return sb.toString()
}

// ── Bitmap Extensions ──

/**
 * Scales a bitmap to the specified maximum dimension while maintaining aspect ratio.
 * Useful for reducing OCR processing time on large screenshots.
 */
fun Bitmap.scaleToMax(maxDimension: Int): Bitmap {
    val ratio = maxDimension.toFloat() / maxOf(width, height)
    if (ratio >= 1.0f) return this // Already small enough

    val newWidth = (width * ratio).toInt()
    val newHeight = (height * ratio).toInt()
    return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
}

/**
 * Rotates the bitmap by the specified degrees.
 */
fun Bitmap.rotate(degrees: Float): Bitmap {
    if (degrees == 0f) return this
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

// ── Coroutine Extensions ──

/**
 * Runs a suspending block with a timeout, returning null on timeout instead of throwing.
 */
suspend fun <T> withTimeoutOrNull(timeoutMs: Long, block: suspend () -> T): T? {
    return try {
        withTimeout(timeoutMs) { block() }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        Logger.w("Operation timed out after ${timeoutMs}ms")
        null
    }
}

/**
 * Retries a suspend block with exponential backoff.
 */
suspend fun <T> retryWithBackoff(
    maxRetries: Int = Constants.MAX_API_RETRIES,
    baseDelayMs: Long = Constants.RETRY_BASE_DELAY_MS,
    block: suspend (attempt: Int) -> T
): T {
    var lastException: Exception? = null
    for (attempt in 0 until maxRetries) {
        try {
            return block(attempt)
        } catch (e: Exception) {
            lastException = e
            if (attempt < maxRetries - 1) {
                val delay = baseDelayMs * (1L shl attempt) // Exponential: 1s, 2s, 4s...
                Logger.w("Retry ${attempt + 1}/$maxRetries after ${delay}ms — ${e.message}")
                kotlinx.coroutines.delay(delay)
            }
        }
    }
    throw lastException ?: RuntimeException("Retry failed with no exception")
}
