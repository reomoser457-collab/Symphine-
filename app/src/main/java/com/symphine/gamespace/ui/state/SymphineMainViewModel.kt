package com.symphine.gamespace.ui.state

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.symphine.gamespace.app.SymphineApplication
import com.symphine.gamespace.backup.BackupValidationResult
import com.symphine.gamespace.backup.SymphineBackupSchema
import com.symphine.gamespace.core.model.BatteryHardwareInfo
import com.symphine.gamespace.core.model.CpuHardwareInfo
import com.symphine.gamespace.core.model.CustomProfileDefinition
import com.symphine.gamespace.core.model.DetectedGame
import com.symphine.gamespace.core.model.DeviceHardwareInfo
import com.symphine.gamespace.core.model.DisplayHardwareInfo
import com.symphine.gamespace.core.model.GameCustomization
import com.symphine.gamespace.core.model.GameProfileType
import com.symphine.gamespace.core.model.GameSessionEntry
import com.symphine.gamespace.core.model.GraphicsHardwareInfo
import com.symphine.gamespace.core.model.LibraryFilter
import com.symphine.gamespace.core.model.LibrarySortOrder
import com.symphine.gamespace.core.model.LibraryViewMode
import com.symphine.gamespace.core.model.MemoryHardwareInfo
import com.symphine.gamespace.core.model.OemCompatibilityReport
import com.symphine.gamespace.core.model.OperationPreview
import com.symphine.gamespace.core.model.OperationRecord
import com.symphine.gamespace.core.model.OperationStatus
import com.symphine.gamespace.core.model.SettingRegistryEntry
import com.symphine.gamespace.core.model.StorageHardwareInfo
import com.symphine.gamespace.core.model.ThermalHardwareInfo
import com.symphine.gamespace.core.model.UiSettingsState
import com.symphine.gamespace.core.permissions.ManagedPermissionState
import com.symphine.gamespace.data.database.CustomProfileEntity
import com.symphine.gamespace.games.ActiveManualSession
import com.symphine.gamespace.games.ScannedAppInfo
import com.symphine.gamespace.overlay.GamingOverlayService
import com.symphine.gamespace.performance.ProfileRecommendation
import com.symphine.gamespace.shizuku.ShizukuStatusState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class TelemetrySample(
    val timestampMillis: Long,
    val ramUsedPercent: Float,
    val ramAvailableMb: Long,
    val batteryPercent: Int,
    val batteryTempC: Float?,
    val cpuUtilPercent: Float?,
    val thermalStatusCode: Int,
    val refreshRateHz: Float
)

data class HardwareDashboardState(
    val isLoading: Boolean = true,
    val device: DeviceHardwareInfo? = null,
    val cpu: CpuHardwareInfo? = null,
    val memory: MemoryHardwareInfo? = null,
    val storage: StorageHardwareInfo? = null,
    val battery: BatteryHardwareInfo? = null,
    val thermal: ThermalHardwareInfo? = null,
    val display: DisplayHardwareInfo? = null,
    val graphics: GraphicsHardwareInfo? = null,
    val oemCompatibility: OemCompatibilityReport? = null,
    val recommendations: List<ProfileRecommendation> = emptyList(),
    val settingsRegistry: List<SettingRegistryEntry> = emptyList(),
    val permissions: List<ManagedPermissionState> = emptyList()
)

data class LibraryUiFilterState(
    val searchQuery: String = "",
    val viewMode: LibraryViewMode = LibraryViewMode.GRID,
    val sortOrder: LibrarySortOrder = LibrarySortOrder.ALPHABETICAL,
    val filter: LibraryFilter = LibraryFilter.ALL,
    val selectedGroup: String = "Default",
    val bulkSelectionMode: Boolean = false,
    val selectedPackages: Set<String> = emptySet()
)

class SymphineMainViewModel(application: Application) : AndroidViewModel(application) {

    private val app: SymphineApplication = application as SymphineApplication

    val uiSettings: StateFlow<UiSettingsState> = app.uiPrefs.uiSettingsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), UiSettingsState()
    )

    val activeGlobalProfileId: StateFlow<String> = app.uiPrefs.activeGlobalProfileFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), GameProfileType.BALANCED.id
    )

    val customGroups: StateFlow<Set<String>> = app.uiPrefs.customGroupsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("Default", "Competitive", "FPS", "RPG", "Casual")
    )

    val whitelistedPackages: StateFlow<Set<String>> = app.uiPrefs.whitelistedPackagesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet()
    )

    val diagnosticsProfile: StateFlow<String> = app.uiPrefs.diagnosticsProfileFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "Default"
    )

    val shizukuStatus: StateFlow<ShizukuStatusState> = app.shizukuManager.state

    val overlayRunning: StateFlow<Boolean> = GamingOverlayService.isRunning

    val isScanningGames: StateFlow<Boolean> = app.gameRepository.isScanning

    val allLaunchableApps: StateFlow<List<ScannedAppInfo>> = app.gameRepository.scannedApps

    val activeSession: StateFlow<ActiveManualSession?> = app.gameRepository.activeSession

    val sessionsHistory: StateFlow<List<GameSessionEntry>> = app.gameRepository.sessionsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val operationHistory: StateFlow<List<OperationRecord>> = app.dao.observeOperationHistory()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snapshotCount: StateFlow<Int> = app.dao.observeSnapshots()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val customProfiles: StateFlow<List<CustomProfileDefinition>> = app.dao.observeProfiles()
        .map { entities ->
            val defaults = defaultBuiltInProfiles()
            val userOnes = entities.map { e ->
                CustomProfileDefinition(
                    id = e.id,
                    name = e.name,
                    description = e.description,
                    baseType = runCatching { GameProfileType.valueOf(e.baseType) }.getOrDefault(GameProfileType.CUSTOM),
                    accentHex = e.accentHex,
                    targetGameMode = e.targetGameMode,
                    targetRefreshHz = e.targetRefreshHz,
                    targetAnimationScale = e.targetAnimationScale,
                    angleMode = e.angleMode,
                    angleLod = e.angleLod,
                    enableBackgroundFlags = e.enableBackgroundFlags,
                    enableMonitoring = e.enableMonitoring,
                    isUserCreated = e.isUserCreated
                )
            }
            defaults + userOnes
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultBuiltInProfiles())

    private val _libraryFilterState = MutableStateFlow(LibraryUiFilterState())
    val libraryFilterState: StateFlow<LibraryUiFilterState> = _libraryFilterState.asStateFlow()

    val allDetectedGames: StateFlow<List<DetectedGame>> = app.gameRepository.gamesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val filteredGames: StateFlow<List<DetectedGame>> = combine(
        allDetectedGames,
        _libraryFilterState
    ) { games, filterState ->
        val query = filterState.searchQuery.trim().lowercase()
        val filtered = games.filter { g ->
            val matchesQuery = query.isEmpty() ||
                g.displayTitle.lowercase().contains(query) ||
                g.packageName.lowercase().contains(query) ||
                g.customization.tags.any { it.lowercase().contains(query) }

            val matchesFilter = when (filterState.filter) {
                LibraryFilter.ALL -> !g.customization.isHidden
                LibraryFilter.FAVORITES -> !g.customization.isHidden && g.customization.isFavorite
                LibraryFilter.PINNED -> !g.customization.isHidden && g.customization.isPinned
                LibraryFilter.HIDDEN -> g.customization.isHidden
                LibraryFilter.RECENTLY_PLAYED -> !g.customization.isHidden && g.lastPlayedTimestamp != null
                LibraryFilter.CUSTOM_GROUP -> !g.customization.isHidden && g.customization.groupName == filterState.selectedGroup
            }
            matchesQuery && matchesFilter
        }

        val sorted = when (filterState.sortOrder) {
            LibrarySortOrder.ALPHABETICAL -> filtered.sortedWith(
                compareByDescending<DetectedGame> { it.customization.isPinned }
                    .thenBy { it.displayTitle.lowercase() }
            )
            LibrarySortOrder.REVERSE_ALPHABETICAL -> filtered.sortedWith(
                compareByDescending<DetectedGame> { it.customization.isPinned }
                    .thenByDescending { it.displayTitle.lowercase() }
            )
            LibrarySortOrder.RECENTLY_INSTALLED -> filtered.sortedWith(
                compareByDescending<DetectedGame> { it.customization.isPinned }
                    .thenByDescending { it.firstInstallTime }
            )
            LibrarySortOrder.RECENTLY_PLAYED -> filtered.sortedWith(
                compareByDescending<DetectedGame> { it.customization.isPinned }
                    .thenByDescending { it.lastPlayedTimestamp ?: 0L }
            )
        }
        sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _hardwareState = MutableStateFlow(HardwareDashboardState())
    val hardwareState: StateFlow<HardwareDashboardState> = _hardwareState.asStateFlow()

    private val _isMonitoringActive = MutableStateFlow(false)
    val isMonitoringActive: StateFlow<Boolean> = _isMonitoringActive.asStateFlow()

    private val _monitoringSamples = MutableStateFlow<List<TelemetrySample>>(emptyList())
    val monitoringSamples: StateFlow<List<TelemetrySample>> = _monitoringSamples.asStateFlow()

    private val _pendingOperationPreview = MutableStateFlow<OperationPreview?>(null)
    val pendingOperationPreview: StateFlow<OperationPreview?> = _pendingOperationPreview.asStateFlow()
    private var pendingOperationBlock: (suspend () -> List<OperationRecord>)? = null

    private val _isExecutingOperation = MutableStateFlow(false)
    val isExecutingOperation: StateFlow<Boolean> = _isExecutingOperation.asStateFlow()

    private val _backupValidationResult = MutableStateFlow<BackupValidationResult?>(null)
    val backupValidationResult: StateFlow<BackupValidationResult?> = _backupValidationResult.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private var monitoringJob: Job? = null

    init {
        refreshHardwareAndGames()
    }

    fun refreshHardwareAndGames() {
        viewModelScope.launch {
            app.gameRepository.rescanGames()
            refreshHardwareSnapshot()
        }
    }

    fun rescanGamesOnly() {
        viewModelScope.launch {
            app.gameRepository.rescanGames()
            emitSnackbar("Game Library scanned (${allDetectedGames.value.size} game(s) detected)")
        }
    }

    fun refreshHardwareSnapshot() {
        viewModelScope.launch(Dispatchers.IO) {
            val device = app.deviceInfoProvider.getDeviceInfo()
            val cpu = app.cpuInfoProvider.getCpuInfo()
            val mem = app.memoryInfoProvider.getMemoryInfo()
            val storage = app.storageInfoProvider.getStorageInfo()
            val battery = app.batteryInfoProvider.getBatteryInfo()
            val thermal = app.thermalInfoProvider.getThermalInfo()
            val display = app.displayInfoProvider.getDisplayInfo()
            val graphics = app.graphicsInfoProvider.getGraphicsInfo()
            val shizuku = app.shizukuManager.state.value
            val compat = app.oemCompatibilityAnalyzer.analyze(device, display, graphics, shizuku)
            val recs = app.recommendationEngine.generateRecommendations(device, mem, display, thermal, graphics, shizuku)
            val registry = app.settingsRegistryProvider.buildRegistryEntries()
            val perms = app.permissionManager.buildPermissionSnapshot(shizuku)

            _hardwareState.value = HardwareDashboardState(
                isLoading = false,
                device = device,
                cpu = cpu,
                memory = mem,
                storage = storage,
                battery = battery,
                thermal = thermal,
                display = display,
                graphics = graphics,
                oemCompatibility = compat,
                recommendations = recs,
                settingsRegistry = registry,
                permissions = perms
            )
        }
    }

    // -------------------- LIFECYCLE-SAFE MONITORING --------------------

    fun toggleMonitoring() {
        if (_isMonitoringActive.value) stopMonitoring() else startMonitoring()
    }

    fun startMonitoring() {
        if (_isMonitoringActive.value) return
        _isMonitoringActive.value = true
        monitoringJob?.cancel()
        monitoringJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive && _isMonitoringActive.value) {
                val interval = uiSettings.value.monitoringIntervalMs.coerceIn(1000L, 10000L)
                val mem = app.memoryInfoProvider.getMemoryInfo()
                val bat = app.batteryInfoProvider.getBatteryInfo()
                val cpu = app.cpuInfoProvider.getCpuInfo()
                val thermal = app.thermalInfoProvider.getThermalInfo()
                val disp = app.displayInfoProvider.getDisplayInfo()

                val ramUsedPct = if (mem.totalRamBytes > 0) {
                    (mem.usedRamBytes.toFloat() / mem.totalRamBytes.toFloat() * 100f).coerceIn(0f, 100f)
                } else 0f

                val sample = TelemetrySample(
                    timestampMillis = System.currentTimeMillis(),
                    ramUsedPercent = ramUsedPct,
                    ramAvailableMb = mem.availableRamBytes / (1024L * 1024L),
                    batteryPercent = bat.percentage,
                    batteryTempC = bat.temperatureCelsius,
                    cpuUtilPercent = cpu.utilizationPercent,
                    thermalStatusCode = thermal.statusCode,
                    refreshRateHz = disp.currentRefreshRateHz
                )

                _monitoringSamples.update { prev -> (prev + sample).takeLast(60) }
                _hardwareState.update { st ->
                    st.copy(
                        cpu = cpu,
                        memory = mem,
                        battery = bat,
                        thermal = thermal,
                        display = disp
                    )
                }
                delay(interval)
            }
        }
    }

    fun stopMonitoring() {
        _isMonitoringActive.value = false
        monitoringJob?.cancel()
        monitoringJob = null
    }

    fun resetMonitoring() {
        _monitoringSamples.value = emptyList()
        emitSnackbar("Monitoring telemetry history cleared")
    }

    fun exportMonitoringCsv(): String {
        val sb = StringBuilder("timestamp_ms,ram_used_pct,ram_avail_mb,battery_pct,battery_temp_c,cpu_util_pct,thermal_code,refresh_hz\n")
        _monitoringSamples.value.forEach { s ->
            sb.append("${s.timestampMillis},${String.format("%.1f", s.ramUsedPercent)},${s.ramAvailableMb},${s.batteryPercent},${s.batteryTempC ?: "N/A"},${s.cpuUtilPercent ?: "N/A"},${s.thermalStatusCode},${s.refreshRateHz}\n")
        }
        return sb.toString()
    }

    // -------------------- GAME LIBRARY CONTROLS --------------------

    fun updateLibraryFilter(transform: (LibraryUiFilterState) -> LibraryUiFilterState) {
        _libraryFilterState.update(transform)
    }

    fun toggleBulkPackage(pkg: String) {
        _libraryFilterState.update { st ->
            val next = if (pkg in st.selectedPackages) st.selectedPackages - pkg else st.selectedPackages + pkg
            st.copy(selectedPackages = next)
        }
    }

    fun clearBulkSelection() {
        _libraryFilterState.update { it.copy(bulkSelectionMode = false, selectedPackages = emptySet()) }
    }

    fun addAppToLibrary(packageName: String) {
        viewModelScope.launch {
            app.gameRepository.addAppToGameLibrary(packageName)
            emitSnackbar("Added $packageName to Symphine Game Library")
        }
    }

    fun removeGameFromLibrary(packageName: String) {
        viewModelScope.launch {
            app.gameRepository.removeGameFromLibrary(packageName)
            emitSnackbar("Removed $packageName from Game Library")
        }
    }

    fun updateGameCustomization(packageName: String, transform: (GameCustomization) -> GameCustomization) {
        viewModelScope.launch {
            app.gameRepository.updateCustomization(packageName, transform)
        }
    }

    fun resetGameCustomization(packageName: String) {
        viewModelScope.launch {
            app.gameRepository.resetCustomization(packageName)
            emitSnackbar("Local game customization reset")
        }
    }

    fun bulkApply(transform: (GameCustomization) -> GameCustomization, label: String) {
        val targets = _libraryFilterState.value.selectedPackages
        if (targets.isEmpty()) return
        viewModelScope.launch {
            app.gameRepository.bulkUpdate(targets, transform)
            clearBulkSelection()
            emitSnackbar("$label applied to ${targets.size} game(s)")
        }
    }

    fun launchGame(game: DetectedGame) {
        viewModelScope.launch {
            val ok = app.gameRepository.launchGame(game)
            if (ok) {
                if (game.customization.overlayEnabled && app.permissionManager.canDrawOverlays()) {
                    GamingOverlayService.startOverlay(app, game.customization.assignedProfileId)
                }
            } else {
                emitSnackbar("Unable to launch ${game.displayTitle} (no launchable activity)")
            }
        }
    }

    fun startManualSession(packageName: String, gameName: String, profileId: String) {
        viewModelScope.launch {
            app.gameRepository.startSessionTracking(packageName, gameName, profileId)
            emitSnackbar("Session timer started for $gameName")
        }
    }

    fun stopActiveSession(notes: String = "") {
        viewModelScope.launch {
            val hw = _hardwareState.value
            val avgRamMb = hw.memory?.let { (it.totalRamBytes - it.availableRamBytes) / (1024L * 1024L) }
            app.gameRepository.stopActiveSession(
                avgRamUsedMb = avgRamMb,
                peakTempC = hw.battery?.temperatureCelsius,
                peakThermal = hw.thermal?.statusLabel,
                notes = notes
            )
            emitSnackbar("Game session saved to local history")
        }
    }

    fun clearSessions() {
        viewModelScope.launch {
            app.gameRepository.clearSessionHistory()
            emitSnackbar("Session history cleared")
        }
    }

    fun openAndroidAppInfo(packageName: String) {
        app.gameRepository.openAndroidAppInfo(packageName)
    }

    fun addCustomGroup(name: String) {
        viewModelScope.launch {
            app.uiPrefs.addCustomGroup(name)
            emitSnackbar("Group '$name' created")
        }
    }

    // -------------------- PROFILES & VERIFIED OPERATIONS --------------------

    fun selectGlobalProfile(profileId: String) {
        viewModelScope.launch {
            app.uiPrefs.setActiveGlobalProfile(profileId)
            emitSnackbar("Active Symphine Profile: $profileId")
            app.notificationHelper.postOperationNotification(
                "Profile Switched",
                "Active SYMPHINE profile set to $profileId"
            )
        }
    }

    fun createOrCloneCustomProfile(
        name: String,
        description: String,
        baseType: GameProfileType,
        accentHex: String,
        targetGameMode: String?,
        targetRefreshHz: Float?,
        targetAnimationScale: Float?,
        angleMode: String,
        angleLod: Int
    ) {
        viewModelScope.launch {
            val entity = CustomProfileEntity(
                id = "custom_${UUID.randomUUID().toString().take(8)}",
                name = name.trim().ifBlank { "Custom Profile" },
                description = description.trim().ifBlank { "User-created profile" },
                baseType = baseType.name,
                accentHex = accentHex,
                targetGameMode = targetGameMode,
                targetRefreshHz = targetRefreshHz,
                targetAnimationScale = targetAnimationScale,
                angleMode = angleMode,
                angleLod = angleLod,
                enableBackgroundFlags = false,
                enableMonitoring = true,
                isUserCreated = true
            )
            app.dao.upsertProfile(entity)
            emitSnackbar("Saved custom profile '${entity.name}'")
        }
    }

    fun deleteUserProfile(id: String) {
        viewModelScope.launch {
            app.dao.deleteUserProfile(id)
            emitSnackbar("Custom profile deleted")
        }
    }

    fun requestOperationWithPreview(
        previewLoader: suspend () -> OperationPreview,
        executor: suspend () -> List<OperationRecord>
    ) {
        viewModelScope.launch {
            val preview = withContext(Dispatchers.IO) { previewLoader() }
            pendingOperationBlock = executor
            _pendingOperationPreview.value = preview
        }
    }

    fun dismissOperationPreview() {
        _pendingOperationPreview.value = null
        pendingOperationBlock = null
    }

    fun confirmAndExecutePendingOperation() {
        val block = pendingOperationBlock ?: return
        _pendingOperationPreview.value = null
        pendingOperationBlock = null
        viewModelScope.launch {
            _isExecutingOperation.value = true
            try {
                val results = withContext(Dispatchers.IO) { block() }
                refreshHardwareSnapshot()
                val verifiedCount = results.count { it.status == OperationStatus.VERIFIED || it.status == OperationStatus.APPLIED || it.status == OperationStatus.RESTORED }
                val failedCount = results.count { it.status == OperationStatus.FAILED || it.status == OperationStatus.REQUIRES_SHIZUKU || it.status == OperationStatus.ROLLBACK_FAILED }
                val summary = when {
                    failedCount == 0 && verifiedCount > 0 -> "Verified ($verifiedCount operation(s))"
                    verifiedCount > 0 && failedCount > 0 -> "Partial: $verifiedCount verified, $failedCount failed/restricted"
                    else -> results.firstOrNull()?.message ?: "Operation did not succeed"
                }
                emitSnackbar(summary)
                if (failedCount > 0) {
                    app.notificationHelper.postOperationNotification("Operation Notice", summary)
                }
            } finally {
                _isExecutingOperation.value = false
            }
        }
    }

    fun requestAngleOperation(packageName: String, gameTitle: String, mode: String, lod: Int, restartGame: Boolean = false) {
        requestOperationWithPreview(
            previewLoader = { app.shizukuRegistry.previewAngleGraphics(packageName, gameTitle, mode, lod) },
            executor = { app.shizukuRegistry.executeAngleGraphics(packageName, mode, lod, restartGame) }
        )
    }

    fun requestArtCompileOperation(packageName: String, gameTitle: String, mode: String) {
        requestOperationWithPreview(
            previewLoader = { app.shizukuRegistry.previewArtCompile(packageName, gameTitle, mode) },
            executor = { listOf(app.shizukuRegistry.executeArtCompile(packageName, mode)) }
        )
    }

    fun requestGameModeOperation(packageName: String, gameTitle: String, mode: String) {
        requestOperationWithPreview(
            previewLoader = { app.shizukuRegistry.previewGameMode(packageName, gameTitle, mode) },
            executor = { listOf(app.shizukuRegistry.executeGameMode(packageName, mode)) }
        )
    }

    fun requestRefreshRateOperation(hz: Float?) {
        requestOperationWithPreview(
            previewLoader = { app.shizukuRegistry.previewRefreshRate(hz) },
            executor = { app.shizukuRegistry.executeRefreshRate(hz) }
        )
    }

    fun requestAnimationScaleOperation(scale: Float) {
        requestOperationWithPreview(
            previewLoader = { app.shizukuRegistry.previewAnimationScale(scale) },
            executor = { app.shizukuRegistry.executeAnimationScale(scale) }
        )
    }

    fun requestBackgroundFlagsOperation() {
        requestOperationWithPreview(
            previewLoader = { app.shizukuRegistry.previewBackgroundFlags() },
            executor = { app.shizukuRegistry.executeBackgroundFlags() }
        )
    }

    fun requestDpiOperation(targetDpi: Int?) {
        requestOperationWithPreview(
            previewLoader = { app.shizukuRegistry.previewDpiChange(targetDpi) },
            executor = { listOf(app.shizukuRegistry.executeDpiChange(targetDpi)) }
        )
    }

    fun requestDownscaleOperation(percent: Int?) {
        requestOperationWithPreview(
            previewLoader = { app.shizukuRegistry.previewDownscale(percent) },
            executor = { app.shizukuRegistry.executeDownscale(percent) }
        )
    }

    fun executeAtomicFullRestore() {
        viewModelScope.launch {
            _isExecutingOperation.value = true
            try {
                val results = withContext(Dispatchers.IO) { app.shizukuRegistry.executeAtomicRestoreAll() }
                refreshHardwareSnapshot()
                val ok = results.count { it.status == OperationStatus.RESTORED || it.status == OperationStatus.VERIFIED }
                emitSnackbar("Atomic Restore finished: $ok item(s) restored/verified")
                app.notificationHelper.postOperationNotification("Atomic Restore", "Restored $ok original setting(s).")
            } finally {
                _isExecutingOperation.value = false
            }
        }
    }

    // -------------------- SHIZUKU CONTROLS --------------------

    fun requestShizukuPermission() = app.shizukuManager.requestPermission()
    fun reconnectShizuku() = app.shizukuManager.reconnect()
    fun refreshShizuku() = app.shizukuManager.refreshStatus(autoBindIfPermitted = true)

    // -------------------- STORAGE, BACKUP, REPORT, UI SETTINGS --------------------

    fun clearAppCacheOnly() {
        viewModelScope.launch(Dispatchers.IO) {
            val freed = app.storageInfoProvider.clearAppCacheOnly()
            refreshHardwareSnapshot()
            emitSnackbar("Cleared ${freed / 1024} KB of Symphine cache")
        }
    }

    fun toggleWhitelistPackage(pkg: String) {
        viewModelScope.launch {
            app.uiPrefs.toggleWhitelist(pkg)
        }
    }

    fun setDiagnosticsProfile(profile: String) {
        viewModelScope.launch {
            app.uiPrefs.setDiagnosticsProfile(profile)
            emitSnackbar("Diagnostics profile: $profile")
        }
    }

    fun updateUiSettings(transform: (UiSettingsState) -> UiSettingsState) {
        viewModelScope.launch {
            app.uiPrefs.updateUiSettings(transform)
        }
    }

    fun resetUiSettings() {
        viewModelScope.launch {
            app.uiPrefs.resetUiSettings()
            emitSnackbar("UI Appearance reset to Symphine Liquid Glass default")
        }
    }

    fun wipeAllLocalAppData() {
        viewModelScope.launch(Dispatchers.IO) {
            app.dao.clearAllCustomizations()
            app.dao.clearSessions()
            app.dao.clearOperationHistory()
            app.dao.clearSnapshots()
            app.installedGameDao.clearInstalledGames()
            app.uiPrefs.clearAllPreferences()
            app.gameRepository.rescanGames()
            emitSnackbar("All local Symphine configurations wiped")
        }
    }

    suspend fun generateBackupJson(): String = app.backupEngine.createBackupJson()

    fun validateBackupImport(rawJson: String) {
        viewModelScope.launch {
            _backupValidationResult.value = app.backupEngine.validateBackupJson(rawJson)
        }
    }

    fun dismissBackupValidation() {
        _backupValidationResult.value = null
    }

    fun applyValidatedBackup(
        schema: SymphineBackupSchema,
        restoreUi: Boolean,
        restoreLibrary: Boolean,
        restoreProfiles: Boolean
    ) {
        viewModelScope.launch {
            val res = app.backupEngine.restoreFromSchema(schema, restoreUi, restoreLibrary, restoreProfiles)
            _backupValidationResult.value = null
            res.onSuccess {
                app.gameRepository.rescanGames()
                emitSnackbar(it)
            }.onFailure {
                emitSnackbar("Restore failed: ${it.message}")
            }
        }
    }

    fun generateDiagnosticsReportText(): String {
        val st = _hardwareState.value
        val device = st.device ?: app.deviceInfoProvider.getDeviceInfo()
        val cpu = st.cpu ?: app.cpuInfoProvider.getCpuInfo()
        val mem = st.memory ?: app.memoryInfoProvider.getMemoryInfo()
        val storage = st.storage ?: app.storageInfoProvider.getStorageInfo()
        val bat = st.battery ?: app.batteryInfoProvider.getBatteryInfo()
        val thermal = st.thermal ?: app.thermalInfoProvider.getThermalInfo()
        val disp = st.display ?: app.displayInfoProvider.getDisplayInfo()
        val graphics = st.graphics
            ?: com.symphine.gamespace.core.model.GraphicsHardwareInfo(
                gpuRenderer = "Loading...",
                gpuVendor = "Unknown",
                openGlEsVersion = "Unknown",
                glVersionDetail = "Unknown",
                vulkanSupported = false,
                vulkanVersion = null,
                eglDriverHint = null,
                hwuiRenderer = null,
                angleInstalled = false,
                anglePackageName = null,
                angleTargetPackages = null,
                angleDriverValues = null,
                angleFeatureOverrides = null,
                gpuUtilizationNote = ""
            )
        val compat = st.oemCompatibility ?: app.oemCompatibilityAnalyzer.analyze(device, disp, graphics, shizukuStatus.value)
        return app.reportGenerator.generateTextReport(
            device = device,
            cpu = cpu,
            memory = mem,
            storage = storage,
            battery = bat,
            thermal = thermal,
            display = disp,
            graphics = graphics,
            shizuku = shizukuStatus.value,
            compat = compat,
            activeProfileId = activeGlobalProfileId.value,
            recentOperations = operationHistory.value
        )
    }

    fun shareTextReport(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SYMPHINE GAME SPACE Diagnostics Report")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share Diagnostics Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { app.startActivity(chooser) }
    }

    fun toggleOverlay(enable: Boolean, profileName: String = activeGlobalProfileId.value) {
        if (enable) {
            if (!app.permissionManager.canDrawOverlays()) {
                emitSnackbar("Overlay permission required — opening permission settings")
                runCatching { app.startActivity(app.permissionManager.createOverlayPermissionIntent()) }
                return
            }
            GamingOverlayService.startOverlay(app, profileName)
            emitSnackbar("Gaming Overlay started")
        } else {
            GamingOverlayService.stopOverlay(app)
            emitSnackbar("Gaming Overlay stopped")
        }
    }

    fun emitSnackbar(message: String) {
        viewModelScope.launch {
            _snackbarEvents.emit(message)
        }
    }

    override fun onCleared() {
        stopMonitoring()
        super.onCleared()
    }

    private fun defaultBuiltInProfiles(): List<CustomProfileDefinition> = listOf(
        CustomProfileDefinition(
            id = GameProfileType.BALANCED.id,
            name = GameProfileType.BALANCED.displayName,
            description = GameProfileType.BALANCED.description,
            baseType = GameProfileType.BALANCED,
            accentHex = "#B8B4C9",
            targetGameMode = "standard",
            targetAnimationScale = 1.0f
        ),
        CustomProfileDefinition(
            id = GameProfileType.GAMING.id,
            name = GameProfileType.GAMING.displayName,
            description = GameProfileType.GAMING.description,
            baseType = GameProfileType.GAMING,
            accentHex = "#A78BFA",
            targetGameMode = "performance",
            targetAnimationScale = 0.5f,
            enableMonitoring = true
        ),
        CustomProfileDefinition(
            id = GameProfileType.BATTERY_SAVER.id,
            name = GameProfileType.BATTERY_SAVER.displayName,
            description = GameProfileType.BATTERY_SAVER.description,
            baseType = GameProfileType.BATTERY_SAVER,
            accentHex = "#6EE7B7",
            targetGameMode = "battery",
            targetAnimationScale = 1.0f
        ),
        CustomProfileDefinition(
            id = GameProfileType.SMART.id,
            name = GameProfileType.SMART.displayName,
            description = GameProfileType.SMART.description,
            baseType = GameProfileType.SMART,
            accentHex = "#C4B5FD",
            targetAnimationScale = 0.5f,
            enableMonitoring = true
        ),
        CustomProfileDefinition(
            id = GameProfileType.RECOMMENDED.id,
            name = GameProfileType.RECOMMENDED.displayName,
            description = GameProfileType.RECOMMENDED.description,
            baseType = GameProfileType.RECOMMENDED,
            accentHex = "#A78BFA",
            targetGameMode = "performance",
            targetAnimationScale = 0.5f
        ),
        CustomProfileDefinition(
            id = GameProfileType.GAMING_STABLE.id,
            name = GameProfileType.GAMING_STABLE.displayName,
            description = GameProfileType.GAMING_STABLE.description,
            baseType = GameProfileType.GAMING_STABLE,
            accentHex = "#93C5FD",
            targetAnimationScale = 0.5f,
            enableMonitoring = true
        ),
        CustomProfileDefinition(
            id = GameProfileType.DISPLAY_FOCUS.id,
            name = GameProfileType.DISPLAY_FOCUS.displayName,
            description = GameProfileType.DISPLAY_FOCUS.description,
            baseType = GameProfileType.DISPLAY_FOCUS,
            accentHex = "#DDD6FE"
        ),
        CustomProfileDefinition(
            id = GameProfileType.MONITORING_FOCUS.id,
            name = GameProfileType.MONITORING_FOCUS.displayName,
            description = GameProfileType.MONITORING_FOCUS.description,
            baseType = GameProfileType.MONITORING_FOCUS,
            accentHex = "#D5D5DF",
            enableMonitoring = true
        )
    )
}
