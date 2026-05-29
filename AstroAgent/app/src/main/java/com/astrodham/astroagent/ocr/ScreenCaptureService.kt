package com.astrodham.astroagent.ocr

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import com.astrodham.astroagent.util.Constants
import com.astrodham.astroagent.util.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Foreground service for screen capture using MediaProjection API.
 *
 * Android 14+ requires:
 * 1. Foreground service with mediaProjection type
 * 2. startForeground() called BEFORE initializing MediaProjection
 * 3. User consent via createScreenCaptureIntent() (single-use token)
 *
 * Usage flow:
 * 1. Activity requests screen capture consent
 * 2. Pass resultCode + data Intent to this service
 * 3. Call captureScreen() to get a single frame Bitmap
 * 4. Service runs in foreground with persistent notification
 *
 * This service captures on-demand only (not continuous) to minimize battery usage.
 */
class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenWidth = 1080
    private var screenHeight = 2400
    private var screenDensity = 420

    companion object {
        @Volatile
        var instance: ScreenCaptureService? = null
            private set

        val isActive: Boolean get() = instance?.mediaProjection != null

        // Pending capture request
        private var pendingCapture: CompletableDeferred<Bitmap?>? = null

        /**
         * Create the intent to start this service with screen capture data.
         */
        fun createStartIntent(
            context: Context,
            resultCode: Int,
            data: Intent
        ): Intent {
            return Intent(context, ScreenCaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
            }
        }

        /**
         * Request a screen capture from outside the service.
         * Returns null if the service is not active or capture fails.
         */
        suspend fun requestCapture(): Bitmap? {
            val service = instance ?: run {
                Logger.w("ScreenCaptureService not active")
                return null
            }
            return service.captureScreen()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        Logger.i("ScreenCaptureService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>("data")

        if (resultCode != Activity.RESULT_OK || data == null) {
            Logger.e("Invalid screen capture consent data")
            stopSelf()
            return START_NOT_STICKY
        }

        // MUST call startForeground before initializing MediaProjection (Android 14+ requirement)
        startForeground(Constants.SCREEN_CAPTURE_NOTIFICATION_ID, createNotification())

        // Get screen metrics
        val windowManager = getSystemService(WindowManager::class.java)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi

        // Initialize MediaProjection
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Logger.i("MediaProjection stopped")
                cleanup()
            }
        }, null)

        Logger.i("MediaProjection initialized (${screenWidth}x${screenHeight} @${screenDensity}dpi)")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        cleanup()
        instance = null
        Logger.i("ScreenCaptureService destroyed")
        super.onDestroy()
    }

    /**
     * Captures a single frame of the screen.
     * Creates a VirtualDisplay → ImageReader pipeline, captures one image,
     * then tears down the display to minimize resource usage.
     *
     * @return Bitmap of the current screen, or null on failure
     */
    private suspend fun captureScreen(): Bitmap? {
        val projection = mediaProjection ?: run {
            Logger.e("MediaProjection not initialized")
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                // Create ImageReader for single frame capture
                imageReader = ImageReader.newInstance(
                    screenWidth, screenHeight,
                    PixelFormat.RGBA_8888, 2
                )

                // Set up image available listener
                imageReader?.setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage()
                    if (image != null) {
                        try {
                            val planes = image.planes
                            val buffer = planes[0].buffer
                            val pixelStride = planes[0].pixelStride
                            val rowStride = planes[0].rowStride
                            val rowPadding = rowStride - pixelStride * screenWidth

                            // Create bitmap from the image data
                            val bitmap = Bitmap.createBitmap(
                                screenWidth + rowPadding / pixelStride,
                                screenHeight,
                                Bitmap.Config.ARGB_8888
                            )
                            bitmap.copyPixelsFromBuffer(buffer)

                            // Crop to actual screen size (remove padding)
                            val croppedBitmap = Bitmap.createBitmap(
                                bitmap, 0, 0, screenWidth, screenHeight
                            )
                            if (croppedBitmap != bitmap) {
                                bitmap.recycle()
                            }

                            Logger.d("Screen captured: ${screenWidth}x${screenHeight}")
                            image.close()

                            // Tear down virtual display after capture
                            virtualDisplay?.release()
                            virtualDisplay = null

                            if (continuation.isActive) {
                                continuation.resume(croppedBitmap)
                            }
                        } catch (e: Exception) {
                            image.close()
                            Logger.e("Failed to process captured image", e)
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    }
                }, null)

                // Create virtual display for capture
                virtualDisplay = projection.createVirtualDisplay(
                    "AstroAgentCapture",
                    screenWidth, screenHeight, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader?.surface,
                    null, null
                )

                continuation.invokeOnCancellation {
                    virtualDisplay?.release()
                    virtualDisplay = null
                    imageReader?.close()
                    imageReader = null
                }
            } catch (e: Exception) {
                Logger.e("Screen capture failed", e)
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    private fun cleanup() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.SCREEN_CAPTURE_CHANNEL_ID,
            getString(com.astrodham.astroagent.R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(com.astrodham.astroagent.R.string.notification_channel_desc)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, Constants.SCREEN_CAPTURE_CHANNEL_ID)
            .setContentTitle(getString(com.astrodham.astroagent.R.string.notification_title))
            .setContentText(getString(com.astrodham.astroagent.R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }
}
