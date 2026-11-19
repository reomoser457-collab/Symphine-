package com.symphine.gamespace

import android.content.Context
import android.os.PowerManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.symphine.gamespace.backup.BackupEngine
import com.symphine.gamespace.core.model.DeviceHardwareInfo
import com.symphine.gamespace.core.model.DisplayHardwareInfo
import com.symphine.gamespace.core.model.DisplayModeInfo
import com.symphine.gamespace.core.model.GameClassification
import com.symphine.gamespace.core.model.GameCustomization
import com.symphine.gamespace.core.model.GameProfileType
import com.symphine.gamespace.core.model.GraphicsHardwareInfo
import com.symphine.gamespace.core.model.MemoryHardwareInfo
import com.symphine.gamespace.core.model.OemBrand
import com.symphine.gamespace.core.model.OperationStatus
import com.symphine.gamespace.core.model.ThermalHardwareInfo
import com.symphine.gamespace.core.permissions.PermissionManager
import com.symphine.gamespace.data.database.GameCustomizationEntity
import com.symphine.gamespace.data.database.InstalledGameEntity
import com.symphine.gamespace.data.database.SymphineDatabase
import com.symphine.gamespace.data.datastore.UiPreferencesDataStore
import com.symphine.gamespace.diagnostics.OemCompatibilityAnalyzer
import com.symphine.gamespace.games.ScannedAppInfo
import com.symphine.gamespace.performance.RecommendationEngine
import com.symphine.gamespace.shizuku.ShizukuConnectionPhase
import com.symphine.gamespace.shizuku.ShizukuStatusState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SymphineCoreLogicTest {

    private lateinit var context: Context
    private lateinit var db: SymphineDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SymphineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun appMetadataAndDeveloperStringsMatchSpec() {
        assertEquals("com.symphine.gamespace", context.packageName)
        assertEquals("SYMPHINE GAME SPACE", context.getString(R.string.app_full_name))
        assertEquals("Reo Moser", context.getString(R.string.developer_name))
    }

    @Test
    fun recommendationEnginePrioritizesThermalStabilityWhenThrottling() {
        val engine = RecommendationEngine()
        val device = sampleDevice(OemBrand.XIAOMI)
        val mem = MemoryHardwareInfo(
            totalRamBytes = 8L * 1024 * 1024 * 1024,
            availableRamBytes = 3L * 1024 * 1024 * 1024,
            usedRamBytes = 5L * 1024 * 1024 * 1024,
            thresholdBytes = 512L * 1024 * 1024,
            isLowMemory = false,
            swapTotalBytes = null,
            swapFreeBytes = null
        )
        val display = sampleDisplay(listOf(60f, 90f, 120f))
        val hotThermal = ThermalHardwareInfo(
            statusCode = PowerManager.THERMAL_STATUS_MODERATE,
            statusLabel = "Moderate Thermal",
            isThrottling = true,
            batteryTempCelsius = 43.2f,
            readableZones = emptyList(),
            sensorAccessNote = "Test"
        )
        val shizuku = ShizukuStatusState(
            isShizukuAppInstalled = true,
            isBinderAlive = true,
            isPermissionGranted = true,
            isUserServiceBound = true,
            phase = ShizukuConnectionPhase.CONNECTED,
            serviceUid = 2000
        )

        val recs = engine.generateRecommendations(device, mem, display, hotThermal, sampleGraphics(), shizuku)
        val recommended = recs.first { it.profileType == GameProfileType.RECOMMENDED }

        assertEquals(60f, recommended.suggestedRefreshHz)
        assertTrue(recommended.warnings.any { it.contains("Thermal status", ignoreCase = true) })
        assertEquals(OperationStatus.AVAILABLE, recommended.compatibilityStatus)
    }

    @Test
    fun oemCompatibilityAnalyzerHonestyAndShizukuGating() {
        val analyzer = OemCompatibilityAnalyzer(PermissionManager(context))
        val device = sampleDevice(OemBrand.SAMSUNG)
        val display = sampleDisplay(listOf(60f, 120f))
        val gfx = sampleGraphics()

        val disconnected = analyzer.analyze(device, display, gfx, ShizukuStatusState())
        val refreshDisconnected = disconnected.features.first { it.featureId == "refresh_override" }
        val gpuUtil = disconnected.features.first { it.featureId == "gpu_util" }

        assertEquals(OperationStatus.REQUIRES_SHIZUKU, refreshDisconnected.status)
        assertEquals(OperationStatus.UNSUPPORTED, gpuUtil.status)

        val operational = ShizukuStatusState(
            isShizukuAppInstalled = true,
            isBinderAlive = true,
            isPermissionGranted = true,
            isUserServiceBound = true,
            phase = ShizukuConnectionPhase.CONNECTED,
            serviceUid = 2000
        )
        val connected = analyzer.analyze(device, display, gfx, operational)
        val refreshConnected = connected.features.first { it.featureId == "refresh_override" }
        assertEquals(OperationStatus.AVAILABLE, refreshConnected.status)
    }

    @Test
    fun backupEngineRoundTripAndValidationSafety() = runTest {
        val dao = db.symphineDao()
        val prefs = UiPreferencesDataStore(context)
        val backupEngine = BackupEngine(dao, prefs)

        dao.upsertCustomization(
            GameCustomizationEntity.fromModel(
                GameCustomization(
                    packageName = "com.dts.freefireth",
                    customLabel = "Free Fire Test",
                    isFavorite = true,
                    assignedProfileId = "gaming",
                    notes = "Test notes"
                )
            )
        )

        val exportedJson = backupEngine.createBackupJson()
        val valid = backupEngine.validateBackupJson(exportedJson)
        assertTrue(valid.isValid)
        assertNotNull(valid.schema)
        assertEquals(1, valid.schema?.gameCustomizations?.size)
        assertEquals(1, valid.conflicts.size)

        val invalidSyntax = backupEngine.validateBackupJson("{not valid json")
        assertFalse(invalidSyntax.isValid)

        val wrongPkgJson = exportedJson.replace("\"com.symphine.gamespace\"", "\"com.untrusted.app\"")
        val wrongPkg = backupEngine.validateBackupJson(wrongPkgJson)
        assertFalse(wrongPkg.isValid)
    }

    @Test
    fun installedGameDaoStoresAndRetrievesPersonalLibraryMetadata() = runTest {
        val gameDao = db.installedGameDao()
        val firstScan = listOf(
            InstalledGameEntity.fromScannedApp(
                ScannedAppInfo(
                    packageName = "com.mobile.legends",
                    appName = "Mobile Legends",
                    versionName = "1.9.10",
                    versionCode = 1910L,
                    firstInstallTime = 1_700_000_000_000L,
                    lastUpdateTime = 1_700_100_000_000L,
                    isLaunchable = true,
                    targetSdkVersion = 34,
                    isSystemApp = false,
                    autoGameClassification = GameClassification.AUTO_CURATED_PACKAGE
                ),
                scannedAtMillis = 1000L
            ),
            InstalledGameEntity.fromScannedApp(
                ScannedAppInfo(
                    packageName = "com.dts.freefireth",
                    appName = "Free Fire",
                    versionName = "1.105.1",
                    versionCode = 2019117000L,
                    firstInstallTime = 1_700_000_000_000L,
                    lastUpdateTime = 1_700_200_000_000L,
                    isLaunchable = true,
                    targetSdkVersion = 35,
                    isSystemApp = false,
                    autoGameClassification = GameClassification.AUTO_ANDROID_CATEGORY
                ),
                scannedAtMillis = 1000L
            )
        )

        gameDao.replaceInstalledLibrary(firstScan)

        val observed = gameDao.observeAllInstalledGames().first()
        assertEquals(2, observed.size)
        // Ordered alphabetically by appName
        assertEquals("Free Fire", observed[0].appName)
        assertEquals("Mobile Legends", observed[1].appName)

        val ff = gameDao.getInstalledGameByPackage("com.dts.freefireth")
        assertNotNull(ff)
        assertEquals(GameClassification.AUTO_ANDROID_CATEGORY, ff?.toClassification())
        assertEquals("1.105.1", ff?.versionName)

        // Syncing a new scan removes uninstalled/excluded stale packages automatically
        gameDao.replaceInstalledLibrary(listOf(firstScan[1]))
        assertEquals(1, gameDao.countInstalledGames())
        assertEquals(null, gameDao.getInstalledGameByPackage("com.mobile.legends"))
    }

    private fun sampleDevice(brand: OemBrand) = DeviceHardwareInfo(
        manufacturer = brand.displayName,
        brand = brand.name,
        model = "TestDevice",
        device = "test",
        androidVersion = "15",
        apiLevel = 35,
        buildId = "AP3A",
        securityPatch = "2026-01-01",
        kernelVersion = "6.1",
        socModel = "Snapdragon",
        hardwareBoard = "qcom",
        primaryAbi = "arm64-v8a",
        supportedAbis = listOf("arm64-v8a"),
        oemBrand = brand
    )

    private fun sampleDisplay(rates: List<Float>) = DisplayHardwareInfo(
        widthPx = 1080,
        heightPx = 2400,
        aspectRatio = "20:9",
        densityDpi = 420,
        densityScale = 2.625f,
        currentRefreshRateHz = rates.last(),
        supportedModes = rates.mapIndexed { idx, hz -> DisplayModeInfo(idx, 1080, 2400, hz) },
        supportedRefreshRates = rates,
        isAdaptiveRefreshSupported = rates.size > 1,
        isHdrSupported = true,
        isWideColorGamut = true,
        peakRefreshSetting = null,
        minRefreshSetting = null,
        wmSizeOverride = null,
        wmDensityOverride = null
    )

    private fun sampleGraphics() = GraphicsHardwareInfo(
        gpuRenderer = "Adreno 740",
        gpuVendor = "Qualcomm",
        openGlEsVersion = "3.2",
        glVersionDetail = "OpenGL ES 3.2",
        vulkanSupported = true,
        vulkanVersion = "1.3",
        eglDriverHint = null,
        hwuiRenderer = "skiagl",
        angleInstalled = true,
        anglePackageName = "com.android.angle",
        angleTargetPackages = null,
        angleDriverValues = null,
        angleFeatureOverrides = null,
        gpuUtilizationNote = "Unavailable"
    )
}
