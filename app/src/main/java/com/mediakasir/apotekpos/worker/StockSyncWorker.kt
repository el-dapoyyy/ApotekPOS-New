package com.mediakasir.apotekpos.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mediakasir.apotekpos.data.repository.SessionRepository
import com.mediakasir.apotekpos.data.sync.DataPreloader
import com.mediakasir.apotekpos.ui.effectiveBranchId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic background worker that refreshes product stock from server.
 * Uses incremental sync (updated_since) via DataPreloader to only fetch changed products.
 * Runs every 5 minutes while network is connected.
 */
@HiltWorker
class StockSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dataPreloader: DataPreloader,
    private val session: SessionRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val license = session.getLicense()
        val user = session.getUser()
        val branchId = effectiveBranchId(license, user)
        if (branchId.isBlank()) return Result.success() // Not logged in yet
        dataPreloader.syncProducts(branchId)
        return Result.success()
    }
}
