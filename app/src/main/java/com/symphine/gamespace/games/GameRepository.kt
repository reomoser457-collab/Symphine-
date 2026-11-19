package com.symphine.gamespace.games

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.symphine.gamespace.core.model.DetectedGame
import com.symphine.gamespace.core.model.GameClassification
import com.symphine.gamespace.core.model.GameCustomization
import com.symphine.gamespace.core.model.GameSessionEntry
import com.symphine.gamespace.data.database.GameCustomizationEntity
import com.symphine.gamespace.data.database.GameSessionEntity
import com.symphine.gamespace.data.database.InstalledGameDao
import com.symphine.gamespace.data.database.InstalledGameEntity
import com.symphine.gamespace.data.database.SymphineDao
import com.symphine.gamespace.data.datastore.UiPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

data class ActiveManualSession(
    val packageName: String,
    val gameName: String,
    val profileId: String,
    val startTimeMillis: Long
)

class GameRepository(
    private val context: Context,
    private val scanner: GameScanner,
    private val dao: SymphineDao,
    private val installedGameDao: InstalledGameDao,
    private val prefs: UiPreferencesDataStore
) {
    private val _scannedApps = MutableStateFlow<List<ScannedAppInfo>>(emptyList())
    val scannedApps: StateFlow<List<ScannedAppInfo>> = _scannedApps.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveManualSession?>(null)
    val activeSession: StateFlow<ActiveManualSession?> = _activeSession.asStateFlow()

    val gamesFlow: Flow<List<DetectedGame>> = combine(
        installedGameDao.observeAllInstalledGames(),
        dao.observeAllCustomizations(),
        dao.observeSessions()
    ) { installedGames, customizations, sessions ->
        val customMap = customizations.associateBy { it.packageName }
        val sessionByPkg = sessions.groupBy { it.packageName }

        installedGames.map { entity ->
            val custom = customMap[entity.packageName]?.toModel()
                ?: GameCustomization(packageName = entity.packageName)
            val pkgSessions = sessionByPkg[entity.packageName].orEmpty()
            DetectedGame(
                packageName = entity.packageName,
                appName = entity.appName,
                versionName = entity.versionName,
                versionCode = entity.versionCode,
                firstInstallTime = entity.firstInstallTime,
                lastUpdateTime = entity.lastUpdateTime,
                isLaunchable = entity.isLaunchable,
                classification = entity.toClassification(),
                targetSdkVersion = entity.targetSdkVersion,
                customization = custom,
                lastPlayedTimestamp = pkgSessions.maxOfOrNull { it.startTimeMillis },
                totalPlayDurationMs = pkgSessions.sumOf { it.durationMillis }
            )
        }
    }

    val sessionsFlow: Flow<List<GameSessionEntry>> = combine(
        dao.observeSessions(),
        _activeSession
    ) { list, _ ->
        list.map { it.toModel() }
    }

    suspend fun rescanGames() {
        _isScanning.value = true
        try {
            val added = prefs.userAddedGamesFlow.first()
            val excluded = prefs.userExcludedGamesFlow.first()
            val scanned = scanner.scanLaunchableApps(added, excluded)
            _scannedApps.value = scanned
            val now = System.currentTimeMillis()
            val libraryEntities = scanned
                .filter { it.autoGameClassification != null }
                .map { InstalledGameEntity.fromScannedApp(it, now) }
            installedGameDao.replaceInstalledLibrary(libraryEntities)
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun addAppToGameLibrary(packageName: String) {
        prefs.addUserGame(packageName)
        rescanGames()
    }

    suspend fun removeGameFromLibrary(packageName: String) {
        prefs.excludeGame(packageName)
        installedGameDao.deleteInstalledGame(packageName)
        rescanGames()
    }

    suspend fun updateCustomization(
        packageName: String,
        transform: (GameCustomization) -> GameCustomization
    ) {
        val current = dao.getCustomizationOnce(packageName)?.toModel()
            ?: GameCustomization(packageName = packageName)
        val updated = transform(current)
        dao.upsertCustomization(GameCustomizationEntity.fromModel(updated))
    }

    suspend fun resetCustomization(packageName: String) {
        dao.deleteCustomization(packageName)
    }

    suspend fun bulkUpdate(
        packages: Set<String>,
        transform: (GameCustomization) -> GameCustomization
    ) {
        packages.forEach { pkg -> updateCustomization(pkg, transform) }
    }

    suspend fun launchGame(game: DetectedGame): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(game.packageName)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(launchIntent)
            startSessionTracking(game.packageName, game.displayTitle, game.customization.assignedProfileId)
            true
        }.getOrDefault(false)
    }

    fun openAndroidAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    suspend fun startSessionTracking(packageName: String, gameName: String, profileId: String) {
        val existing = _activeSession.value
        if (existing != null && existing.packageName != packageName) {
            stopActiveSession(avgRamUsedMb = null, peakTempC = null, peakThermal = null, notes = "Switched game")
        }
        _activeSession.value = ActiveManualSession(
            packageName = packageName,
            gameName = gameName,
            profileId = profileId,
            startTimeMillis = System.currentTimeMillis()
        )
    }

    suspend fun stopActiveSession(
        avgRamUsedMb: Long? = null,
        peakTempC: Float? = null,
        peakThermal: String? = null,
        notes: String = ""
    ) {
        val current = _activeSession.value ?: return
        _activeSession.value = null
        val end = System.currentTimeMillis()
        val duration = (end - current.startTimeMillis).coerceAtLeast(1000L)
        dao.insertSession(
            GameSessionEntity(
                packageName = current.packageName,
                gameName = current.gameName,
                startTimeMillis = current.startTimeMillis,
                endTimeMillis = end,
                durationMillis = duration,
                profileId = current.profileId,
                avgRamUsedMb = avgRamUsedMb,
                peakBatteryTempC = peakTempC,
                peakThermalStatus = peakThermal,
                notes = notes
            )
        )
    }

    suspend fun clearSessionHistory() {
        dao.clearSessions()
    }
}
