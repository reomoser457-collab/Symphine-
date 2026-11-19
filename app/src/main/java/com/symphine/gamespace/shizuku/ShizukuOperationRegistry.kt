package com.symphine.gamespace.shizuku

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.symphine.gamespace.core.model.OperationPreview
import com.symphine.gamespace.core.model.OperationRecord
import com.symphine.gamespace.core.model.OperationStatus
import com.symphine.gamespace.core.model.RiskLevel
import com.symphine.gamespace.core.model.SettingChangePreview
import com.symphine.gamespace.data.database.OriginalBackupSnapshotEntity
import com.symphine.gamespace.data.database.OperationHistoryEntity
import com.symphine.gamespace.data.database.SymphineDao
import com.symphine.gamespace.device.DisplayInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ShizukuOperationRegistry(
    private val context: Context,
    private val shizukuManager: ShizukuManager,
    private val displayInfoProvider: DisplayInfoProvider,
    private val dao: SymphineDao
) {

    // -------------------- PREVIEW GENERATORS (Step 1-6 of Verification Pipeline) --------------------

    suspend fun previewAngleGraphics(
        packageName: String,
        gameTitle: String,
        mode: String, // "smooth" | "pixel" | "reset"
        lod: Int
    ): OperationPreview = withContext(Dispatchers.IO) {
        val currentPkgs = readSetting("global", "angle_gl_driver_selection_pkgs")
        val currentVals = readSetting("global", "angle_gl_driver_selection_values")
        val status = checkShizukuOperationStatus()

        if (mode == "reset") {
            return@withContext OperationPreview(
                operationId = "angle_reset_$packageName",
                title = "Reset ANGLE Driver Selection",
                description = "Clears global ANGLE package/driver selection back to system default.",
                targetPackage = packageName,
                changes = listOf(
                    SettingChangePreview("angle_gl_driver_selection_pkgs", "global", currentPkgs, "(delete / default)"),
                    SettingChangePreview("angle_gl_driver_selection_values", "global", currentVals, "(delete / default)")
                ),
                compatibilityStatus = status,
                riskLevel = RiskLevel.MODERATE,
                warning = "Driver compatibility depends on device GPU & ROM ANGLE support.",
                restoreInstructions = "Use 'Restore Original Settings' or 'Reset ANGLE Driver' anytime."
            )
        }

        val featureStr = when (mode) {
            "smooth" -> "disableAnisotropicFiltering:forceTextureLodOffset$lod"
            else -> "forceNearestFiltering:disableAnisotropicFiltering:forceTextureLodOffset$lod"
        }

        OperationPreview(
            operationId = "angle_${mode}_${packageName}_$lod",
            title = "ANGLE Graphics Profile (${mode.uppercase()} • LOD $lod)",
            description = "Configures Android ANGLE GL driver selection for $gameTitle ($packageName). Optional debug.angle.feature_overrides_enabled is attempted only if permitted by SELinux/root.",
            targetPackage = packageName,
            changes = listOf(
                SettingChangePreview("angle_gl_driver_selection_pkgs", "global", currentPkgs, packageName),
                SettingChangePreview("angle_gl_driver_selection_values", "global", currentVals, "angle"),
                SettingChangePreview("debug.angle.feature_overrides_enabled", "prop", readProp("debug.angle.feature_overrides_enabled"), featureStr)
            ),
            compatibilityStatus = status,
            riskLevel = RiskLevel.MODERATE,
            warning = "Does not modify game APK or memory. Selecting ANGLE does not guarantee higher FPS on every GPU.",
            restoreInstructions = "Original values are snapshotted before applying. Tap Restore in Shizuku Center or Game Detail."
        )
    }

    suspend fun previewArtCompile(packageName: String, gameTitle: String, mode: String): OperationPreview = withContext(Dispatchers.IO) {
        OperationPreview(
            operationId = "art_compile_${packageName}_$mode",
            title = "ART Bytecode Compiler ($mode)",
            description = "Runs Android Runtime ahead-of-time compilation (cmd package compile -m $mode -f) for $gameTitle.",
            targetPackage = packageName,
            changes = listOf(
                SettingChangePreview("art_compiler_filter", "cmd package", "Current dexopt state", mode)
            ),
            compatibilityStatus = checkShizukuOperationStatus(),
            riskLevel = RiskLevel.LOW,
            warning = "Compilation takes 10–45 seconds depending on game size.",
            restoreInstructions = "ART compilation profiles remain safe and reversible via Android package manager."
        )
    }

    suspend fun previewGameMode(packageName: String, gameTitle: String, mode: String): OperationPreview = withContext(Dispatchers.IO) {
        val status = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            OperationStatus.UNSUPPORTED
        } else {
            checkShizukuOperationStatus()
        }
        OperationPreview(
            operationId = "gamemode_${packageName}_$mode",
            title = "Android Game Mode ($mode)",
            description = "Requests Android GameManager mode '$mode' for $gameTitle via cmd game mode.",
            targetPackage = packageName,
            changes = listOf(
                SettingChangePreview("cmd game mode", "game_service", "Current ROM state", mode)
            ),
            compatibilityStatus = status,
            riskLevel = RiskLevel.LOW,
            warning = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                "Android Game Mode requires Android 12 (API 31)+."
            } else {
                "OEM ROMs decide whether a game honors GameManager hints."
            },
            restoreInstructions = "Switch back to 'standard' anytime."
        )
    }

    suspend fun previewRefreshRate(hz: Float?): OperationPreview = withContext(Dispatchers.IO) {
        val currentPeak = readSetting("system", "peak_refresh_rate")
        val currentMin = readSetting("system", "min_refresh_rate")
        val targetStr = hz?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "(delete / ROM default)"
        OperationPreview(
            operationId = "refresh_${hz ?: "reset"}",
            title = if (hz != null) "Set Refresh Rate Target (${targetStr} Hz)" else "Reset Refresh Rate to ROM Default",
            description = "Updates system peak_refresh_rate and min_refresh_rate using verified device Display.Mode values.",
            changes = listOf(
                SettingChangePreview("peak_refresh_rate", "system", currentPeak, targetStr),
                SettingChangePreview("min_refresh_rate", "system", currentMin, targetStr)
            ),
            compatibilityStatus = checkShizukuOperationStatus(),
            riskLevel = RiskLevel.MODERATE,
            warning = "Some OEM ROMs enforce per-app refresh rate overrides independently of Settings.System.",
            restoreInstructions = "Original peak/min refresh rates are snapshotted and can be restored anytime."
        )
    }

    suspend fun previewAnimationScale(scale: Float): OperationPreview = withContext(Dispatchers.IO) {
        val scaleStr = if (scale % 1f == 0f) scale.toInt().toString() else scale.toString()
        OperationPreview(
            operationId = "anim_$scaleStr",
            title = "UI & Touch Animation Scale (${scaleStr}x)",
            description = "Sets window, transition, and animator duration scales in Settings.Global.",
            changes = listOf(
                SettingChangePreview("window_animation_scale", "global", readSetting("global", "window_animation_scale"), scaleStr),
                SettingChangePreview("transition_animation_scale", "global", readSetting("global", "transition_animation_scale"), scaleStr),
                SettingChangePreview("animator_duration_scale", "global", readSetting("global", "animator_duration_scale"), scaleStr)
            ),
            compatibilityStatus = checkShizukuOperationStatus(),
            riskLevel = RiskLevel.LOW,
            warning = "Affects system UI transitions only; does not alter hardware touch sampling rate.",
            restoreInstructions = "Set to 1.0x or run Atomic Restore."
        )
    }

    suspend fun previewBackgroundFlags(): OperationPreview = withContext(Dispatchers.IO) {
        OperationPreview(
            operationId = "background_optimizer",
            title = "Background & Power Flag Profile",
            description = "Applies safe documented Settings.Global power/freezer flags (no force-stopping of apps).",
            changes = listOf(
                SettingChangePreview("app_standby_enabled", "global", readSetting("global", "app_standby_enabled"), "1"),
                SettingChangePreview("app_restriction_enabled", "global", readSetting("global", "app_restriction_enabled"), "0"),
                SettingChangePreview("background_check_enabled", "global", readSetting("global", "background_check_enabled"), "0"),
                SettingChangePreview("low_power", "global", readSetting("global", "low_power"), "0"),
                SettingChangePreview("cached_apps_freezer", "global", readSetting("global", "cached_apps_freezer"), "enabled")
            ),
            compatibilityStatus = checkShizukuOperationStatus(),
            riskLevel = RiskLevel.MODERATE,
            warning = "Support varies by Android version and OEM ROM. Unsupported keys will be marked Failed/Unsupported transparently.",
            restoreInstructions = "All original keys are backed up prior to modification."
        )
    }

    suspend fun previewDpiChange(targetDpi: Int?): OperationPreview = withContext(Dispatchers.IO) {
        val currentDensity = shizukuManager.getServiceOrNull()?.let { runCatching { it.wmDensity }.getOrNull() }
            ?: "${displayInfoProvider.getDisplayInfo().densityDpi} dpi"
        OperationPreview(
            operationId = "dpi_${targetDpi ?: "reset"}",
            title = if (targetDpi != null) "Set Display Density ($targetDpi DPI)" else "Reset Display Density",
            description = "Changes display density via WindowManager (wm density). Includes original snapshot and rollback.",
            changes = listOf(
                SettingChangePreview("wm density", "wm", currentDensity, targetDpi?.toString() ?: "reset")
            ),
            compatibilityStatus = checkShizukuOperationStatus(),
            riskLevel = RiskLevel.HIGH,
            warning = "Extreme DPI values can make UI elements hard to tap. Validated safe range: 160–720 DPI (hard limit 120–1000).",
            restoreInstructions = "Tap 'Restore DPI' or run 'wm density reset' via ADB/Shizuku if needed."
        )
    }

    suspend fun previewDownscale(percent: Int?): OperationPreview = withContext(Dispatchers.IO) {
        val info = displayInfoProvider.getDisplayInfo()
        val wmSize = shizukuManager.getServiceOrNull()?.let { runCatching { it.wmSize }.getOrNull() }
            ?: "${info.widthPx}x${info.heightPx}"
        if (percent == null) {
            return@withContext OperationPreview(
                operationId = "downscale_reset",
                title = "Reset Resolution & Density",
                description = "Restores physical screen resolution (wm size reset) and default density (wm density reset).",
                changes = listOf(
                    SettingChangePreview("wm size", "wm", wmSize, "reset"),
                    SettingChangePreview("wm density", "wm", "${info.densityDpi}", "reset")
                ),
                compatibilityStatus = checkShizukuOperationStatus(),
                riskLevel = RiskLevel.HIGH,
                warning = null,
                restoreInstructions = "Resets immediately to hardware physical resolution."
            )
        }
        val physW = parsePhysicalDimension(wmSize)?.first ?: info.widthPx
        val physH = parsePhysicalDimension(wmSize)?.second ?: info.heightPx
        val newW = ((physW * percent / 100.0) / 2.0).toInt() * 2
        val newH = ((physH * percent / 100.0) / 2.0).toInt() * 2
        val newDpi = ((info.densityDpi * percent) / 100).coerceAtLeast(120)

        OperationPreview(
            operationId = "downscale_$percent",
            title = "Proportional Screen Downscale ($percent%)",
            description = "Proportionally scales resolution and DPI while preserving aspect ratio.",
            changes = listOf(
                SettingChangePreview("wm size", "wm", "${physW}x${physH}", "${newW}x${newH}"),
                SettingChangePreview("wm density (proportional)", "wm", "${info.densityDpi}", "$newDpi")
            ),
            compatibilityStatus = checkShizukuOperationStatus(),
            riskLevel = RiskLevel.HIGH,
            warning = "Resolution downscale affects the entire display until reset. Some launchers resize icons.",
            restoreInstructions = "Use 'Reset Resolution & Density' or Atomic Full Restore."
        )
    }

    // -------------------- VERIFIED EXECUTIONS (Step 7-13 of Pipeline) --------------------

    suspend fun executeAngleGraphics(
        packageName: String,
        mode: String,
        lod: Int,
        restartGame: Boolean = false
    ): List<OperationRecord> = withContext(Dispatchers.IO) {
        val svc = shizukuManager.getServiceOrNull() ?: return@withContext listOf(
            recordFailure("ANGLE Graphics", packageName, "angle_gl_driver_selection_pkgs", OperationStatus.REQUIRES_SHIZUKU, "Shizuku UserService is not connected.")
        )
        if (mode == "reset") {
            return@withContext listOf(
                deleteAndVerify(svc, "Reset ANGLE Packages", packageName, "global", "angle_gl_driver_selection_pkgs"),
                deleteAndVerify(svc, "Reset ANGLE Driver", packageName, "global", "angle_gl_driver_selection_values")
            )
        }
        val records = mutableListOf<OperationRecord>()
        records += putAndVerifySetting(svc, "ANGLE Target Package", packageName, "global", "angle_gl_driver_selection_pkgs", packageName)
        records += putAndVerifySetting(svc, "ANGLE Driver Mode", packageName, "global", "angle_gl_driver_selection_values", "angle")

        val featureStr = when (mode) {
            "smooth" -> "disableAnisotropicFiltering:forceTextureLodOffset$lod"
            else -> "forceNearestFiltering:disableAnisotropicFiltering:forceTextureLodOffset$lod"
        }
        val origProp = runCatching { svc.getSystemProperty("debug.angle.feature_overrides_enabled") }.getOrNull()
        snapshotIfAbsent("prop", "debug.angle.feature_overrides_enabled", origProp ?: "null")
        val propOut = runCatching { svc.setAngleFeatureOverride(featureStr) }.getOrElse { "ERROR:${it.message}" }
        val readBack = runCatching { svc.getSystemProperty("debug.angle.feature_overrides_enabled") }.getOrNull()
        val propRecord = if (readBack == featureStr) {
            OperationRecord(
                id = UUID.randomUUID().toString(),
                operationName = "ANGLE Feature Property",
                targetPackage = packageName,
                settingKey = "debug.angle.feature_overrides_enabled",
                originalValue = origProp,
                requestedValue = featureStr,
                verifiedValue = readBack,
                status = OperationStatus.VERIFIED,
                message = "Verified property write: $featureStr"
            )
        } else {
            OperationRecord(
                id = UUID.randomUUID().toString(),
                operationName = "ANGLE Feature Property",
                targetPackage = packageName,
                settingKey = "debug.angle.feature_overrides_enabled",
                originalValue = origProp,
                requestedValue = featureStr,
                verifiedValue = readBack,
                status = OperationStatus.UNSUPPORTED,
                message = "Skipped or restricted by SELinux (requires root on most ROMs): $propOut"
            )
        }
        saveRecord(propRecord)
        records += propRecord

        if (restartGame) {
            val stopOut = runCatching { svc.forceStopGamePackage(packageName) }.getOrElse { "ERROR:${it.message}" }
            val stopRec = OperationRecord(
                id = UUID.randomUUID().toString(),
                operationName = "Restart Game Process",
                targetPackage = packageName,
                status = if (!stopOut.startsWith("ERROR")) OperationStatus.APPLIED else OperationStatus.FAILED,
                message = if (!stopOut.startsWith("ERROR")) "Game process stopped cleanly for driver reload." else stopOut
            )
            saveRecord(stopRec)
            records += stopRec
        }
        records
    }

    suspend fun executeArtCompile(packageName: String, mode: String): OperationRecord = withContext(Dispatchers.IO) {
        val svc = shizukuManager.getServiceOrNull() ?: return@withContext recordFailure(
            "ART Bytecode Compiler", packageName, "cmd package compile", OperationStatus.REQUIRES_SHIZUKU, "Shizuku UserService is not connected."
        )
        val out = runCatching { svc.compilePackage(packageName, mode) }.getOrElse { "ERROR:${it.message}" }
        val success = !out.startsWith("ERROR") && (out.contains("Success", ignoreCase = true) || out == "OK")
        val rec = OperationRecord(
            id = UUID.randomUUID().toString(),
            operationName = "ART Bytecode Compiler ($mode)",
            targetPackage = packageName,
            settingKey = "art_dexopt",
            requestedValue = mode,
            verifiedValue = out,
            status = if (success) OperationStatus.VERIFIED else OperationStatus.FAILED,
            message = if (success) "ART compilation ($mode) completed: $out" else "ART compilation rejected or failed: $out"
        )
        saveRecord(rec)
        rec
    }

    suspend fun executeGameMode(packageName: String, mode: String): OperationRecord = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return@withContext recordFailure("Android Game Mode", packageName, "cmd game mode", OperationStatus.UNSUPPORTED, "Requires Android 12 (API 31)+")
        }
        val svc = shizukuManager.getServiceOrNull() ?: return@withContext recordFailure(
            "Android Game Mode", packageName, "cmd game mode", OperationStatus.REQUIRES_SHIZUKU, "Shizuku UserService is not connected."
        )
        val out = runCatching { svc.setGameMode(packageName, mode) }.getOrElse { "ERROR:${it.message}" }
        val ok = !out.startsWith("ERROR")
        val dump = if (ok) runCatching { svc.getGameServiceDump(packageName) }.getOrNull() else null
        val rec = OperationRecord(
            id = UUID.randomUUID().toString(),
            operationName = "Android Game Mode ($mode)",
            targetPackage = packageName,
            settingKey = "cmd game mode",
            requestedValue = mode,
            verifiedValue = dump?.lines()?.firstOrNull() ?: out,
            status = if (ok) OperationStatus.APPLIED else OperationStatus.FAILED,
            message = if (ok) "Game mode '$mode' accepted ($out)" else "Rejected by system: $out"
        )
        saveRecord(rec)
        rec
    }

    suspend fun executeRefreshRate(hz: Float?): List<OperationRecord> = withContext(Dispatchers.IO) {
        val svc = shizukuManager.getServiceOrNull() ?: return@withContext listOf(
            recordFailure("Refresh Rate", null, "peak_refresh_rate", OperationStatus.REQUIRES_SHIZUKU, "Shizuku UserService is not connected.")
        )
        if (hz == null) {
            return@withContext listOf(
                deleteAndVerify(svc, "Reset Peak Refresh Rate", null, "system", "peak_refresh_rate"),
                deleteAndVerify(svc, "Reset Min Refresh Rate", null, "system", "min_refresh_rate")
            )
        }
        val hzStr = if (hz % 1f == 0f) hz.toInt().toString() else hz.toString()
        listOf(
            putAndVerifySetting(svc, "Peak Refresh Rate", null, "system", "peak_refresh_rate", hzStr),
            putAndVerifySetting(svc, "Min Refresh Rate", null, "system", "min_refresh_rate", hzStr)
        )
    }

    suspend fun executeAnimationScale(scale: Float): List<OperationRecord> = withContext(Dispatchers.IO) {
        val svc = shizukuManager.getServiceOrNull() ?: return@withContext listOf(
            recordFailure("Animation Scale", null, "window_animation_scale", OperationStatus.REQUIRES_SHIZUKU, "Shizuku UserService is not connected.")
        )
        val valStr = if (scale % 1f == 0f) scale.toInt().toString() else scale.toString()
        listOf(
            putAndVerifySetting(svc, "Window Animation Scale", null, "global", "window_animation_scale", valStr),
            putAndVerifySetting(svc, "Transition Animation Scale", null, "global", "transition_animation_scale", valStr),
            putAndVerifySetting(svc, "Animator Duration Scale", null, "global", "animator_duration_scale", valStr)
        )
    }

    suspend fun executeBackgroundFlags(): List<OperationRecord> = withContext(Dispatchers.IO) {
        val svc = shizukuManager.getServiceOrNull() ?: return@withContext listOf(
            recordFailure("Background Flags", null, "app_standby_enabled", OperationStatus.REQUIRES_SHIZUKU, "Shizuku UserService is not connected.")
        )
        listOf(
            putAndVerifySetting(svc, "App Standby Flag", null, "global", "app_standby_enabled", "1"),
            putAndVerifySetting(svc, "App Restriction Flag", null, "global", "app_restriction_enabled", "0"),
            putAndVerifySetting(svc, "Background Check Flag", null, "global", "background_check_enabled", "0"),
            putAndVerifySetting(svc, "Low Power Mode Off", null, "global", "low_power", "0"),
            putAndVerifySetting(svc, "Cached Apps Freezer", null, "global", "cached_apps_freezer", "enabled")
        )
    }

    suspend fun executeDpiChange(targetDpi: Int?): OperationRecord = withContext(Dispatchers.IO) {
        val svc = shizukuManager.getServiceOrNull() ?: return@withContext recordFailure(
            "Display Density", null, "wm density", OperationStatus.REQUIRES_SHIZUKU, "Shizuku UserService is not connected."
        )
        val before = runCatching { svc.wmDensity }.getOrDefault("")
        snapshotIfAbsent("wm", "density", parseOverrideValue(before, "Override density:") ?: "null")

        if (targetDpi == null) {
            val out = runCatching { svc.resetWmDensity() }.getOrElse { "ERROR:${it.message}" }
            val after = runCatching { svc.wmDensity }.getOrDefault("")
            val cleared = !after.contains("Override density:") && !out.startsWith("ERROR")
            val rec = OperationRecord(
                id = UUID.randomUUID().toString(),
                operationName = "Reset Display Density",
                settingKey = "wm density",
                originalValue = before,
                requestedValue = "reset",
                verifiedValue = after,
                status = if (cleared) OperationStatus.VERIFIED else OperationStatus.FAILED,
                message = if (cleared) "Density reset verified ($after)" else "Reset verification failed: $out / $after"
            )
            saveRecord(rec)
            return@withContext rec
        }

        if (targetDpi !in 120..1000) {
            return@withContext recordFailure("Display Density", null, "wm density", OperationStatus.FAILED, "DPI $targetDpi outside validated 120–1000 range")
        }
        val out = runCatching { svc.setWmDensity(targetDpi) }.getOrElse { "ERROR:${it.message}" }
        val after = runCatching { svc.wmDensity }.getOrDefault("")
        val verified = after.contains("Override density: $targetDpi")
        val rec = OperationRecord(
            id = UUID.randomUUID().toString(),
            operationName = "Set Display Density ($targetDpi DPI)",
            settingKey = "wm density",
            originalValue = before,
            requestedValue = targetDpi.toString(),
            verifiedValue = after,
            status = if (verified) OperationStatus.VERIFIED else OperationStatus.FAILED,
            message = if (verified) "Density $targetDpi verified" else "Verification failed ($out): $after"
        )
        saveRecord(rec)
        rec
    }

    suspend fun executeDownscale(percent: Int?): List<OperationRecord> = withContext(Dispatchers.IO) {
        val svc = shizukuManager.getServiceOrNull() ?: return@withContext listOf(
            recordFailure("Proportional Downscale", null, "wm size", OperationStatus.REQUIRES_SHIZUKU, "Shizuku UserService is not connected.")
        )
        if (percent == null) {
            val resOut = runCatching { svc.resetWmSize() }.getOrElse { "ERROR:${it.message}" }
            val dpiOut = runCatching { svc.resetWmDensity() }.getOrElse { "ERROR:${it.message}" }
            val sizeAfter = runCatching { svc.wmSize }.getOrDefault("")
            val dpiAfter = runCatching { svc.wmDensity }.getOrDefault("")
            val ok = !resOut.startsWith("ERROR") && !dpiOut.startsWith("ERROR") && !sizeAfter.contains("Override size:")
            val rec = OperationRecord(
                id = UUID.randomUUID().toString(),
                operationName = "Reset Resolution & Density",
                settingKey = "wm size + density",
                requestedValue = "reset",
                verifiedValue = "$sizeAfter | $dpiAfter",
                status = if (ok) OperationStatus.VERIFIED else OperationStatus.FAILED,
                message = if (ok) "Physical resolution & density restored" else "Reset error: $resOut / $dpiOut"
            )
            saveRecord(rec)
            return@withContext listOf(rec)
        }

        if (percent !in 70..100) {
            return@withContext listOf(recordFailure("Proportional Downscale", null, "wm size", OperationStatus.FAILED, "Percentage must be 70–100%"))
        }
        val wmSizeBefore = runCatching { svc.wmSize }.getOrDefault("")
        val wmDensityBefore = runCatching { svc.wmDensity }.getOrDefault("")
        snapshotIfAbsent("wm", "size", parseOverrideValue(wmSizeBefore, "Override size:") ?: "null")
        snapshotIfAbsent("wm", "density", parseOverrideValue(wmDensityBefore, "Override density:") ?: "null")

        val phys = parsePhysicalDimension(wmSizeBefore)
            ?: return@withContext listOf(recordFailure("Proportional Downscale", null, "wm size", OperationStatus.FAILED, "Could not parse Physical size from: $wmSizeBefore"))
        val physDpi = parseOverrideValue(wmDensityBefore, "Physical density:")?.toIntOrNull()
            ?: displayInfoProvider.getDisplayInfo().densityDpi

        val newW = ((phys.first * percent / 100.0) / 2.0).toInt() * 2
        val newH = ((phys.second * percent / 100.0) / 2.0).toInt() * 2
        val targetRes = "${newW}x${newH}"
        val targetDpi = ((physDpi * percent) / 100).coerceAtLeast(120)

        val sizeOut = runCatching { svc.setWmSize(targetRes) }.getOrElse { "ERROR:${it.message}" }
        val sizeAfter = runCatching { svc.wmSize }.getOrDefault("")
        val sizeVerified = sizeAfter.contains("Override size: $targetRes")
        val sizeRec = OperationRecord(
            id = UUID.randomUUID().toString(),
            operationName = "Proportional Screen Downscale ($percent%)",
            settingKey = "wm size",
            originalValue = wmSizeBefore,
            requestedValue = targetRes,
            verifiedValue = sizeAfter,
            status = if (sizeVerified) OperationStatus.VERIFIED else OperationStatus.FAILED,
            message = if (sizeVerified) "Resolution $targetRes verified" else "Failed ($sizeOut): $sizeAfter"
        )
        saveRecord(sizeRec)
        if (!sizeVerified) return@withContext listOf(sizeRec)

        val dpiRec = executeDpiChange(targetDpi)
        listOf(sizeRec, dpiRec)
    }

    suspend fun executeAtomicRestoreAll(): List<OperationRecord> = withContext(Dispatchers.IO) {
        val svc = shizukuManager.getServiceOrNull() ?: return@withContext listOf(
            recordFailure("Atomic Restore", null, "snapshots", OperationStatus.REQUIRES_SHIZUKU, "Shizuku UserService is not connected.")
        )
        val snapshots = dao.getSnapshotsOnce()
        if (snapshots.isEmpty()) {
            return@withContext listOf(
                OperationRecord(
                    id = UUID.randomUUID().toString(),
                    operationName = "Atomic Full Restore",
                    status = OperationStatus.VERIFIED,
                    message = "No modified system snapshots recorded yet — system is already at baseline."
                )
            )
        }
        val results = mutableListOf<OperationRecord>()
        snapshots.forEach { snap ->
            when (snap.namespace) {
                "global", "system", "secure" -> {
                    val rec = if (snap.originalValue == "null" || snap.originalValue.isBlank()) {
                        deleteAndVerify(svc, "Restore ${snap.settingKey}", null, snap.namespace, snap.settingKey)
                    } else {
                        putAndVerifySetting(svc, "Restore ${snap.settingKey}", null, snap.namespace, snap.settingKey, snap.originalValue, skipSnapshot = true)
                    }
                    results += rec.copy(status = if (rec.status == OperationStatus.VERIFIED) OperationStatus.RESTORED else OperationStatus.ROLLBACK_FAILED)
                }
                "wm" -> {
                    if (snap.settingKey == "size") {
                        if (snap.originalValue == "null") svc.resetWmSize() else svc.setWmSize(snap.originalValue)
                        val after = svc.wmSize
                        val ok = if (snap.originalValue == "null") !after.contains("Override size:") else after.contains(snap.originalValue)
                        val rec = OperationRecord(
                            id = UUID.randomUUID().toString(),
                            operationName = "Restore wm size",
                            settingKey = "wm size",
                            requestedValue = snap.originalValue,
                            verifiedValue = after,
                            status = if (ok) OperationStatus.RESTORED else OperationStatus.ROLLBACK_FAILED,
                            message = if (ok) "Resolution restored" else "Resolution rollback check: $after"
                        )
                        saveRecord(rec)
                        results += rec
                    } else if (snap.settingKey == "density") {
                        val dpiVal = snap.originalValue.toIntOrNull()
                        if (dpiVal == null) svc.resetWmDensity() else svc.setWmDensity(dpiVal)
                        val after = svc.wmDensity
                        val ok = if (dpiVal == null) !after.contains("Override density:") else after.contains("Override density: $dpiVal")
                        val rec = OperationRecord(
                            id = UUID.randomUUID().toString(),
                            operationName = "Restore wm density",
                            settingKey = "wm density",
                            requestedValue = snap.originalValue,
                            verifiedValue = after,
                            status = if (ok) OperationStatus.RESTORED else OperationStatus.ROLLBACK_FAILED,
                            message = if (ok) "Density restored" else "Density rollback check: $after"
                        )
                        saveRecord(rec)
                        results += rec
                    }
                }
                "prop" -> {
                    if (snap.originalValue != "null") {
                        svc.setAngleFeatureOverride(snap.originalValue)
                    }
                }
            }
        }
        if (results.none { it.status == OperationStatus.ROLLBACK_FAILED }) {
            dao.clearSnapshots()
        }
        results
    }

    // -------------------- INTERNAL HELPERS --------------------

    private suspend fun putAndVerifySetting(
        svc: ISymphineShizukuService,
        opName: String,
        pkg: String?,
        namespace: String,
        key: String,
        value: String,
        skipSnapshot: Boolean = false
    ): OperationRecord {
        val original = runCatching { svc.getSetting(namespace, key) }.getOrNull()
        if (!skipSnapshot) {
            snapshotIfAbsent(namespace, key, original ?: "null")
        }
        val putOut = runCatching { svc.putSetting(namespace, key, value) }.getOrElse { "ERROR:${it.message}" }
        val readBack = runCatching { svc.getSetting(namespace, key) }.getOrNull()
        val verified = readBack == value && !putOut.startsWith("ERROR")
        val rec = OperationRecord(
            id = UUID.randomUUID().toString(),
            operationName = opName,
            targetPackage = pkg,
            settingKey = "$namespace/$key",
            originalValue = original,
            requestedValue = value,
            verifiedValue = readBack,
            status = if (verified) OperationStatus.VERIFIED else OperationStatus.FAILED,
            message = if (verified) "Verified $key = $readBack" else "Write/Verify failed ($putOut). Actual: $readBack"
        )
        saveRecord(rec)
        return rec
    }

    private suspend fun deleteAndVerify(
        svc: ISymphineShizukuService,
        opName: String,
        pkg: String?,
        namespace: String,
        key: String
    ): OperationRecord {
        val original = runCatching { svc.getSetting(namespace, key) }.getOrNull()
        val delOut = runCatching { svc.deleteSetting(namespace, key) }.getOrElse { "ERROR:${it.message}" }
        val readBack = runCatching { svc.getSetting(namespace, key) }.getOrNull()
        val verified = (readBack.isNullOrBlank() || readBack == "null") && !delOut.startsWith("ERROR")
        val rec = OperationRecord(
            id = UUID.randomUUID().toString(),
            operationName = opName,
            targetPackage = pkg,
            settingKey = "$namespace/$key",
            originalValue = original,
            requestedValue = "null (delete)",
            verifiedValue = readBack ?: "null",
            status = if (verified) OperationStatus.VERIFIED else OperationStatus.FAILED,
            message = if (verified) "Cleared $key" else "Failed to clear $key ($delOut)"
        )
        saveRecord(rec)
        return rec
    }

    private suspend fun snapshotIfAbsent(namespace: String, key: String, value: String) {
        dao.insertSnapshotIfAbsent(
            OriginalBackupSnapshotEntity(
                compositeKey = "$namespace|$key",
                namespace = namespace,
                settingKey = key,
                originalValue = value,
                capturedAtMillis = System.currentTimeMillis()
            )
        )
    }

    private suspend fun saveRecord(record: OperationRecord) {
        dao.insertOperationHistory(OperationHistoryEntity.fromModel(record))
    }

    private suspend fun recordFailure(
        opName: String,
        pkg: String?,
        key: String?,
        status: OperationStatus,
        msg: String
    ): OperationRecord {
        val rec = OperationRecord(
            id = UUID.randomUUID().toString(),
            operationName = opName,
            targetPackage = pkg,
            settingKey = key,
            status = status,
            message = msg
        )
        saveRecord(rec)
        return rec
    }

    private fun checkShizukuOperationStatus(): OperationStatus {
        val st = shizukuManager.state.value
        return when {
            st.isOperational -> OperationStatus.AVAILABLE
            st.isBinderAlive && !st.isPermissionGranted -> OperationStatus.REQUIRES_PERMISSION
            else -> OperationStatus.REQUIRES_SHIZUKU
        }
    }

    private fun readSetting(ns: String, key: String): String? {
        shizukuManager.getServiceOrNull()?.let { svc ->
            val v = runCatching { svc.getSetting(ns, key) }.getOrNull()
            if (v != null && !v.startsWith("ERROR")) return v
        }
        return when (ns) {
            "global" -> runCatching { Settings.Global.getString(context.contentResolver, key) }.getOrNull()
            "system" -> runCatching { Settings.System.getString(context.contentResolver, key) }.getOrNull()
            else -> null
        }
    }

    private fun readProp(prop: String): String? {
        shizukuManager.getServiceOrNull()?.let { svc ->
            val v = runCatching { svc.getSystemProperty(prop) }.getOrNull()
            if (v != null && !v.startsWith("ERROR")) return v.ifBlank { null }
        }
        return null
    }

    private fun parsePhysicalDimension(wmSizeOutput: String): Pair<Int, Int>? {
        val match = Regex("Physical size:\\s*(\\d+)x(\\d+)").find(wmSizeOutput) ?: return null
        val w = match.groupValues[1].toIntOrNull() ?: return null
        val h = match.groupValues[2].toIntOrNull() ?: return null
        return w to h
    }

    private fun parseOverrideValue(wmOutput: String, prefix: String): String? {
        return wmOutput.lines()
            .firstOrNull { it.contains(prefix) }
            ?.substringAfter(prefix)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
