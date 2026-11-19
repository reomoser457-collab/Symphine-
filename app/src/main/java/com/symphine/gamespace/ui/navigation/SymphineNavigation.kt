package com.symphine.gamespace.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.symphine.gamespace.core.model.NavAlignment
import com.symphine.gamespace.core.model.NavBarStyle
import com.symphine.gamespace.core.model.UiSettingsState
import com.symphine.gamespace.ui.components.BackupValidationDialog
import com.symphine.gamespace.ui.components.OperationVerificationDialog
import com.symphine.gamespace.ui.screens.GameDetailScreen
import com.symphine.gamespace.ui.screens.GameLibraryScreen
import com.symphine.gamespace.ui.screens.HomeScreen
import com.symphine.gamespace.ui.screens.MonitoringScreen
import com.symphine.gamespace.ui.screens.PerformanceScreen
import com.symphine.gamespace.ui.screens.SettingsScreen
import com.symphine.gamespace.ui.state.SymphineMainViewModel
import com.symphine.gamespace.ui.theme.LocalSymphineColors
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object GameSpaceRoute
@Serializable data class PerformanceRoute(val tab: Int = 0)
@Serializable data class MonitoringRoute(val tab: Int = 0)
@Serializable data class SettingsRoute(val tab: Int = 0)
@Serializable data class GameDetailRoute(val packageName: String)

private enum class SymphineTopDest(
    val id: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Rounded.Home),
    GAMESPACE("gamespace", "Game Space", Icons.Rounded.SportsEsports),
    PERFORMANCE("performance", "Performance", Icons.Rounded.Speed),
    MONITORING("monitoring", "Monitoring", Icons.Rounded.MonitorHeart),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings)
}

@Composable
fun SymphineAppRoot(viewModel: SymphineMainViewModel) {
    val colors = LocalSymphineColors.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiSettings by viewModel.uiSettings.collectAsStateWithLifecycle()
    val hardware by viewModel.hardwareState.collectAsStateWithLifecycle()
    val allGames by viewModel.allDetectedGames.collectAsStateWithLifecycle()
    val filteredGames by viewModel.filteredGames.collectAsStateWithLifecycle()
    val allLaunchableApps by viewModel.allLaunchableApps.collectAsStateWithLifecycle()
    val libraryFilter by viewModel.libraryFilterState.collectAsStateWithLifecycle()
    val customGroups by viewModel.customGroups.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanningGames.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.activeGlobalProfileId.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val sessionsHistory by viewModel.sessionsHistory.collectAsStateWithLifecycle()
    val profiles by viewModel.customProfiles.collectAsStateWithLifecycle()
    val shizuku by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val snapshotCount by viewModel.snapshotCount.collectAsStateWithLifecycle()
    val operationHistory by viewModel.operationHistory.collectAsStateWithLifecycle()
    val whitelistedPackages by viewModel.whitelistedPackages.collectAsStateWithLifecycle()
    val diagnosticsProfile by viewModel.diagnosticsProfile.collectAsStateWithLifecycle()
    val isMonitoring by viewModel.isMonitoringActive.collectAsStateWithLifecycle()
    val monitoringSamples by viewModel.monitoringSamples.collectAsStateWithLifecycle()
    val overlayRunning by viewModel.overlayRunning.collectAsStateWithLifecycle()
    val pendingPreview by viewModel.pendingOperationPreview.collectAsStateWithLifecycle()
    val isExecutingOperation by viewModel.isExecutingOperation.collectAsStateWithLifecycle()
    val backupValidation by viewModel.backupValidationResult.collectAsStateWithLifecycle()

    var settingsTabState by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            if (uiSettings.toastFeedbackEnabled) {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshShizuku()
        viewModel.refreshHardwareSnapshot()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (isMonitoring && !overlayRunning) {
            viewModel.stopMonitoring()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination
    val isDetailRoute = currentDest?.hasRoute(GameDetailRoute::class) == true

    val activeTopDest = when {
        currentDest?.hasRoute(GameSpaceRoute::class) == true -> SymphineTopDest.GAMESPACE
        currentDest?.hasRoute(PerformanceRoute::class) == true -> SymphineTopDest.PERFORMANCE
        currentDest?.hasRoute(MonitoringRoute::class) == true -> SymphineTopDest.MONITORING
        currentDest?.hasRoute(SettingsRoute::class) == true -> SymphineTopDest.SETTINGS
        else -> SymphineTopDest.HOME
    }

    fun navigateTop(dest: SymphineTopDest, tab: Int = 0) {
        if (dest == SymphineTopDest.SETTINGS) settingsTabState = tab
        val route: Any = when (dest) {
            SymphineTopDest.HOME -> HomeRoute
            SymphineTopDest.GAMESPACE -> GameSpaceRoute
            SymphineTopDest.PERFORMANCE -> PerformanceRoute(tab)
            SymphineTopDest.MONITORING -> MonitoringRoute(tab)
            SymphineTopDest.SETTINGS -> SettingsRoute(tab)
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = (dest == SymphineTopDest.HOME || dest == SymphineTopDest.GAMESPACE)
        }
    }

    pendingPreview?.let { preview ->
        OperationVerificationDialog(
            preview = preview,
            onConfirm = { viewModel.confirmAndExecutePendingOperation() },
            onDismiss = { viewModel.dismissOperationPreview() }
        )
    }

    backupValidation?.let { validation ->
        BackupValidationDialog(
            validation = validation,
            onConfirmRestore = { schema, restoreUi, restoreLibrary, restoreProfiles ->
                viewModel.applyValidatedBackup(schema, restoreUi, restoreLibrary, restoreProfiles)
            },
            onDismiss = { viewModel.dismissBackupValidation() }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && !isDetailRoute) {
                SymphineNavigationRail(
                    active = activeTopDest,
                    onSelect = { navigateTop(it) }
                )
            }

            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                containerColor = colors.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = colors.surfaceElevated,
                            contentColor = colors.softSilver,
                            actionColor = colors.purpleAccent,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .statusBarsPadding()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable<HomeRoute> {
                            HomeScreen(
                                hardware = hardware,
                                games = allGames,
                                activeProfileId = activeProfileId,
                                activeSession = activeSession,
                                shizuku = shizuku,
                                uiSettings = uiSettings,
                                isMonitoring = isMonitoring,
                                onRefreshAll = { viewModel.refreshHardwareAndGames() },
                                onScanGames = { viewModel.rescanGamesOnly() },
                                onOpenGameSpace = { navigateTop(SymphineTopDest.GAMESPACE) },
                                onOpenGameDetail = { pkg -> navController.navigate(GameDetailRoute(pkg)) },
                                onOpenPerformance = { tab -> navigateTop(SymphineTopDest.PERFORMANCE, tab) },
                                onOpenMonitoring = { tab -> navigateTop(SymphineTopDest.MONITORING, tab) },
                                onOpenSettings = { tab -> navigateTop(SymphineTopDest.SETTINGS, tab) },
                                onLaunchGame = { g -> viewModel.launchGame(g) },
                                onStopSession = { viewModel.stopActiveSession() }
                            )
                        }

                        composable<GameSpaceRoute> {
                            GameLibraryScreen(
                                games = filteredGames,
                                allLaunchableApps = allLaunchableApps,
                                filterState = libraryFilter,
                                customGroups = customGroups,
                                isScanning = isScanning,
                                onRescan = { viewModel.rescanGamesOnly() },
                                onFilterChange = { viewModel.updateLibraryFilter(it) },
                                onToggleBulkPackage = { viewModel.toggleBulkPackage(it) },
                                onClearBulk = { viewModel.clearBulkSelection() },
                                onBulkApply = { transform, label -> viewModel.bulkApply(transform, label) },
                                onAddAppToLibrary = { viewModel.addAppToLibrary(it) },
                                onAddCustomGroup = { viewModel.addCustomGroup(it) },
                                onUpdateGameCustomization = { pkg, t -> viewModel.updateGameCustomization(pkg, t) },
                                onLaunchGame = { g -> viewModel.launchGame(g) },
                                onOpenGameDetail = { pkg -> navController.navigate(GameDetailRoute(pkg)) }
                            )
                        }

                        composable<PerformanceRoute> { entry ->
                            val args = entry.toRoute<PerformanceRoute>()
                            PerformanceScreen(
                                initialTab = args.tab,
                                hardware = hardware,
                                games = allGames,
                                allApps = allLaunchableApps,
                                profiles = profiles,
                                activeProfileId = activeProfileId,
                                shizuku = shizuku,
                                snapshotCount = snapshotCount,
                                operationHistory = operationHistory,
                                whitelistedPackages = whitelistedPackages,
                                diagnosticsProfile = diagnosticsProfile,
                                onSelectProfile = { viewModel.selectGlobalProfile(it) },
                                onCreateProfile = { n, d, b, hex, gm, hz, anim, am, lod ->
                                    viewModel.createOrCloneCustomProfile(n, d, b, hex, gm, hz, anim, am, lod)
                                },
                                onDeleteProfile = { viewModel.deleteUserProfile(it) },
                                onRequestRefreshRate = { viewModel.requestRefreshRateOperation(it) },
                                onRequestAnimationScale = { viewModel.requestAnimationScaleOperation(it) },
                                onRequestBackgroundFlags = { viewModel.requestBackgroundFlagsOperation() },
                                onRequestDpi = { viewModel.requestDpiOperation(it) },
                                onRequestDownscale = { viewModel.requestDownscaleOperation(it) },
                                onRequestAngleOp = { pkg, title, mode, lod, restart ->
                                    viewModel.requestAngleOperation(pkg, title, mode, lod, restart)
                                },
                                onRequestArtCompile = { pkg, title, mode ->
                                    viewModel.requestArtCompileOperation(pkg, title, mode)
                                },
                                onRequestGameMode = { pkg, title, mode ->
                                    viewModel.requestGameModeOperation(pkg, title, mode)
                                },
                                onRequestShizukuPermission = { viewModel.requestShizukuPermission() },
                                onReconnectShizuku = { viewModel.reconnectShizuku() },
                                onRefreshShizuku = { viewModel.refreshShizuku() },
                                onAtomicRestoreAll = { viewModel.executeAtomicFullRestore() },
                                onToggleWhitelist = { viewModel.toggleWhitelistPackage(it) },
                                onSetDiagnosticsProfile = { viewModel.setDiagnosticsProfile(it) }
                            )
                        }

                        composable<MonitoringRoute> { entry ->
                            val args = entry.toRoute<MonitoringRoute>()
                            MonitoringScreen(
                                initialTab = args.tab,
                                hardware = hardware,
                                uiSettings = uiSettings,
                                isMonitoring = isMonitoring,
                                samples = monitoringSamples,
                                sessions = sessionsHistory,
                                onToggleMonitoring = { viewModel.toggleMonitoring() },
                                onResetMonitoring = { viewModel.resetMonitoring() },
                                onSetIntervalMs = { ms -> viewModel.updateUiSettings { it.copy(monitoringIntervalMs = ms) } },
                                onExportCsv = { viewModel.exportMonitoringCsv() },
                                onClearAppCache = { viewModel.clearAppCacheOnly() },
                                onClearSessions = { viewModel.clearSessions() },
                                onGenerateReportText = { viewModel.generateDiagnosticsReportText() },
                                onShareText = { viewModel.shareTextReport(it) },
                                onNotifySnackbar = { viewModel.emitSnackbar(it) }
                            )
                        }

                        composable<SettingsRoute> { entry ->
                            val args = entry.toRoute<SettingsRoute>()
                            LaunchedEffect(args.tab) {
                                settingsTabState = args.tab
                            }
                            SettingsScreen(
                                selectedTab = settingsTabState,
                                onSelectTab = { settingsTabState = it },
                                uiSettings = uiSettings,
                                hardware = hardware,
                                snapshotCount = snapshotCount,
                                onUpdateUiSettings = { viewModel.updateUiSettings(it) },
                                onResetUiSettings = { viewModel.resetUiSettings() },
                                onWipeAllData = { viewModel.wipeAllLocalAppData() },
                                onGenerateBackupJson = { viewModel.generateBackupJson() },
                                onValidateBackupImport = { viewModel.validateBackupImport(it) },
                                onExecuteAtomicRestore = { viewModel.executeAtomicFullRestore() },
                                onRefreshPermissions = { viewModel.refreshHardwareSnapshot() },
                                onRequestShizuku = { viewModel.requestShizukuPermission() },
                                onOpenShizukuTab = { navigateTop(SymphineTopDest.PERFORMANCE, 3) },
                                onEmitSnackbar = { viewModel.emitSnackbar(it) }
                            )
                        }

                        composable<GameDetailRoute> { entry ->
                            val args = entry.toRoute<GameDetailRoute>()
                            val game = allGames.find { it.packageName == args.packageName }
                            GameDetailScreen(
                                game = game,
                                hardware = hardware,
                                shizuku = shizuku,
                                profiles = profiles,
                                customGroups = customGroups,
                                whitelistedPackages = whitelistedPackages,
                                operationHistory = operationHistory,
                                settingsRegistry = hardware.settingsRegistry,
                                activeSession = activeSession,
                                isMonitoring = isMonitoring,
                                telemetrySamples = monitoringSamples,
                                overlayRunning = overlayRunning,
                                onBack = { navController.popBackStack() },
                                onLaunchGame = { viewModel.launchGame(it) },
                                onUpdateCustomization = { pkg, t -> viewModel.updateGameCustomization(pkg, t) },
                                onResetCustomization = { viewModel.resetGameCustomization(it) },
                                onRequestAngleOp = { pkg, title, mode, lod, restart ->
                                    viewModel.requestAngleOperation(pkg, title, mode, lod, restart)
                                },
                                onRequestArtCompile = { pkg, title, mode ->
                                    viewModel.requestArtCompileOperation(pkg, title, mode)
                                },
                                onRequestGameMode = { pkg, title, mode ->
                                    viewModel.requestGameModeOperation(pkg, title, mode)
                                },
                                onRequestRefreshRate = { viewModel.requestRefreshRateOperation(it) },
                                onRequestAnimationScale = { viewModel.requestAnimationScaleOperation(it) },
                                onRequestDpi = { viewModel.requestDpiOperation(it) },
                                onRequestDownscale = { viewModel.requestDownscaleOperation(it) },
                                onToggleOverlay = { enable, profile -> viewModel.toggleOverlay(enable, profile) },
                                onToggleMonitoring = { viewModel.toggleMonitoring() },
                                onResetMonitoring = { viewModel.resetMonitoring() },
                                onToggleWhitelist = { viewModel.toggleWhitelistPackage(it) },
                                onStartManualSession = { pkg, title, prof -> viewModel.startManualSession(pkg, title, prof) },
                                onStopManualSession = { viewModel.stopActiveSession() },
                                onOpenAndroidAppInfo = { viewModel.openAndroidAppInfo(it) },
                                onRemoveFromLibrary = { pkg ->
                                    viewModel.removeGameFromLibrary(pkg)
                                    navController.popBackStack()
                                },
                                onAtomicRestoreAll = { viewModel.executeAtomicFullRestore() },
                                onCreateProfile = { n, d, b, hex, gm, hz, anim, am, lod ->
                                    viewModel.createOrCloneCustomProfile(n, d, b, hex, gm, hz, anim, am, lod)
                                },
                                onShareGameConfig = { txt -> viewModel.shareTextReport(txt) }
                            )
                        }
                    }

                    if (!useRail && !isDetailRoute) {
                        SymphineBottomNavContainer(
                            active = activeTopDest,
                            uiSettings = uiSettings,
                            onSelect = { navigateTop(it) },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    if (isExecutingOperation) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x9908090D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = colors.surfaceElevated,
                                border = BorderStroke(1.dp, colors.purpleAccent.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = colors.purpleAccent
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Executing & verifying via Shizuku...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.softSilver
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SymphineNavigationRail(
    active: SymphineTopDest,
    onSelect: (SymphineTopDest) -> Unit
) {
    val colors = LocalSymphineColors.current
    NavigationRail(
        containerColor = colors.surface,
        contentColor = colors.softSilver,
        modifier = Modifier.statusBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))
        SymphineTopDest.entries.forEach { dest ->
            NavigationRailItem(
                selected = active == dest,
                onClick = { onSelect(dest) },
                modifier = Modifier.testTag("nav_${dest.id}"),
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = colors.purpleAccent,
                    selectedTextColor = colors.purpleAccent,
                    unselectedIconColor = colors.mutedText,
                    unselectedTextColor = colors.mutedText,
                    indicatorColor = colors.purpleAccent.copy(alpha = 0.16f)
                )
            )
        }
    }
}

@Composable
private fun SymphineBottomNavContainer(
    active: SymphineTopDest,
    uiSettings: UiSettingsState,
    onSelect: (SymphineTopDest) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSymphineColors.current

    if (uiSettings.navBarStyle == NavBarStyle.MATERIAL_BAR) {
        NavigationBar(
            modifier = modifier.fillMaxWidth(),
            containerColor = colors.surface.copy(alpha = 0.96f),
            contentColor = colors.softSilver
        ) {
            SymphineTopDest.entries.forEach { dest ->
                NavigationBarItem(
                    selected = active == dest,
                    onClick = { onSelect(dest) },
                    modifier = Modifier.testTag("nav_${dest.id}"),
                    icon = {
                        Icon(
                            dest.icon,
                            contentDescription = dest.label,
                            modifier = Modifier.size(uiSettings.navIconSizeDp.dp)
                        )
                    },
                    label = {
                        Text(
                            dest.label,
                            fontSize = uiSettings.navTextSizeSp.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.purpleAccent,
                        selectedTextColor = colors.purpleAccent,
                        unselectedIconColor = colors.mutedText,
                        unselectedTextColor = colors.mutedText,
                        indicatorColor = colors.purpleAccent.copy(alpha = 0.16f)
                    )
                )
            }
        }
        return
    }

    val boxAlign = when (uiSettings.navAlignment) {
        NavAlignment.LEFT -> Alignment.CenterStart
        NavAlignment.CENTER -> Alignment.Center
        NavAlignment.RIGHT -> Alignment.CenterEnd
    }

    val shape = when (uiSettings.navBarStyle) {
        NavBarStyle.PILL -> RoundedCornerShape(28.dp)
        NavBarStyle.CAPSULE -> CircleShape
        NavBarStyle.INDICATOR -> RoundedCornerShape(18.dp)
        NavBarStyle.MATERIAL_BAR -> RoundedCornerShape(0.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = boxAlign
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .height(uiSettings.navHeightDp.coerceIn(56, 86).dp)
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colors.surfaceElevated.copy(alpha = 0.94f),
                            colors.surface.copy(alpha = 0.94f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (uiSettings.glassGlowEnabled) colors.purpleAccent.copy(alpha = 0.35f) else colors.border,
                    shape = shape
                )
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SymphineTopDest.entries.forEach { dest ->
                val selected = active == dest
                val itemModifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .clickable { onSelect(dest) }
                    .testTag("nav_${dest.id}")
                    .padding(vertical = 6.dp, horizontal = 2.dp)

                Column(
                    modifier = itemModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (selected && uiSettings.navBarStyle != NavBarStyle.INDICATOR) {
                                    colors.purpleAccent.copy(alpha = 0.20f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = dest.label,
                            tint = if (selected) colors.purpleAccent else colors.mutedText,
                            modifier = Modifier.size(uiSettings.navIconSizeDp.coerceIn(18, 28).dp)
                        )
                    }
                    Text(
                        text = dest.label,
                        fontSize = uiSettings.navTextSizeSp.coerceIn(9, 13).sp,
                        color = if (selected) colors.secondaryAccent else colors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selected && uiSettings.navBarStyle == NavBarStyle.INDICATOR) {
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(colors.purpleAccent)
                        )
                    }
                }
            }
        }
    }
}
