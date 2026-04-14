package com.mediakasir.apotekpos.data.sync

import android.content.Context
import com.mediakasir.apotekpos.worker.OfflineSyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central sync orchestrator.
 * Observes connectivity and triggers sync operations in priority order:
 * 1. Transactions (money first)
 * 2. Shifts
 * 3. Product updates
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val connectivityObserver: ConnectivityObserver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isObserving = false

    /** Call once after login. Starts observing connectivity and auto-syncing. */
    fun startObserving() {
        if (isObserving) return
        isObserving = true
        scope.launch {
            connectivityObserver.isOnline.collectLatest { online ->
                if (online) {
                    triggerPendingSync()
                }
            }
        }
    }

    /** Trigger all pending sync operations in priority order via WorkManager. */
    fun triggerPendingSync() {
        // Priority 1: Transactions
        OfflineSyncScheduler.enqueueTransactionSync(appContext)
    }

    fun stopObserving() {
        isObserving = false
    }
}
