package com.symphine.gamespace.core.permissions

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.symphine.gamespace.core.model.OperationStatus
import com.symphine.gamespace.shizuku.ShizukuStatusState

data class ManagedPermissionState(
    val id: String,
    val title: String,
    val reason: String,
    val status: OperationStatus,
    val isGranted: Boolean,
    val recoveryInstructions: String,
    val fallbackInfo: String
)

class PermissionManager(private val context: Context) {

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasDndAccess(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun hasUsageStatsAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun buildPermissionSnapshot(shizuku: ShizukuStatusState): List<ManagedPermissionState> {
        val overlayGranted = canDrawOverlays()
        val notifGranted = hasNotificationPermission()
        val dndGranted = hasDndAccess()
        val usageGranted = hasUsageStatsAccess()

        return listOf(
            ManagedPermissionState(
                id = "package_visibility",
                title = "Installed Game Visibility (<queries>)",
                reason = "Detects launchable installed games via Android PackageManager without broad storage permissions.",
                status = OperationStatus.VERIFIED,
                isGranted = true,
                recoveryInstructions = "Declared statically via <queries> in AndroidManifest.xml.",
                fallbackInfo = "You can always add any installed app manually from the Library."
            ),
            ManagedPermissionState(
                id = "overlay",
                title = "Display Over Other Apps (Overlay)",
                reason = "Enables the optional draggable Gaming Telemetry Overlay while playing.",
                status = if (overlayGranted) OperationStatus.VERIFIED else OperationStatus.REQUIRES_PERMISSION,
                isGranted = overlayGranted,
                recoveryInstructions = "Open Android Settings > Apps > Special app access > Display over other apps > Symphine Space.",
                fallbackInfo = "Use the in-app Monitoring Center without overlay."
            ),
            ManagedPermissionState(
                id = "notifications",
                title = "Post Notifications",
                reason = "Shows profile applied/restored, thermal warning, and active overlay service notifications.",
                status = if (notifGranted) OperationStatus.VERIFIED else OperationStatus.REQUIRES_PERMISSION,
                isGranted = notifGranted,
                recoveryInstructions = "Grant Notifications in Android App Settings for Symphine Space.",
                fallbackInfo = "All feedback remains visible inside the app snackbars and logs."
            ),
            ManagedPermissionState(
                id = "dnd",
                title = "Do Not Disturb Policy Access",
                reason = "Allows toggling Gaming Do Not Disturb mode from Gaming Tools.",
                status = if (dndGranted) OperationStatus.VERIFIED else OperationStatus.REQUIRES_PERMISSION,
                isGranted = dndGranted,
                recoveryInstructions = "Enable Symphine Space in Settings > Notifications > Do Not Disturb access.",
                fallbackInfo = "Toggle DND manually from Android Quick Settings."
            ),
            ManagedPermissionState(
                id = "usage_stats",
                title = "Usage Access (Optional Session Telemetry)",
                reason = "Optional helper for checking recent foreground activity; Symphine never forces this permission.",
                status = if (usageGranted) OperationStatus.VERIFIED else OperationStatus.REQUIRES_PERMISSION,
                isGranted = usageGranted,
                recoveryInstructions = "Enable in Settings > Security/Privacy > Usage access.",
                fallbackInfo = "Built-in Symphine Launch & Manual Session Tracker works with zero permissions."
            ),
            ManagedPermissionState(
                id = "shizuku",
                title = "Shizuku Shell Integration (Optional)",
                reason = "Enables verified system writes (ANGLE, ART compile, Game Mode, Refresh Rate, DPI/Downscale).",
                status = if (shizuku.isOperational) OperationStatus.VERIFIED else OperationStatus.REQUIRES_SHIZUKU,
                isGranted = shizuku.isOperational,
                recoveryInstructions = "Start Shizuku via Wireless Debugging and tap Request Permission in Shizuku Center.",
                fallbackInfo = "Core Game Library, Device Diagnostics, Live Monitoring, Customization & JSON Backup work without Shizuku."
            ),
            ManagedPermissionState(
                id = "saf_picker",
                title = "Photo Picker & Storage Access Framework",
                reason = "Zero-permission selection for custom game banners, fonts, and JSON backups.",
                status = OperationStatus.VERIFIED,
                isGranted = true,
                recoveryInstructions = "Handled securely by Android system file & photo pickers.",
                fallbackInfo = "Built-in presets remain available."
            )
        )
    }

    fun createOverlayPermissionIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun createDndAccessIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun createUsageAccessIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun createAppNotificationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
