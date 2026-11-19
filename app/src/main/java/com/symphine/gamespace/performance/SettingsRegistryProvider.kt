package com.symphine.gamespace.performance

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.symphine.gamespace.core.model.OperationStatus
import com.symphine.gamespace.core.model.RiskLevel
import com.symphine.gamespace.core.model.SettingRegistryEntry
import com.symphine.gamespace.shizuku.ShizukuManager

class SettingsRegistryProvider(
    private val context: Context,
    private val shizukuManager: ShizukuManager
) {

    fun buildRegistryEntries(selectedGamePackage: String? = null): List<SettingRegistryEntry> {
        val shizukuReady = shizukuManager.state.value.isOperational
        val writeStatus = if (shizukuReady) OperationStatus.AVAILABLE else OperationStatus.REQUIRES_SHIZUKU

        fun readGlobal(key: String): String? = runCatching {
            Settings.Global.getString(context.contentResolver, key)
        }.getOrNull()

        fun readSystem(key: String): String? = runCatching {
            Settings.System.getString(context.contentResolver, key)
        }.getOrNull()

        return listOf(
            SettingRegistryEntry(
                settingId = "angle_pkgs",
                gamePackage = selectedGamePackage,
                category = "Graphics & ANGLE",
                name = "ANGLE Driver Target Packages",
                description = "Comma-separated package list routed through Android ANGLE OpenGL-over-Vulkan translation layer.",
                namespace = "global",
                key = "angle_gl_driver_selection_pkgs",
                defaultValue = "null",
                currentValue = readGlobal("angle_gl_driver_selection_pkgs"),
                supportedStatus = writeStatus,
                requiredPermission = "Shizuku (WRITE_SECURE_SETTINGS via Shell UID 2000)",
                androidCompatibility = "Android 10+ (API 29+)",
                deviceCompatibility = "Requires ROM ANGLE & Vulkan support",
                riskLevel = RiskLevel.MODERATE,
                validationResult = "Package name allowlist validated",
                operationStatus = writeStatus,
                verificationResult = "Read-back via Settings.Global",
                documentation = "Maps specific games to ANGLE driver; does not modify game APK or assets."
            ),
            SettingRegistryEntry(
                settingId = "angle_values",
                gamePackage = selectedGamePackage,
                category = "Graphics & ANGLE",
                name = "ANGLE Driver Mode Values",
                description = "Driver selection value matching angle_gl_driver_selection_pkgs (angle / native / default).",
                namespace = "global",
                key = "angle_gl_driver_selection_values",
                defaultValue = "null",
                currentValue = readGlobal("angle_gl_driver_selection_values"),
                supportedStatus = writeStatus,
                requiredPermission = "Shizuku",
                androidCompatibility = "Android 10+ (API 29+)",
                deviceCompatibility = "Depends on GPU driver",
                riskLevel = RiskLevel.MODERATE,
                validationResult = "Enumerated value (angle/native)",
                operationStatus = writeStatus,
                verificationResult = "Read-back via Settings.Global",
                documentation = "Paired 1:1 with angle_gl_driver_selection_pkgs."
            ),
            SettingRegistryEntry(
                settingId = "peak_refresh",
                category = "Display",
                name = "Peak Refresh Rate",
                description = "Upper bound display refresh rate in Hz (populated strictly from hardware Display.Mode).",
                namespace = "system",
                key = "peak_refresh_rate",
                defaultValue = "ROM Default",
                currentValue = readSystem("peak_refresh_rate"),
                supportedStatus = writeStatus,
                requiredPermission = "Shizuku",
                androidCompatibility = "Android 10+ (API 29+)",
                deviceCompatibility = "Multi-Hz panel required",
                riskLevel = RiskLevel.MODERATE,
                validationResult = "Validated against Display.supportedModes",
                operationStatus = writeStatus,
                verificationResult = "Read-back via Settings.System",
                documentation = "Some OEM ROMs override this per-app internally."
            ),
            SettingRegistryEntry(
                settingId = "min_refresh",
                category = "Display",
                name = "Minimum Refresh Rate",
                description = "Lower bound display refresh rate in Hz.",
                namespace = "system",
                key = "min_refresh_rate",
                defaultValue = "ROM Default",
                currentValue = readSystem("min_refresh_rate"),
                supportedStatus = writeStatus,
                requiredPermission = "Shizuku",
                androidCompatibility = "Android 10+ (API 29+)",
                deviceCompatibility = "Multi-Hz panel required",
                riskLevel = RiskLevel.MODERATE,
                validationResult = "Validated against Display.supportedModes",
                operationStatus = writeStatus,
                verificationResult = "Read-back via Settings.System",
                documentation = "Setting min_refresh_rate equal to peak_refresh_rate requests a fixed refresh rate."
            ),
            SettingRegistryEntry(
                settingId = "window_anim",
                category = "UI & Input Transitions",
                name = "Window Animation Scale",
                description = "Controls system window transition speed (0, 0.25, 0.5, 0.75, 1.0).",
                namespace = "global",
                key = "window_animation_scale",
                defaultValue = "1.0",
                currentValue = readGlobal("window_animation_scale"),
                supportedStatus = writeStatus,
                requiredPermission = "Shizuku",
                androidCompatibility = "All Android versions",
                deviceCompatibility = "Universal",
                riskLevel = RiskLevel.LOW,
                validationResult = "Allowed scale set",
                operationStatus = writeStatus,
                verificationResult = "Read-back via Settings.Global",
                documentation = "Cosmetic UI transition speed only; does not change touch hardware polling."
            ),
            SettingRegistryEntry(
                settingId = "transition_anim",
                category = "UI & Input Transitions",
                name = "Transition Animation Scale",
                description = "Controls activity transition animation speed.",
                namespace = "global",
                key = "transition_animation_scale",
                defaultValue = "1.0",
                currentValue = readGlobal("transition_animation_scale"),
                supportedStatus = writeStatus,
                requiredPermission = "Shizuku",
                androidCompatibility = "All Android versions",
                deviceCompatibility = "Universal",
                riskLevel = RiskLevel.LOW,
                validationResult = "Allowed scale set",
                operationStatus = writeStatus,
                verificationResult = "Read-back via Settings.Global",
                documentation = "Standard Developer Options animation scale."
            ),
            SettingRegistryEntry(
                settingId = "animator_duration",
                category = "UI & Input Transitions",
                name = "Animator Duration Scale",
                description = "Controls ValueAnimator duration multiplier across UI.",
                namespace = "global",
                key = "animator_duration_scale",
                defaultValue = "1.0",
                currentValue = readGlobal("animator_duration_scale"),
                supportedStatus = writeStatus,
                requiredPermission = "Shizuku",
                androidCompatibility = "All Android versions",
                deviceCompatibility = "Universal",
                riskLevel = RiskLevel.LOW,
                validationResult = "Allowed scale set",
                operationStatus = writeStatus,
                verificationResult = "Read-back via Settings.Global",
                documentation = "Standard Developer Options animator scale."
            ),
            SettingRegistryEntry(
                settingId = "app_standby",
                category = "Background & Doze",
                name = "App Standby Enabled",
                description = "Controls Android App Standby bucket enforcement.",
                namespace = "global",
                key = "app_standby_enabled",
                defaultValue = "1",
                currentValue = readGlobal("app_standby_enabled"),
                supportedStatus = writeStatus,
                requiredPermission = "Shizuku",
                androidCompatibility = "Android 10+",
                deviceCompatibility = "ROM-dependent",
                riskLevel = RiskLevel.MODERATE,
                validationResult = "Binary 0/1",
                operationStatus = writeStatus,
                verificationResult = "Read-back via Settings.Global",
                documentation = "Does not kill foreground apps; controls standby scheduling."
            ),
            SettingRegistryEntry(
                settingId = "cached_freezer",
                category = "Memory & Background",
                name = "Cached Apps Freezer",
                description = "Android cgroup freezer state for cached background processes.",
                namespace = "global",
                key = "cached_apps_freezer",
                defaultValue = "device_default",
                currentValue = readGlobal("cached_apps_freezer"),
                supportedStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) writeStatus else OperationStatus.UNSUPPORTED,
                requiredPermission = "Shizuku",
                androidCompatibility = "Android 11+ (API 30+)",
                deviceCompatibility = "Requires kernel cgroup v2 freezer support",
                riskLevel = RiskLevel.MODERATE,
                validationResult = "enabled/disabled/device_default",
                operationStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) writeStatus else OperationStatus.UNSUPPORTED,
                verificationResult = "Read-back via Settings.Global",
                documentation = "Freezes cached apps instead of killing them, saving CPU & battery."
            ),
            SettingRegistryEntry(
                settingId = "low_power",
                category = "Battery & Power",
                name = "System Battery Saver State",
                description = "Indicates whether Android Battery Saver (low_power) is active.",
                namespace = "global",
                key = "low_power",
                defaultValue = "0",
                currentValue = readGlobal("low_power"),
                supportedStatus = writeStatus,
                requiredPermission = "Shizuku",
                androidCompatibility = "Android 10+",
                deviceCompatibility = "Universal",
                riskLevel = RiskLevel.LOW,
                validationResult = "0/1",
                operationStatus = writeStatus,
                verificationResult = "Read-back via Settings.Global",
                documentation = "Read-only via PowerManager.isPowerSaveMode; writable via Shizuku."
            ),
            SettingRegistryEntry(
                settingId = "game_mode_cmd",
                gamePackage = selectedGamePackage,
                category = "GameManager & ART",
                name = "Android Game Mode (cmd game mode)",
                description = "Assigns performance, standard, or battery game mode hint per game package.",
                namespace = "cmd",
                key = "game mode",
                defaultValue = "standard",
                currentValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Supported on API ${Build.VERSION.SDK_INT}" else "Unsupported (< API 31)",
                supportedStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) writeStatus else OperationStatus.UNSUPPORTED,
                requiredPermission = "Shizuku (Android 12+)",
                androidCompatibility = "Android 12+ (API 31+)",
                deviceCompatibility = "ROM GameManager dependent",
                riskLevel = RiskLevel.LOW,
                validationResult = "performance/standard/battery",
                operationStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) writeStatus else OperationStatus.UNSUPPORTED,
                verificationResult = "Verified via dumpsys game",
                documentation = "Documented Android 12 GameManager shell command."
            ),
            SettingRegistryEntry(
                settingId = "art_compiler",
                gamePackage = selectedGamePackage,
                category = "GameManager & ART",
                name = "ART Bytecode Compiler (cmd package compile)",
                description = "Ahead-of-time compiles game DEX bytecode using speed-profile, speed, or everything.",
                namespace = "cmd",
                key = "package compile",
                defaultValue = "speed-profile",
                currentValue = "On-demand execution",
                supportedStatus = writeStatus,
                requiredPermission = "Shizuku",
                androidCompatibility = "Android 10+",
                deviceCompatibility = "Universal",
                riskLevel = RiskLevel.LOW,
                validationResult = "speed-profile/speed/everything",
                operationStatus = writeStatus,
                verificationResult = "Command exit code + Success output",
                documentation = "Pre-compiles game methods via Android Runtime dexopt."
            )
        )
    }
}
