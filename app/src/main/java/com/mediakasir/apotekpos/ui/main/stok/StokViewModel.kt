package com.mediakasir.apotekpos.ui.main.stok

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediakasir.apotekpos.data.model.*
import com.mediakasir.apotekpos.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StokViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _batches = MutableStateFlow<List<Batch>>(emptyList())
    val batches: StateFlow<List<Batch>> = _batches.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()

    fun loadProducts(branchId: String, search: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = api.getProducts(branchId, search)
                _products.value = res.data
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadBatches(branchId: String, productId: String) {
        viewModelScope.launch {
            try {
                _batches.value = api.getBatches(branchId, productId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun createProduct(data: ProductCreate, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                api.createProduct(data)
                _success.value = "Produk berhasil ditambahkan"
                onDone()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateProduct(id: String, data: ProductCreate, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                api.updateProduct(id, data)
                _success.value = "Produk berhasil diperbarui"
                onDone()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteProduct(id: String, branchId: String, search: String = "") {
        viewModelScope.launch {
            try {
                api.deleteProduct(id)
                _success.value = "Produk berhasil dihapus"
                loadProducts(branchId, search)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun createBatch(data: BatchCreate, branchId: String, productId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                api.createBatch(data)
                _success.value = "Batch berhasil ditambahkan"
                loadBatches(branchId, productId)
                onDone()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateBatch(id: String, data: BatchCreate, branchId: String, productId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                api.updateBatch(id, data)
                _success.value = "Batch berhasil diperbarui"
                loadBatches(branchId, productId)
                onDone()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteBatch(id: String, branchId: String, productId: String) {
        viewModelScope.launch {
            try {
                api.deleteBatch(id)
                _success.value = "Batch berhasil dihapus"
                loadBatches(branchId, productId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearSuccess() { _success.value = null }
}
