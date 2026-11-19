package com.symphine.gamespace.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        GameCustomizationEntity::class,
        CustomProfileEntity::class,
        GameSessionEntity::class,
        OperationHistoryEntity::class,
        OriginalBackupSnapshotEntity::class,
        InstalledGameEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SymphineDatabase : RoomDatabase() {
    abstract fun symphineDao(): SymphineDao
    abstract fun installedGameDao(): InstalledGameDao

    companion object {
        @Volatile
        private var INSTANCE: SymphineDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `installed_games` (
                        `packageName` TEXT NOT NULL,
                        `appName` TEXT NOT NULL,
                        `versionName` TEXT NOT NULL,
                        `versionCode` INTEGER NOT NULL,
                        `firstInstallTime` INTEGER NOT NULL,
                        `lastUpdateTime` INTEGER NOT NULL,
                        `isLaunchable` INTEGER NOT NULL,
                        `classification` TEXT NOT NULL,
                        `targetSdkVersion` INTEGER NOT NULL,
                        `lastScannedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`packageName`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): SymphineDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SymphineDatabase::class.java,
                    "symphine_gamespace.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
