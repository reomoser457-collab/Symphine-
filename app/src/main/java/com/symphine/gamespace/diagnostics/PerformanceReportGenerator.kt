package com.symphine.gamespace.diagnostics

import com.symphine.gamespace.core.model.BatteryHardwareInfo
import com.symphine.gamespace.core.model.CpuHardwareInfo
import com.symphine.gamespace.core.model.DeviceHardwareInfo
import com.symphine.gamespace.core.model.DisplayHardwareInfo
import com.symphine.gamespace.core.model.GraphicsHardwareInfo
import com.symphine.gamespace.core.model.MemoryHardwareInfo
import com.symphine.gamespace.core.model.OemCompatibilityReport
import com.symphine.gamespace.core.model.OperationRecord
import com.symphine.gamespace.core.model.StorageHardwareInfo
import com.symphine.gamespace.core.model.ThermalHardwareInfo
import com.symphine.gamespace.shizuku.ShizukuStatusState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PerformanceReportGenerator {

    fun generateTextReport(
        device: DeviceHardwareInfo,
        cpu: CpuHardwareInfo,
        memory: MemoryHardwareInfo,
        storage: StorageHardwareInfo,
        battery: BatteryHardwareInfo,
        thermal: ThermalHardwareInfo,
        display: DisplayHardwareInfo,
        graphics: GraphicsHardwareInfo,
        shizuku: ShizukuStatusState,
        compat: OemCompatibilityReport,
        activeProfileId: String,
        recentOperations: List<OperationRecord>
    ): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val totalRamMb = memory.totalRamBytes / (1024 * 1024)
        val availRamMb = memory.availableRamBytes / (1024 * 1024)
        val totalStorageGb = storage.totalBytes / (1024.0 * 1024.0 * 1024.0)
        val availStorageGb = storage.availableBytes / (1024.0 * 1024.0 * 1024.0)

        return buildString {
            appendLine("====================================================")
            appendLine("   SYMPHINE GAME SPACE — DIAGNOSTICS REPORT v1.0.0  ")
            appendLine("====================================================")
            appendLine("Generated     : $dateStr")
            appendLine("Developer     : Reo Moser")
            appendLine("Package       : com.symphine.gamespace")
            appendLine("Active Profile: $activeProfileId")
            appendLine()
            appendLine("[DEVICE]")
            appendLine("Manufacturer  : ${device.manufacturer} (${device.oemBrand.displayName})")
            appendLine("Model         : ${device.model} (${device.device})")
            appendLine("Android       : ${device.androidVersion} (API ${device.apiLevel})")
            appendLine("Build         : ${device.buildId}")
            appendLine("SoC / Board   : ${device.socModel} / ${device.hardwareBoard}")
            appendLine("ABIs          : ${device.supportedAbis.joinToString(", ")}")
            appendLine("Kernel        : ${device.kernelVersion}")
            appendLine()
            appendLine("[CPU & MEMORY & STORAGE]")
            appendLine("CPU Model     : ${cpu.modelName} (${cpu.architecture})")
            appendLine("CPU Cores     : ${cpu.coreCount}")
            appendLine("CPU Util      : ${cpu.utilizationPercent?.let { String.format("%.1f%%", it) } ?: cpu.utilizationNote}")
            appendLine("RAM           : $availRamMb MB available / $totalRamMb MB total (LowMem=${memory.isLowMemory})")
            appendLine("Storage       : ${String.format("%.2f", availStorageGb)} GB free / ${String.format("%.2f", totalStorageGb)} GB total")
            appendLine()
            appendLine("[DISPLAY & GRAPHICS]")
            appendLine("Resolution    : ${display.widthPx}x${display.heightPx} (${display.aspectRatio})")
            appendLine("Density       : ${display.densityDpi} dpi (${display.densityScale}x)")
            appendLine("Current Hz    : ${display.currentRefreshRateHz} Hz")
            appendLine("Supported Hz  : ${display.supportedRefreshRates.joinToString(", ")} Hz")
            appendLine("Peak/Min Sys  : peak=${display.peakRefreshSetting ?: "null"}, min=${display.minRefreshSetting ?: "null"}")
            appendLine("GPU Renderer  : ${graphics.gpuRenderer} (${graphics.gpuVendor})")
            appendLine("OpenGL ES     : ${graphics.openGlEsVersion} | ${graphics.glVersionDetail}")
            appendLine("Vulkan        : ${if (graphics.vulkanSupported) graphics.vulkanVersion ?: "Supported" else "Unsupported"}")
            appendLine("ANGLE         : ${graphics.anglePackageName} | pkgs=${graphics.angleTargetPackages ?: "null"} | val=${graphics.angleDriverValues ?: "null"}")
            appendLine()
            appendLine("[BATTERY & THERMAL]")
            appendLine("Battery       : ${battery.percentage}% (${battery.statusLabel}, ${battery.pluggedSource})")
            appendLine("Battery Temp  : ${battery.temperatureCelsius?.let { String.format("%.1f C", it) } ?: "Unavailable"}")
            appendLine("Thermal State : ${thermal.statusLabel} (Throttling=${thermal.isThrottling})")
            appendLine("Sensor Note   : ${thermal.sensorAccessNote}")
            appendLine()
            appendLine("[SHIZUKU STATUS]")
            appendLine("Installed     : ${shizuku.isShizukuAppInstalled}")
            appendLine("Phase         : ${shizuku.phase.label}")
            appendLine("Binder/Perm   : binder=${shizuku.isBinderAlive}, perm=${shizuku.isPermissionGranted}, uid=${shizuku.serviceUid ?: "N/A"}")
            appendLine()
            appendLine("[COMPATIBILITY SUMMARY]")
            compat.features.forEach { f ->
                appendLine("- [${f.status.label}] ${f.name}: ${f.detail}")
            }
            appendLine()
            appendLine("[RECENT OPERATIONS (${recentOperations.size})]")
            if (recentOperations.isEmpty()) {
                appendLine("No operations recorded.")
            } else {
                recentOperations.take(15).forEach { op ->
                    appendLine("- [${op.status.label}] ${op.operationName} (${op.targetPackage ?: "global"}): ${op.message}")
                }
            }
        }
    }
}
