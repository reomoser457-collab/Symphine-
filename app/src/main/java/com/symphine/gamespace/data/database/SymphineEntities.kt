package com.symphine.gamespace.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.symphine.gamespace.core.model.GameCardStyle
import com.symphine.gamespace.core.model.GameCustomization
import com.symphine.gamespace.core.model.GameProfileType
import com.symphine.gamespace.core.model.GameSessionEntry
import com.symphine.gamespace.core.model.OperationRecord
import com.symphine.gamespace.core.model.OperationStatus

@Entity(tableName = "game_customizations")
data class GameCustomizationEntity(
    @PrimaryKey val packageName: String,
    val customLabel: String? = null,
    val customBannerUri: String? = null,
    val customIconUri: String? = null,
    val customAccentHex: String? = null,
    val cardStyle: String = GameCardStyle.LIQUID_GLASS.name,
    val cardOpacity: Float = 0.86f,
    val glowEnabled: Boolean = true,
    val blurIntensity: Float = 0.7f,
    val compactDetailLayout: Boolean = false,
    val notes: String = "",
    val tagsCsv: String = "",
    val groupName: String = "Default",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val overlayEnabled: Boolean = false,
    val assignedProfileId: String = GameProfileType.BALANCED.id,
    val angleGraphicsMode: String = "none",
    val angleLodOffset: Int = 2,
    val preferredRefreshRateHz: Float? = null,
    val preferredDpi: Int? = null,
    val preferredDownscalePercent: Int? = null,
    val preferredAnimationScale: Float? = null,
    val preferredGameMode: String? = null
) {
    fun toModel(): GameCustomization = GameCustomization(
        packageName = packageName,
        customLabel = customLabel,
        customBannerUri = customBannerUri,
        customIconUri = customIconUri,
        customAccentHex = customAccentHex,
        cardStyle = runCatching { GameCardStyle.valueOf(cardStyle) }.getOrDefault(GameCardStyle.LIQUID_GLASS),
        cardOpacity = cardOpacity,
        glowEnabled = glowEnabled,
        blurIntensity = blurIntensity,
        compactDetailLayout = compactDetailLayout,
        notes = notes,
        tags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        groupName = groupName,
        isFavorite = isFavorite,
        isPinned = isPinned,
        isHidden = isHidden,
        overlayEnabled = overlayEnabled,
        assignedProfileId = assignedProfileId,
        angleGraphicsMode = angleGraphicsMode,
        angleLodOffset = angleLodOffset,
        preferredRefreshRateHz = preferredRefreshRateHz,
        preferredDpi = preferredDpi,
        preferredDownscalePercent = preferredDownscalePercent,
        preferredAnimationScale = preferredAnimationScale,
        preferredGameMode = preferredGameMode
    )

    companion object {
        fun fromModel(model: GameCustomization): GameCustomizationEntity = GameCustomizationEntity(
            packageName = model.packageName,
            customLabel = model.customLabel,
            customBannerUri = model.customBannerUri,
            customIconUri = model.customIconUri,
            customAccentHex = model.customAccentHex,
            cardStyle = model.cardStyle.name,
            cardOpacity = model.cardOpacity,
            glowEnabled = model.glowEnabled,
            blurIntensity = model.blurIntensity,
            compactDetailLayout = model.compactDetailLayout,
            notes = model.notes,
            tagsCsv = model.tags.joinToString(","),
            groupName = model.groupName,
            isFavorite = model.isFavorite,
            isPinned = model.isPinned,
            isHidden = model.isHidden,
            overlayEnabled = model.overlayEnabled,
            assignedProfileId = model.assignedProfileId,
            angleGraphicsMode = model.angleGraphicsMode,
            angleLodOffset = model.angleLodOffset,
            preferredRefreshRateHz = model.preferredRefreshRateHz,
            preferredDpi = model.preferredDpi,
            preferredDownscalePercent = model.preferredDownscalePercent,
            preferredAnimationScale = model.preferredAnimationScale,
            preferredGameMode = model.preferredGameMode
        )
    }
}

@Entity(tableName = "custom_profiles")
data class CustomProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val baseType: String,
    val accentHex: String,
    val targetGameMode: String?,
    val targetRefreshHz: Float?,
    val targetAnimationScale: Float?,
    val angleMode: String,
    val angleLod: Int,
    val enableBackgroundFlags: Boolean,
    val enableMonitoring: Boolean,
    val isUserCreated: Boolean
)

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val gameName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val durationMillis: Long,
    val profileId: String,
    val avgRamUsedMb: Long?,
    val peakBatteryTempC: Float?,
    val peakThermalStatus: String?,
    val notes: String
) {
    fun toModel(): GameSessionEntry = GameSessionEntry(
        id = id,
        packageName = packageName,
        gameName = gameName,
        startTimeMillis = startTimeMillis,
        endTimeMillis = endTimeMillis,
        durationMillis = durationMillis,
        profileId = profileId,
        avgRamUsedMb = avgRamUsedMb,
        peakBatteryTempC = peakBatteryTempC,
        peakThermalStatus = peakThermalStatus,
        notes = notes
    )
}

@Entity(tableName = "operation_history")
data class OperationHistoryEntity(
    @PrimaryKey val id: String,
    val operationName: String,
    val targetPackage: String?,
    val settingKey: String?,
    val originalValue: String?,
    val requestedValue: String?,
    val verifiedValue: String?,
    val status: String,
    val message: String,
    val timestampMillis: Long
) {
    fun toModel(): OperationRecord = OperationRecord(
        id = id,
        operationName = operationName,
        targetPackage = targetPackage,
        settingKey = settingKey,
        originalValue = originalValue,
        requestedValue = requestedValue,
        verifiedValue = verifiedValue,
        status = runCatching { OperationStatus.valueOf(status) }.getOrDefault(OperationStatus.UNKNOWN),
        message = message,
        timestampMillis = timestampMillis
    )

    companion object {
        fun fromModel(record: OperationRecord): OperationHistoryEntity = OperationHistoryEntity(
            id = record.id,
            operationName = record.operationName,
            targetPackage = record.targetPackage,
            settingKey = record.settingKey,
            originalValue = record.originalValue,
            requestedValue = record.requestedValue,
            verifiedValue = record.verifiedValue,
            status = record.status.name,
            message = record.message,
            timestampMillis = record.timestampMillis
        )
    }
}

@Entity(tableName = "original_system_snapshots")
data class OriginalBackupSnapshotEntity(
    @PrimaryKey val compositeKey: String, // e.g. "global|angle_gl_driver_selection_pkgs" or "wm|density"
    val namespace: String,
    val settingKey: String,
    val originalValue: String, // "null" if unset originally
    val capturedAtMillis: Long
)
