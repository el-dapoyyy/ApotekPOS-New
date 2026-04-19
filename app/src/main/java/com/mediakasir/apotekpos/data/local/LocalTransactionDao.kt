package com.mediakasir.apotekpos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalTransactionDao {

    // ── Insert ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: LocalTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<LocalTransactionItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<LocalPaymentEntity>)

    /** Atomic insert: transaction + items + payments in one Room @Transaction. */
    @Transaction
    suspend fun insertFull(
        tx: LocalTransactionEntity,
        items: List<LocalTransactionItemEntity>,
        payments: List<LocalPaymentEntity>,
    ) {
        insertTransaction(tx)
        insertItems(items)
        insertPayments(payments)
    }

    // ── Query pending for sync ──

    @Query("SELECT * FROM local_transactions WHERE (syncStatus = 'pending' OR syncStatus = 'failed') ORDER BY createdAtEpochMs ASC")
    suspend fun getPendingTransactions(): List<LocalTransactionEntity>

    @Query("SELECT * FROM local_transaction_items WHERE transactionId = :txId")
    suspend fun getItemsForTransaction(txId: String): List<LocalTransactionItemEntity>

    @Query("SELECT * FROM local_payments WHERE transactionId = :txId")
    suspend fun getPaymentsForTransaction(txId: String): List<LocalPaymentEntity>

    // ── Sync status updates ──

    @Query("UPDATE local_transactions SET syncStatus = :status WHERE id = :txId")
    suspend fun updateSyncStatus(txId: String, status: String)

    @Query("""
        UPDATE local_transactions 
        SET syncStatus = 'synced', serverTransactionId = :serverId, invoiceNumber = :invoiceNum 
        WHERE id = :txId
    """)
    suspend fun markSynced(txId: String, serverId: Int, invoiceNum: String)

    @Query("""
        UPDATE local_transactions 
        SET syncStatus = 'failed', syncAttempts = syncAttempts + 1, lastSyncError = :error 
        WHERE id = :txId
    """)
    suspend fun markSyncFailed(txId: String, error: String)

    // ── Counts ──

    @Query("SELECT COUNT(*) FROM local_transactions WHERE syncStatus = 'pending' OR syncStatus = 'failed'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM local_transactions WHERE syncStatus = 'pending' OR syncStatus = 'failed'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM local_transactions WHERE syncStatus = 'failed'")
    suspend fun getFailedCount(): Int

    /**
     * On app startup, any transaction left at "syncing" (app was killed mid-sync) must be
     * reset to "pending" so the background worker can retry it.
     */
    @Query("UPDATE local_transactions SET syncStatus = 'pending' WHERE syncStatus = 'syncing'")
    suspend fun resetStuckSyncing()

    /** Returns failed transactions with their error messages for diagnostics. */
    @Query("""
        SELECT id, grandTotal, completedAt, syncAttempts, lastSyncError
        FROM local_transactions
        WHERE (syncStatus = 'failed' OR (syncStatus = 'pending' AND syncAttempts > 0))
        ORDER BY createdAtEpochMs DESC
        LIMIT 20
    """)
    suspend fun getFailedSyncDetails(): List<FailedSyncInfo>

    /** Force-reset all failed transactions back to pending — keep error history so user can still see it. */
    @Query("UPDATE local_transactions SET syncStatus = 'pending' WHERE syncStatus = 'failed'")
    suspend fun resetFailedToPending()

    // ── History / shift queries ──

    @Query("SELECT * FROM local_transactions WHERE branchId = :branchId ORDER BY createdAtEpochMs DESC LIMIT :limit")
    suspend fun getRecentTransactions(branchId: String, limit: Int = 50): List<LocalTransactionEntity>

    @Query("SELECT * FROM local_transactions WHERE shiftId = :shiftId ORDER BY createdAtEpochMs ASC")
    suspend fun getTransactionsForShift(shiftId: String): List<LocalTransactionEntity>

    @Query("""
        SELECT i.productName AS name, SUM(i.quantity) AS qty
        FROM local_transaction_items i
        INNER JOIN local_transactions t ON t.id = i.transactionId
        WHERE t.shiftId = :shiftId AND i.isRacikan = 0
        GROUP BY i.productId, i.productName
        ORDER BY qty DESC
        LIMIT :limit
    """)
    suspend fun getTopProductsForShift(shiftId: String, limit: Int = 5): List<TopProductStat>

    @Query("""
        SELECT COALESCE(SUM(grandTotal), 0.0) 
        FROM local_transactions 
        WHERE shiftId = :shiftId
    """)
    suspend fun getTotalSalesForShift(shiftId: String): Double

    @Query("""
        SELECT COUNT(*) 
        FROM local_transactions 
        WHERE shiftId = :shiftId
    """)
    suspend fun getTransactionCountForShift(shiftId: String): Int

    @Query("SELECT * FROM local_transactions WHERE id = :txId")
    suspend fun getTransaction(txId: String): LocalTransactionEntity?

    /** Returns all transactions not yet confirmed by server (pending/syncing/failed). */
    @Query("SELECT * FROM local_transactions WHERE syncStatus != 'synced' AND syncStatus != 'voided' ORDER BY createdAtEpochMs DESC LIMIT 100")
    suspend fun getUnsyncedTransactions(): List<LocalTransactionEntity>

    /** Mark a local (not-yet-synced) transaction as voided so it is excluded from sync. */
    @Query("UPDATE local_transactions SET syncStatus = 'voided' WHERE id = :txId AND syncStatus != 'synced'")
    suspend fun voidTransaction(txId: String)

    @Query("SELECT COUNT(*) FROM local_transaction_items WHERE transactionId = :txId")
    suspend fun getItemCount(txId: String): Int

    /**
     * Returns the total quantity of each product that is in unsynced (pending/syncing/failed)
     * local transactions. Used to correct server stock values during sync so locally-sold
     * items are not shown as available before the transaction reaches the server.
     */
    @Query("""
        SELECT i.productId, CAST(SUM(i.quantity) AS INTEGER) as totalQty
        FROM local_transaction_items i
        INNER JOIN local_transactions t ON t.id = i.transactionId
        WHERE t.syncStatus != 'synced'
        GROUP BY i.productId
    """)
    suspend fun getPendingDeductionsPerProduct(): List<PendingItemDeduction>
}

/** Projected result for pending stock deduction calculation. */
data class PendingItemDeduction(
    val productId: Int,
    val totalQty: Int,
)

/** Projected result for displaying failed sync diagnostics to users. */
data class FailedSyncInfo(
    val id: String,
    val grandTotal: Double,
    val completedAt: String,
    val syncAttempts: Int,
    val lastSyncError: String?,
)

data class TopProductStat(
    val name: String,
    val qty: Double,
)
