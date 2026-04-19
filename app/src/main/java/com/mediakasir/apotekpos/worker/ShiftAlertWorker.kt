package com.mediakasir.apotekpos.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mediakasir.apotekpos.data.local.LocalShiftDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.Instant

@HiltWorker
class ShiftAlertWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val localShiftDao: LocalShiftDao,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val shift = localShiftDao.getAnyActiveShift() ?: run {
            clearAlertState()
            return Result.success()
        }

        val elapsedHours = runCatching {
            Duration.between(Instant.parse(shift.startedAt), Instant.now()).toMinutes() / 60.0
        }.getOrDefault(0.0)

        val level = when {
            elapsedHours >= 12.0 -> 2
            elapsedHours >= 8.0 -> 1
            else -> 0
        }

        if (level == 0) {
            clearAlertState()
            return Result.success()
        }

        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastShiftId = prefs.getString(KEY_SHIFT_ID, null)
        val lastLevel = prefs.getInt(KEY_LEVEL, 0)
        if (lastShiftId == shift.id && lastLevel >= level) {
            return Result.success()
        }

        createChannel()
        val (title, text, notifId) = if (level == 2) {
            Triple(
                "Batas waktu shift berakhir",
                "Shift melebihi 12 jam. Tutup shift sekarang untuk lanjut transaksi.",
                HARD_NOTIFICATION_ID,
            )
        } else {
            Triple(
                "Pengingat Tutup Shift",
                "Shift Anda sudah berjalan 8 jam. Jangan lupa tutup shift saat pergantian petugas.",
                SOFT_NOTIFICATION_ID,
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(if (level == 2) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notifId, notification)
        prefs.edit()
            .putString(KEY_SHIFT_ID, shift.id)
            .putInt(KEY_LEVEL, level)
            .apply()
        return Result.success()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Shift Alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Pengingat durasi shift kasir"
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun clearAlertState() {
        applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SHIFT_ID)
            .remove(KEY_LEVEL)
            .apply()
    }

    companion object {
        private const val CHANNEL_ID = "shift_alerts"
        private const val PREFS = "shift_alert_worker"
        private const val KEY_SHIFT_ID = "last_shift_id"
        private const val KEY_LEVEL = "last_level"
        private const val SOFT_NOTIFICATION_ID = 8101
        private const val HARD_NOTIFICATION_ID = 8102
    }
}
