package com.symphine.gamespace.games

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.symphine.gamespace.core.model.GameClassification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScannedAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isLaunchable: Boolean,
    val targetSdkVersion: Int,
    val isSystemApp: Boolean,
    val autoGameClassification: GameClassification?
)

class GameScanner(private val context: Context) {

    private val knownGamePrefixes = listOf(
        "com.dts.freefire",
        "com.tencent.ig",
        "com.pubg.",
        "com.mobile.legends",
        "com.miHoYo.",
        "com.HoYoverse.",
        "com.activision.callofduty",
        "com.garena.",
        "com.supercell.",
        "com.roblox.",
        "com.mojang.",
        "com.riotgames.",
        "com.epicgames.",
        "com.krafton.",
        "com.levelinfinite.",
        "com.netease.",
        "com.ea.game.",
        "com.ea.gp.",
        "com.gameloft.",
        "com.nexon.",
        "com.kurogame.",
        "com.yostar.",
        "com.square_enix.",
        "com.bandainamcoent.",
        "com.innersloth.",
        "com.firsttouchgames.",
        "com.rockstargames.",
        "com.miniclip.",
        "com.kiloo.",
        "com.sybo.",
        "com.king.",
        "com.nianticlabs.",
        "com.playrix.",
        "com.zynga."
    )

    suspend fun scanLaunchableApps(
        userAddedPackages: Set<String>,
        userExcludedPackages: Set<String>
    ): List<ScannedAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val seenPackages = HashSet<String>()
        val results = mutableListOf<ScannedAppInfo>()

        for (ri in resolveInfos) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == context.packageName || !seenPackages.add(pkg)) continue
            val appInfo = ri.activityInfo?.applicationInfo ?: continue
            val pkgInfo = getPackageInfoSafe(pm, pkg)

            val label = ri.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
            val versionName = pkgInfo?.versionName ?: "Unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo?.longVersionCode ?: 0L
            } else {
                @Suppress("DEPRECATION")
                pkgInfo?.versionCode?.toLong() ?: 0L
            }

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val classification = classifyApp(pkg, appInfo, userAddedPackages, userExcludedPackages)

            results.add(
                ScannedAppInfo(
                    packageName = pkg,
                    appName = label,
                    versionName = versionName,
                    versionCode = versionCode,
                    firstInstallTime = pkgInfo?.firstInstallTime ?: 0L,
                    lastUpdateTime = pkgInfo?.lastUpdateTime ?: 0L,
                    isLaunchable = pm.getLaunchIntentForPackage(pkg) != null,
                    targetSdkVersion = appInfo.targetSdkVersion,
                    isSystemApp = isSystem,
                    autoGameClassification = classification
                )
            )
        }

        results.sortedBy { it.appName.lowercase() }
    }

    fun loadAppIcon(packageName: String): Drawable? {
        return runCatching {
            context.packageManager.getApplicationIcon(packageName)
        }.getOrNull()
    }

    private fun classifyApp(
        pkg: String,
        appInfo: ApplicationInfo,
        userAdded: Set<String>,
        userExcluded: Set<String>
    ): GameClassification? {
        if (pkg in userExcluded) return null
        if (pkg in userAdded) return GameClassification.USER_ADDED
        if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
            return GameClassification.AUTO_ANDROID_CATEGORY
        }
        @Suppress("DEPRECATION")
        if ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
            return GameClassification.AUTO_ANDROID_CATEGORY
        }
        if (knownGamePrefixes.any { pkg.startsWith(it, ignoreCase = true) }) {
            return GameClassification.AUTO_CURATED_PACKAGE
        }
        return null
    }

    private fun getPackageInfoSafe(pm: PackageManager, pkg: String): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
        }.getOrNull()
    }
}
