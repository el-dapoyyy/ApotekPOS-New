package com.mediakasir.apotekpos.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.mediakasir.apotekpos.data.local.LocalTransactionDao
import com.mediakasir.apotekpos.data.local.PendingSyncDao
import com.mediakasir.apotekpos.data.model.PosPaymentLineDto
import com.mediakasir.apotekpos.data.model.PosTransactionLineDto
import com.mediakasir.apotekpos.data.model.PosTransactionSyncDto
import com.mediakasir.apotekpos.data.model.TransactionSyncRequest
import com.mediakasir.apotekpos.data.network.ApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TYPE_TRANSACTION = "TRANSACTION_SYNC"

@HiltWorker
class TransactionSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ApiService,
    private val pendingSyncDao: PendingSyncDao,
    private val localTransactionDao: LocalTransactionDao,
    private val gson: Gson,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        var hadFailure = false

        // ── 1. Sync from new local_transactions table (offline-first) ──
        hadFailure = syncLocalTransactions() || hadFailure

        // ── 2. Legacy: drain old pending_sync table ──
        hadFailure = syncLegacyPending() || hadFailure

        return if (hadFailure) Result.retry() else Result.success()
    }

    private suspend fun syncLocalTransactions(): Boolean {
        val pending = localTransactionDao.getPendingTransactions()
        if (pending.isEmpty()) return false
        var hadFailure = false
        for (tx in pending) {
            try {
                localTransactionDao.updateSyncStatus(tx.id, "syncing")
                val items = localTransactionDao.getItemsForTransaction(tx.id)
                val payments = localTransactionDao.getPaymentsForTransaction(tx.id)

                val body = TransactionSyncRequest(
                    transactions = listOf(
                        PosTransactionSyncDto(
                            localTransactionId = tx.id,
                            customerId = tx.customerId,
                            prescriptionId = tx.prescriptionId,
                            discountId = tx.discountId,
                            subtotal = tx.subtotal,
                            discountAmount = tx.discountAmount,
                            taxAmount = tx.taxAmount,
                            tuslaAmount = 0.0,
                            embalseAmount = 0.0,
                            grandTotal = tx.grandTotal,
                            paymentMethod = tx.paymentMethod,
                            paymentStatus = tx.paymentStatus,
                            notes = tx.notes,
                            completedAt = tx.completedAt,
                            items = items.map {
                                PosTransactionLineDto(
                                    productId = it.productId,
                                    batchId = it.batchId,
                                    quantity = it.quantity,
                                    unitPrice = it.unitPrice,
                                    discount = it.discount,
                                    subtotal = it.subtotal,
                                    isRacikan = it.isRacikan,
                                )
                            },
                            payments = payments.map {
                                PosPaymentLineDto(
                                    method = it.method,
                                    amount = it.amount,
                                    reference = it.reference,
                                )
                            },
                        ),
                    ),
                )

                val sync = api.syncTransactions(body)
                val first = sync.data?.results?.firstOrNull()
                if (sync.success == true && first?.success == true && first.serverTransactionId != null) {
                    localTransactionDao.markSynced(tx.id, first.serverTransactionId, first.invoiceNumber.orEmpty())
                } else {
                    localTransactionDao.markSyncFailed(tx.id, first?.message ?: sync.message ?: "Sinkron gagal")
                    hadFailure = true
                }
            } catch (e: Exception) {
                localTransactionDao.markSyncFailed(tx.id, e.message ?: e.javaClass.simpleName)
                hadFailure = true
            }
        }
        return hadFailure
    }

    /** Drain legacy pending_sync table for backwards compat. */
    private suspend fun syncLegacyPending(): Boolean {
        val pending = pendingSyncDao.getAllPending().filter { it.type == TYPE_TRANSACTION }
        if (pending.isEmpty()) return false
        var hadFailure = false
        for (row in pending) {
            try {
                val body = gson.fromJson(row.payloadJson, TransactionSyncRequest::class.java)
                val sync = api.syncTransactions(body)
                val first = sync.data?.results?.firstOrNull()
                val ok = sync.success == true && first?.success == true
                if (ok) {
                    pendingSyncDao.deleteById(row.id)
                } else {
                    pendingSyncDao.bumpAttempt(row.id, first?.message ?: sync.message ?: "Sinkron gagal")
                    hadFailure = true
                }
            } catch (e: Exception) {
                pendingSyncDao.bumpAttempt(row.id, e.message ?: e.javaClass.simpleName)
                hadFailure = true
            }
        }
        return hadFailure
    }
}
