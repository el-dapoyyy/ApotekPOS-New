package com.mediakasir.apotekpos.ui.main.pos

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

data class CartItem(
    val product: Product,
    val qty: Int
)

data class PaymentEntry(
    val id: Long = System.currentTimeMillis(),
    val method: String = "Tunai",
    val amount: String = "",
    val reference: String = ""
)

@HiltViewModel
class POSViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _payments = MutableStateFlow(listOf(PaymentEntry()))
    val payments: StateFlow<List<PaymentEntry>> = _payments.asStateFlow()

    private val _discount = MutableStateFlow("0")
    val discount: StateFlow<String> = _discount.asStateFlow()

    private val _isLoadingProducts = MutableStateFlow(false)
    val isLoadingProducts: StateFlow<Boolean> = _isLoadingProducts.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _receipt = MutableStateFlow<Transaction?>(null)
    val receipt: StateFlow<Transaction?> = _receipt.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadProducts(branchId: String, search: String = "") {
        viewModelScope.launch {
            _isLoadingProducts.value = true
            try {
                val res = api.getProducts(branchId, search)
                _products.value = res.data
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoadingProducts.value = false
            }
        }
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

    fun setDiscount(d: String) { _discount.value = d }

    fun addPayment() {
        _payments.value = _payments.value + PaymentEntry(method = "Transfer")
    }

    fun removePayment(id: Long) {
        _payments.value = _payments.value.filter { it.id != id }
    }

    fun updatePayment(id: Long, method: String? = null, amount: String? = null) {
        _payments.value = _payments.value.map {
            if (it.id == id) it.copy(
                method = method ?: it.method,
                amount = amount ?: it.amount
            ) else it
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
                val res = api.createTransaction(
                    TransactionCreate(
                        branchId = branchId,
                        branchName = branchName,
                        cashierId = cashierId,
                        cashierName = cashierName,
                        items = _cart.value.map {
                            TransactionItemInput(
                                productId = it.product.id,
                                productName = it.product.name,
                                qty = it.qty,
                                unit = it.product.unit,
                                sellPrice = it.product.sellPrice
                            )
                        },
                        discount = getDiscountAmt(),
                        paymentDetails = _payments.value
                            .filter { (it.amount.toDoubleOrNull() ?: 0.0) > 0 }
                            .map { PaymentDetailInput(it.method.lowercase(), it.amount.toDouble()) }
                    )
                )
                _receipt.value = res
                clearCart()
                loadProducts(branchId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Transaksi gagal"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _payments.value = listOf(PaymentEntry())
        _discount.value = "0"
    }

    fun dismissReceipt() { _receipt.value = null }
    fun clearError() { _error.value = null }
}
