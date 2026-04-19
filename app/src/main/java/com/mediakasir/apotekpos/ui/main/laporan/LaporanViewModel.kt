package com.mediakasir.apotekpos.ui.main.laporan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediakasir.apotekpos.data.model.TransactionHistoryRowDto
import com.mediakasir.apotekpos.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class LaporanSummary(
    val totalTransactions: Int,
    val totalRevenue: Double,
    val avgTransaction: Double,
)

@HiltViewModel
class LaporanViewModel @Inject constructor(
    private val api: ApiService,
) : ViewModel() {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _transactions = MutableStateFlow<List<TransactionHistoryRowDto>>(emptyList())
    val transactions: StateFlow<List<TransactionHistoryRowDto>> = _transactions.asStateFlow()

    private val _summary = MutableStateFlow<LaporanSummary?>(null)
    val summary: StateFlow<LaporanSummary?> = _summary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Default: first → last day of current month */
    val defaultStart: LocalDate get() = LocalDate.now().withDayOfMonth(1)
    val defaultEnd: LocalDate get() = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())

    fun loadReport(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val allRows = mutableListOf<TransactionHistoryRowDto>()
                var page = 1
                val perPage = 100
                while (true) {
                    val env = api.getTransactionHistory(
                        page = page,
                        perPage = perPage,
                        startDate = startDate.format(fmt),
                        endDate = endDate.format(fmt),
                    )
                    val rows = env.data?.transactions.orEmpty()
                    allRows.addAll(rows)
                    val p = env.data?.pagination
                    val hasMore = p?.hasMore == true ||
                        ((p?.currentPage ?: page) < (p?.totalPages ?: 0))
                    if (!hasMore || rows.isEmpty()) break
                    page++
                    if (page > 10) break // safety cap: max 1000 rows
                }
                _transactions.value = allRows
                val total = allRows.sumOf { it.grandTotal ?: 0.0 }
                _summary.value = LaporanSummary(
                    totalTransactions = allRows.size,
                    totalRevenue = total,
                    avgTransaction = if (allRows.isEmpty()) 0.0 else total / allRows.size,
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Gagal memuat laporan"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
}
