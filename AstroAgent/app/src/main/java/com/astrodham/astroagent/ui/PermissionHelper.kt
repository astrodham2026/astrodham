package com.astrodham.astroagent.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.astrodham.astroagent.accessibility.AstroAccessibilityService
import com.astrodham.astroagent.util.Logger

/**
 * Utility for checking and requesting runtime permissions.
 */
object PermissionHelper {

    /**
     * Checks if the AstroAgent AccessibilityService is currently enabled.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any {
            it.resolveInfo?.serviceInfo?.let { si ->
                "${si.packageName}/${si.name}" ==
                        "${context.packageName}/${AstroAccessibilityService::class.java.canonicalName}"
            } == true
        }
    }

    /**
     * Opens Android's Accessibility Settings for the user to enable the service.
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Checks if RECORD_AUDIO permission is granted.
     */
    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if POST_NOTIFICATIONS permission is granted (Android 13+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        // POST_NOTIFICATIONS only exists on Android 13+ (API 33)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns a list of permissions that still need to be requested.
     */
    fun getMissingPermissions(context: Context): List<String> {
        val needed = mutableListOf<String>()

        if (!hasAudioPermission(context)) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return needed
    }
}
