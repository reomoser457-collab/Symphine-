package com.symphine.gamespace.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledGameDao {

    @Query("SELECT * FROM installed_games ORDER BY appName COLLATE NOCASE ASC")
    fun observeAllInstalledGames(): Flow<List<InstalledGameEntity>>

    @Query("SELECT * FROM installed_games ORDER BY appName COLLATE NOCASE ASC")
    suspend fun getInstalledGamesOnce(): List<InstalledGameEntity>

    @Query("SELECT * FROM installed_games WHERE packageName = :packageName LIMIT 1")
    suspend fun getInstalledGameByPackage(packageName: String): InstalledGameEntity?

    @Query("SELECT COUNT(*) FROM installed_games")
    suspend fun countInstalledGames(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstalledGame(game: InstalledGameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstalledGames(games: List<InstalledGameEntity>)

    @Query("DELETE FROM installed_games WHERE packageName = :packageName")
    suspend fun deleteInstalledGame(packageName: String)

    @Query("DELETE FROM installed_games WHERE packageName NOT IN (:activePackages)")
    suspend fun deleteStaleInstalledGames(activePackages: List<String>)

    @Query("DELETE FROM installed_games")
    suspend fun clearInstalledGames()

    @Transaction
    suspend fun replaceInstalledLibrary(games: List<InstalledGameEntity>) {
        if (games.isEmpty()) {
            clearInstalledGames()
        } else {
            upsertInstalledGames(games)
            deleteStaleInstalledGames(games.map { it.packageName })
        }
    }
}
