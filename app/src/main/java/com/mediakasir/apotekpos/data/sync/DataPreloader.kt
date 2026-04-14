package com.mediakasir.apotekpos.data.sync

import com.mediakasir.apotekpos.data.local.LocalTransactionDao
import com.mediakasir.apotekpos.data.local.ProductCacheDao
import com.mediakasir.apotekpos.data.local.toCachedEntity
import com.mediakasir.apotekpos.data.network.ApiService
import com.mediakasir.apotekpos.util.NetworkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class PreloadProgress(
    val phase: String = "",
    val current: Int = 0,
    val total: Int = 0,
    val isActive: Boolean = false,
)

/**
 * Progressive background data loader.
 * Phase 1 (instant): load from Room cache — handled by POSViewModel.loadProducts()
 * Phase 2 (background): incremental product sync using updated_since
 * Phase 3 (lazy): full catalog sync page-by-page
 */
@Singleton
class DataPreloader @Inject constructor(
    private val api: ApiService,
    private val productCacheDao: ProductCacheDao,
    private val networkStatus: NetworkStatus,
    private val localTransactionDao: LocalTransactionDao,
) {
    private val _progress = MutableStateFlow(PreloadProgress())
    val progress: StateFlow<PreloadProgress> = _progress.asStateFlow()

    /**
     * Run incremental product sync in background.
     * Uses updated_since from last sync timestamp to only fetch changed products.
     * Falls back to full sync if no cache exists yet.
     */
    suspend fun syncProducts(branchId: String) {
        if (!networkStatus.isConnected()) return

        _progress.value = PreloadProgress(phase = "Memperbarui produk...", isActive = true)
        try {
            val lastSync = productCacheDao.getLastSyncTime(branchId)
            val updatedSince = lastSync?.let {
                Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT)
            }

            val now = System.currentTimeMillis()
            var page = 1
            var totalSynced = 0
            val maxPages = 25

            while (page <= maxPages) {
                val env = api.syncProducts(page = page, perPage = 100, updatedSince = updatedSince)
                if (env.success != true) break
                val payload = env.data ?: break
                val products = payload.products
                if (products.isNotEmpty()) {
                    // Upsert (not clearBranch) — keep existing products, update changed ones
                    productCacheDao.insertAll(products.map { it.toCachedEntity(branchId, now) })
                    totalSynced += products.size
                }
                _progress.value = PreloadProgress(
                    phase = "Memperbarui produk...",
                    current = totalSynced,
                    total = payload.pagination?.totalItems ?: totalSynced,
                    isActive = true,
                )
                val p = payload.pagination
                val hasMore = p?.hasMore == true ||
                    ((p?.currentPage ?: page) < (p?.totalPages ?: 0))
                if (!hasMore) break
                page++
            }

            // Re-apply pending local deductions so products sold but not yet confirmed by server
            // are not shown as available (prevents phantom stock after offline transactions).
            if (totalSynced > 0) {
                val deductions = localTransactionDao.getPendingDeductionsPerProduct()
                for (d in deductions) {
                    productCacheDao.deductStock(d.productId.toString(), d.totalQty)
                }
            }
        } catch (_: Exception) {
            // Background sync — fail silently
        } finally {
            _progress.value = PreloadProgress(isActive = false)
        }
    }

    /** Sync discounts & promotions in background. */
    suspend fun syncMarketingData(): Pair<Boolean, Boolean> {
        if (!networkStatus.isConnected()) return false to false
        var discountsOk = false
        var promotionsOk = false
        try {
            val d = api.getDiscounts()
            discountsOk = d.success == true
        } catch (_: Exception) {}
        try {
            val p = api.getPromotions()
            promotionsOk = p.success == true
        } catch (_: Exception) {}
        return discountsOk to promotionsOk
    }
}
