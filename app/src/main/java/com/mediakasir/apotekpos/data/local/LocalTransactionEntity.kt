package com.mediakasir.apotekpos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline-first local transaction record.
 * Every checkout saves here FIRST, then syncs to server in background.
 */
@Entity(tableName = "local_transactions")
data class LocalTransactionEntity(
    @PrimaryKey val id: String,
    val branchId: String,
    val cashierId: String,
    val cashierName: String,
    val branchName: String,
    val shiftId: String? = null,
    val customerId: Int? = null,
    val customerName: String = "Umum",
    val prescriptionId: Int? = null,
    val discountId: Int? = null,
    val discountLabel: String? = null,
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double,
    val paymentMethod: String,
    val paymentStatus: String = "paid",
    val notes: String? = null,
    val completedAt: String,
    /** pending | syncing | synced | failed */
    val syncStatus: String = "pending",
    val serverTransactionId: Int? = null,
    val invoiceNumber: String? = null,
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "local_transaction_items")
data class LocalTransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val productId: Int,
    val productName: String,
    val batchId: Int? = null,
    val quantity: Double,
    val unitPrice: Double,
    val discount: Double = 0.0,
    val subtotal: Double,
    val unit: String = "pcs",
    val isRacikan: Boolean = false,
)

@Entity(tableName = "local_payments")
data class LocalPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val method: String,
    val amount: Double,
    val reference: String? = null,
)
