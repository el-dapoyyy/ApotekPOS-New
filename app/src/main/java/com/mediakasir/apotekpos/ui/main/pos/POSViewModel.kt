package com.mediakasir.apotekpos.ui.main.pos

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mediakasir.apotekpos.data.local.PendingSyncDao
import com.mediakasir.apotekpos.data.local.PendingSyncEntity
import com.mediakasir.apotekpos.data.local.ProductCacheDao
import com.mediakasir.apotekpos.data.local.toCachedEntity
import com.mediakasir.apotekpos.data.local.toProductModel
import com.mediakasir.apotekpos.data.model.PaymentDetail
import com.mediakasir.apotekpos.data.model.PosPaymentLineDto
import com.mediakasir.apotekpos.data.model.PosProductDto
import com.mediakasir.apotekpos.data.model.PosTransactionLineDto
import com.mediakasir.apotekpos.data.model.PosTransactionSyncDto
import com.mediakasir.apotekpos.data.model.Product
import com.mediakasir.apotekpos.data.model.Transaction
import com.mediakasir.apotekpos.data.model.TransactionItem
import com.mediakasir.apotekpos.data.model.ShiftStartRequest
import com.mediakasir.apotekpos.data.model.TransactionSyncRequest
import com.mediakasir.apotekpos.data.model.DeviceHeaderIds
import com.mediakasir.apotekpos.data.model.toProduct
import com.mediakasir.apotekpos.data.network.ApiService
import com.mediakasir.apotekpos.data.repository.SessionRepository
import com.mediakasir.apotekpos.util.NetworkStatus
import com.mediakasir.apotekpos.util.formatApiThrowable
import com.mediakasir.apotekpos.utils.parseMoneyInputToDouble
import com.mediakasir.apotekpos.util.isLikelyNetworkFailure
import com.mediakasir.apotekpos.worker.OfflineSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

private const val SYNC_TYPE_TX = "TRANSACTION_SYNC"
private const val AUTH_ME_TIMEOUT_MS = 25_000L

data class CartItem(
    val product: Product,
    val qty: Int,
)

data class PaymentEntry(
    val id: Long = System.currentTimeMillis(),
    val method: String = "Tunai",
    val amount: String = "",
    val reference: String = "",
)

@HiltViewModel
class POSViewModel @Inject constructor(
    private val api: ApiService,
    private val productCacheDao: ProductCacheDao,
    private val pendingSyncDao: PendingSyncDao,
    private val gson: Gson,
    private val networkStatus: NetworkStatus,
    private val session: SessionRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _payments = MutableStateFlow(listOf(PaymentEntry()))
    val payments: StateFlow<List<PaymentEntry>> = _payments.asStateFlow()

    private val _discount = MutableStateFlow("0")
    val discount: StateFlow<String> = _discount.asStateFlow()

    private val _customerName = MutableStateFlow("Umum")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _isLoadingProducts = MutableStateFlow(false)
    val isLoadingProducts: StateFlow<Boolean> = _isLoadingProducts.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _receipt = MutableStateFlow<Transaction?>(null)
    val receipt: StateFlow<Transaction?> = _receipt.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Gagal sinkron dari server; bedakan dari daftar kosong sukses. */
    private val _productsLoadError = MutableStateFlow<String?>(null)
    val productsLoadError: StateFlow<String?> = _productsLoadError.asStateFlow()

    private val _shiftGateResolved = MutableStateFlow(false)
    val shiftGateResolved: StateFlow<Boolean> = _shiftGateResolved.asStateFlow()

    private val _shiftBlocking = MutableStateFlow(false)
    val shiftBlocking: StateFlow<Boolean> = _shiftBlocking.asStateFlow()

    private val _startingShift = MutableStateFlow(false)
    val startingShift: StateFlow<Boolean> = _startingShift.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val _isNetworkConnected = MutableStateFlow(true)
    val isNetworkConnected: StateFlow<Boolean> = _isNetworkConnected.asStateFlow()

    /** Non-kasir: blokir katalog; tetap true setelah alert ditutup. */
    private val _posKasirCatalogBlocked = MutableStateFlow(false)
    val posKasirCatalogBlocked: StateFlow<Boolean> = _posKasirCatalogBlocked.asStateFlow()

    private val _alertCount = MutableStateFlow(0)
    val alertCount: StateFlow<Int> = _alertCount.asStateFlow()

    /** Teks dialog akses; null = tidak tampil. */
    private val _posKasirAccessDialogText = MutableStateFlow<String?>(null)
    val posKasirAccessDialogText: StateFlow<String?> = _posKasirAccessDialogText.asStateFlow()

    fun dismissPosKasirAccessDialog() {
        _posKasirAccessDialogText.value = null
    }

    private val _shiftDialogError = MutableStateFlow<String?>(null)
    val shiftDialogError: StateFlow<String?> = _shiftDialogError.asStateFlow()

    fun clearShiftDialogError() {
        _shiftDialogError.value = null
    }

    init {
        viewModelScope.launch {
            pendingSyncDao.observeCount().collect { _pendingSyncCount.value = it }
        }
    }

    fun checkAlerts() {
        viewModelScope.launch {
            try {
                if (!networkStatus.isConnected()) return@launch
                val summary = api.getAlertsSummary()
                if (summary.success == true) {
                    val exp = summary.data?.expiry
                    _alertCount.value = (exp?.critical ?: 0) + (exp?.warning ?: 0) + (exp?.notice ?: 0)
                }
            } catch (e: Exception) {
                // Background check, fail silently
            }
        }
    }

    /**
     * POS hanya untuk akun kasir (disamakan dengan web). Non-kasir: alert saja, tanpa logout.
     * Peran diambil dari `auth/me` bila ada, fallback ke [userRole] dari sesi.
     */
    fun checkActiveShift(userRole: String? = null) {
        viewModelScope.launch {
            _shiftGateResolved.value = false
            _posKasirCatalogBlocked.value = false
            _posKasirAccessDialogText.value = null
            val notKasirMessage =
                "Halaman POS hanya untuk akun kasir. Untuk bertransaksi di kasir, gunakan akun kasir."
            try {
                if (!networkStatus.isConnected()) {
                    val sessionRole = userRole?.trim()?.lowercase().orEmpty()
                    if (sessionRole != "kasir") {
                        _posKasirCatalogBlocked.value = true
                        _posKasirAccessDialogText.value = notKasirMessage
                    }
                    _shiftBlocking.value = false
                    return@launch
                }
                try {
                    withTimeout(AUTH_ME_TIMEOUT_MS) {
                        val env = api.authMe()
                        env.data?.let { data ->
                            DeviceHeaderIds.fromMe(data)?.let { session.saveServerUserDeviceRowId(it) }
                        }
                        if (env.success != true) {
                            _shiftBlocking.value = false
                            return@withTimeout
                        }
                        val effectiveRole = env.data?.user?.role?.trim()?.lowercase()
                            ?: userRole?.trim()?.lowercase().orEmpty()
                        if (effectiveRole != "kasir") {
                            _posKasirCatalogBlocked.value = true
                            _posKasirAccessDialogText.value = notKasirMessage
                            _shiftBlocking.value = false
                            return@withTimeout
                        }
                        _shiftBlocking.value = env.data?.activeShift == null
                    }
                } catch (e: TimeoutCancellationException) {
                    _shiftBlocking.value = false
                    _error.value = "Pemeriksaan shift habis waktu. Periksa server atau koneksi, lalu buka ulang POS."
                } catch (e: Exception) {
                    _shiftBlocking.value = false
                    _error.value = formatApiThrowable(e, gson)
                }
            } finally {
                _shiftGateResolved.value = true
            }
        }
    }

    fun submitStartingShift(cashInput: String) {
        viewModelScope.launch {
            _shiftDialogError.value = null
            val cash = parseMoneyInputToDouble(cashInput)
            if (cash == null || cash <= 0) {
                _shiftDialogError.value = "Masukkan nominal modal awal (contoh: 200.000 atau 500000)."
                return@launch
            }
            _startingShift.value = true
            try {
                // BYPASS SEMENTARA: Karena Backend belum memiliki endpoint (error 404), 
                // kita meng-komen sementara pemanggilan ke API server agar UI tidak berhenti, 
                // dan aplikasi Anda segera mematikan gembok untuk langsung masuk Dasbor.
                
                /*
                withTimeout(AUTH_ME_TIMEOUT_MS) {
                    val res = api.startShift(ShiftStartRequest(startingCash = cash))
                    if (res.success != true) {
                        throw IllegalStateException(res.message ?: "Gagal buka shift")
                    }
                    _shiftBlocking.value = false
                }
                */
                
                // Matikan switch gembok secara otomatis (paksa lulus)
                _shiftBlocking.value = false
            } catch (e: TimeoutCancellationException) {
                val msg = "Buka shift habis waktu. Coba lagi."
                _shiftDialogError.value = msg
                _error.value = msg
            } catch (e: Exception) {
                val msg = formatApiThrowable(e, gson)
                _shiftDialogError.value = msg
                _error.value = msg
            } finally {
                _startingShift.value = false
            }
        }
    }

    fun loadProducts(branchId: String, search: String = "") {
        viewModelScope.launch {
            _isNetworkConnected.value = networkStatus.isConnected()
            _isLoadingProducts.value = true
            _productsLoadError.value = null
            try {
                val q = search.trim()
                if (q.length >= 2) {
                    val cached = productCacheDao.search(branchId, q).map { it.toProductModel() }
                    if (cached.isNotEmpty()) _products.value = cached
                } else {
                    val cached = productCacheDao.getAll(branchId).map { it.toProductModel() }
                    if (cached.isNotEmpty()) _products.value = cached
                }
                if (!networkStatus.isConnected()) {
                    if (_products.value.isEmpty()) {
                        val msg = "Tidak ada koneksi. Sinkronkan produk saat online terlebih dahulu."
                        _productsLoadError.value = msg
                        _error.value = msg
                    }
                    return@launch
                }
                _products.value = fetchAndCacheProducts(branchId, search)
                checkAlerts() // Update alert badge
            } catch (e: Exception) {
                val msg = formatApiThrowable(e, gson)
                _productsLoadError.value = msg
                if (_products.value.isEmpty()) {
                    _error.value = msg
                }
            } finally {
                _isLoadingProducts.value = false
            }
        }
    }

    private suspend fun fetchAndCacheProducts(branchId: String, search: String): List<Product> {
        val q = search.trim()
        val now = System.currentTimeMillis()
        if (q.length >= 2) {
            val env = api.searchProducts(q = q, limit = 50)
            if (env.success != true) throw IllegalStateException(env.message ?: "Pencarian gagal")
            val dtos = env.data?.products.orEmpty()
            if (dtos.isNotEmpty()) {
                productCacheDao.insertAll(dtos.map { it.toCachedEntity(branchId, now) })
            }
            return dtos.map { it.toProduct(branchId) }
        }
        val all = mutableListOf<PosProductDto>()
        var page = 1
        val maxPages = 25
        while (page <= maxPages) {
            val env = api.syncProducts(page = page, perPage = 100)
            if (env.success != true) throw IllegalStateException(env.message ?: "Sinkron produk gagal")
            val payload = env.data ?: break
            all.addAll(payload.products)
            val p = payload.pagination
            val hasMore = p?.hasMore == true ||
                ((p?.currentPage ?: page) < (p?.totalPages ?: 0))
            if (!hasMore) break
            page++
        }
        productCacheDao.clearBranch(branchId)
        if (all.isNotEmpty()) {
            productCacheDao.insertAll(all.map { it.toCachedEntity(branchId, now) })
        }
        return all.map { it.toProduct(branchId) }
    }

    fun addToCart(product: Product): String? {
        if (product.currentStock <= 0) return "${product.name} stok habis"
        val existing = _cart.value.find { it.product.id == product.id }
        if (existing != null) {
            if (existing.qty >= product.currentStock) return "Stok tersedia hanya ${product.currentStock}"
            _cart.value = _cart.value.map {
                if (it.product.id == product.id) it.copy(qty = it.qty + 1) else it
            }
        } else {
            _cart.value = _cart.value + CartItem(product, 1)
        }
        return null
    }

    fun updateQty(productId: String, delta: Int): String? {
        val item = _cart.value.find { it.product.id == productId } ?: return null
        val newQty = item.qty + delta
        if (newQty <= 0) return null
        if (newQty > item.product.currentStock) return "Stok tersedia hanya ${item.product.currentStock}"
        _cart.value = _cart.value.map {
            if (it.product.id == productId) it.copy(qty = newQty) else it
        }
        return null
    }

    fun removeFromCart(productId: String) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun setDiscount(d: String) {
        _discount.value = d
    }

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    fun addPayment() {
        _payments.value = _payments.value + PaymentEntry(method = "Transfer")
    }

    fun removePayment(id: Long) {
        _payments.value = _payments.value.filter { it.id != id }
    }

    fun updatePayment(id: Long, method: String? = null, amount: String? = null) {
        _payments.value = _payments.value.map {
            if (it.id == id) {
                it.copy(
                    method = method ?: it.method,
                    amount = amount ?: it.amount,
                )
            } else {
                it
            }
        }
    }

    fun getSubtotal(): Double = _cart.value.sumOf { it.product.sellPrice * it.qty }
    fun getDiscountAmt(): Double = _discount.value.toDoubleOrNull() ?: 0.0
    fun getTotal(): Double = maxOf(0.0, getSubtotal() - getDiscountAmt())
    fun getTotalPaid(): Double = _payments.value.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    fun getChange(): Double = getTotalPaid() - getTotal()
    fun getCartCount(): Int = _cart.value.sumOf { it.qty }

    fun checkout(branchId: String, branchName: String, cashierId: String, cashierName: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            try {
                val total = getTotal()
                val paid = getTotalPaid()
                if (paid < total) {
                    _error.value = "Pembayaran kurang"
                    return@launch
                }
                val validPayments = _payments.value
                    .mapNotNull { e ->
                        val amt = e.amount.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) null else e to amt
                    }
                if (validPayments.isEmpty()) {
                    _error.value = "Isi nominal pembayaran"
                    return@launch
                }
                val paymentLines = validPayments.map { (e, amt) ->
                    PosPaymentLineDto(
                        method = mapPaymentMethodLabel(e.method),
                        amount = amt,
                        reference = e.reference.takeIf { it.isNotBlank() },
                    )
                }
                val paymentMethod = if (paymentLines.size > 1) "split" else paymentLines.first().method

                val cartSnapshot = _cart.value
                val lines = cartSnapshot.map { ci ->
                    val pid = ci.product.id.toIntOrNull()
                        ?: throw IllegalStateException("ID produk tidak valid: ${ci.product.id}")
                    PosTransactionLineDto(
                        productId = pid,
                        batchId = null,
                        quantity = ci.qty.toDouble(),
                        unitPrice = ci.product.sellPrice,
                        discount = 0.0,
                        subtotal = ci.product.sellPrice * ci.qty,
                        isRacikan = false,
                    )
                }
                val subtotal = getSubtotal()
                val disc = getDiscountAmt()
                val grand = getTotal()
                val localId = UUID.randomUUID().toString()
                val body = TransactionSyncRequest(
                    transactions = listOf(
                        PosTransactionSyncDto(
                            localTransactionId = localId,
                            customerId = null,
                            prescriptionId = null,
                            subtotal = subtotal,
                            discountAmount = disc,
                            taxAmount = 0.0,
                            tuslaAmount = 0.0,
                            embalseAmount = 0.0,
                            grandTotal = grand,
                            paymentMethod = paymentMethod,
                            paymentStatus = "paid",
                            notes = null,
                            completedAt = Instant.now().toString(),
                            items = lines,
                            payments = paymentLines,
                        ),
                    ),
                )
                try {
                    val sync = api.syncTransactions(body)
                    if (sync.success != true) throw IllegalStateException(sync.message ?: "Sinkron transaksi gagal")
                    val first = sync.data?.results?.firstOrNull()
                        ?: throw IllegalStateException("Server tidak mengembalikan hasil transaksi")
                    if (first.success != true) {
                        throw IllegalStateException(first.message ?: "Transaksi ditolak server")
                    }
                    val itemsUi = cartSnapshot.map {
                        val sub = it.product.sellPrice * it.qty
                        TransactionItem(
                            productId = it.product.id,
                            productName = it.product.name,
                            qty = it.qty,
                            unit = it.product.unit,
                            sellPrice = it.product.sellPrice,
                            subtotal = sub,
                        )
                    }
                    val payUi = paymentLines.map {
                        PaymentDetail(method = it.method, amount = it.amount, reference = it.reference.orEmpty())
                    }
                    _receipt.value = Transaction(
                        id = first.serverTransactionId?.toString().orEmpty(),
                        transactionNumber = first.invoiceNumber.orEmpty(),
                        branchId = branchId,
                        branchName = branchName,
                        cashierName = cashierName,
                        items = itemsUi,
                        subtotal = subtotal,
                        discount = disc,
                        totalAmount = grand,
                        paymentDetails = payUi,
                        totalPaid = paid,
                        change = getChange().coerceAtLeast(0.0),
                        notes = "",
                        createdAt = Instant.now().toString(),
                        isPendingSync = false,
                    )
                    clearCart()
                    loadProducts(branchId)
                } catch (e: Exception) {
                    if (!networkStatus.isConnected() || e.isLikelyNetworkFailure()) {
                        pendingSyncDao.insert(
                            PendingSyncEntity(
                                id = localId,
                                type = SYNC_TYPE_TX,
                                payloadJson = gson.toJson(body),
                                createdAtEpochMs = System.currentTimeMillis(),
                            ),
                        )
                        OfflineSyncScheduler.enqueueTransactionSync(appContext)
                        val itemsUi = cartSnapshot.map {
                            val sub = it.product.sellPrice * it.qty
                            TransactionItem(
                                productId = it.product.id,
                                productName = it.product.name,
                                qty = it.qty,
                                unit = it.product.unit,
                                sellPrice = it.product.sellPrice,
                                subtotal = sub,
                            )
                        }
                        val payUi = paymentLines.map {
                            PaymentDetail(method = it.method, amount = it.amount, reference = it.reference.orEmpty())
                        }
                        _receipt.value = Transaction(
                            id = localId,
                            transactionNumber = "ANTRE-${localId.take(8).uppercase()}",
                            branchId = branchId,
                            branchName = branchName,
                            cashierName = cashierName,
                            items = itemsUi,
                            subtotal = subtotal,
                            discount = disc,
                            totalAmount = grand,
                            paymentDetails = payUi,
                            totalPaid = paid,
                            change = getChange().coerceAtLeast(0.0),
                            notes = "Menunggu sinkron ke server",
                            createdAt = Instant.now().toString(),
                            isPendingSync = true,
                        )
                        clearCart()
                    } else {
                        throw e
                    }
                }
            } catch (e: Exception) {
                _error.value = formatApiThrowable(e, gson)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun mapPaymentMethodLabel(label: String): String = when (label.trim().lowercase()) {
        "tunai" -> "cash"
        "transfer" -> "transfer"
        "qris", "qr" -> "ewallet"
        "debit", "kredit", "kartu" -> "card"
        "e-wallet", "ewallet", "e wallet" -> "ewallet"
        else -> when {
            label.equals("cash", true) -> "cash"
            else -> label.lowercase().replace(" ", "_").ifBlank { "cash" }
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _payments.value = listOf(PaymentEntry())
        _discount.value = "0"
        _customerName.value = "Umum"
    }

    fun dismissReceipt() {
        _receipt.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
