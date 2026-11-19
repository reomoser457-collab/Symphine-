package com.symphine.gamespace.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class GameClassification(val label: String) {
    AUTO_ANDROID_CATEGORY("Android Game Category"),
    AUTO_CURATED_PACKAGE("Recognized Game Package"),
    USER_ADDED("User Added to Library")
}

@Serializable
enum class GameProfileType(
    val id: String,
    val displayName: String,
    val description: String
) {
    BALANCED("balanced", "Balanced", "Default system behavior with clean telemetry and standard frame pacing."),
    GAMING("gaming", "Gaming", "Prioritizes supported Game Mode (Performance), snappy animations, and ANGLE/ART options."),
    BATTERY_SAVER("battery_saver", "Battery Saver", "Reduces refresh rate target where supported and uses battery-conscious game mode."),
    SMART("smart", "Smart", "Adapts based on current thermal status and available memory."),
    RECOMMENDED("recommended", "Recommended", "Device-calibrated offline profile tailored to your hardware and permissions."),
    GAMING_STABLE("gaming_stable", "Gaming Stable", "Focuses on thermal headroom, 0.5x UI animations, and speed-profile ART compilation."),
    DISPLAY_FOCUS("display_focus", "Display Focus", "Focuses on highest verified display refresh mode and optional safe DPI preset."),
    MONITORING_FOCUS("monitoring_focus", "Monitoring Focus", "Enables live telemetry tracking and session recording with minimal system changes."),
    CUSTOM("custom", "Custom", "User-defined custom profile configuration."),
    CUSTOM_TEMPLATE("custom_template", "Custom Template", "Reusable template cloned or customized by the user.")
}

@Serializable
enum class LibraryViewMode(val label: String) {
    GRID("Grid"),
    LIST("List"),
    COMPACT("Compact"),
    LARGE_CARDS("Large Cards")
}

@Serializable
enum class LibrarySortOrder(val label: String) {
    ALPHABETICAL("A → Z"),
    REVERSE_ALPHABETICAL("Z → A"),
    RECENTLY_INSTALLED("Recently Installed"),
    RECENTLY_PLAYED("Recently Played")
}

@Serializable
enum class LibraryFilter(val label: String) {
    ALL("All Games"),
    FAVORITES("Favorites"),
    PINNED("Pinned"),
    HIDDEN("Hidden"),
    RECENTLY_PLAYED("Recently Played"),
    CUSTOM_GROUP("Group")
}

@Serializable
enum class GameCardStyle(val label: String) {
    LIQUID_GLASS("Liquid Glass"),
    AMOLED_MINIMAL("AMOLED Minimal"),
    LAVENDER_GLOW("Lavender Glow"),
    SILVER_OUTLINE("Silver Outline")
}

@Serializable
data class GameCustomization(
    val packageName: String,
    val customLabel: String? = null,
    val customBannerUri: String? = null,
    val customIconUri: String? = null,
    val customAccentHex: String? = null,
    val cardStyle: GameCardStyle = GameCardStyle.LIQUID_GLASS,
    val cardOpacity: Float = 0.86f,
    val glowEnabled: Boolean = true,
    val blurIntensity: Float = 0.7f,
    val compactDetailLayout: Boolean = false,
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val groupName: String = "Default",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val overlayEnabled: Boolean = false,
    val assignedProfileId: String = GameProfileType.BALANCED.id,
    val angleGraphicsMode: String = "none", // none, smooth, pixel
    val angleLodOffset: Int = 2,
    val preferredRefreshRateHz: Float? = null,
    val preferredDpi: Int? = null,
    val preferredDownscalePercent: Int? = null,
    val preferredAnimationScale: Float? = null,
    val preferredGameMode: String? = null // performance, standard, battery
)

@Serializable
data class DetectedGame(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isLaunchable: Boolean,
    val classification: GameClassification,
    val targetSdkVersion: Int,
    val customization: GameCustomization,
    val lastPlayedTimestamp: Long? = null,
    val totalPlayDurationMs: Long = 0L
) {
    val displayTitle: String
        get() = customization.customLabel?.takeIf { it.isNotBlank() } ?: appName
}

@Serializable
data class CustomProfileDefinition(
    val id: String,
    val name: String,
    val description: String,
    val baseType: GameProfileType,
    val accentHex: String = "#A78BFA",
    val targetGameMode: String? = null,
    val targetRefreshHz: Float? = null,
    val targetAnimationScale: Float? = null,
    val angleMode: String = "none",
    val angleLod: Int = 2,
    val enableBackgroundFlags: Boolean = false,
    val enableMonitoring: Boolean = false,
    val isUserCreated: Boolean = false
)

@Serializable
data class GameSessionEntry(
    val id: Long = 0,
    val packageName: String,
    val gameName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val durationMillis: Long = 0L,
    val profileId: String,
    val avgRamUsedMb: Long? = null,
    val peakBatteryTempC: Float? = null,
    val peakThermalStatus: String? = null,
    val notes: String = ""
)
