package com.mediakasir.apotekpos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediakasir.apotekpos.data.model.LicenseInfo
import com.mediakasir.apotekpos.data.model.UserInfo
import com.mediakasir.apotekpos.data.network.ApiService
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

    fun activateLicense(token: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Simpan token ke session
                session.saveToken(token)
                // Buat LicenseInfo minimal agar navigasi ke Login berjalan
                val licenseInfo = LicenseInfo(
                    branchId     = "",
                    branchName   = "",
                    pharmacyName = "MediKasir Apotek"
                )
                session.saveLicense(licenseInfo)
                _license.value = licenseInfo
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Gagal menyimpan token"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = api.login(LoginRequest(email, password))
                if (response.success) {
                    val d = response.data
                    val userInfo = UserInfo(
                        userId    = d.user.id.toString(),
                        name      = d.user.name,
                        email     = d.user.email,
                        role      = d.user.role,
                        branchId  = d.branch.id.toString(),
                        branchName = d.branch.name,
                        partnerName = d.partner.name,
                        token     = d.token
                    )
                    val licenseInfo = LicenseInfo(
                        branchId      = d.branch.id.toString(),
                        branchName    = d.branch.name,
                        pharmacyName  = d.partner.name,
                        address       = d.branch.address,
                        phone         = d.branch.phone,
                        status        = d.license.status,
                        expiredAt     = d.license.expiredAt,
                        daysRemaining = d.license.daysRemaining,
                        isTrial       = d.license.isTrial
                    )
                    session.saveSession(userInfo, d.token)
                    session.saveLicense(licenseInfo)
                    _user.value    = userInfo
                    _license.value = licenseInfo
                    onSuccess()
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Login gagal"
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
