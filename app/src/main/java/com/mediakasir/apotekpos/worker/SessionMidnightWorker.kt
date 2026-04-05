package com.mediakasir.apotekpos.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mediakasir.apotekpos.data.repository.SessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic check: if calendar day changed vs last stored business day, clear session (kasir harian).
 */
@HiltWorker
class SessionMidnightWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sessionRepository: SessionRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        sessionRepository.applyCalendarDayRolloverIfNeeded()
        return Result.success()
    }
}
