package com.mediakasir.apotekpos.ui.main.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediakasir.apotekpos.data.model.AlertData
import com.mediakasir.apotekpos.data.model.DashboardData
import com.mediakasir.apotekpos.data.model.toAlertBatch
import com.mediakasir.apotekpos.data.model.toLowStockProduct
import com.mediakasir.apotekpos.data.network.ApiService
import com.mediakasir.apotekpos.util.mapNetworkOrGenericError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val appContext: Context,
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
                val summary = api.getAlertsSummary()
                if (summary.success != true) {
                    throw IllegalStateException(summary.message ?: "Gagal memuat ringkasan alert")
                }
                val s = summary.data
                val exp = s?.expiry
                val lowStockCount = (s?.stock?.lowStock ?: 0) + (s?.stock?.outOfStock ?: 0)
                val expiringCount = (exp?.critical ?: 0) + (exp?.warning ?: 0) + (exp?.notice ?: 0)
                _dashboard.value = DashboardData(
                    todayRevenue = 0.0,
                    todayTransactions = 0,
                    totalProducts = 0,
                    lowStockCount = lowStockCount,
                    expiringCount = expiringCount,
                )

                // Cabang di-scope oleh Bearer token (Sanctum); jangan kirim branch_id ke API.
                val expiryEnv = api.getExpiryAlerts(branchId = null, perPage = 20, unacknowledged = true)
                val stockEnv = api.getStockAlerts(branchId = null, perPage = 20, unacknowledged = true)
                if (expiryEnv.success != true) {
                    throw IllegalStateException(expiryEnv.message ?: "Gagal memuat alert kadaluarsa")
                }
                if (stockEnv.success != true) {
                    throw IllegalStateException(stockEnv.message ?: "Gagal memuat alert stok")
                }
                val expiryRows = expiryEnv.data?.data.orEmpty()
                val expiredBatches = expiryRows.filter { it.isExpired == true }.map { it.toAlertBatch(branchId) }
                val expiringBatches = expiryRows.filter { it.isExpired != true }.map { it.toAlertBatch(branchId) }
                val lowStockProducts = stockEnv.data?.data.orEmpty().map { it.toLowStockProduct() }
                _alerts.value = AlertData(
                    expiredBatches = expiredBatches,
                    expiringBatches = expiringBatches,
                    lowStockProducts = lowStockProducts,
                )
            } catch (e: Exception) {
                _error.value = appContext.mapNetworkOrGenericError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
