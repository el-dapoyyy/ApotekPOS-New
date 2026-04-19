package com.mediakasir.apotekpos.ui.main.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediakasir.apotekpos.data.local.LocalTransactionDao
import com.mediakasir.apotekpos.data.local.LocalTransactionEntity
import com.mediakasir.apotekpos.data.model.PaymentDetail
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
    private val api: ApiService,
    private val localTransactionDao: LocalTransactionDao,
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
                // Load local pending/unsynced transactions (show at top on first page only)
                val localRows = if (refresh || nextPage == 1) {
                    localTransactionDao.getUnsyncedTransactions().map { tx ->
                        val count = localTransactionDao.getItemCount(tx.id)
                        tx.toHistoryTransaction(count)
                    }
                } else emptyList()

                val env = api.getTransactionHistory(page = nextPage, perPage = 20)
                if (env.success != true) {
                    throw IllegalStateException(env.message ?: "Gagal memuat riwayat")
                }
                val payload = env.data
                val serverRows = payload?.transactions.orEmpty().map { it.toHistoryRowTransaction() }

                // Deduplicate: server rows that match a local unsynced tx by localTransactionId are already shown locally
                val localIds = localRows.map { it.id }.toSet()
                val filteredServer = serverRows.filter { sr ->
                    // toHistoryRowTransaction maps TransactionHistoryRowDto which has localTransactionId field
                    // We rely on id comparison; server rows have numeric IDs so no collision
                    sr.id !in localIds
                }

                _transactions.value = if (refresh || nextPage == 1) {
                    localRows + filteredServer
                } else {
                    _transactions.value + filteredServer
                }
                val p = payload?.pagination
                _total.value = p?.totalItems?.plus(localRows.size) ?: _transactions.value.size
                val cur = p?.currentPage ?: nextPage
                val totalPages = p?.totalPages
                hasMore = when {
                    serverRows.isEmpty() -> false
                    p?.hasMore != null -> p.hasMore == true
                    totalPages != null -> cur < totalPages
                    else -> serverRows.size >= 20
                }
                if (serverRows.isNotEmpty()) nextPage = cur + 1
            } catch (_: Exception) {
                // On API failure, still show local pending transactions
                if (refresh || nextPage == 1) {
                    val localRows = localTransactionDao.getUnsyncedTransactions().map { tx ->
                        val count = localTransactionDao.getItemCount(tx.id)
                        tx.toHistoryTransaction(count)
                    }
                    if (localRows.isNotEmpty()) _transactions.value = localRows
                }
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

    /** Void a local (not-yet-synced) transaction. Has no effect on already-synced transactions. */
    fun voidLocalTransaction(txId: String, branchId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            localTransactionDao.voidTransaction(txId)
            load(branchId, refresh = true)
            onResult(true)
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

/** Maps a local Room transaction to the UI Transaction model for History display. */
private fun LocalTransactionEntity.toHistoryTransaction(itemCount: Int = 0): Transaction = Transaction(
    id = id,
    transactionNumber = invoiceNumber ?: "LOCAL-${id.take(8).uppercase()}",
    branchId = branchId,
    branchName = branchName,
    cashierName = cashierName,
    items = emptyList(),
    subtotal = subtotal,
    discount = discountAmount,
    totalAmount = grandTotal,
    paymentDetails = listOf(PaymentDetail(method = paymentMethod, amount = grandTotal)),
    totalPaid = grandTotal,
    change = 0.0,
    notes = notes ?: "",
    createdAt = completedAt,
    listItemsCount = itemCount,
    isPendingSync = syncStatus != "synced",
)
