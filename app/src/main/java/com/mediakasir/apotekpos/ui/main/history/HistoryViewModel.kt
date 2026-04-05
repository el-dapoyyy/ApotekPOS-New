package com.mediakasir.apotekpos.ui.main.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediakasir.apotekpos.data.model.PosReturnCreateRequest
import com.mediakasir.apotekpos.data.model.PosReturnItemRequest
import com.mediakasir.apotekpos.data.model.Transaction
import com.mediakasir.apotekpos.data.model.TransactionItem
import com.mediakasir.apotekpos.data.model.toDetailTransaction
import com.mediakasir.apotekpos.data.model.toHistoryRowTransaction
import com.mediakasir.apotekpos.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total.asStateFlow()

    private var nextPage = 1
    private var hasMore = true

    fun load(branchId: String, refresh: Boolean = false) {
        viewModelScope.launch {
            if (_isLoading.value && !refresh) return@launch
            if (refresh) {
                nextPage = 1
                hasMore = true
                _transactions.value = emptyList()
            }
            if (!hasMore && !refresh) return@launch
            _isLoading.value = true
            try {
                val env = api.getTransactionHistory(page = nextPage, perPage = 20)
                if (env.success != true) {
                    throw IllegalStateException(env.message ?: "Gagal memuat riwayat")
                }
                val payload = env.data
                val rows = payload?.transactions.orEmpty().map { it.toHistoryRowTransaction() }
                _transactions.value = if (refresh || nextPage == 1) {
                    rows
                } else {
                    _transactions.value + rows
                }
                val p = payload?.pagination
                _total.value = p?.totalItems ?: _transactions.value.size
                val cur = p?.currentPage ?: nextPage
                val totalPages = p?.totalPages
                hasMore = when {
                    rows.isEmpty() -> false
                    p?.hasMore != null -> p.hasMore == true
                    totalPages != null -> cur < totalPages
                    else -> rows.size >= 20
                }
                if (rows.isNotEmpty()) nextPage = cur + 1
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchTransactionDetail(id: String, onResult: (Transaction?) -> Unit) {
        viewModelScope.launch {
            try {
                val tid = id.toIntOrNull() ?: run {
                    onResult(null)
                    return@launch
                }
                val env = api.getTransactionDetail(tid)
                if (env.success != true || env.data == null) {
                    onResult(null)
                    return@launch
                }
                onResult(env.data.toDetailTransaction())
            } catch (_: Exception) {
                onResult(null)
            }
        }
    }

    fun submitReturn(
        transactionId: Int,
        reason: String,
        lines: List<Pair<TransactionItem, Int>>,
        refundMethod: String = "cash",
        notes: String? = null,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val items = lines.map { (item, qty) ->
                    val pid = item.productId.toIntOrNull()
                        ?: throw IllegalStateException("ID produk tidak valid: ${item.productId}")
                    PosReturnItemRequest(
                        productId = pid,
                        batchId = null,
                        quantity = qty,
                        subtotal = item.sellPrice * qty,
                        condition = "good",
                        restock = true,
                    )
                }
                val body = PosReturnCreateRequest(
                    transactionId = transactionId,
                    reason = reason.trim(),
                    refundMethod = refundMethod,
                    notes = notes?.takeIf { it.isNotBlank() },
                    items = items,
                )
                val env = api.createReturn(body)
                if (env.success == true) {
                    onResult(null)
                } else {
                    onResult(env.message ?: "Retur gagal")
                }
            } catch (e: Exception) {
                onResult(e.message ?: "Retur gagal")
            }
        }
    }
}
