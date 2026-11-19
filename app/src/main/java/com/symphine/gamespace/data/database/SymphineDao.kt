package com.symphine.gamespace.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SymphineDao {

    // Game Customizations
    @Query("SELECT * FROM game_customizations")
    fun observeAllCustomizations(): Flow<List<GameCustomizationEntity>>

    @Query("SELECT * FROM game_customizations")
    suspend fun getAllCustomizationsOnce(): List<GameCustomizationEntity>

    @Query("SELECT * FROM game_customizations WHERE packageName = :packageName")
    fun observeCustomization(packageName: String): Flow<GameCustomizationEntity?>

    @Query("SELECT * FROM game_customizations WHERE packageName = :packageName")
    suspend fun getCustomizationOnce(packageName: String): GameCustomizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomization(entity: GameCustomizationEntity)

    @Query("DELETE FROM game_customizations WHERE packageName = :packageName")
    suspend fun deleteCustomization(packageName: String)

    @Query("DELETE FROM game_customizations")
    suspend fun clearAllCustomizations()

    // Custom Profiles
    @Query("SELECT * FROM custom_profiles ORDER BY isUserCreated ASC, name ASC")
    fun observeProfiles(): Flow<List<CustomProfileEntity>>

    @Query("SELECT * FROM custom_profiles")
    suspend fun getProfilesOnce(): List<CustomProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: CustomProfileEntity)

    @Query("DELETE FROM custom_profiles WHERE id = :id AND isUserCreated = 1")
    suspend fun deleteUserProfile(id: String)

    // Sessions
    @Query("SELECT * FROM game_sessions ORDER BY startTimeMillis DESC")
    fun observeSessions(): Flow<List<GameSessionEntity>>

    @Query("SELECT * FROM game_sessions ORDER BY startTimeMillis DESC")
    suspend fun getSessionsOnce(): List<GameSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: GameSessionEntity): Long

    @Query("DELETE FROM game_sessions")
    suspend fun clearSessions()

    // Operation History
    @Query("SELECT * FROM operation_history ORDER BY timestampMillis DESC LIMIT 200")
    fun observeOperationHistory(): Flow<List<OperationHistoryEntity>>

    @Query("SELECT * FROM operation_history ORDER BY timestampMillis DESC LIMIT 200")
    suspend fun getOperationHistoryOnce(): List<OperationHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperationHistory(entry: OperationHistoryEntity)

    @Query("DELETE FROM operation_history")
    suspend fun clearOperationHistory()

    // Original System Backup Snapshots (write-once per key unless cleared on full restore)
    @Query("SELECT * FROM original_system_snapshots ORDER BY compositeKey ASC")
    fun observeSnapshots(): Flow<List<OriginalBackupSnapshotEntity>>

    @Query("SELECT * FROM original_system_snapshots")
    suspend fun getSnapshotsOnce(): List<OriginalBackupSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSnapshotIfAbsent(snapshot: OriginalBackupSnapshotEntity)

    @Query("DELETE FROM original_system_snapshots")
    suspend fun clearSnapshots()
}
