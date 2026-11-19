package com.symphine.gamespace.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.symphine.gamespace.core.model.GameClassification
import com.symphine.gamespace.games.ScannedAppInfo

@Entity(tableName = "installed_games")
data class InstalledGameEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isLaunchable: Boolean,
    val classification: String,
    val targetSdkVersion: Int,
    val lastScannedAtMillis: Long
) {
    fun toClassification(): GameClassification =
        runCatching { GameClassification.valueOf(classification) }
            .getOrDefault(GameClassification.USER_ADDED)

    companion object {
        fun fromScannedApp(
            app: ScannedAppInfo,
            scannedAtMillis: Long = System.currentTimeMillis()
        ): InstalledGameEntity = InstalledGameEntity(
            packageName = app.packageName,
            appName = app.appName,
            versionName = app.versionName,
            versionCode = app.versionCode,
            firstInstallTime = app.firstInstallTime,
            lastUpdateTime = app.lastUpdateTime,
            isLaunchable = app.isLaunchable,
            classification = (app.autoGameClassification ?: GameClassification.USER_ADDED).name,
            targetSdkVersion = app.targetSdkVersion,
            lastScannedAtMillis = scannedAtMillis
        )
    }
}
