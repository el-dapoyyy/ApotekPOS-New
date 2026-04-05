package com.mediakasir.apotekpos.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.mediakasir.apotekpos.data.local.PendingSyncDao
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
    private val gson: Gson,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = pendingSyncDao.getAllPending().filter { it.type == TYPE_TRANSACTION }
        if (pending.isEmpty()) return Result.success()
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
        return if (hadFailure) Result.retry() else Result.success()
    }
}
