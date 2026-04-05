package com.mediakasir.apotekpos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey val id: String,
    /** e.g. TRANSACTION_SYNC */
    val type: String,
    val payloadJson: String,
    val createdAtEpochMs: Long,
    val attempts: Int = 0,
    val lastError: String? = null,
)
