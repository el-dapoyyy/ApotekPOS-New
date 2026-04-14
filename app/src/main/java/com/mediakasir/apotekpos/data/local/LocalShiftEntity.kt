package com.mediakasir.apotekpos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local shift tracking. Stored offline-first, synced when online.
 */
@Entity(tableName = "local_shifts")
data class LocalShiftEntity(
    @PrimaryKey val id: String,
    val cashierId: String,
    val cashierName: String,
    val branchId: String,
    /** "pagi" | "sore" | "malam" */
    val shiftType: String,
    val startedAt: String,
    val startingCash: Double,
    val endedAt: String? = null,
    val endingCash: Double? = null,
    val endingCashExpected: Double? = null,
    val totalSales: Double = 0.0,
    val totalCashSales: Double = 0.0,
    val totalNonCashSales: Double = 0.0,
    val totalTransactions: Int = 0,
    val notes: String? = null,
    /** pending | synced | failed */
    val syncStatus: String = "pending",
    val serverShiftId: Int? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)
