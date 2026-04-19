package com.mediakasir.apotekpos.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mediakasir.apotekpos.data.local.LocalCashExpenseAuditDao
import com.mediakasir.apotekpos.data.local.LocalCashExpenseDao
import com.mediakasir.apotekpos.data.model.CashExpenseAuditSyncDto
import com.mediakasir.apotekpos.data.model.CashExpenseSyncDto
import com.mediakasir.apotekpos.data.model.CashExpenseSyncRequest
import com.mediakasir.apotekpos.data.network.ApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CashExpenseSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ApiService,
    private val localCashExpenseDao: LocalCashExpenseDao,
    private val localCashExpenseAuditDao: LocalCashExpenseAuditDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        localCashExpenseDao.resetStuckSyncing()
        localCashExpenseAuditDao.resetStuckSyncing()

        val pendingExpenses = localCashExpenseDao.getPendingCashExpenses()
        val pendingAudits = localCashExpenseAuditDao.getPendingAudits()
        if (pendingExpenses.isEmpty() && pendingAudits.isEmpty()) return Result.success()

        pendingExpenses.forEach { localCashExpenseDao.updateSyncStatus(it.id, "syncing") }
        pendingAudits.forEach { localCashExpenseAuditDao.updateSyncStatus(it.id, "syncing") }

        return try {
            val request = CashExpenseSyncRequest(
                expenses = pendingExpenses.map {
                    CashExpenseSyncDto(
                        localCashExpenseId = it.id,
                        serverCashExpenseId = it.serverCashExpenseId,
                        shiftId = it.shiftId,
                        branchId = it.branchId,
                        cashierId = it.cashierId,
                        amount = it.amount,
                        category = it.category,
                        note = it.note,
                        receiptPhotoPath = it.receiptPhotoPath,
                        createdAt = it.createdAt,
                    )
                },
                audits = pendingAudits.map {
                    CashExpenseAuditSyncDto(
                        localAuditId = it.id,
                        serverAuditId = it.serverAuditId,
                        cashExpenseId = it.cashExpenseId,
                        shiftId = it.shiftId,
                        action = it.action,
                        actorCashierId = it.actorCashierId,
                        oldAmount = it.oldAmount,
                        newAmount = it.newAmount,
                        oldCategory = it.oldCategory,
                        newCategory = it.newCategory,
                        oldNote = it.oldNote,
                        newNote = it.newNote,
                        oldReceiptPhotoPath = it.oldReceiptPhotoPath,
                        newReceiptPhotoPath = it.newReceiptPhotoPath,
                        createdAt = it.createdAt,
                    )
                },
            )

            val sync = api.syncCashExpenses(request)

            if (sync.success == true) {
                val expenseResults = sync.data?.expenseResults.orEmpty().associateBy { it.localCashExpenseId.orEmpty() }
                val auditResults = sync.data?.auditResults.orEmpty().associateBy { it.localAuditId.orEmpty() }

                var hadFailure = false

                pendingExpenses.forEach { row ->
                    val result = expenseResults[row.id]
                    if (result == null || result.success == true) {
                        localCashExpenseDao.markSynced(row.id, result?.serverCashExpenseId ?: row.serverCashExpenseId)
                    } else {
                        localCashExpenseDao.markSyncFailed(row.id, result.message ?: "Sinkron CP gagal")
                        hadFailure = true
                    }
                }

                pendingAudits.forEach { row ->
                    val result = auditResults[row.id]
                    if (result == null || result.success == true) {
                        localCashExpenseAuditDao.markSynced(row.id, result?.serverAuditId ?: row.serverAuditId)
                    } else {
                        localCashExpenseAuditDao.markSyncFailed(row.id, result.message ?: "Sinkron audit CP gagal")
                        hadFailure = true
                    }
                }

                if (hadFailure) Result.retry() else Result.success()
            } else {
                val message = sync.message ?: "Sinkron CP gagal"
                pendingExpenses.forEach { localCashExpenseDao.markSyncFailed(it.id, message) }
                pendingAudits.forEach { localCashExpenseAuditDao.markSyncFailed(it.id, message) }
                Result.retry()
            }
        } catch (e: Exception) {
            val message = e.message ?: e.javaClass.simpleName
            pendingExpenses.forEach { localCashExpenseDao.markSyncFailed(it.id, message) }
            pendingAudits.forEach { localCashExpenseAuditDao.markSyncFailed(it.id, message) }
            Result.retry()
        }
    }
}
