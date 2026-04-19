package com.mediakasir.apotekpos.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "local_cash_expenses")
data class LocalCashExpenseEntity(
    @PrimaryKey val id: String,
    val shiftId: String,
    val branchId: String,
    val cashierId: String,
    val amount: Double,
    val category: String,
    val note: String? = null,
    val receiptPhotoPath: String? = null,
    val createdAt: String,
    /** pending | syncing | synced | failed */
    val syncStatus: String = "pending",
    val serverCashExpenseId: Int? = null,
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

@Dao
interface LocalCashExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: LocalCashExpenseEntity)

    @Query("SELECT * FROM local_cash_expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LocalCashExpenseEntity?

    @Query("SELECT * FROM local_cash_expenses WHERE (syncStatus = 'pending' OR syncStatus = 'failed') ORDER BY createdAtEpochMs ASC")
    suspend fun getPendingCashExpenses(): List<LocalCashExpenseEntity>

    @Query("UPDATE local_cash_expenses SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("""
        UPDATE local_cash_expenses
        SET syncStatus = 'synced',
            serverCashExpenseId = :serverId,
            lastSyncError = NULL
        WHERE id = :id
    """)
    suspend fun markSynced(id: String, serverId: Int?)

    @Query("""
        UPDATE local_cash_expenses
        SET syncStatus = 'failed',
            syncAttempts = syncAttempts + 1,
            lastSyncError = :error
        WHERE id = :id
    """)
    suspend fun markSyncFailed(id: String, error: String)

    @Query("UPDATE local_cash_expenses SET syncStatus = 'pending' WHERE syncStatus = 'syncing'")
    suspend fun resetStuckSyncing()

    @Query("""
        UPDATE local_cash_expenses
        SET amount = :amount,
            category = :category,
            note = :note,
            receiptPhotoPath = :receiptPhotoPath
        WHERE id = :id
    """)
    suspend fun updateCashOut(
        id: String,
        amount: Double,
        category: String,
        note: String?,
        receiptPhotoPath: String?,
    )

    @Query("DELETE FROM local_cash_expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM local_cash_expenses WHERE shiftId = :shiftId")
    suspend fun getTotalCashOutForShift(shiftId: String): Double

    @Query("SELECT * FROM local_cash_expenses WHERE shiftId = :shiftId ORDER BY createdAtEpochMs DESC")
    suspend fun getCashOutForShift(shiftId: String): List<LocalCashExpenseEntity>

    @Query("""
        SELECT category, COALESCE(SUM(amount), 0.0) AS total
        FROM local_cash_expenses
        WHERE shiftId = :shiftId
        GROUP BY category
        ORDER BY total DESC
    """)
    suspend fun getCashOutByCategory(shiftId: String): List<CashOutCategoryTotal>
}

@Entity(tableName = "local_cash_expense_audits")
data class LocalCashExpenseAuditEntity(
    @PrimaryKey val id: String,
    val cashExpenseId: String,
    val shiftId: String,
    /** update | delete */
    val action: String,
    val actorCashierId: String,
    val oldAmount: Double,
    val newAmount: Double? = null,
    val oldCategory: String,
    val newCategory: String? = null,
    val oldNote: String? = null,
    val newNote: String? = null,
    val oldReceiptPhotoPath: String? = null,
    val newReceiptPhotoPath: String? = null,
    val createdAt: String,
    /** pending | syncing | synced | failed */
    val syncStatus: String = "pending",
    val serverAuditId: Int? = null,
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

@Dao
interface LocalCashExpenseAuditDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audit: LocalCashExpenseAuditEntity)

    @Query("SELECT * FROM local_cash_expense_audits WHERE (syncStatus = 'pending' OR syncStatus = 'failed') ORDER BY createdAtEpochMs ASC")
    suspend fun getPendingAudits(): List<LocalCashExpenseAuditEntity>

    @Query("UPDATE local_cash_expense_audits SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("""
        UPDATE local_cash_expense_audits
        SET syncStatus = 'synced',
            serverAuditId = :serverId,
            lastSyncError = NULL
        WHERE id = :id
    """)
    suspend fun markSynced(id: String, serverId: Int?)

    @Query("""
        UPDATE local_cash_expense_audits
        SET syncStatus = 'failed',
            syncAttempts = syncAttempts + 1,
            lastSyncError = :error
        WHERE id = :id
    """)
    suspend fun markSyncFailed(id: String, error: String)

    @Query("UPDATE local_cash_expense_audits SET syncStatus = 'pending' WHERE syncStatus = 'syncing'")
    suspend fun resetStuckSyncing()
}

data class CashOutCategoryTotal(
    val category: String,
    val total: Double,
)
