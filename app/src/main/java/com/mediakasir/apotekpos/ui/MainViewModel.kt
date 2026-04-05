package com.mediakasir.apotekpos.ui

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediakasir.apotekpos.BuildConfig
import com.mediakasir.apotekpos.R
import com.mediakasir.apotekpos.data.model.GoogleLoginRequest
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.LoginRequest
import com.mediakasir.apotekpos.data.model.LoginResponse
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.data.model.blocksAppUse
import com.mediakasir.apotekpos.data.model.blocksLogin
import com.mediakasir.apotekpos.data.network.ApiService
import com.mediakasir.apotekpos.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val api: ApiService,
    private val session: SessionRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _license = MutableStateFlow<LicenseInfo?>(null)
    val license: StateFlow<LicenseInfo?> = _license.asStateFlow()

    private val _user = MutableStateFlow<UserInfo?>(null)
    val user: StateFlow<UserInfo?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            _license.value = session.getLicense()
            _user.value = session.getUser()
        }
    }

    /**
     * Minimum splash duration + read session; returns [Screen] route for navigation.
     */
    suspend fun resolveRouteAfterSplash(): String {
        val minMs = 850L
        val start = System.currentTimeMillis()
        session.applyCalendarDayRolloverIfNeeded()
        val token = session.getToken()
        val userJson = session.getUser()
        val storedLicense = session.getLicense()
        _license.value = storedLicense
        _user.value = userJson
        val elapsed = System.currentTimeMillis() - start
        if (elapsed < minMs) delay(minMs - elapsed)
        if (!token.isNullOrEmpty() && userJson != null) {
            if (storedLicense?.blocksAppUse() == true) {
                session.clearSession()
                _license.value = null
                _user.value = null
                return Screen.Login.route
            }
            return Screen.POS.route
        }
        return Screen.Login.route
    }

    fun refreshSessionAfterDayCheck() {
        viewModelScope.launch {
            session.applyCalendarDayRolloverIfNeeded()
            val lic = session.getLicense()
            val u = session.getUser()
            if (lic?.blocksAppUse() == true) {
                session.clearSession()
                _license.value = null
                _user.value = null
            } else {
                _license.value = lic
                _user.value = u
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = api.login(buildPasswordLoginRequest(email.trim(), password))
                applyLoginResponse(response, onSuccess)
            } catch (e: HttpException) {
                _error.value = e.response()?.errorBody()?.string()?.trim()?.takeIf { it.length in 1..280 }
                    ?: appContext.getString(R.string.login_error_generic)
            } catch (e: Exception) {
                _error.value = mapNetworkError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loginWithGoogleIdToken(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = api.loginWithGoogle(buildGoogleLoginRequest(idToken))
                applyLoginResponse(response, onSuccess)
            } catch (e: HttpException) {
                _error.value = appContext.getString(R.string.error_login_google_http, e.code())
            } catch (e: Exception) {
                _error.value = mapNetworkError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun applyLoginResponse(response: LoginResponse, onSuccess: () -> Unit) {
        if (response.success) {
            val d = response.data
            if (d.license.blocksLogin()) {
                _error.value = appContext.getString(R.string.login_license_blocked)
                return
            }
            val b = d.branch
            val userInfo = UserInfo(
                userId = d.user.id.toString(),
                name = d.user.name,
                email = d.user.email,
                role = d.user.role,
                branchId = b?.id?.toString() ?: "",
                branchName = b?.name ?: "",
                partnerName = d.partner.name,
                token = d.token
            )
            val licenseInfo = LicenseInfo(
                branchId = b?.id?.toString() ?: "",
                branchName = b?.name ?: "",
                pharmacyName = d.partner.name,
                address = b?.address ?: "",
                phone = b?.phone ?: "",
                status = d.license.status,
                expiredAt = d.license.expiredAt,
                daysRemaining = d.license.daysRemaining,
                isTrial = d.license.isTrial
            )
            session.saveSession(userInfo, d.token)
            session.saveLicense(licenseInfo)
            _user.value = userInfo
            _license.value = licenseInfo
            onSuccess()
        } else {
            _error.value = response.message
        }
    }

    private suspend fun buildPasswordLoginRequest(email: String, password: String): LoginRequest {
        val deviceId = session.getDeviceId()
        val deviceName = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
            .ifBlank { null }
        val deviceModel = Build.MODEL.takeIf { it.isNotBlank() }
        return LoginRequest(
            email = email,
            password = password,
            deviceId = deviceId,
            deviceName = deviceName,
            deviceModel = deviceModel
        )
    }

    private suspend fun buildGoogleLoginRequest(idToken: String): GoogleLoginRequest {
        val deviceId = session.getDeviceId()
        val deviceName = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
            .ifBlank { null }
        val deviceModel = Build.MODEL.takeIf { it.isNotBlank() }
        return GoogleLoginRequest(
            idToken = idToken,
            deviceId = deviceId,
            deviceName = deviceName,
            deviceModel = deviceModel
        )
    }

    private fun mapNetworkError(e: Throwable): String {
        val msg = e.message.orEmpty()
        val debugBase = if (BuildConfig.DEBUG) {
            "\n" + appContext.getString(R.string.error_network_debug_base, BuildConfig.BASE_URL)
        } else {
            ""
        }
        return when {
            e is UnknownHostException ->
                appContext.getString(R.string.error_network_unreachable) + debugBase
            msg.contains("Unable to resolve host", ignoreCase = true) ->
                appContext.getString(R.string.error_network_unreachable) + debugBase
            e is SocketTimeoutException -> appContext.getString(R.string.error_network_timeout) + debugBase
            msg.contains("timeout", ignoreCase = true) ->
                appContext.getString(R.string.error_network_timeout) + debugBase
            else -> e.message?.takeIf { it.isNotBlank() } ?: appContext.getString(R.string.login_error_generic)
        }
    }

    fun logout() {
        viewModelScope.launch {
            session.clearSession()
            _user.value = null
            _license.value = null
        }
    }

    fun resetAppData() {
        viewModelScope.launch {
            session.clearAll()
            _license.value = null
            _user.value = null
        }
    }

    fun clearError() { _error.value = null }

    /** Credential Manager / UI-level errors (e.g. Google sign-in flow). */
    fun reportAuthError(message: String) {
        _error.value = message
    }
}
