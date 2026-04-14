package com.mediakasir.apotekpos.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object OfflineSyncScheduler {
    const val UNIQUE_SYNC_WORK = "apoapps_pending_transaction_sync"
    private const val PERIODIC_SYNC_WORK = "apoapps_periodic_sync"
    private const val PERIODIC_STOCK_SYNC_WORK = "apoapps_periodic_stock_sync"

    fun enqueueTransactionSync(context: Context) {
        val req = OneTimeWorkRequestBuilder<TransactionSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_SYNC_WORK,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            req,
        )
    }

    /** Schedule periodic transaction sync every 15 minutes when connected. */
    fun schedulePeriodicSync(context: Context) {
        val req = PeriodicWorkRequestBuilder<TransactionSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            req,
        )
    }

    /** Schedule periodic stock sync every 5 minutes when connected. */
    fun schedulePeriodicStockSync(context: Context) {
        val req = PeriodicWorkRequestBuilder<StockSyncWorker>(5, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_STOCK_SYNC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            req,
        )
    }
}
