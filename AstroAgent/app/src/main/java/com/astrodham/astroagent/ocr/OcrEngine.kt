package com.astrodham.astroagent.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.astrodham.astroagent.util.Logger
import com.astrodham.astroagent.util.scaleToMax
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * OCR engine using Google ML Kit Text Recognition.
 *
 * Provides:
 * - Full-screen text extraction from bitmaps
 * - Structured results with bounding boxes
 * - Button/label detection via position analysis
 * - Optimized processing (scaled bitmaps for lower battery usage)
 *
 * Uses the bundled ML Kit model (no Google Play Services dependency for OCR itself).
 */
object OcrEngine {

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Performs text recognition on a bitmap.
     *
     * @param bitmap The screen capture bitmap
     * @param maxDimension Max dimension to scale to before OCR (saves battery/CPU)
     * @return OcrResult containing full text and structured blocks
     */
    suspend fun recognizeText(
        bitmap: Bitmap,
        maxDimension: Int = 1280
    ): OcrResult = withContext(Dispatchers.Default) {
        try {
            // Scale down large bitmaps for faster processing
            val processedBitmap = bitmap.scaleToMax(maxDimension)

            val inputImage = InputImage.fromBitmap(processedBitmap, 0)
            val visionText = processImage(inputImage)

            if (visionText == null) {
                Logger.w("OCR returned null result")
                return@withContext OcrResult.empty()
            }

            // Parse results into structured format
            val blocks = visionText.textBlocks.map { block ->
                OcrTextBlock(
                    text = block.text,
                    boundingBox = block.boundingBox,
                    lines = block.lines.map { line ->
                        OcrTextLine(
                            text = line.text,
                            boundingBox = line.boundingBox,
                            elements = line.elements.map { element ->
                                OcrTextElement(
                                    text = element.text,
                                    boundingBox = element.boundingBox
                                )
                            }
                        )
                    }
                )
            }

            val fullText = visionText.text
            Logger.i("OCR complete: ${blocks.size} blocks, ${fullText.length} chars")

            OcrResult(
                fullText = fullText,
                blocks = blocks,
                screenWidth = bitmap.width,
                screenHeight = bitmap.height
            )
        } catch (e: Exception) {
            Logger.e("OCR failed", e)
            OcrResult.empty()
        }
    }

    /**
     * Captures the current screen and performs OCR in one call.
     * Combines ScreenCaptureService + OCR processing.
     *
     * @return OcrResult, or empty result if capture fails
     */
    suspend fun captureAndRecognize(): OcrResult {
        val bitmap = ScreenCaptureService.requestCapture()
        if (bitmap == null) {
            Logger.w("Screen capture returned null — cannot perform OCR")
            return OcrResult.empty()
        }

        return try {
            recognizeText(bitmap)
        } finally {
            // Recycle bitmap to free memory
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    /**
     * Detects potential buttons on screen by analyzing text block positions and sizes.
     * Heuristic: Small text blocks that are horizontally centered or in typical button regions.
     *
     * @param ocrResult Previous OCR result
     * @return List of text blocks that likely represent buttons
     */
    fun detectButtons(ocrResult: OcrResult): List<OcrTextBlock> {
        if (ocrResult.screenWidth == 0 || ocrResult.screenHeight == 0) return emptyList()

        return ocrResult.blocks.filter { block ->
            val box = block.boundingBox ?: return@filter false
            val width = box.width()
            val height = box.height()

            // Button heuristics:
            // 1. Text is relatively short (< 30 chars)
            // 2. Block is wider than tall (button-like aspect ratio)
            // 3. Block height is within typical button range
            block.text.length < 30 &&
                    width > height &&
                    height in 40..200
        }
    }

    /**
     * Finds the bounding box of a specific text string in OCR results.
     * Useful for locating elements to tap.
     *
     * @param ocrResult Previous OCR result
     * @param searchText Text to find
     * @return Bounding box of the found text, or null
     */
    fun findTextLocation(ocrResult: OcrResult, searchText: String): Rect? {
        // Search in elements first (most precise)
        for (block in ocrResult.blocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    if (element.text.contains(searchText, ignoreCase = true)) {
                        return element.boundingBox
                    }
                }
                // Then check full lines
                if (line.text.contains(searchText, ignoreCase = true)) {
                    return line.boundingBox
                }
            }
        }
        return null
    }

    // ── Private ──

    private suspend fun processImage(
        inputImage: InputImage
    ): com.google.mlkit.vision.text.Text? = suspendCancellableCoroutine { continuation ->
        recognizer.process(inputImage)
            .addOnSuccessListener { text ->
                if (continuation.isActive) {
                    continuation.resume(text)
                }
            }
            .addOnFailureListener { e ->
                Logger.e("ML Kit processing failed", e)
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
    }
}

// ── Data Models ──

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrTextBlock>,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0
) {
    val isEmpty: Boolean get() = fullText.isBlank()

    companion object {
        fun empty() = OcrResult("", emptyList())
    }

    /**
     * Returns a summary suitable for sending to the AI as screen context.
     */
    fun toContextString(): String {
        if (isEmpty) return "[Screen is empty or unreadable]"

        return buildString {
            appendLine("=== SCREEN CONTENT (OCR) ===")
            blocks.forEachIndexed { index, block ->
                val box = block.boundingBox
                val position = if (box != null) " [y=${box.top}]" else ""
                appendLine("Block ${index + 1}$position: ${block.text}")
            }
        }
    }
}

data class OcrTextBlock(
    val text: String,
    val boundingBox: Rect?,
    val lines: List<OcrTextLine>
)

data class OcrTextLine(
    val text: String,
    val boundingBox: Rect?,
    val elements: List<OcrTextElement>
)

data class OcrTextElement(
    val text: String,
    val boundingBox: Rect?
)
