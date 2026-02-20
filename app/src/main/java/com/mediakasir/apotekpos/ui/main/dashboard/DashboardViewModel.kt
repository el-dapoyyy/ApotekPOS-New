package com.mediakasir.apotekpos.ui.main.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediakasir.apotekpos.data.model.AlertData
import com.mediakasir.apotekpos.data.model.DashboardData
import com.mediakasir.apotekpos.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _dashboard = MutableStateFlow<DashboardData?>(null)
    val dashboard: StateFlow<DashboardData?> = _dashboard.asStateFlow()

    private val _alerts = MutableStateFlow<AlertData?>(null)
    val alerts: StateFlow<AlertData?> = _alerts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load(branchId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val dash = api.getDashboard(branchId)
                val alertData = api.getAlerts(branchId)
                _dashboard.value = dash
                _alerts.value = alertData
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun seedData(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                api.seed()
                onDone()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
