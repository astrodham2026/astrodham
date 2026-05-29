package com.astrodham.astroagent.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.astrodham.astroagent.util.Logger
import com.astrodham.astroagent.util.findFirst
import com.astrodham.astroagent.util.getDisplayText

/**
 * Utility for finding UI nodes in the accessibility tree.
 *
 * All methods operate on the current window's root node obtained
 * from AstroAccessibilityService. Returns null when the service
 * is not active or the requested node cannot be found.
 */
object NodeFinder {

    /**
     * Finds a node whose text exactly matches the given string.
     *
     * @param text The exact text to search for
     * @param caseSensitive Whether the match should be case-sensitive
     * @return The matching node, or null if not found
     */
    fun findByText(text: String, caseSensitive: Boolean = false): AccessibilityNodeInfo? {
        val root = getRoot() ?: return null

        // First try the built-in method (faster)
        val nodes = root.findAccessibilityNodeInfosByText(text)
        if (!nodes.isNullOrEmpty()) {
            // Return the most specific (deepest/clickable) match
            return nodes.firstOrNull { it.isClickable }
                ?: nodes.firstOrNull { it.isEnabled }
                ?: nodes.first()
        }

        // Fallback: manual tree traversal for partial/case-insensitive matching
        return root.findFirst { node ->
            val nodeText = node.getDisplayText()
            if (caseSensitive) {
                nodeText.contains(text)
            } else {
                nodeText.contains(text, ignoreCase = true)
            }
        }
    }

    /**
     * Finds a node by its content description (used for icon buttons, image buttons).
     *
     * @param description The content description to search for
     * @return The matching node, or null
     */
    fun findByContentDescription(description: String): AccessibilityNodeInfo? {
        val root = getRoot() ?: return null
        return root.findFirst { node ->
            node.contentDescription?.toString()?.contains(description, ignoreCase = true) == true
        }
    }

    /**
     * Finds a node by its view resource ID.
     * Format expected: "com.example.app:id/my_button"
     *
     * @param viewId The full resource ID string
     * @return The matching node, or null
     */
    fun findByViewId(viewId: String): AccessibilityNodeInfo? {
        val root = getRoot() ?: return null
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        return nodes?.firstOrNull()
    }

    /**
     * Finds the first editable text field (EditText, TextInputEditText, etc.).
     * Useful for locating input fields to type into.
     *
     * @return The first editable node, or null
     */
    fun findEditableNode(): AccessibilityNodeInfo? {
        val root = getRoot() ?: return null
        return root.findFirst { node ->
            node.isEditable
        }
    }

    /**
     * Finds all editable text fields on screen.
     *
     * @return List of editable nodes (may be empty)
     */
    fun findAllEditableNodes(): List<AccessibilityNodeInfo> {
        val root = getRoot() ?: return emptyList()
        val results = mutableListOf<AccessibilityNodeInfo>()
        collectMatching(root, results) { it.isEditable }
        return results
    }

    /**
     * Finds the first scrollable container on screen.
     * Useful for scrolling through lists/feeds.
     *
     * @return The first scrollable node, or null
     */
    fun findScrollableNode(): AccessibilityNodeInfo? {
        val root = getRoot() ?: return null
        return root.findFirst { node ->
            node.isScrollable
        }
    }

    /**
     * Finds a clickable node containing or near the specified text.
     * Traverses up the tree from a text match to find the nearest clickable ancestor.
     *
     * @param text The text to search near
     * @return A clickable node associated with the text, or null
     */
    fun findClickableByText(text: String): AccessibilityNodeInfo? {
        val root = getRoot() ?: return null

        // Find the text node first
        val textNode = findByText(text) ?: return null

        // If the text node itself is clickable, return it
        if (textNode.isClickable) return textNode

        // Walk up the tree to find a clickable parent
        var parent = textNode.parent
        var depth = 0
        while (parent != null && depth < 5) {
            if (parent.isClickable) return parent
            parent = parent.parent
            depth++
        }

        // Last resort: just return the text node (caller can try clicking anyway)
        return textNode
    }

    /**
     * Collects visible text from all nodes on screen.
     * Useful for generating a text representation of the current UI state.
     *
     * @return Concatenated visible text from the screen
     */
    fun getScreenText(): String {
        val root = getRoot() ?: return ""
        val sb = StringBuilder()
        collectText(root, sb)
        return sb.toString().trim()
    }

    // ── Private Helpers ──

    private fun getRoot(): AccessibilityNodeInfo? {
        val service = AstroAccessibilityService.instance
        if (service == null) {
            Logger.w("NodeFinder: AccessibilityService not active")
            return null
        }
        return service.getRootNode()
    }

    private fun collectText(node: AccessibilityNodeInfo, sb: StringBuilder) {
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()

        if (!text.isNullOrBlank()) {
            sb.appendLine(text)
        } else if (!desc.isNullOrBlank()) {
            sb.appendLine("[${desc}]")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, sb)
        }
    }

    private fun collectMatching(
        node: AccessibilityNodeInfo,
        results: MutableList<AccessibilityNodeInfo>,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ) {
        if (predicate(node)) {
            results.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectMatching(child, results, predicate)
        }
    }
}
