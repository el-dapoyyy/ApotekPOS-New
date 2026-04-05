package com.mediakasir.apotekpos.ui.main.stok

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mediakasir.apotekpos.data.local.ProductCacheDao
import com.mediakasir.apotekpos.data.local.toCachedEntity
import com.mediakasir.apotekpos.data.local.toProductModel
import com.mediakasir.apotekpos.data.model.Batch
import com.mediakasir.apotekpos.data.model.PosProductDto
import com.mediakasir.apotekpos.data.model.Product
import com.mediakasir.apotekpos.data.model.toBatch
import com.mediakasir.apotekpos.data.model.toProduct
import com.mediakasir.apotekpos.data.network.ApiService
import com.mediakasir.apotekpos.util.NetworkStatus
import com.mediakasir.apotekpos.util.formatApiThrowable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StokViewModel @Inject constructor(
    private val api: ApiService,
    private val productCacheDao: ProductCacheDao,
    private val networkStatus: NetworkStatus,
    private val gson: Gson,
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _batches = MutableStateFlow<List<Batch>>(emptyList())
    val batches: StateFlow<List<Batch>> = _batches.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _productsLoadError = MutableStateFlow<String?>(null)
    val productsLoadError: StateFlow<String?> = _productsLoadError.asStateFlow()

    fun loadProducts(branchId: String, search: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
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
                        val msg = "Offline — tidak ada cache untuk cabang ini."
                        _productsLoadError.value = msg
                        _error.value = msg
                    }
                    return@launch
                }
                _products.value = fetchAndCache(branchId, search)
            } catch (e: Exception) {
                val msg = formatApiThrowable(e, gson)
                _productsLoadError.value = msg
                if (_products.value.isEmpty()) {
                    _error.value = msg
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchAndCache(branchId: String, search: String): List<Product> {
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

    fun loadBatches(branchId: String, productId: String) {
        viewModelScope.launch {
            try {
                val id = productId.toIntOrNull()
                    ?: throw IllegalStateException("ID produk tidak valid")
                val env = api.getProductDetail(id)
                if (env.success != true || env.data == null) {
                    throw IllegalStateException(env.message ?: "Gagal memuat batch")
                }
                val d = env.data
                _batches.value = d.batches.orEmpty().map { it.toBatch(d.id.toString(), d.name, branchId) }
            } catch (e: Exception) {
                _error.value = formatApiThrowable(e, gson)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
