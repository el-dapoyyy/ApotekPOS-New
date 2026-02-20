package com.mediakasir.apotekpos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.data.network.ApiService
import com.mediakasir.apotekpos.data.model.LicenseValidateRequest
import com.mediakasir.apotekpos.data.model.LoginRequest
import com.mediakasir.apotekpos.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val api: ApiService,
    private val session: SessionRepository
) : ViewModel() {

    private val _license = MutableStateFlow<LicenseInfo?>(null)
    val license: StateFlow<LicenseInfo?> = _license.asStateFlow()

    private val _user = MutableStateFlow<UserInfo?>(null)
    val user: StateFlow<UserInfo?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
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
            _isLoading.value = false
        }
    }

    fun validateLicense(key: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val info = api.validateLicense(LicenseValidateRequest(key))
                session.saveLicense(info)
                _license.value = info
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Lisensi tidak valid"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(username: String, pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userInfo = api.login(LoginRequest(username, pin))
                session.saveSession(userInfo, userInfo.token)
                _user.value = userInfo
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Username atau PIN salah"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            session.clearSession()
            _user.value = null
        }
    }

    fun clearLicense() {
        viewModelScope.launch {
            session.clearAll()
            _license.value = null
            _user.value = null
        }
    }

    fun clearError() { _error.value = null }
}
