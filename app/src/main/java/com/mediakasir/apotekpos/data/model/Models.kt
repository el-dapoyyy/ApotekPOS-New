package com.mediakasir.apotekpos.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

// ===== Auth Models =====

// --- Login Request (POST /api/auth/login): email, password, device_id wajib; nama/model perangkat opsional. ---
data class LoginRequest(
    val email: String,
    val password: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String? = null,
    @SerializedName("device_model") val deviceModel: String? = null
)

/** POST `auth/google` — backend must verify `id_token` (Google OAuth Web client). */
data class GoogleLoginRequest(
    @SerializedName("id_token") val idToken: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String? = null,
    @SerializedName("device_model") val deviceModel: String? = null
)

// --- Login Response (nested) ---
data class LoginUserData(
    val id: Int,
    val name: String,
    val email: String,
    val role: String
)

data class LoginBranchData(
    val id: Int,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    @SerializedName("sia_number") val siaNumber: String? = null,
    @SerializedName("apoteker_name") val apotekerName: String? = null,
    @SerializedName("sipa_number") val sipaNumber: String? = null
)

data class LoginPartnerData(
    val id: Int,
    val name: String
)

data class LoginLicenseData(
    val status: String,
    @SerializedName("expired_at") val expiredAt: String,
    @SerializedName("days_remaining") val daysRemaining: Double,
    @SerializedName("is_trial") val isTrial: Boolean
)

/** Baris `user_devices` di server — `id` dipakai header `X-Device-ID` / FK `sync_logs.device_id`. */
data class UserDeviceSummary(
    val id: Int? = null,
    @SerializedName("device_id") val deviceFingerprint: String? = null,
)

data class LoginResponseData(
    val user: LoginUserData,
    val branch: LoginBranchData?,
    val partner: LoginPartnerData,
    val license: LoginLicenseData,
    val token: String,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("user_device_id") val userDeviceId: Int? = null,
    @SerializedName("user_device") val userDevice: UserDeviceSummary? = null,
    @SerializedName("device") val device: UserDeviceSummary? = null,
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: LoginResponseData
)

// --- GET /api/auth/me ---
data class AuthMeEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: AuthMeData? = null,
)

data class AuthMeData(
    val user: AuthMeUser? = null,
    val branch: LoginBranchData? = null,
    @SerializedName("active_shift") val activeShift: ActiveShiftInfo? = null,
    @SerializedName("user_device_id") val userDeviceId: Int? = null,
    @SerializedName("user_device") val userDevice: UserDeviceSummary? = null,
    @SerializedName("device") val device: UserDeviceSummary? = null,
)

/** PK `user_devices.id` untuk header `X-Device-ID` (FK `sync_logs.device_id`). */
object DeviceHeaderIds {
    fun fromLogin(d: LoginResponseData): Int? =
        d.userDeviceId ?: d.userDevice?.id ?: d.device?.id

    fun fromMe(d: AuthMeData): Int? =
        d.userDeviceId ?: d.userDevice?.id ?: d.device?.id
}

data class AuthMeUser(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
)

data class ActiveShiftInfo(
    val id: Int,
    @SerializedName("clock_in") val clockIn: String,
    @SerializedName("starting_cash") val startingCash: Double,
)

/** POST buka shift — path backend bisa disesuaikan jika berbeda. */
data class ShiftStartRequest(
    @SerializedName("starting_cash") val startingCash: Double,
)

data class ShiftStartEnvelope(
    val success: Boolean = false,
    val message: String? = null,
)

// --- Session Models (disimpan di DataStore) ---
data class UserInfo(
    val userId: String,
    val name: String,
    val email: String,
    val role: String,
    val branchId: String,
    val branchName: String,
    val partnerName: String,
    val token: String
)

data class LicenseInfo(
    @SerializedName("branch_id") val branchId: String,
    @SerializedName("branch_name") val branchName: String,
    @SerializedName("pharmacy_name") val pharmacyName: String,
    val address: String = "",
    val phone: String = "",
    val status: String = "active",
    @SerializedName("expired_at") val expiredAt: String = "",
    @SerializedName("days_remaining") val daysRemaining: Double = 0.0,
    @SerializedName("is_trial") val isTrial: Boolean = false
)

private fun licenseExpiredByDate(expiredAtRaw: String): Boolean {
    val s = expiredAtRaw.trim()
    if (s.length < 10) return false
    val parsed = runCatching { LocalDate.parse(s.take(10)) }.getOrNull() ?: return false
    return parsed.isBefore(LocalDate.now())
}

private fun licenseStatusBlocks(statusRaw: String): Boolean {
    val s = statusRaw.trim().lowercase()
    return s.isNotEmpty() && s != "active"
}

/**
 * Backend biasanya mengembalikan `success: false` + `LICENSE_EXPIRED` (§ login API).
 * Ini pertahanan tambahan jika respons sukses tapi field lisensi tidak konsisten.
 */
fun LoginLicenseData.blocksLogin(): Boolean =
    licenseExpiredByDate(expiredAt) ||
        daysRemaining < 0 ||
        licenseStatusBlocks(status)

/** Sesi tersimpan: keluarkan pengguna jika lisensi tidak lagi valid. */
fun LicenseInfo.blocksAppUse(): Boolean =
    licenseExpiredByDate(expiredAt) ||
        daysRemaining < 0 ||
        licenseStatusBlocks(status)

data class ChangePinRequest(
    val email: String,
    @SerializedName("old_password") val oldPassword: String,
    @SerializedName("new_password") val newPassword: String
)

// Kept for backward compat if license/validate endpoint still used
data class LicenseValidateRequest(
    @SerializedName("license_key") val licenseKey: String,
    @SerializedName("device_id") val deviceId: String = ""
)

// ===== Product Models =====

data class Product(
    val id: String = "",
    val sku: String = "",
    val barcode: String = "",
    val name: String,
    val category: String = "Umum",
    val unit: String = "tablet",
    @SerializedName("sell_price") val sellPrice: Double,
    @SerializedName("buy_price") val buyPrice: Double = 0.0,
    @SerializedName("min_stock") val minStock: Int = 10,
    @SerializedName("branch_id") val branchId: String = "",
    @SerializedName("current_stock") val currentStock: Int = 0,
    @SerializedName("is_active") val isActive: Boolean = true,
    /** Label untuk menampilkan diskon (e.g. "Diskon 20%" atau "Potongan Rp50.000"). */
    val discountLabel: String? = null,
    /** Label untuk menampilkan promo (e.g. "Beli 2 dapat 1"). */
    val promoLabel: String? = null,
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
    @SerializedName("created_at") val createdAt: String = "",
    /** From POS history API when item list is not loaded */
    @SerializedName("items_count") val listItemsCount: Int = 0,
    /** True when checkout disimpan lokal menunggu sinkron ke server */
    val isPendingSync: Boolean = false,
) {
    fun displayItemCount(): Int = if (items.isNotEmpty()) items.size else listItemsCount
}

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
    @SerializedName("alert_id") val alertId: String = "",
    val name: String,
    @SerializedName("current_stock") val currentStock: Int,
    @SerializedName("min_stock") val minStock: Int
)

data class MessageResponse(val message: String)
