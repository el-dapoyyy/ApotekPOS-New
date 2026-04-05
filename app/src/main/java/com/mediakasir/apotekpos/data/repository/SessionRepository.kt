package com.mediakasir.apotekpos.data.repository

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.UserInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("apotek_session")

@Singleton
class SessionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) {
    private val TOKEN_KEY = stringPreferencesKey("token")
    private val USER_KEY = stringPreferencesKey("user")
    private val LICENSE_KEY = stringPreferencesKey("license")
    /** Fingerprint perangkat untuk body login (`device_id`) — biasanya ANDROID_ID penuh. */
    private val LOGIN_DEVICE_FINGERPRINT_KEY = stringPreferencesKey("login_device_fingerprint")
    /**
     * PK baris `user_devices.id` untuk header `X-Device-ID`.
     * Wajib selaras dengan FK `sync_logs.device_id` di backend.
     */
    private val SERVER_USER_DEVICE_ROW_ID_KEY = stringPreferencesKey("server_user_device_row_id")
    /** Legacy: dulu dipakai untuk CRC/hex palsu — dibuang saat migrasi. */
    private val DEVICE_ID_LEGACY_KEY = stringPreferencesKey("device_id")

    /** ISO local date (yyyy-MM-dd) untuk rollover sesi kasir harian */
    private val BUSINESS_DAY_KEY = stringPreferencesKey("business_calendar_day")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val userFlow: Flow<UserInfo?> = context.dataStore.data.map { prefs ->
        prefs[USER_KEY]?.let { gson.fromJson(it, UserInfo::class.java) }
    }
    val licenseFlow: Flow<LicenseInfo?> = context.dataStore.data.map { prefs ->
        prefs[LICENSE_KEY]?.let { gson.fromJson(it, LicenseInfo::class.java) }
    }

    suspend fun getToken(): String? = tokenFlow.first()
    suspend fun getUser(): UserInfo? = userFlow.first()
    suspend fun getLicense(): LicenseInfo? = licenseFlow.first()

    /**
     * Untuk JSON login: string stabil per perangkat (ANDROID_ID), agar backend cocokkan ke `user_devices`.
     */
    suspend fun getLoginDeviceFingerprint(): String {
        val prefs = context.dataStore.data.first()
        prefs[LOGIN_DEVICE_FINGERPRINT_KEY]?.takeIf { it.isNotBlank() }?.let { return it }
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ).orEmpty()
        context.dataStore.edit { e ->
            e.remove(DEVICE_ID_LEGACY_KEY)
            e[LOGIN_DEVICE_FINGERPRINT_KEY] = androidId
        }
        return androidId
    }

    /** Nilai header `X-Device-ID` = `user_devices.id` (integer sebagai string). */
    suspend fun getServerUserDeviceRowIdForHeader(): String? =
        context.dataStore.data.first()[SERVER_USER_DEVICE_ROW_ID_KEY]?.takeIf { it.isNotBlank() }

    suspend fun saveServerUserDeviceRowId(id: Int) {
        context.dataStore.edit { it[SERVER_USER_DEVICE_ROW_ID_KEY] = id.toString() }
    }

    suspend fun saveSession(user: UserInfo, token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_KEY] = gson.toJson(user)
            prefs[BUSINESS_DAY_KEY] = LocalDate.now().toString()
        }
    }

    /**
     * Jika tanggal kalender berbeda dari hari terakhir login aktif, sesi dihapus (kasir login ulang).
     */
    suspend fun applyCalendarDayRolloverIfNeeded() {
        val today = LocalDate.now().toString()
        val prefs = context.dataStore.data.first()
        val stored = prefs[BUSINESS_DAY_KEY]
        val token = prefs[TOKEN_KEY]
        if (stored == null) {
            if (!token.isNullOrEmpty()) {
                context.dataStore.edit { it[BUSINESS_DAY_KEY] = today }
            }
            return
        }
        if (stored != today) {
            clearSession()
            context.dataStore.edit { it[BUSINESS_DAY_KEY] = today }
        }
    }

    suspend fun saveLicense(license: LicenseInfo) {
        context.dataStore.edit { prefs ->
            prefs[LICENSE_KEY] = gson.toJson(license)
        }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_KEY)
            prefs.remove(LICENSE_KEY)
            prefs.remove(SERVER_USER_DEVICE_ROW_ID_KEY)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
