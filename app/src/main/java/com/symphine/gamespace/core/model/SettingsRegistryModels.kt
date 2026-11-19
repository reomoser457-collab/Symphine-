package com.symphine.gamespace.core.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingRegistryEntry(
    val settingId: String,
    val gamePackage: String? = null,
    val category: String,
    val name: String,
    val description: String,
    val namespace: String,
    val key: String,
    val defaultValue: String? = null,
    val originalValue: String? = null,
    val selectedValue: String? = null,
    val currentValue: String? = null,
    val supportedStatus: OperationStatus,
    val requiredPermission: String,
    val androidCompatibility: String,
    val deviceCompatibility: String,
    val riskLevel: RiskLevel,
    val validationResult: String,
    val operationStatus: OperationStatus,
    val verificationResult: String,
    val lastModified: Long? = null,
    val restoreStatus: String = "Not modified",
    val documentation: String,
    val history: List<String> = emptyList()
)

@Serializable
enum class ThemePreset(val label: String) {
    SYMPHINE_AMOLED("Symphine AMOLED Lavender"),
    MIDNIGHT_SILVER("Midnight Silver"),
    OBSIDIAN_PURPLE("Obsidian Purple"),
    CUSTOM("Custom Hex Palette")
}

@Serializable
enum class NavBarStyle(val label: String) {
    PILL("Floating Pill"),
    CAPSULE("Liquid Capsule"),
    INDICATOR("Minimal Indicator"),
    MATERIAL_BAR("Material 3 Bar")
}

@Serializable
enum class NavAlignment(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

@Serializable
enum class TextScalePreset(val label: String, val multiplier: Float) {
    SMALL("Small", 0.90f),
    DEFAULT("Default", 1.00f),
    LARGE("Large", 1.10f),
    LARGER("Larger", 1.20f)
}

@Serializable
data class UiSettingsState(
    val themePreset: ThemePreset = ThemePreset.SYMPHINE_AMOLED,
    val primaryHex: String = "#A78BFA",
    val secondaryHex: String = "#C4B5FD",
    val accentHex: String = "#A78BFA",
    val backgroundHex: String = "#08090D",
    val surfaceHex: String = "#111319",
    val surfaceElevatedHex: String = "#191B23",
    val borderHex: String = "#292B35",
    val textHex: String = "#D5D5DF",
    val cornerRadiusDp: Int = 20,
    val textScale: TextScalePreset = TextScalePreset.DEFAULT,
    val iconSizeDp: Int = 22,
    val customFontName: String = "Space Grotesk + Plus Jakarta Sans",
    val customFontUri: String? = null,
    val customBannerUri: String? = null,
    val bannerZoom: Float = 1.0f,
    val bannerOffsetX: Float = 0f,
    val bannerOffsetY: Float = 0f,
    val bannerRotationDeg: Float = 0f,
    val navBarStyle: NavBarStyle = NavBarStyle.CAPSULE,
    val navAlignment: NavAlignment = NavAlignment.CENTER,
    val navHeightDp: Int = 68,
    val navIconSizeDp: Int = 22,
    val navTextSizeSp: Int = 11,
    val glassBlurAmount: Float = 0.65f,
    val glassSurfaceOpacity: Float = 0.82f,
    val glassSaturation: Float = 1.15f,
    val glassGlowEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val toastFeedbackEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val monitoringIntervalMs: Long = 2000L
)
