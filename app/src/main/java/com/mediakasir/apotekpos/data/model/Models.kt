package com.mediakasir.apotekpos.data.model

import com.google.gson.annotations.SerializedName

// ===== Auth Models =====

data class LicenseValidateRequest(
    @SerializedName("license_key") val licenseKey: String,
    @SerializedName("device_id") val deviceId: String = ""
)

data class LicenseInfo(
    @SerializedName("branch_id") val branchId: String,
    @SerializedName("branch_name") val branchName: String,
    @SerializedName("pharmacy_name") val pharmacyName: String,
    val address: String = "",
    val phone: String = ""
)

data class LoginRequest(
    val username: String,
    val pin: String
)

data class UserInfo(
    @SerializedName("user_id") val userId: String,
    val username: String,
    @SerializedName("full_name") val fullName: String,
    val role: String,
    @SerializedName("branch_id") val branchId: String,
    val token: String
)

data class ChangePinRequest(
    val username: String,
    @SerializedName("old_pin") val oldPin: String,
    @SerializedName("new_pin") val newPin: String
)

// ===== Product Models =====

data class Product(
    val id: String = "",
    val barcode: String = "",
    val name: String,
    val category: String = "Umum",
    val unit: String = "tablet",
    @SerializedName("sell_price") val sellPrice: Double,
    @SerializedName("buy_price") val buyPrice: Double = 0.0,
    @SerializedName("min_stock") val minStock: Int = 10,
    @SerializedName("branch_id") val branchId: String = "",
    @SerializedName("current_stock") val currentStock: Int = 0,
    @SerializedName("is_active") val isActive: Boolean = true
)

data class ProductCreate(
    val barcode: String = "",
    val name: String,
    val category: String = "Umum",
    val unit: String = "tablet",
    @SerializedName("sell_price") val sellPrice: Double,
    @SerializedName("buy_price") val buyPrice: Double = 0.0,
    @SerializedName("min_stock") val minStock: Int = 10,
    @SerializedName("branch_id") val branchId: String
)

data class ProductsResponse(
    val data: List<Product>,
    val total: Int,
    val page: Int,
    val limit: Int
)

// ===== Batch Models =====

data class Batch(
    val id: String = "",
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String = "",
    @SerializedName("batch_number") val batchNumber: String,
    @SerializedName("expiry_date") val expiryDate: String,
    @SerializedName("current_qty") val currentQty: Int,
    @SerializedName("initial_qty") val initialQty: Int = 0,
    @SerializedName("buy_price") val buyPrice: Double = 0.0,
    @SerializedName("branch_id") val branchId: String = "",
    @SerializedName("is_expired") val isExpired: Boolean = false,
    @SerializedName("is_expiring_soon") val isExpiringSoon: Boolean = false
)

data class BatchCreate(
    @SerializedName("product_id") val productId: String,
    @SerializedName("batch_number") val batchNumber: String,
    @SerializedName("expiry_date") val expiryDate: String,
    @SerializedName("current_qty") val currentQty: Int,
    @SerializedName("buy_price") val buyPrice: Double = 0.0,
    @SerializedName("branch_id") val branchId: String
)

// ===== Transaction Models =====

data class TransactionItemInput(
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String,
    val qty: Int,
    val unit: String,
    @SerializedName("sell_price") val sellPrice: Double
)

data class PaymentDetailInput(
    val method: String,
    val amount: Double,
    val reference: String = ""
)

data class TransactionCreate(
    @SerializedName("branch_id") val branchId: String,
    @SerializedName("branch_name") val branchName: String,
    @SerializedName("cashier_id") val cashierId: String,
    @SerializedName("cashier_name") val cashierName: String,
    val items: List<TransactionItemInput>,
    val discount: Double = 0.0,
    @SerializedName("payment_details") val paymentDetails: List<PaymentDetailInput>,
    val notes: String = ""
)

data class TransactionItem(
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String,
    val qty: Int,
    val unit: String,
    @SerializedName("sell_price") val sellPrice: Double,
    val subtotal: Double
)

data class PaymentDetail(
    val method: String,
    val amount: Double,
    val reference: String = ""
)

data class Transaction(
    val id: String = "",
    @SerializedName("transaction_number") val transactionNumber: String = "",
    @SerializedName("branch_id") val branchId: String = "",
    @SerializedName("branch_name") val branchName: String = "",
    @SerializedName("cashier_name") val cashierName: String = "",
    val items: List<TransactionItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    @SerializedName("total_amount") val totalAmount: Double = 0.0,
    @SerializedName("payment_details") val paymentDetails: List<PaymentDetail> = emptyList(),
    @SerializedName("total_paid") val totalPaid: Double = 0.0,
    val change: Double = 0.0,
    val notes: String = "",
    @SerializedName("created_at") val createdAt: String = ""
)

data class TransactionsResponse(
    val data: List<Transaction>,
    val total: Int,
    val page: Int,
    val limit: Int
)

// ===== Dashboard Models =====

data class DashboardData(
    @SerializedName("today_revenue") val todayRevenue: Double,
    @SerializedName("today_transactions") val todayTransactions: Int,
    @SerializedName("total_products") val totalProducts: Int,
    @SerializedName("low_stock_count") val lowStockCount: Int,
    @SerializedName("expiring_count") val expiringCount: Int
)

data class AlertData(
    @SerializedName("expired_batches") val expiredBatches: List<Batch>,
    @SerializedName("expiring_batches") val expiringBatches: List<Batch>,
    @SerializedName("low_stock_products") val lowStockProducts: List<LowStockProduct>
)

data class LowStockProduct(
    val id: String,
    val name: String,
    @SerializedName("current_stock") val currentStock: Int,
    @SerializedName("min_stock") val minStock: Int
)

data class MessageResponse(val message: String)
