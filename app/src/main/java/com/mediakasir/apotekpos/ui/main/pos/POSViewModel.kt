package com.mediakasir.apotekpos.ui.main.pos

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mediakasir.apotekpos.data.local.LocalPaymentEntity
import com.mediakasir.apotekpos.data.local.LocalShiftDao
import com.mediakasir.apotekpos.data.local.LocalShiftEntity
import com.mediakasir.apotekpos.data.local.LocalTransactionDao
import com.mediakasir.apotekpos.data.local.LocalTransactionEntity
import com.mediakasir.apotekpos.data.local.LocalTransactionItemEntity
import com.mediakasir.apotekpos.data.local.PendingSyncDao
import com.mediakasir.apotekpos.data.local.PendingSyncEntity
import com.mediakasir.apotekpos.data.local.ProductCacheDao
import com.mediakasir.apotekpos.data.local.toCachedEntity
import com.mediakasir.apotekpos.data.local.toProductModel
import com.mediakasir.apotekpos.data.sync.ConnectivityObserver
import com.mediakasir.apotekpos.data.sync.SyncManager
import com.mediakasir.apotekpos.data.model.PaymentDetail
import com.mediakasir.apotekpos.data.model.PosCustomerDto
import com.mediakasir.apotekpos.data.model.PosDiscountDto
import com.mediakasir.apotekpos.data.model.PosPaymentLineDto
import com.mediakasir.apotekpos.data.model.PosPromotionDto
import com.mediakasir.apotekpos.data.model.PosProductDto
import com.mediakasir.apotekpos.data.model.PosTransactionLineDto
import com.mediakasir.apotekpos.data.model.PosTransactionSyncDto
import com.mediakasir.apotekpos.data.model.Product
import com.mediakasir.apotekpos.data.model.Transaction
import com.mediakasir.apotekpos.data.model.TransactionItem
import com.mediakasir.apotekpos.data.model.ShiftStartRequest
import com.mediakasir.apotekpos.data.model.ShiftCloseRequest
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
import java.time.LocalDate
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

data class ShiftSummaryData(
    val shiftType: String,
    val cashierName: String,
    val branchName: String,
    val startedAt: String,
    val endedAt: String,
    val startingCash: Double,
    val endingCash: Double,
    val expectedCash: Double,
    val difference: Double,
    val totalSales: Double,
    val totalCashSales: Double,
    val totalNonCashSales: Double,
    val totalTransactions: Int,
)

@HiltViewModel
class POSViewModel @Inject constructor(
    private val api: ApiService,
    private val productCacheDao: ProductCacheDao,
    private val pendingSyncDao: PendingSyncDao,
    private val localTransactionDao: LocalTransactionDao,
    private val localShiftDao: LocalShiftDao,
    private val syncManager: SyncManager,
    private val connectivityObserver: ConnectivityObserver,
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

    private val _selectedCustomer = MutableStateFlow<PosCustomerDto?>(null)
    val selectedCustomer: StateFlow<PosCustomerDto?> = _selectedCustomer.asStateFlow()

    private val _customerResults = MutableStateFlow<List<PosCustomerDto>>(emptyList())
    val customerResults: StateFlow<List<PosCustomerDto>> = _customerResults.asStateFlow()

    private val _customerSearching = MutableStateFlow(false)
    val customerSearching: StateFlow<Boolean> = _customerSearching.asStateFlow()

    private val _discounts = MutableStateFlow<List<PosDiscountDto>>(emptyList())
    val discounts: StateFlow<List<PosDiscountDto>> = _discounts.asStateFlow()

    private val _promotions = MutableStateFlow<List<PosPromotionDto>>(emptyList())
    val promotions: StateFlow<List<PosPromotionDto>> = _promotions.asStateFlow()

    private val _selectedDiscountId = MutableStateFlow<Int?>(null)
    val selectedDiscountId: StateFlow<Int?> = _selectedDiscountId.asStateFlow()

    private val _selectedDiscountLabel = MutableStateFlow<String?>(null)
    val selectedDiscountLabel: StateFlow<String?> = _selectedDiscountLabel.asStateFlow()

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

    private val _activeShift = MutableStateFlow<LocalShiftEntity?>(null)
    val activeShift: StateFlow<LocalShiftEntity?> = _activeShift.asStateFlow()

    private val _showCloseShiftDialog = MutableStateFlow(false)
    val showCloseShiftDialog: StateFlow<Boolean> = _showCloseShiftDialog.asStateFlow()

    private val _closingShift = MutableStateFlow(false)
    val closingShift: StateFlow<Boolean> = _closingShift.asStateFlow()

    private val _shiftSummary = MutableStateFlow<ShiftSummaryData?>(null)
    val shiftSummary: StateFlow<ShiftSummaryData?> = _shiftSummary.asStateFlow()

    /** True when the active shift has run past its scheduled end hour. */
    private val _shiftExpiredWarning = MutableStateFlow(false)
    val shiftExpiredWarning: StateFlow<Boolean> = _shiftExpiredWarning.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    /** Errors from failed sync attempts — shown when user taps the pending badge. */
    private val _syncErrors = MutableStateFlow<List<com.mediakasir.apotekpos.data.local.FailedSyncInfo>>(emptyList())
    val syncErrors: StateFlow<List<com.mediakasir.apotekpos.data.local.FailedSyncInfo>> = _syncErrors.asStateFlow()

    private val _showSyncErrorDialog = MutableStateFlow(false)
    val showSyncErrorDialog: StateFlow<Boolean> = _showSyncErrorDialog.asStateFlow()

    private val _isRetryingSync = MutableStateFlow(false)
    val isRetryingSync: StateFlow<Boolean> = _isRetryingSync.asStateFlow()

    /** Message shown inside the sync dialog after a retry attempt completes. */
    private val _syncRetryMessage = MutableStateFlow<String?>(null)
    val syncRetryMessage: StateFlow<String?> = _syncRetryMessage.asStateFlow()

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

    /** Last branchId used to load products — used for background stock refresh on reconnect. */
    private var lastBranchId = ""

    fun clearShiftDialogError() {
        _shiftDialogError.value = null
    }

    init {
        // ── Startup: rescue any transactions left at "syncing" by a previous crash ──
        viewModelScope.launch {
            localTransactionDao.resetStuckSyncing()
        }
        viewModelScope.launch {
            localTransactionDao.observePendingCount().collect { _pendingSyncCount.value = it }
        }
        viewModelScope.launch {
            var wasOffline = false
            connectivityObserver.isOnline.collect { isOnline ->
                _isNetworkConnected.value = isOnline
                if (isOnline && wasOffline) {
                    // Priority 1: drain pending transactions
                    OfflineSyncScheduler.enqueueTransactionSync(appContext)
                    // Priority 2: refresh stock from server in background
                    val branchId = lastBranchId
                    if (branchId.isNotBlank()) {
                        viewModelScope.launch {
                            backgroundProductSync(branchId)
                        }
                    }
                }
                wasOffline = !isOnline
            }
        }
        // Periodic shift expiry check — every 5 minutes
        viewModelScope.launch {
            while (true) {
                checkShiftExpiry()
                kotlinx.coroutines.delay(5 * 60_000L)
            }
        }
        syncManager.startObserving()
        OfflineSyncScheduler.schedulePeriodicSync(appContext)
        OfflineSyncScheduler.schedulePeriodicStockSync(appContext)
    }

    /** Called when user taps the pending-count badge. Loads error details then shows dialog. */
    fun openSyncErrorDialog() {
        viewModelScope.launch {
            _syncRetryMessage.value = null
            _syncErrors.value = localTransactionDao.getFailedSyncDetails()
            _showSyncErrorDialog.value = true
        }
    }

    fun closeSyncErrorDialog() {
        _showSyncErrorDialog.value = false
        _syncRetryMessage.value = null
    }

    /**
     * Directly attempt to sync all pending/failed transactions inline (not just queue to WorkManager).
     * Shows immediate success/failure feedback inside the dialog.
     */
    fun retrySync() {
        viewModelScope.launch {
            _isRetryingSync.value = true
            _syncRetryMessage.value = null
            try {
                // Reset failed → pending (keep error history via syncAttempts/lastSyncError)
                localTransactionDao.resetFailedToPending()

                // Attempt direct inline sync for immediate feedback
                val pending = localTransactionDao.getPendingTransactions()
                if (pending.isEmpty()) {
                    _syncRetryMessage.value = "✅ Semua transaksi sudah tersinkron."
                    _syncErrors.value = emptyList()
                    return@launch
                }

                var successCount = 0
                var failCount = 0
                var lastError: String? = null

                for (tx in pending) {
                    try {
                        localTransactionDao.updateSyncStatus(tx.id, "syncing")
                        val items = localTransactionDao.getItemsForTransaction(tx.id)
                        val payments = localTransactionDao.getPaymentsForTransaction(tx.id)
                        val body = TransactionSyncRequest(
                            transactions = listOf(
                                PosTransactionSyncDto(
                                    localTransactionId = tx.id,
                                    customerId = tx.customerId,
                                    prescriptionId = tx.prescriptionId,
                                    discountId = tx.discountId,
                                    subtotal = tx.subtotal,
                                    discountAmount = tx.discountAmount,
                                    taxAmount = tx.taxAmount,
                                    tuslaAmount = 0.0,
                                    embalseAmount = 0.0,
                                    grandTotal = tx.grandTotal,
                                    paymentMethod = tx.paymentMethod,
                                    paymentStatus = tx.paymentStatus,
                                    notes = tx.notes,
                                    completedAt = tx.completedAt,
                                    items = items.map {
                                        PosTransactionLineDto(
                                            productId = it.productId,
                                            batchId = it.batchId,
                                            quantity = it.quantity,
                                            unitPrice = it.unitPrice,
                                            discount = it.discount,
                                            subtotal = it.subtotal,
                                            isRacikan = it.isRacikan,
                                        )
                                    },
                                    payments = payments.map {
                                        PosPaymentLineDto(
                                            method = it.method,
                                            amount = it.amount,
                                            reference = it.reference,
                                        )
                                    },
                                ),
                            ),
                        )
                        val sync = api.syncTransactions(body)
                        val first = sync.data?.results?.firstOrNull()
                        if (sync.success == true && first?.success == true && first.serverTransactionId != null) {
                            localTransactionDao.markSynced(tx.id, first.serverTransactionId, first.invoiceNumber.orEmpty())
                            successCount++
                        } else {
                            val err = first?.message ?: sync.message ?: "Server menolak permintaan"
                            localTransactionDao.markSyncFailed(tx.id, err)
                            lastError = err
                            failCount++
                        }
                    } catch (e: Exception) {
                        val err = e.message ?: e.javaClass.simpleName
                        localTransactionDao.markSyncFailed(tx.id, err)
                        lastError = err
                        failCount++
                    }
                }

                // Refresh errors for display
                _syncErrors.value = localTransactionDao.getFailedSyncDetails()

                _syncRetryMessage.value = when {
                    failCount == 0 -> "✅ Semua $successCount transaksi berhasil dikirim!"
                    successCount > 0 -> "⚠️ $successCount berhasil, $failCount gagal\n$lastError"
                    else -> "❌ Gagal mengirim. Server error: $lastError"
                }
            } catch (e: Exception) {
                _syncRetryMessage.value = "❌ Error: ${e.message}"
            } finally {
                _isRetryingSync.value = false
                // Also enqueue background worker for any remaining items
                OfflineSyncScheduler.enqueueTransactionSync(appContext)
            }
        }
    }

    /**
     * Shift end hours (based on creation time rule):
     *  pagi  (00–14) → expires at 15:00
     *  sore  (15–21) → expires at 22:00
     *  malam (22–23) → expires when we enter daytime again (07:00–21:59)
     */
    private fun checkShiftExpiry() {
        val shift = _activeShift.value ?: run { _shiftExpiredWarning.value = false; return }
        val h = java.time.LocalTime.now().hour
        _shiftExpiredWarning.value = when (shift.shiftType) {
            "pagi"  -> h >= 15
            "sore"  -> h >= 22
            "malam" -> h in 7..21   // next-day morning/afternoon window
            else    -> false
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
    fun checkActiveShift(userRole: String? = null, branchId: String = "", cashierId: String = "") {
        viewModelScope.launch {
            _shiftGateResolved.value = false
            _posKasirCatalogBlocked.value = false
            _posKasirAccessDialogText.value = null
            val notKasirMessage =
                "Halaman POS hanya untuk akun kasir. Untuk bertransaksi di kasir, gunakan akun kasir."
            try {
                // ── Check local shift first (offline-first) ──
                val localShift = if (cashierId.isNotEmpty() && branchId.isNotEmpty()) {
                    localShiftDao.getActiveShift(cashierId, branchId)
                } else null

                if (localShift != null) {
                    _activeShift.value = localShift
                    _shiftBlocking.value = false
                    // Still check role if online
                    if (networkStatus.isConnected()) {
                        checkRoleOnline(userRole, notKasirMessage)
                    } else {
                        val sessionRole = userRole?.trim()?.lowercase().orEmpty()
                        if (sessionRole != "kasir") {
                            _posKasirCatalogBlocked.value = true
                            _posKasirAccessDialogText.value = notKasirMessage
                        }
                    }
                    return@launch
                }

                // ── No local shift — check server ──
                if (!networkStatus.isConnected()) {
                    val sessionRole = userRole?.trim()?.lowercase().orEmpty()
                    if (sessionRole != "kasir") {
                        _posKasirCatalogBlocked.value = true
                        _posKasirAccessDialogText.value = notKasirMessage
                    }
                    // No local shift + offline = must open shift
                    _shiftBlocking.value = true
                    return@launch
                }
                try {
                    withTimeout(AUTH_ME_TIMEOUT_MS) {
                        val env = api.authMe()
                        env.data?.let { data ->
                            DeviceHeaderIds.fromMe(data)?.let { session.saveServerUserDeviceRowId(it) }
                        }
                        if (env.success != true) {
                            _shiftBlocking.value = true
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
                    _shiftBlocking.value = true
                    _error.value = "Pemeriksaan shift habis waktu. Periksa server atau koneksi, lalu buka ulang POS."
                } catch (e: Exception) {
                    _shiftBlocking.value = true
                    _error.value = formatApiThrowable(e, gson)
                }
            } finally {
                _shiftGateResolved.value = true
            }
        }
    }

    private suspend fun checkRoleOnline(userRole: String?, notKasirMessage: String) {
        try {
            withTimeout(AUTH_ME_TIMEOUT_MS) {
                val env = api.authMe()
                env.data?.let { data ->
                    DeviceHeaderIds.fromMe(data)?.let { session.saveServerUserDeviceRowId(it) }
                }
                val effectiveRole = env.data?.user?.role?.trim()?.lowercase()
                    ?: userRole?.trim()?.lowercase().orEmpty()
                if (effectiveRole != "kasir") {
                    _posKasirCatalogBlocked.value = true
                    _posKasirAccessDialogText.value = notKasirMessage
                }
            }
        } catch (_: Exception) { /* best-effort role check */ }
    }

    fun submitStartingShift(cashInput: String, branchId: String = "", cashierId: String = "", cashierName: String = "") {
        viewModelScope.launch {
            _shiftDialogError.value = null
            val cash = parseMoneyInputToDouble(cashInput)
            if (cash == null || cash <= 0) {
                _shiftDialogError.value = "Masukkan nominal modal awal (contoh: 200.000 atau 500000)."
                return@launch
            }
            _startingShift.value = true
            try {
                // Determine shift type from current time
                val hour = java.time.LocalTime.now().hour
                val shiftType = when {
                    hour < 15 -> "pagi"
                    hour < 22 -> "sore"
                    else -> "malam"
                }

                val shiftId = UUID.randomUUID().toString()
                val shift = LocalShiftEntity(
                    id = shiftId,
                    cashierId = cashierId,
                    cashierName = cashierName,
                    branchId = branchId,
                    shiftType = shiftType,
                    startedAt = Instant.now().toString(),
                    startingCash = cash,
                )
                localShiftDao.insert(shift)
                _activeShift.value = shift

                // Try server sync in background (best-effort)
                if (networkStatus.isConnected()) {
                    try {
                        val res = api.startShift(ShiftStartRequest(startingCash = cash))
                        val serverId = res.data?.id
                        if (res.success == true && serverId != null) {
                            localShiftDao.markSynced(shiftId, serverId)
                            _activeShift.value = shift.copy(serverShiftId = serverId)
                        }
                    } catch (_: Exception) {
                        // Server sync failed — shift is saved locally, will sync later
                    }
                }

                _shiftBlocking.value = false
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
        if (branchId.isNotBlank()) lastBranchId = branchId
        viewModelScope.launch {
            _isNetworkConnected.value = networkStatus.isConnected()
            _isLoadingProducts.value = true
            _productsLoadError.value = null
            try {
                // ── Phase 1: Load from cache immediately (zero latency) ──
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
                // ── Phase 2: Background incremental sync ──
                // For search queries, fetch from server directly
                if (q.length >= 2) {
                    _products.value = fetchAndCacheProducts(branchId, search)
                } else {
                    // Incremental sync: only fetch products updated since last sync
                    backgroundProductSync(branchId)
                }
                if (_discounts.value.isEmpty() || _promotions.value.isEmpty()) {
                    loadMarketingData()
                }
                checkAlerts()
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

    private suspend fun backgroundProductSync(branchId: String) {
        val lastSync = productCacheDao.getLastSyncTime(branchId)
        val updatedSince = lastSync?.let {
            java.time.Instant.ofEpochMilli(it).atOffset(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ISO_INSTANT)
        }
        val now = System.currentTimeMillis()
        var page = 1
        val maxPages = 25
        var anyUpdated = false

        while (page <= maxPages) {
            val env = api.syncProducts(page = page, perPage = 100, updatedSince = updatedSince)
            if (env.success != true) break
            val payload = env.data ?: break
            val products = payload.products
            if (products.isNotEmpty()) {
                productCacheDao.insertAll(products.map { it.toCachedEntity(branchId, now) })
                anyUpdated = true
            }
            val p = payload.pagination
            val hasMore = p?.hasMore == true ||
                ((p?.currentPage ?: page) < (p?.totalPages ?: 0))
            if (!hasMore) break
            page++
        }

        if (anyUpdated) {
            // Re-apply pending local transaction deductions so products sold but not yet synced
            // are not shown with the old (higher) server stock.
            applyPendingDeductionsToCache(branchId)
            val refreshed = productCacheDao.getAll(branchId).map { it.toProductModel() }
            if (refreshed.isNotEmpty()) _products.value = refreshed
        }
    }

    /**
     * After writing server stock data to cache, re-subtract the quantities from any local
     * transactions that have not yet been confirmed by the server. This prevents "phantom stock"
     * where a just-sold product reappears as available because the server hasn't processed
     * the transaction yet.
     */
    private suspend fun applyPendingDeductionsToCache(branchId: String) {
        val deductions = localTransactionDao.getPendingDeductionsPerProduct()
        for (d in deductions) {
            productCacheDao.deductStock(d.productId.toString(), d.totalQty)
        }
    }

    private suspend fun loadMarketingData() {
        runCatching {
            val discountEnv = api.getDiscounts()
            if (discountEnv.success == true) {
                _discounts.value = discountEnv.data.orEmpty()
            }
        }
        runCatching {
            val promotionEnv = api.getPromotions()
            if (promotionEnv.success == true) {
                _promotions.value = promotionEnv.data.orEmpty()
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
        _selectedDiscountId.value = null
        _selectedDiscountLabel.value = null
    }

    fun applyDiscount(discount: PosDiscountDto): String? {
        val subtotal = getSubtotal()
        val minPurchase = discount.minPurchase ?: 0.0
        if (subtotal < minPurchase) {
            return "Minimal belanja ${minPurchase.toInt()} untuk promo ini"
        }
        val usageLimit = discount.usageLimit
        val usageCount = discount.usageCount ?: 0
        if (usageLimit != null && usageCount >= usageLimit) {
            return "Voucher sudah mencapai batas penggunaan"
        }
        val today = LocalDate.now()
        val start = discount.validFrom?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val end = discount.validUntil?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (start != null && today.isBefore(start)) return "Voucher belum aktif"
        if (end != null && today.isAfter(end)) return "Voucher sudah kedaluwarsa"

        val raw = when (discount.type?.lowercase()) {
            "percentage" -> subtotal * ((discount.value ?: 0.0) / 100.0)
            else -> discount.value ?: 0.0
        }
        val capped = discount.maxDiscount?.let { max -> minOf(raw, max) } ?: raw
        val appliedAmount = capped.coerceAtMost(subtotal).coerceAtLeast(0.0)
        _discount.value = appliedAmount.toString()
        _selectedDiscountId.value = discount.id
        _selectedDiscountLabel.value = discount.code ?: discount.name ?: "Promo #${discount.id}"
        return null
    }

    fun clearAppliedDiscount() {
        _discount.value = "0"
        _selectedDiscountId.value = null
        _selectedDiscountLabel.value = null
    }

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    fun setSelectedCustomer(customer: PosCustomerDto?) {
        _selectedCustomer.value = customer
        _customerName.value = customer?.name ?: "Umum"
    }

    fun searchCustomers(query: String) {
        viewModelScope.launch {
            _customerSearching.value = true
            runCatching {
                val env = api.listCustomers(search = query.takeIf { it.isNotBlank() }, perPage = 20)
                _customerResults.value = env.data?.customers.orEmpty()
            }
            _customerSearching.value = false
        }
    }

    fun addPayment() {
        _payments.value = _payments.value + PaymentEntry(method = "Transfer")
    }

    fun addPayment(entry: PaymentEntry) {
        _payments.value = _payments.value + entry
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

    fun updatePayment(index: Int, amount: String) {
        if (index >= 0 && index < _payments.value.size) {
            val payment = _payments.value[index]
            _payments.value = _payments.value.toMutableList().apply {
                set(index, payment.copy(amount = amount))
            }
        }
    }

    fun getPayments(): List<PaymentEntry> = _payments.value

    fun setCashAmount(amount: Double) {
        if (amount <= 0.0) return
        val normalized = amount.toLong().toString()
        val existingIndex = _payments.value.indexOfFirst { it.method.equals("Tunai", ignoreCase = true) }
        if (existingIndex >= 0) {
            updatePayment(existingIndex, normalized)
            return
        }

        val placeholderIndex = _payments.value.indexOfFirst {
            it.method.equals("Transfer", ignoreCase = true) && parseMoneyInputToDouble(it.amount) == null
        }
        if (placeholderIndex >= 0) {
            val current = _payments.value[placeholderIndex]
            _payments.value = _payments.value.toMutableList().apply {
                set(placeholderIndex, current.copy(method = "Tunai", amount = normalized))
            }
            return
        }

        addPayment(PaymentEntry(method = "Tunai", amount = normalized))
    }

    fun clearCashAmount() {
        val existingIndex = _payments.value.indexOfFirst { it.method.equals("Tunai", ignoreCase = true) }
        if (existingIndex >= 0) {
            val current = _payments.value[existingIndex]
            _payments.value = _payments.value.toMutableList().apply {
                set(existingIndex, current.copy(amount = ""))
            }
        }
    }

    fun getSubtotal(): Double = _cart.value.sumOf { it.product.sellPrice * it.qty }
    fun getDiscountAmt(): Double = _discount.value.toDoubleOrNull() ?: 0.0
    fun getTotal(): Double = maxOf(0.0, getSubtotal() - getDiscountAmt())
    fun getTotalPaid(): Double = _payments.value.sumOf { parseMoneyInputToDouble(it.amount) ?: 0.0 }
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
                        val amt = parseMoneyInputToDouble(e.amount) ?: 0.0
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
                val subtotal = getSubtotal()
                val disc = getDiscountAmt()
                val grand = getTotal()
                val localId = UUID.randomUUID().toString()
                val now = Instant.now().toString()

                // ── 1. Save to Room FIRST (offline-first) ──
                val localTx = LocalTransactionEntity(
                    id = localId,
                    branchId = branchId,
                    cashierId = cashierId,
                    cashierName = cashierName,
                    branchName = branchName,
                    shiftId = _activeShift.value?.id,
                    customerId = null,
                    customerName = _customerName.value,
                    discountId = _selectedDiscountId.value,
                    discountLabel = _selectedDiscountLabel.value,
                    subtotal = subtotal,
                    discountAmount = disc,
                    grandTotal = grand,
                    paymentMethod = paymentMethod,
                    completedAt = now,
                )
                val localItems = cartSnapshot.map { ci ->
                    val pid = ci.product.id.toIntOrNull()
                        ?: throw IllegalStateException("ID produk tidak valid: ${ci.product.id}")
                    LocalTransactionItemEntity(
                        transactionId = localId,
                        productId = pid,
                        productName = ci.product.name,
                        quantity = ci.qty.toDouble(),
                        unitPrice = ci.product.sellPrice,
                        subtotal = ci.product.sellPrice * ci.qty,
                        unit = ci.product.unit,
                    )
                }
                val localPayments = paymentLines.map {
                    LocalPaymentEntity(
                        transactionId = localId,
                        method = it.method,
                        amount = it.amount,
                        reference = it.reference,
                    )
                }
                localTransactionDao.insertFull(localTx, localItems, localPayments)

                // ── 2. Deduct local stock ──
                for (ci in cartSnapshot) {
                    productCacheDao.deductStock(ci.product.id, ci.qty)
                }

                // ── 3. Show receipt immediately from local data ──
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
                    transactionNumber = "LOCAL-${localId.take(8).uppercase()}",
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
                    createdAt = now,
                    isPendingSync = true,
                )
                clearCart()

                // ── 4. Queue background sync ──
                OfflineSyncScheduler.enqueueTransactionSync(appContext)

                // ── 5. Try immediate sync if online (best-effort, non-blocking for receipt) ──
                if (networkStatus.isConnected()) {
                    tryImmediateSync(localId, localTx, localItems, paymentLines, branchId)
                }

                // ── 6. Refresh product list from LOCAL cache only ──
                // Do NOT fetch from server here: the transaction may not be synced yet, so the
                // server would return the pre-sale stock and overwrite our correct local deductions.
                val refreshed = productCacheDao.getAll(branchId).map { it.toProductModel() }
                if (refreshed.isNotEmpty()) _products.value = refreshed
            } catch (e: Exception) {
                _error.value = formatApiThrowable(e, gson)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Best-effort immediate sync after local save.
     * If it fails, the background worker will retry later — receipt is already shown.
     */
    private suspend fun tryImmediateSync(
        localId: String,
        localTx: LocalTransactionEntity,
        items: List<LocalTransactionItemEntity>,
        paymentLines: List<PosPaymentLineDto>,
        branchId: String,
    ) {
        try {
            localTransactionDao.updateSyncStatus(localId, "syncing")
            val lines = items.map { item ->
                PosTransactionLineDto(
                    productId = item.productId,
                    batchId = item.batchId,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discount = item.discount,
                    subtotal = item.subtotal,
                    isRacikan = item.isRacikan,
                )
            }
            val body = TransactionSyncRequest(
                transactions = listOf(
                    PosTransactionSyncDto(
                        localTransactionId = localId,
                        customerId = localTx.customerId,
                        prescriptionId = localTx.prescriptionId,
                        discountId = localTx.discountId,
                        subtotal = localTx.subtotal,
                        discountAmount = localTx.discountAmount,
                        taxAmount = localTx.taxAmount,
                        tuslaAmount = 0.0,
                        embalseAmount = 0.0,
                        grandTotal = localTx.grandTotal,
                        paymentMethod = localTx.paymentMethod,
                        paymentStatus = localTx.paymentStatus,
                        notes = localTx.notes,
                        completedAt = localTx.completedAt,
                        items = lines,
                        payments = paymentLines,
                    ),
                ),
            )
            val sync = api.syncTransactions(body)
            val first = sync.data?.results?.firstOrNull()
            if (sync.success == true && first?.success == true && first.serverTransactionId != null) {
                localTransactionDao.markSynced(localId, first.serverTransactionId, first.invoiceNumber.orEmpty())
                // Update receipt with server invoice number
                _receipt.value = _receipt.value?.copy(
                    transactionNumber = first.invoiceNumber.orEmpty(),
                    isPendingSync = false,
                    notes = "",
                )
            } else {
                localTransactionDao.updateSyncStatus(localId, "pending")
            }
        } catch (_: Exception) {
            // Sync failed — keep as pending, worker will retry
            localTransactionDao.updateSyncStatus(localId, "pending")
        }
    }

    private fun mapPaymentMethodLabel(label: String): String = when (label.trim().lowercase()) {
        "tunai", "cash" -> "cash"
        "transfer" -> "transfer"
        "qris", "qr", "e-wallet", "ewallet", "e wallet" -> "qris"
        "debit" -> "debit"
        "kredit", "credit" -> "credit"
        "kartu", "card" -> "debit"
        "split" -> "split"
        else -> "cash"
    }

    // ── Shift Close Logic ──

    fun requestCloseShift() {
        _showCloseShiftDialog.value = true
    }

    fun dismissCloseShiftDialog() {
        _showCloseShiftDialog.value = false
    }

    fun dismissShiftSummary() {
        _shiftSummary.value = null
    }

    fun closeShift(endingCashInput: String, notes: String = "", branchName: String = "") {
        viewModelScope.launch {
            _closingShift.value = true
            _shiftDialogError.value = null
            try {
                val endingCash = parseMoneyInputToDouble(endingCashInput)
                if (endingCash == null || endingCash < 0) {
                    _shiftDialogError.value = "Masukkan nominal kas akhir."
                    return@launch
                }
                val shift = _activeShift.value ?: run {
                    _shiftDialogError.value = "Tidak ada shift aktif."
                    return@launch
                }

                // Calculate shift summary from local transactions
                val totalSales = localTransactionDao.getTotalSalesForShift(shift.id)
                val txCount = localTransactionDao.getTransactionCountForShift(shift.id)
                val transactions = localTransactionDao.getTransactionsForShift(shift.id)

                // Calculate cash vs non-cash breakdown
                var totalCashSales = 0.0
                var totalNonCashSales = 0.0
                for (tx in transactions) {
                    if (tx.paymentMethod == "cash") {
                        totalCashSales += tx.grandTotal
                    } else {
                        totalNonCashSales += tx.grandTotal
                    }
                }

                val expectedCash = shift.startingCash + totalCashSales
                val now = Instant.now().toString()

                localShiftDao.closeShift(
                    shiftId = shift.id,
                    endedAt = now,
                    endingCash = endingCash,
                    expectedCash = expectedCash,
                    totalSales = totalSales,
                    totalCashSales = totalCashSales,
                    totalNonCashSales = totalNonCashSales,
                    totalTransactions = txCount,
                    notes = notes.takeIf { it.isNotBlank() },
                )

                _shiftSummary.value = ShiftSummaryData(
                    shiftType = shift.shiftType,
                    cashierName = shift.cashierName,
                    branchName = branchName,
                    startedAt = shift.startedAt,
                    endedAt = now,
                    startingCash = shift.startingCash,
                    endingCash = endingCash,
                    expectedCash = expectedCash,
                    difference = endingCash - expectedCash,
                    totalSales = totalSales,
                    totalCashSales = totalCashSales,
                    totalNonCashSales = totalNonCashSales,
                    totalTransactions = txCount,
                )

                // Sync close to server (best-effort)
                val serverId = shift.serverShiftId
                if (serverId != null && networkStatus.isConnected()) {
                    try {
                        api.closeShiftOnServer(
                            id = serverId,
                            body = ShiftCloseRequest(
                                closingCash = endingCash,
                                closingNotes = notes.takeIf { it.isNotBlank() },
                            ),
                        )
                    } catch (_: Exception) {
                        // best-effort — shift sudah ditutup di lokal
                    }
                }

                _activeShift.value = null
                _shiftExpiredWarning.value = false
                _showCloseShiftDialog.value = false
                _shiftBlocking.value = true // Require new shift to continue
            } catch (e: Exception) {
                _shiftDialogError.value = formatApiThrowable(e, gson)
            } finally {
                _closingShift.value = false
            }
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _payments.value = listOf(PaymentEntry())
        _discount.value = "0"
        _selectedDiscountId.value = null
        _selectedDiscountLabel.value = null
        _customerName.value = "Umum"
        _selectedCustomer.value = null
        _customerResults.value = emptyList()
    }

    fun dismissReceipt() {
        _receipt.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
