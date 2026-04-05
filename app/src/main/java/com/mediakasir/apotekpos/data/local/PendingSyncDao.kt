package com.mediakasir.apotekpos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {

    @Query("SELECT * FROM pending_sync ORDER BY createdAtEpochMs ASC")
    suspend fun getAllPending(): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingSyncEntity)

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        UPDATE pending_sync SET attempts = attempts + 1, lastError = :err
        WHERE id = :id
        """,
    )
    suspend fun bumpAttempt(id: String, err: String)

    @Query("SELECT COUNT(*) FROM pending_sync")
    fun observeCount(): Flow<Int>
}
