package com.symphine.gamespace.performance

import android.os.Build
import android.os.PowerManager
import com.symphine.gamespace.core.model.DeviceHardwareInfo
import com.symphine.gamespace.core.model.DisplayHardwareInfo
import com.symphine.gamespace.core.model.GameProfileType
import com.symphine.gamespace.core.model.GraphicsHardwareInfo
import com.symphine.gamespace.core.model.MemoryHardwareInfo
import com.symphine.gamespace.core.model.OperationStatus
import com.symphine.gamespace.core.model.ThermalHardwareInfo
import com.symphine.gamespace.shizuku.ShizukuStatusState

data class ProfileRecommendation(
    val profileType: GameProfileType,
    val title: String,
    val reason: String,
    val supportedChanges: List<String>,
    val unsupportedOrConditional: List<String>,
    val warnings: List<String>,
    val compatibilityStatus: OperationStatus,
    val suggestedRefreshHz: Float?,
    val suggestedAnimationScale: Float?,
    val suggestedGameMode: String?,
    val suggestedArtCompileMode: String?
)

class RecommendationEngine {

    fun generateRecommendations(
        device: DeviceHardwareInfo,
        memory: MemoryHardwareInfo,
        display: DisplayHardwareInfo,
        thermal: ThermalHardwareInfo,
        graphics: GraphicsHardwareInfo,
        shizuku: ShizukuStatusState
    ): List<ProfileRecommendation> {
        val totalRamGb = memory.totalRamBytes / (1024.0 * 1024.0 * 1024.0)
        val availRamGb = memory.availableRamBytes / (1024.0 * 1024.0 * 1024.0)
        val maxHz = display.supportedRefreshRates.maxOrNull() ?: 60f
        val minHz = display.supportedRefreshRates.minOrNull() ?: 60f
        val supportsGameMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val shizukuReady = shizuku.isOperational

        val list = mutableListOf<ProfileRecommendation>()

        // 1. Recommended (Hardware-Adaptive)
        val recHz = when {
            thermal.statusCode >= PowerManager.THERMAL_STATUS_MODERATE -> minHz
            display.supportedRefreshRates.any { it in 89f..91f } && totalRamGb <= 6.5 -> 90f
            else -> maxHz
        }
        val recReason = buildString {
            append("Analyzed ${device.manufacturer} ${device.model} (${String.format("%.1f", totalRamGb)} GB RAM, ")
            append("Android ${device.androidVersion}, Thermal: ${thermal.statusLabel}). ")
            if (thermal.statusCode >= PowerManager.THERMAL_STATUS_MODERATE) {
                append("Elevated thermal load detected — prioritizing thermal stability over peak refresh rate.")
            } else if (availRamGb < 1.5) {
                append("Low available RAM (${String.format("%.2f", availRamGb)} GB) — recommending speed-profile ART compilation & balanced refresh.")
            } else {
                append("Healthy thermal headroom & ${display.supportedRefreshRates.joinToString("/")} Hz display modes detected.")
            }
        }

        list += ProfileRecommendation(
            profileType = GameProfileType.RECOMMENDED,
            title = "Recommended for ${device.model}",
            reason = recReason,
            supportedChanges = listOfNotNull(
                "Local telemetry & session logging (Always supported)",
                "Refresh target: ${recHz.toInt()} Hz (from verified Display.Mode)".takeIf { display.supportedRefreshRates.size > 1 },
                "UI Animation Scale: 0.5x".takeIf { shizukuReady },
                "ART Bytecode speed-profile".takeIf { shizukuReady },
                "Android Game Mode: performance".takeIf { shizukuReady && supportsGameMode }
            ),
            unsupportedOrConditional = listOfNotNull(
                "System-wide Settings/Display writes require Shizuku".takeIf { !shizukuReady },
                "Android Game Mode requires Android 12+".takeIf { !supportsGameMode },
                "CPU/GPU clock & voltage locking unsupported on non-root stock kernels"
            ),
            warnings = listOfNotNull(
                "Thermal status is currently ${thermal.statusLabel}.".takeIf { thermal.isThrottling },
                "No fake FPS guarantees — real stability depends on game engine & OEM thermal policy."
            ),
            compatibilityStatus = if (shizukuReady) OperationStatus.AVAILABLE else OperationStatus.REQUIRES_SHIZUKU,
            suggestedRefreshHz = recHz,
            suggestedAnimationScale = 0.5f,
            suggestedGameMode = if (supportsGameMode) "performance" else null,
            suggestedArtCompileMode = "speed-profile"
        )

        // 2. Gaming Profile
        list += ProfileRecommendation(
            profileType = GameProfileType.GAMING,
            title = "Gaming Performance",
            reason = "Prioritizes highest verified panel refresh rate (${maxHz.toInt()} Hz), 0.5x transition animations, and Game Mode Performance.",
            supportedChanges = listOfNotNull(
                "Refresh target: ${maxHz.toInt()} Hz",
                "UI animation scale: 0.5x",
                "Game Mode: performance (API 31+)".takeIf { supportsGameMode },
                "Optional ANGLE driver assignment (${graphics.anglePackageName})".takeIf { graphics.angleInstalled }
            ),
            unsupportedOrConditional = listOf(
                if (shizukuReady) "debug.angle.feature_overrides_enabled requires root/SELinux permissive on most ROMs" else "Requires Shizuku connection",
                "GPU utilization % unavailable via standard Android API"
            ),
            warnings = listOf("Higher refresh rates increase battery drain and thermal output."),
            compatibilityStatus = if (shizukuReady) OperationStatus.AVAILABLE else OperationStatus.REQUIRES_SHIZUKU,
            suggestedRefreshHz = maxHz,
            suggestedAnimationScale = 0.5f,
            suggestedGameMode = if (supportsGameMode) "performance" else null,
            suggestedArtCompileMode = "speed"
        )

        // 3. Balanced Profile
        list += ProfileRecommendation(
            profileType = GameProfileType.BALANCED,
            title = "Balanced Default",
            reason = "Restores standard Android defaults (1.0x animations, standard Game Mode, ROM-managed refresh).",
            supportedChanges = listOf(
                "UI animation scale: 1.0x",
                "Reset peak/min refresh rate to ROM default",
                "Game Mode: standard (API 31+)"
            ),
            unsupportedOrConditional = emptyList(),
            warnings = emptyList(),
            compatibilityStatus = OperationStatus.AVAILABLE,
            suggestedRefreshHz = null,
            suggestedAnimationScale = 1.0f,
            suggestedGameMode = if (supportsGameMode) "standard" else null,
            suggestedArtCompileMode = null
        )

        // 4. Battery Saver Profile
        list += ProfileRecommendation(
            profileType = GameProfileType.BATTERY_SAVER,
            title = "Battery & Thermal Saver",
            reason = "Targets lowest verified panel refresh rate (${minHz.toInt()} Hz) and Android Game Mode Battery.",
            supportedChanges = listOfNotNull(
                "Refresh target: ${minHz.toInt()} Hz",
                "Game Mode: battery (API 31+)".takeIf { supportsGameMode }
            ),
            unsupportedOrConditional = listOf(
                if (shizukuReady) "ROM adaptive refresh override may still apply" else "Requires Shizuku for refresh & game mode writes"
            ),
            warnings = listOf("Lower refresh rates trade smoothness for longer battery endurance."),
            compatibilityStatus = if (shizukuReady) OperationStatus.AVAILABLE else OperationStatus.REQUIRES_SHIZUKU,
            suggestedRefreshHz = minHz,
            suggestedAnimationScale = 1.0f,
            suggestedGameMode = if (supportsGameMode) "battery" else null,
            suggestedArtCompileMode = null
        )

        return list
    }
}
