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
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("apotek_session")

@Singleton
class SessionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val TOKEN_KEY = stringPreferencesKey("token")
    private val USER_KEY = stringPreferencesKey("user")
    private val LICENSE_KEY = stringPreferencesKey("license")
    private val DEVICE_ID_KEY = stringPreferencesKey("device_id")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val userFlow: Flow<UserInfo?> = context.dataStore.data.map { prefs ->
        prefs[USER_KEY]?.let { gson.fromJson(it, UserInfo::class.java) }
    }
    val licenseFlow: Flow<LicenseInfo?> = context.dataStore.data.map { prefs ->
        prefs[LICENSE_KEY]?.let { gson.fromJson(it, LicenseInfo::class.java) }
    }
    val deviceIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[DEVICE_ID_KEY]
    }

    suspend fun getToken(): String? = tokenFlow.first()
    suspend fun getUser(): UserInfo? = userFlow.first()
    suspend fun getLicense(): LicenseInfo? = licenseFlow.first()

    suspend fun getDeviceId(): String {
        val stored = deviceIdFlow.first()
        if (!stored.isNullOrEmpty()) return stored

        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: ""

        context.dataStore.edit { prefs ->
            prefs[DEVICE_ID_KEY] = androidId
        }

        return androidId
    }

    suspend fun saveSession(user: UserInfo, token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_KEY] = gson.toJson(user)
        }
    }

    suspend fun saveLicense(license: LicenseInfo) {
        context.dataStore.edit { prefs ->
            prefs[LICENSE_KEY] = gson.toJson(license)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_KEY)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
