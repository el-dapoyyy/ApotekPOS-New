package com.mediakasir.apotekpos.ui.main.pos

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mediakasir.apotekpos.data.local.LocalCashExpenseAuditDao
import com.mediakasir.apotekpos.data.local.LocalCashExpenseAuditEntity
import com.mediakasir.apotekpos.data.local.LocalPaymentEntity
import com.mediakasir.apotekpos.data.local.LocalCashExpenseDao
import com.mediakasir.apotekpos.data.local.LocalCashExpenseEntity
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
import com.mediakasir.apotekpos.data.model.CompoundRecipeDto
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
import com.mediakasir.apotekpos.util.ThermalPrinterManager
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
    val isRacikan: Boolean = false,
    val compoundRecipeId: Int? = null,
    val compoundRecipeName: String? = null,
)

data class PaymentEntry(
    val id: Long = System.currentTimeMillis(),
    val method: String = "Tunai",
    val amount: String = "",
    val reference: String = "",
)

data class TopSellingProduct(
    val name: String,
    val qty: Double,
)

data class CashOutByCategory(
    val category: String,
    val total: Double,
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
    val totalQrisSales: Double,
    val totalCashOut: Double,
    val totalTransactions: Int,
    val topSellingProducts: List<TopSellingProduct>,
    val cashOutByCategory: List<CashOutByCategory> = emptyList(),
)

@HiltViewModel
class POSViewModel @Inject constructor(
    private val api: ApiService,
    private val productCacheDao: ProductCacheDao,
    private val pendingSyncDao: PendingSyncDao,
    private val localTransactionDao: LocalTransactionDao,
    private val localShiftDao: LocalShiftDao,
    private val localCashExpenseDao: LocalCashExpenseDao,
    private val localCashExpenseAuditDao: LocalCashExpenseAuditDao,
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

    private val _cashOutHistory = MutableStateFlow<List<LocalCashExpenseEntity>>(emptyList())
    val cashOutHistory: StateFlow<List<LocalCashExpenseEntity>> = _cashOutHistory.asStateFlow()

    private val _cashOutTotal = MutableStateFlow(0.0)
    val cashOutTotal: StateFlow<Double> = _cashOutTotal.asStateFlow()

    private val _cashOutLoading = MutableStateFlow(false)
    val cashOutLoading: StateFlow<Boolean> = _cashOutLoading.asStateFlow()

    /** True when the active shift has run past its scheduled end hour. */
    private val _shiftExpiredWarning = MutableStateFlow(false)
    val shiftExpiredWarning: StateFlow<Boolean> = _shiftExpiredWarning.asStateFlow()

    private val _shiftSoftAlert = MutableStateFlow(false)
    val shiftSoftAlert: StateFlow<Boolean> = _shiftSoftAlert.asStateFlow()

    private val _shiftHardLock = MutableStateFlow(false)
    val shiftHardLock: StateFlow<Boolean> = _shiftHardLock.asStateFlow()

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

    private val _compoundRecipes = MutableStateFlow<List<CompoundRecipeDto>>(emptyList())
    val compoundRecipes: StateFlow<List<CompoundRecipeDto>> = _compoundRecipes.asStateFlow()

    private val _isLoadingRacikan = MutableStateFlow(false)
    val isLoadingRacikan: StateFlow<Boolean> = _isLoadingRacikan.asStateFlow()

    private val _racikanError = MutableStateFlow<String?>(null)
    val racikanError: StateFlow<String?> = _racikanError.asStateFlow()

    private val _showRacikanDialog = MutableStateFlow(false)
    val showRacikanDialog: StateFlow<Boolean> = _showRacikanDialog.asStateFlow()

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
            localCashExpenseDao.resetStuckSyncing()
            localCashExpenseAuditDao.resetStuckSyncing()
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
                    OfflineSyncScheduler.enqueueCashExpenseSync(appContext)
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
        OfflineSyncScheduler.schedulePeriodicCashExpenseSync(appContext)
        OfflineSyncScheduler.schedulePeriodicStockSync(appContext)
        OfflineSyncScheduler.schedulePeriodicShiftAlert(appContext)
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
        val shift = _activeShift.value ?: run {
            _shiftExpiredWarning.value = false
            _shiftSoftAlert.value = false
            _shiftHardLock.value = false
            return
        }
        val elapsedHours = runCatching {
            val start = Instant.parse(shift.startedAt)
            java.time.Duration.between(start, Instant.now()).toMinutes() / 60.0
        }.getOrDefault(0.0)

        _shiftSoftAlert.value = elapsedHours >= 8.0 && elapsedHours < 12.0
        _shiftHardLock.value = elapsedHours >= 12.0
        _shiftExpiredWarning.value = _shiftHardLock.value
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
                checkShiftExpiry()
                tryAutoPrintShiftOpen(shift = _activeShift.value ?: shift, branchName = branchId)
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
        // Warn if batch terdekat kadaluarsa
        if (product.isNearestExpired) {
            _error.value = "⚠️ ${product.name}: batch terdekat sudah KADALUARSA (${product.nearestExpiryDate})"
        } else if (product.isNearestExpiringSoon) {
            _error.value = "⚠️ ${product.name}: mendekati kadaluarsa (${product.nearestExpiryDate})"
        }
        return null
    }

    fun updateQty(productId: String, delta: Int): String? {
        val item = _cart.value.find { it.product.id == productId } ?: return null
        val newQty = item.qty + delta
        if (newQty <= 0) {
            removeFromCart(productId)
            return null
        }
        if (newQty > item.product.currentStock) return "Stok tersedia hanya ${item.product.currentStock}"
        _cart.value = _cart.value.map {
            if (it.product.id == productId) it.copy(qty = newQty) else it
        }
        return null
    }

    fun removeFromCart(productId: String) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    /**
     * Called after a barcode scan. Looks up the product in the in-memory list first (instant),
     * falls back to an API search if not found locally. Adds the matched product directly to
     * the cart — the cashier never needs to type or tap anything.
     */
    fun addByBarcode(barcode: String, branchId: String) {
        val code = barcode.trim()
        if (code.isBlank()) return
        // Fast path: find in already-loaded products
        val local = _products.value.find { it.barcode.equals(code, ignoreCase = true) }
        if (local != null) {
            val err = addToCart(local)
            if (err != null) _error.value = err
            return
        }
        // Slow path: query server
        viewModelScope.launch {
            _isLoadingProducts.value = true
            try {
                val env = api.searchProducts(q = code, limit = 10)
                val dto = env.data?.products
                    ?.find { it.barcode.equals(code, ignoreCase = true) }
                    ?: env.data?.products?.firstOrNull()
                if (dto != null) {
                    val now = System.currentTimeMillis()
                    productCacheDao.insertAll(listOf(dto.toCachedEntity(branchId, now)))
                    val p = dto.toProduct(branchId)
                    if (_products.value.none { it.id == p.id }) {
                        _products.value = _products.value + p
                    }
                    val err = addToCart(p)
                    if (err != null) _error.value = err
                } else {
                    _error.value = "Produk dengan barcode \"$code\" tidak ditemukan"
                }
            } catch (e: Exception) {
                _error.value = "Gagal scan barcode: ${e.message}"
            } finally {
                _isLoadingProducts.value = false
            }
        }
    }

    fun showRacikanDialog() {
        _showRacikanDialog.value = true
        if (_compoundRecipes.value.isEmpty()) loadCompoundRecipes()
    }

    fun hideRacikanDialog() {
        _showRacikanDialog.value = false
    }

    fun loadCompoundRecipes() {
        viewModelScope.launch {
            _isLoadingRacikan.value = true
            _racikanError.value = null
            try {
                val env = api.getCompoundRecipes()
                _compoundRecipes.value = env.data?.filter { it.isActive } ?: emptyList()
                if (_compoundRecipes.value.isEmpty()) {
                    _racikanError.value = "Tidak ada resep racikan aktif ditemukan"
                }
            } catch (e: Exception) {
                _racikanError.value = "Gagal memuat: ${e.message}"
            } finally {
                _isLoadingRacikan.value = false
            }
        }
    }

    fun addRacikanToCart(recipe: CompoundRecipeDto) {
        val sentinelId = "racikan_${recipe.id}"
        val sentinelProduct = Product(
            id = sentinelId,
            name = recipe.name,
            category = "Racikan",
            unit = "bungkus",
            sellPrice = recipe.totalPrice,
            currentStock = 999,
        )
        val existing = _cart.value.find { it.product.id == sentinelId }
        if (existing != null) {
            _cart.value = _cart.value.map {
                if (it.product.id == sentinelId) it.copy(qty = it.qty + 1) else it
            }
        } else {
            _cart.value = _cart.value + CartItem(
                product = sentinelProduct,
                qty = 1,
                isRacikan = true,
                compoundRecipeId = recipe.id,
                compoundRecipeName = recipe.name,
            )
        }
        hideRacikanDialog()
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
                if (_activeShift.value == null || _shiftBlocking.value) {
                    _error.value = "Shift belum dibuka. Input modal awal dulu sebelum transaksi."
                    return@launch
                }
                if (_shiftHardLock.value) {
                    _error.value = "Batas waktu shift berakhir. Tutup shift dulu untuk lanjut transaksi."
                    return@launch
                }
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
                    if (ci.isRacikan) {
                        LocalTransactionItemEntity(
                            transactionId = localId,
                            productId = 0,
                            productName = ci.compoundRecipeName ?: ci.product.name,
                            quantity = ci.qty.toDouble(),
                            unitPrice = ci.product.sellPrice,
                            subtotal = ci.product.sellPrice * ci.qty,
                            unit = ci.product.unit,
                            isRacikan = true,
                            compoundRecipeId = ci.compoundRecipeId,
                            itemName = ci.compoundRecipeName ?: ci.product.name,
                        )
                    } else {
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

                // ── 2. Deduct local stock (skip racikan sentinel products) ──
                for (ci in cartSnapshot) {
                    if (!ci.isRacikan) {
                        productCacheDao.deductStock(ci.product.id, ci.qty)
                    }
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

    fun recordCashOut(
        shiftId: String,
        branchId: String,
        cashierId: String,
        amount: Double,
        category: String,
        note: String? = null,
        receiptPhotoPath: String? = null,
    ) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val shift = localShiftDao.getShift(shiftId)
            if (shift == null || shift.endedAt != null) {
                _error.value = "Shift sudah ditutup. Kas Keluar tidak bisa ditambahkan."
                return@launch
            }
            localCashExpenseDao.insert(
                LocalCashExpenseEntity(
                    id = UUID.randomUUID().toString(),
                    shiftId = shiftId,
                    branchId = branchId,
                    cashierId = cashierId,
                    amount = amount,
                    category = category.ifBlank { "Lainnya" },
                    note = note?.takeIf { it.isNotBlank() },
                    receiptPhotoPath = receiptPhotoPath?.takeIf { it.isNotBlank() },
                    createdAt = Instant.now().toString(),
                ),
            )
            OfflineSyncScheduler.enqueueCashExpenseSync(appContext)
            _cashOutTotal.value = localCashExpenseDao.getTotalCashOutForShift(shiftId)
            _cashOutHistory.value = localCashExpenseDao.getCashOutForShift(shiftId)
        }
    }

    fun loadCashOutHistory(shiftId: String) {
        viewModelScope.launch {
            _cashOutLoading.value = true
            try {
                _cashOutTotal.value = localCashExpenseDao.getTotalCashOutForShift(shiftId)
                _cashOutHistory.value = localCashExpenseDao.getCashOutForShift(shiftId)
            } finally {
                _cashOutLoading.value = false
            }
        }
    }

    fun updateCashOut(
        id: String,
        shiftId: String,
        actorCashierId: String,
        amount: Double,
        category: String,
        note: String? = null,
        receiptPhotoPath: String? = null,
    ) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val shift = localShiftDao.getShift(shiftId)
            if (shift == null || shift.endedAt != null) {
                _error.value = "Shift sudah ditutup. Kas Keluar tidak bisa diperbarui."
                return@launch
            }
            val existing = localCashExpenseDao.getById(id)
            if (existing == null) {
                _error.value = "Data Kas Keluar tidak ditemukan."
                return@launch
            }
            val newCategory = category.ifBlank { "Lainnya" }
            val newNote = note?.takeIf { it.isNotBlank() }
            val newReceipt = receiptPhotoPath?.takeIf { it.isNotBlank() }
            localCashExpenseDao.updateCashOut(
                id = id,
                amount = amount,
                category = newCategory,
                note = newNote,
                receiptPhotoPath = newReceipt,
            )
            localCashExpenseAuditDao.insert(
                LocalCashExpenseAuditEntity(
                    id = UUID.randomUUID().toString(),
                    cashExpenseId = id,
                    shiftId = shiftId,
                    action = "update",
                    actorCashierId = actorCashierId.ifBlank { "unknown" },
                    oldAmount = existing.amount,
                    newAmount = amount,
                    oldCategory = existing.category,
                    newCategory = newCategory,
                    oldNote = existing.note,
                    newNote = newNote,
                    oldReceiptPhotoPath = existing.receiptPhotoPath,
                    newReceiptPhotoPath = newReceipt,
                    createdAt = Instant.now().toString(),
                ),
            )
            OfflineSyncScheduler.enqueueCashExpenseSync(appContext)
            _cashOutTotal.value = localCashExpenseDao.getTotalCashOutForShift(shiftId)
            _cashOutHistory.value = localCashExpenseDao.getCashOutForShift(shiftId)
        }
    }

    fun deleteCashOut(id: String, shiftId: String, actorCashierId: String) {
        viewModelScope.launch {
            val shift = localShiftDao.getShift(shiftId)
            if (shift == null || shift.endedAt != null) {
                _error.value = "Shift sudah ditutup. Kas Keluar tidak bisa dihapus."
                return@launch
            }
            val existing = localCashExpenseDao.getById(id)
            if (existing == null) {
                _error.value = "Data Kas Keluar tidak ditemukan."
                return@launch
            }
            localCashExpenseDao.deleteById(id)
            localCashExpenseAuditDao.insert(
                LocalCashExpenseAuditEntity(
                    id = UUID.randomUUID().toString(),
                    cashExpenseId = id,
                    shiftId = shiftId,
                    action = "delete",
                    actorCashierId = actorCashierId.ifBlank { "unknown" },
                    oldAmount = existing.amount,
                    oldCategory = existing.category,
                    oldNote = existing.note,
                    oldReceiptPhotoPath = existing.receiptPhotoPath,
                    createdAt = Instant.now().toString(),
                ),
            )
            OfflineSyncScheduler.enqueueCashExpenseSync(appContext)
            _cashOutTotal.value = localCashExpenseDao.getTotalCashOutForShift(shiftId)
            _cashOutHistory.value = localCashExpenseDao.getCashOutForShift(shiftId)
        }
    }

    fun rememberPreferredPrinter(address: String) {
        viewModelScope.launch {
            session.savePreferredBtPrinterAddress(address)
        }
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
                var totalQrisSales = 0.0
                for (tx in transactions) {
                    val paymentRows = localTransactionDao.getPaymentsForTransaction(tx.id)
                    if (paymentRows.isNotEmpty()) {
                        for (p in paymentRows) {
                            val method = p.method.trim().lowercase()
                            if (method == "cash") {
                                totalCashSales += p.amount
                            } else {
                                totalNonCashSales += p.amount
                                if (method == "qris") totalQrisSales += p.amount
                            }
                        }
                    } else {
                        if (tx.paymentMethod == "cash") totalCashSales += tx.grandTotal
                        else totalNonCashSales += tx.grandTotal
                    }
                }

                val totalCashOut = localCashExpenseDao.getTotalCashOutForShift(shift.id)
                val cashOutByCategory = localCashExpenseDao.getCashOutByCategory(shift.id)
                    .map { CashOutByCategory(category = it.category, total = it.total) }
                val topSelling = localTransactionDao.getTopProductsForShift(shift.id, limit = 5)
                    .map { TopSellingProduct(name = it.name, qty = it.qty) }

                val expectedCash = shift.startingCash + totalCashSales - totalCashOut
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
                    totalQrisSales = totalQrisSales,
                    totalCashOut = totalCashOut,
                    totalTransactions = txCount,
                    topSellingProducts = topSelling,
                    cashOutByCategory = cashOutByCategory,
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
                _shiftSoftAlert.value = false
                _shiftHardLock.value = false
                _showCloseShiftDialog.value = false
                _shiftBlocking.value = true // Require new shift to continue

                tryAutoPrintShiftClose(_shiftSummary.value)
            } catch (e: Exception) {
                _shiftDialogError.value = formatApiThrowable(e, gson)
            } finally {
                _closingShift.value = false
            }
        }
    }

    private suspend fun tryAutoPrintShiftOpen(shift: LocalShiftEntity, branchName: String) {
        val preferred = session.getPreferredBtPrinterAddress() ?: return
        val printer = ThermalPrinterManager.getPairedPrinters(appContext)
            .firstOrNull { it.address == preferred }
            ?: return
        val data = ThermalPrinterManager.ShiftOpenReceiptData(
            pharmacyName = "ApotekPOS",
            shiftType = shift.shiftType,
            cashierName = shift.cashierName,
            branchName = branchName,
            startedAt = shift.startedAt,
            startingCash = shift.startingCash,
        )
        val result = ThermalPrinterManager.printShiftOpenSlip(appContext, printer, data)
        result.exceptionOrNull()?.let {
            _error.value = "Shift dibuka, tetapi auto-cetak gagal: ${it.message ?: "printer tidak tersedia"}"
        }
    }

    private suspend fun tryAutoPrintShiftClose(summary: ShiftSummaryData?) {
        val data = summary ?: return
        val preferred = session.getPreferredBtPrinterAddress() ?: return
        val printer = ThermalPrinterManager.getPairedPrinters(appContext)
            .firstOrNull { it.address == preferred }
            ?: return
        val report = ThermalPrinterManager.ShiftReportData(
            pharmacyName = "ApotekPOS",
            shiftType = data.shiftType,
            cashierName = data.cashierName,
            branchName = data.branchName,
            startedAt = data.startedAt,
            endedAt = data.endedAt,
            startingCash = data.startingCash,
            endingCash = data.endingCash,
            expectedCash = data.expectedCash,
            difference = data.difference,
            totalSales = data.totalSales,
            totalCashSales = data.totalCashSales,
            totalNonCashSales = data.totalNonCashSales,
            totalQrisSales = data.totalQrisSales,
            totalCashOut = data.totalCashOut,
            totalTransactions = data.totalTransactions,
            topSellingProducts = data.topSellingProducts.map {
                ThermalPrinterManager.TopSellingLine(name = it.name, qty = it.qty)
            },
        )
        val result = ThermalPrinterManager.printShiftReport(appContext, printer, report)
        result.exceptionOrNull()?.let {
            _error.value = "Shift ditutup, tetapi auto-cetak gagal: ${it.message ?: "printer tidak tersedia"}"
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
