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
    const val UNIQUE_CASH_EXPENSE_SYNC_WORK = "apoapps_pending_cash_expense_sync"
    private const val PERIODIC_SYNC_WORK = "apoapps_periodic_sync"
    private const val PERIODIC_CASH_EXPENSE_SYNC_WORK = "apoapps_periodic_cash_expense_sync"
    private const val PERIODIC_STOCK_SYNC_WORK = "apoapps_periodic_stock_sync"
    private const val PERIODIC_SHIFT_ALERT_WORK = "apoapps_periodic_shift_alert"

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

    fun enqueueCashExpenseSync(context: Context) {
        val req = OneTimeWorkRequestBuilder<CashExpenseSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_CASH_EXPENSE_SYNC_WORK,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            req,
        )
    }

    /** Schedule periodic cash expense sync every 15 minutes when connected. */
    fun schedulePeriodicCashExpenseSync(context: Context) {
        val req = PeriodicWorkRequestBuilder<CashExpenseSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_CASH_EXPENSE_SYNC_WORK,
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

    /** Schedule periodic shift-duration alerts (soft 8h, hard 12h). */
    fun schedulePeriodicShiftAlert(context: Context) {
        val req = PeriodicWorkRequestBuilder<ShiftAlertWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SHIFT_ALERT_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            req,
        )
    }
}
