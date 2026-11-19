package com.symphine.gamespace.app

import android.app.Application
import com.symphine.gamespace.backup.BackupEngine
import com.symphine.gamespace.core.notifications.NotificationHelper
import com.symphine.gamespace.core.permissions.PermissionManager
import com.symphine.gamespace.data.database.SymphineDatabase
import com.symphine.gamespace.data.datastore.UiPreferencesDataStore
import com.symphine.gamespace.device.BatteryInfoProvider
import com.symphine.gamespace.device.CpuInfoProvider
import com.symphine.gamespace.device.DeviceInfoProvider
import com.symphine.gamespace.device.DisplayInfoProvider
import com.symphine.gamespace.device.GraphicsInfoProvider
import com.symphine.gamespace.device.MemoryInfoProvider
import com.symphine.gamespace.device.StorageInfoProvider
import com.symphine.gamespace.device.ThermalInfoProvider
import com.symphine.gamespace.diagnostics.OemCompatibilityAnalyzer
import com.symphine.gamespace.diagnostics.PerformanceReportGenerator
import com.symphine.gamespace.games.GameRepository
import com.symphine.gamespace.games.GameScanner
import com.symphine.gamespace.performance.RecommendationEngine
import com.symphine.gamespace.performance.SettingsRegistryProvider
import com.symphine.gamespace.shizuku.ShizukuManager
import com.symphine.gamespace.shizuku.ShizukuOperationRegistry

class SymphineApplication : Application() {

    val database: SymphineDatabase by lazy { SymphineDatabase.getInstance(this) }
    val dao by lazy { database.symphineDao() }
    val installedGameDao by lazy { database.installedGameDao() }
    val uiPrefs: UiPreferencesDataStore by lazy { UiPreferencesDataStore(this) }

    val deviceInfoProvider by lazy { DeviceInfoProvider() }
    val cpuInfoProvider by lazy { CpuInfoProvider() }
    val memoryInfoProvider by lazy { MemoryInfoProvider(this) }
    val storageInfoProvider by lazy { StorageInfoProvider(this) }
    val batteryInfoProvider by lazy { BatteryInfoProvider(this) }
    val thermalInfoProvider by lazy { ThermalInfoProvider(this, batteryInfoProvider) }
    val displayInfoProvider by lazy { DisplayInfoProvider(this) }
    val graphicsInfoProvider by lazy { GraphicsInfoProvider(this) }

    val notificationHelper by lazy { NotificationHelper(this) }
    val permissionManager by lazy { PermissionManager(this) }

    val shizukuManager by lazy { ShizukuManager(this) }
    val shizukuRegistry by lazy {
        ShizukuOperationRegistry(
            context = this,
            shizukuManager = shizukuManager,
            displayInfoProvider = displayInfoProvider,
            dao = dao
        )
    }

    val gameScanner by lazy { GameScanner(this) }
    val gameRepository by lazy {
        GameRepository(
            context = this,
            scanner = gameScanner,
            dao = dao,
            installedGameDao = installedGameDao,
            prefs = uiPrefs
        )
    }

    val recommendationEngine by lazy { RecommendationEngine() }
    val settingsRegistryProvider by lazy { SettingsRegistryProvider(this, shizukuManager) }
    val oemCompatibilityAnalyzer by lazy { OemCompatibilityAnalyzer(permissionManager) }
    val reportGenerator by lazy { PerformanceReportGenerator() }
    val backupEngine by lazy { BackupEngine(dao, uiPrefs) }

    override fun onCreate() {
        super.onCreate()
        notificationHelper.ensureChannels()
        shizukuManager.initialize(
            onDisconnected = {
                notificationHelper.postOperationNotification(
                    "Shizuku Disconnected",
                    "Shizuku service disconnected; core offline features remain active.",
                    2003
                )
            }
        )
    }
}
