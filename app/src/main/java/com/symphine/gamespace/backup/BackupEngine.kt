package com.symphine.gamespace.backup

import android.os.Build
import com.symphine.gamespace.core.model.CustomProfileDefinition
import com.symphine.gamespace.core.model.GameCustomization
import com.symphine.gamespace.core.model.GameProfileType
import com.symphine.gamespace.core.model.GameSessionEntry
import com.symphine.gamespace.core.model.OperationRecord
import com.symphine.gamespace.core.model.UiSettingsState
import com.symphine.gamespace.data.database.CustomProfileEntity
import com.symphine.gamespace.data.database.GameCustomizationEntity
import com.symphine.gamespace.data.database.SymphineDao
import com.symphine.gamespace.data.datastore.UiPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SymphineBackupSchema(
    val schemaVersion: Int = 1,
    val appName: String = "SYMPHINE GAME SPACE",
    val appVersion: String = "1.0.0",
    val packageName: String = "com.symphine.gamespace",
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val deviceManufacturer: String = Build.MANUFACTURER ?: "Unknown",
    val deviceModel: String = Build.MODEL ?: "Unknown",
    val androidApiLevel: Int = Build.VERSION.SDK_INT,
    val activeGlobalProfileId: String = GameProfileType.BALANCED.id,
    val uiPreferences: UiSettingsState = UiSettingsState(),
    val userAddedGames: List<String> = emptyList(),
    val userExcludedGames: List<String> = emptyList(),
    val customGroups: List<String> = emptyList(),
    val whitelistedPackages: List<String> = emptyList(),
    val gameCustomizations: List<GameCustomization> = emptyList(),
    val customProfiles: List<CustomProfileDefinition> = emptyList(),
    val recentSessions: List<GameSessionEntry> = emptyList(),
    val operationHistory: List<OperationRecord> = emptyList(),
    val recordedOriginalSnapshots: Map<String, String> = emptyMap()
)

data class BackupValidationResult(
    val isValid: Boolean,
    val schema: SymphineBackupSchema? = null,
    val errorMessage: String? = null,
    val conflicts: List<String> = emptyList(),
    val compatibilityNotes: List<String> = emptyList()
)

class BackupEngine(
    private val dao: SymphineDao,
    private val prefs: UiPreferencesDataStore
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val customizations = dao.getAllCustomizationsOnce().map { it.toModel() }
        val customProfiles = dao.getProfilesOnce().map { entity ->
            CustomProfileDefinition(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                baseType = runCatching { GameProfileType.valueOf(entity.baseType) }.getOrDefault(GameProfileType.CUSTOM),
                accentHex = entity.accentHex,
                targetGameMode = entity.targetGameMode,
                targetRefreshHz = entity.targetRefreshHz,
                targetAnimationScale = entity.targetAnimationScale,
                angleMode = entity.angleMode,
                angleLod = entity.angleLod,
                enableBackgroundFlags = entity.enableBackgroundFlags,
                enableMonitoring = entity.enableMonitoring,
                isUserCreated = entity.isUserCreated
            )
        }
        val sessions = dao.getSessionsOnce().take(100).map { it.toModel() }
        val history = dao.getOperationHistoryOnce().take(100).map { it.toModel() }
        val snapshots = dao.getSnapshotsOnce().associate { it.compositeKey to it.originalValue }

        val schema = SymphineBackupSchema(
            activeGlobalProfileId = prefs.activeGlobalProfileFlow.first(),
            uiPreferences = prefs.uiSettingsFlow.first(),
            userAddedGames = prefs.userAddedGamesFlow.first().toList(),
            userExcludedGames = prefs.userExcludedGamesFlow.first().toList(),
            customGroups = prefs.customGroupsFlow.first().toList(),
            whitelistedPackages = prefs.whitelistedPackagesFlow.first().toList(),
            gameCustomizations = customizations,
            customProfiles = customProfiles,
            recentSessions = sessions,
            operationHistory = history,
            recordedOriginalSnapshots = snapshots
        )
        json.encodeToString(schema)
    }

    suspend fun validateBackupJson(rawJson: String): BackupValidationResult = withContext(Dispatchers.IO) {
        if (rawJson.isBlank() || rawJson.length > 5_000_000) {
            return@withContext BackupValidationResult(
                isValid = false,
                errorMessage = "Backup file is empty or exceeds 5 MB safety limit."
            )
        }
        val parsed = runCatching { json.decodeFromString<SymphineBackupSchema>(rawJson) }
            .getOrElse { err ->
                return@withContext BackupValidationResult(
                    isValid = false,
                    errorMessage = "Malformed or invalid Symphine JSON: ${err.localizedMessage ?: "Parse error"}"
                )
            }

        if (parsed.schemaVersion < 1 || parsed.schemaVersion > 1) {
            return@withContext BackupValidationResult(
                isValid = false,
                errorMessage = "Unsupported backup schemaVersion: ${parsed.schemaVersion}"
            )
        }

        if (parsed.packageName != "com.symphine.gamespace") {
            return@withContext BackupValidationResult(
                isValid = false,
                errorMessage = "Package mismatch (${parsed.packageName}). Refusing to import untrusted file."
            )
        }

        val existingGames = dao.getAllCustomizationsOnce().map { it.packageName }.toSet()
        val conflicts = parsed.gameCustomizations
            .filter { it.packageName in existingGames }
            .map { "Existing customization for ${it.packageName} will be overwritten." }

        val compatNotes = buildList {
            if (parsed.deviceModel != Build.MODEL) {
                add("Backup exported from ${parsed.deviceManufacturer} ${parsed.deviceModel} (API ${parsed.androidApiLevel}); current device is ${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT}).")
            } else {
                add("Exported from this same device model (${Build.MODEL}).")
            }
            add("System-level settings & display overrides are NOT automatically executed during import for safety.")
        }

        BackupValidationResult(
            isValid = true,
            schema = parsed,
            conflicts = conflicts,
            compatibilityNotes = compatNotes
        )
    }

    suspend fun restoreFromSchema(
        schema: SymphineBackupSchema,
        restoreUi: Boolean = true,
        restoreLibrary: Boolean = true,
        restoreProfiles: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            var itemsRestored = 0
            if (restoreUi) {
                prefs.replaceUiSettings(schema.uiPreferences)
                itemsRestored++
            }
            if (restoreLibrary) {
                schema.customGroups.forEach { prefs.addCustomGroup(it) }
                schema.userAddedGames.forEach { prefs.addUserGame(it) }
                schema.userExcludedGames.forEach { prefs.excludeGame(it) }
                schema.gameCustomizations.forEach { custom ->
                    dao.upsertCustomization(GameCustomizationEntity.fromModel(custom))
                    itemsRestored++
                }
            }
            if (restoreProfiles) {
                prefs.setActiveGlobalProfile(schema.activeGlobalProfileId)
                schema.customProfiles.forEach { p ->
                    dao.upsertProfile(
                        CustomProfileEntity(
                            id = p.id,
                            name = p.name,
                            description = p.description,
                            baseType = p.baseType.name,
                            accentHex = p.accentHex,
                            targetGameMode = p.targetGameMode,
                            targetRefreshHz = p.targetRefreshHz,
                            targetAnimationScale = p.targetAnimationScale,
                            angleMode = p.angleMode,
                            angleLod = p.angleLod,
                            enableBackgroundFlags = p.enableBackgroundFlags,
                            enableMonitoring = p.enableMonitoring,
                            isUserCreated = p.isUserCreated
                        )
                    )
                    itemsRestored++
                }
            }
            "Restored $itemsRestored configuration item(s) locally and safely."
        }
    }
}
