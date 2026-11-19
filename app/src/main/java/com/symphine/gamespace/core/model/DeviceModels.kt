package com.symphine.gamespace.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class OemBrand(val displayName: String) {
    REALME("Realme"),
    OPPO("OPPO"),
    XIAOMI("Xiaomi"),
    REDMI("Redmi"),
    POCO("POCO"),
    SAMSUNG("Samsung"),
    ASUS("ASUS"),
    VIVO("Vivo"),
    IQOO("iQOO"),
    MOTOROLA("Motorola"),
    PIXEL("Google Pixel"),
    OTHER("Android Device")
}

@Serializable
data class DeviceHardwareInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val androidVersion: String,
    val apiLevel: Int,
    val buildId: String,
    val securityPatch: String,
    val kernelVersion: String,
    val socModel: String,
    val hardwareBoard: String,
    val primaryAbi: String,
    val supportedAbis: List<String>,
    val oemBrand: OemBrand
)

@Serializable
data class CpuHardwareInfo(
    val architecture: String,
    val modelName: String,
    val coreCount: Int,
    val coreFrequenciesKhz: List<Long?>,
    val coreMaxFrequenciesKhz: List<Long?>,
    val governors: List<String?>,
    val utilizationPercent: Float?,
    val utilizationNote: String
)

@Serializable
data class MemoryHardwareInfo(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val usedRamBytes: Long,
    val thresholdBytes: Long,
    val isLowMemory: Boolean,
    val swapTotalBytes: Long?,
    val swapFreeBytes: Long?
)

@Serializable
data class StorageHardwareInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val appCacheBytes: Long,
    val appDataBytes: Long
)

@Serializable
data class BatteryHardwareInfo(
    val percentage: Int,
    val statusLabel: String,
    val isCharging: Boolean,
    val pluggedSource: String,
    val temperatureCelsius: Float?,
    val voltageMillivolts: Int?,
    val currentMicroamps: Int?,
    val technology: String?,
    val healthLabel: String
)

@Serializable
data class ThermalZoneReading(
    val name: String,
    val temperatureCelsius: Float
)

@Serializable
data class ThermalHardwareInfo(
    val statusCode: Int,
    val statusLabel: String,
    val isThrottling: Boolean,
    val batteryTempCelsius: Float?,
    val readableZones: List<ThermalZoneReading>,
    val sensorAccessNote: String
)

@Serializable
data class DisplayModeInfo(
    val modeId: Int,
    val width: Int,
    val height: Int,
    val refreshRateHz: Float
)

@Serializable
data class DisplayHardwareInfo(
    val widthPx: Int,
    val heightPx: Int,
    val aspectRatio: String,
    val densityDpi: Int,
    val densityScale: Float,
    val currentRefreshRateHz: Float,
    val supportedModes: List<DisplayModeInfo>,
    val supportedRefreshRates: List<Float>,
    val isAdaptiveRefreshSupported: Boolean,
    val isHdrSupported: Boolean,
    val isWideColorGamut: Boolean,
    val peakRefreshSetting: String?,
    val minRefreshSetting: String?,
    val wmSizeOverride: String?,
    val wmDensityOverride: String?
)

@Serializable
data class GraphicsHardwareInfo(
    val gpuRenderer: String,
    val gpuVendor: String,
    val openGlEsVersion: String,
    val glVersionDetail: String,
    val vulkanSupported: Boolean,
    val vulkanVersion: String?,
    val eglDriverHint: String?,
    val hwuiRenderer: String?,
    val angleInstalled: Boolean,
    val anglePackageName: String?,
    val angleTargetPackages: String?,
    val angleDriverValues: String?,
    val angleFeatureOverrides: String?,
    val gpuUtilizationNote: String
)

@Serializable
data class OemFeatureCompatibility(
    val featureId: String,
    val name: String,
    val category: String,
    val status: OperationStatus,
    val detail: String
)

@Serializable
data class OemCompatibilityReport(
    val oemBrand: OemBrand,
    val manufacturer: String,
    val model: String,
    val apiLevel: Int,
    val features: List<OemFeatureCompatibility>
)
