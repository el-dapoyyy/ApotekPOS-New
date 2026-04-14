package com.mediakasir.apotekpos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalShiftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shift: LocalShiftEntity)

    @Query("SELECT * FROM local_shifts WHERE id = :shiftId")
    suspend fun getShift(shiftId: String): LocalShiftEntity?

    /** Active shift = started but not yet ended, for this cashier today. */
    @Query("""
        SELECT * FROM local_shifts 
        WHERE cashierId = :cashierId AND branchId = :branchId AND endedAt IS NULL 
        ORDER BY createdAtEpochMs DESC 
        LIMIT 1
    """)
    suspend fun getActiveShift(cashierId: String, branchId: String): LocalShiftEntity?

    @Query("""
        SELECT * FROM local_shifts 
        WHERE cashierId = :cashierId AND branchId = :branchId AND endedAt IS NULL 
        ORDER BY createdAtEpochMs DESC 
        LIMIT 1
    """)
    fun observeActiveShift(cashierId: String, branchId: String): Flow<LocalShiftEntity?>

    @Query("""
        UPDATE local_shifts 
        SET endedAt = :endedAt, 
            endingCash = :endingCash, 
            endingCashExpected = :expectedCash,
            totalSales = :totalSales, 
            totalCashSales = :totalCashSales,
            totalNonCashSales = :totalNonCashSales,
            totalTransactions = :totalTransactions, 
            notes = :notes 
        WHERE id = :shiftId
    """)
    suspend fun closeShift(
        shiftId: String,
        endedAt: String,
        endingCash: Double,
        expectedCash: Double,
        totalSales: Double,
        totalCashSales: Double,
        totalNonCashSales: Double,
        totalTransactions: Int,
        notes: String?,
    )

    @Query("UPDATE local_shifts SET syncStatus = 'synced', serverShiftId = :serverId WHERE id = :shiftId")
    suspend fun markSynced(shiftId: String, serverId: Int)

    @Query("SELECT * FROM local_shifts WHERE syncStatus != 'synced' ORDER BY createdAtEpochMs ASC")
    suspend fun getPendingShifts(): List<LocalShiftEntity>

    @Query("""
        SELECT * FROM local_shifts 
        WHERE branchId = :branchId 
        ORDER BY createdAtEpochMs DESC 
        LIMIT :limit
    """)
    suspend fun getRecentShifts(branchId: String, limit: Int = 20): List<LocalShiftEntity>
}
