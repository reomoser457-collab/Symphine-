package com.symphine.gamespace.diagnostics

import android.os.Build
import com.symphine.gamespace.core.model.DeviceHardwareInfo
import com.symphine.gamespace.core.model.DisplayHardwareInfo
import com.symphine.gamespace.core.model.GraphicsHardwareInfo
import com.symphine.gamespace.core.model.OemBrand
import com.symphine.gamespace.core.model.OemCompatibilityReport
import com.symphine.gamespace.core.model.OemFeatureCompatibility
import com.symphine.gamespace.core.model.OperationStatus
import com.symphine.gamespace.core.permissions.PermissionManager
import com.symphine.gamespace.shizuku.ShizukuStatusState

class OemCompatibilityAnalyzer(
    private val permissionManager: PermissionManager
) {
    fun analyze(
        device: DeviceHardwareInfo,
        display: DisplayHardwareInfo,
        graphics: GraphicsHardwareInfo,
        shizuku: ShizukuStatusState
    ): OemCompatibilityReport {
        val shizukuStatus = if (shizuku.isOperational) OperationStatus.AVAILABLE else OperationStatus.REQUIRES_SHIZUKU
        val features = listOf(
            OemFeatureCompatibility(
                featureId = "game_scanner",
                name = "Installed Game Detection & Library",
                category = "Core",
                status = OperationStatus.VERIFIED,
                detail = "Uses PackageManager launcher queries & CATEGORY_GAME metadata."
            ),
            OemFeatureCompatibility(
                featureId = "live_monitoring",
                name = "RAM, Battery, Storage & PowerManager Thermal",
                category = "Monitoring",
                status = OperationStatus.VERIFIED,
                detail = "Genuine Android API telemetry available without root or Shizuku."
            ),
            OemFeatureCompatibility(
                featureId = "display_modes",
                name = "Refresh Rate Mode Detection (${display.supportedRefreshRates.joinToString("/")} Hz)",
                category = "Display",
                status = OperationStatus.VERIFIED,
                detail = "${display.supportedModes.size} hardware Display.Mode(s) exposed by DisplayManager."
            ),
            OemFeatureCompatibility(
                featureId = "refresh_override",
                name = "System Peak/Min Refresh Rate Write",
                category = "Display",
                status = if (display.supportedRefreshRates.size > 1) shizukuStatus else OperationStatus.UNSUPPORTED,
                detail = oemRefreshNote(device.oemBrand, display.supportedRefreshRates.size > 1)
            ),
            OemFeatureCompatibility(
                featureId = "wm_density_size",
                name = "Display DPI & Proportional Downscale (wm density/size)",
                category = "Display",
                status = shizukuStatus,
                detail = "Executed via Shizuku UserService with atomic original-value backup."
            ),
            OemFeatureCompatibility(
                featureId = "angle_driver",
                name = "ANGLE GL Driver Selection",
                category = "Graphics",
                status = if (graphics.angleInstalled) shizukuStatus else OperationStatus.UNSUPPORTED,
                detail = "ANGLE status: ${graphics.anglePackageName ?: "Unavailable"}. Vulkan: ${graphics.vulkanSupported}."
            ),
            OemFeatureCompatibility(
                featureId = "angle_debug_prop",
                name = "ANGLE Feature Property Override (debug.angle.*)",
                category = "Graphics",
                status = if ((shizuku.serviceUid ?: -1) == 0) OperationStatus.AVAILABLE else OperationStatus.UNSUPPORTED,
                detail = "Writing debug.* system properties requires Root (UID 0) or permissive SELinux; Shell UID 2000 gracefully skips."
            ),
            OemFeatureCompatibility(
                featureId = "game_mode_api",
                name = "Android Game Mode (cmd game mode)",
                category = "Performance",
                status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) shizukuStatus else OperationStatus.UNSUPPORTED,
                detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Supported on Android 12+ via Shizuku." else "Requires Android 12 (API 31)+."
            ),
            OemFeatureCompatibility(
                featureId = "art_compile",
                name = "ART Bytecode Compiler (cmd package compile)",
                category = "Performance",
                status = shizukuStatus,
                detail = "Runs dexopt speed-profile/speed compilation via Shizuku."
            ),
            OemFeatureCompatibility(
                featureId = "gaming_overlay",
                name = "Floating Gaming Overlay",
                category = "Tools",
                status = if (permissionManager.canDrawOverlays()) OperationStatus.VERIFIED else OperationStatus.REQUIRES_PERMISSION,
                detail = "Requires Display Over Other Apps (SYSTEM_ALERT_WINDOW)."
            ),
            OemFeatureCompatibility(
                featureId = "dnd_control",
                name = "Gaming Do Not Disturb Shortcut",
                category = "Tools",
                status = if (permissionManager.hasDndAccess()) OperationStatus.VERIFIED else OperationStatus.REQUIRES_PERMISSION,
                detail = "Requires Notification Policy Access."
            ),
            OemFeatureCompatibility(
                featureId = "gpu_util",
                name = "Live GPU Utilization (%) Counter",
                category = "Diagnostics",
                status = OperationStatus.UNSUPPORTED,
                detail = "Not exposed by standard Android SDK without vendor-privileged sysfs access."
            )
        )

        return OemCompatibilityReport(
            oemBrand = device.oemBrand,
            manufacturer = device.manufacturer,
            model = device.model,
            apiLevel = device.apiLevel,
            features = features
        )
    }

    private fun oemRefreshNote(brand: OemBrand, multiHz: Boolean): String {
        if (!multiHz) return "Device exposes a single refresh rate mode."
        return when (brand) {
            OemBrand.XIAOMI, OemBrand.REDMI, OemBrand.POCO ->
                "HyperOS/MIUI may enforce per-app refresh rate via Joyose/DisplayFeature alongside Settings.System."
            OemBrand.SAMSUNG ->
                "One UI Motion Smoothness uses peak_refresh_rate & min_refresh_rate; LTPO panels may scale dynamically."
            OemBrand.REALME, OemBrand.OPPO ->
                "ColorOS/RealmeUI may cap certain games at 60Hz unless configured in system display refresh settings."
            OemBrand.VIVO, OemBrand.IQOO ->
                "FuntouchOS/OriginOS controls per-app high refresh via system display whitelist."
            OemBrand.ASUS ->
                "ROG/ZenUI generally honors standard Android refresh rate & GameMode signals."
            OemBrand.PIXEL ->
                "Pixel Stock Android honors peak/min refresh rate and Smooth Display settings reliably."
            OemBrand.MOTOROLA ->
                "MyUX honors standard Android refresh settings."
            OemBrand.OTHER ->
                "Compatibility varies by ROM display composer implementation."
        }
    }
}
