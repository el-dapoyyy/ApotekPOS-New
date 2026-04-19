package com.mediakasir.apotekpos.data.model

import com.google.gson.annotations.SerializedName

// --- Generic helpers ---

fun <T> requireApiSuccess(envelope: ApiMessageEnvelope, data: T?, message: String? = null): T {
    if (envelope.success != true || data == null) {
        throw IllegalStateException(message ?: envelope.message ?: "Permintaan gagal")
    }
    return data
}

data class ApiMessageEnvelope(
    val success: Boolean = false,
    val message: String? = null,
)

// --- Products: sync ---

data class ProductSyncEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: ProductSyncPayload? = null,
)

data class ProductSyncPayload(
    val products: List<PosProductDto> = emptyList(),
    val pagination: PosPagination? = null,
    @SerializedName("sync_info") val syncInfo: PosSyncInfo? = null,
)

data class PosPagination(
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null,
    @SerializedName("total_items") val totalItems: Int? = null,
    @SerializedName("total_pages") val totalPages: Int? = null,
    @SerializedName("has_more") val hasMore: Boolean? = null,
)

data class PosSyncInfo(
    @SerializedName("server_time") val serverTime: String? = null,
    @SerializedName("next_sync_token") val nextSyncToken: String? = null,
)

data class PosStockDto(
    val total: Int? = 0,
    @SerializedName("is_low") val isLow: Boolean? = false,
)

data class PosNearestExpiryDto(
    @SerializedName("batch_number") val batchNumber: String? = null,
    @SerializedName("expired_date") val expiredDate: String? = null,
    val quantity: Int? = null,
)

data class PosProductDto(
    val id: Int,
    val sku: String? = null,
    val barcode: String? = null,
    val name: String,
    @SerializedName("generic_name") val genericName: String? = null,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("unit_id") val unitId: Int? = null,
    @SerializedName("unit_name") val unitName: String? = null,
    @SerializedName("unit_abbreviation") val unitAbbreviation: String? = null,
    @SerializedName("brand_name") val brandName: String? = null,
    @SerializedName("purchase_price") val purchasePrice: Double? = 0.0,
    @SerializedName("selling_price") val sellingPrice: Double = 0.0,
    @SerializedName("min_stock") val minStock: Int = 0,
    val stock: PosStockDto? = null,
    @SerializedName("nearest_expiry") val nearestExpiry: PosNearestExpiryDto? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    /** List of active discounts for this product (calculated by backend). */
    val discounts: List<Map<String, Any>>? = null,
    /** List of active promotions for this product. */
    val promotions: List<Map<String, Any>>? = null,
)

fun PosProductDto.toProduct(branchId: String): Product {
    val code = sku?.takeIf { it.isNotBlank() }
        ?: barcode?.takeIf { it.isNotBlank() }
        ?: "OBT-$id"
    val discountLabel = discounts?.firstOrNull()?.let { first ->
        val type = first["discount_type"]?.toString()?.lowercase().orEmpty()
        val value = first["discount_value"]?.toString()?.trim().orEmpty()
        when {
            type == "percentage" && value.isNotEmpty() -> "Diskon $value%"
            type == "fixed" && value.isNotEmpty() -> "Potongan Rp$value"
            else -> first["name"]?.toString()?.takeIf { it.isNotBlank() } ?: "Diskon"
        }
    }
    val promoLabel = promotions?.firstOrNull()?.let { first ->
        first["name"]?.toString()?.takeIf { it.isNotBlank() } ?: "Promo"
    }

    val expDateStr = nearestExpiry?.expiredDate?.take(10)
    val expDate = expDateStr?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
    val today = java.time.LocalDate.now()
    val isExpired = expDate != null && expDate.isBefore(today)
    val isExpiringSoon = expDate != null && !isExpired &&
        java.time.temporal.ChronoUnit.DAYS.between(today, expDate) <= 90

    return Product(
        id = id.toString(),
        sku = code,
        barcode = barcode.orEmpty(),
        name = name,
        category = categoryName ?: "Umum",
        unit = unitName ?: unitAbbreviation ?: "unit",
        sellPrice = sellingPrice,
        buyPrice = purchasePrice ?: 0.0,
        minStock = minStock,
        branchId = branchId,
        currentStock = stock?.total ?: 0,
        isActive = true,
        discountLabel = discountLabel,
        promoLabel = promoLabel,
        nearestExpiryDate = expDateStr,
        isNearestExpired = isExpired,
        isNearestExpiringSoon = isExpiringSoon,
    )
}

// --- Products: search ---

data class ProductSearchEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: ProductSearchPayload? = null,
)

data class ProductSearchPayload(
    val products: List<PosProductDto> = emptyList(),
    val total: Int? = 0,
)

// --- Products: detail (batches) ---

data class ProductDetailEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: PosProductDetailDto? = null,
)

data class PosProductDetailDto(
    val id: Int,
    val sku: String? = null,
    val barcode: String? = null,
    val name: String,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("unit_name") val unitName: String? = null,
    @SerializedName("unit_abbreviation") val unitAbbreviation: String? = null,
    @SerializedName("purchase_price") val purchasePrice: Double? = 0.0,
    @SerializedName("selling_price") val sellingPrice: Double = 0.0,
    @SerializedName("min_stock") val minStock: Int = 0,
    val stock: PosStockDto? = null,
    val batches: List<PosBatchDto>? = null,
)

data class PosBatchDto(
    val id: Int,
    @SerializedName("batch_number") val batchNumber: String,
    @SerializedName("expired_date") val expiredDate: String,
    val quantity: Int,
    @SerializedName("purchase_price") val purchasePrice: Double? = 0.0,
)

fun PosBatchDto.toBatch(productId: String, productName: String, branchId: String): Batch {
    val today = java.time.LocalDate.now()
    val exp = runCatching { java.time.LocalDate.parse(expiredDate.take(10)) }.getOrNull()
    val expired = exp != null && exp.isBefore(today)
    val soon = exp != null && !expired && java.time.temporal.ChronoUnit.DAYS.between(today, exp) <= 90
    return Batch(
        id = id.toString(),
        productId = productId,
        productName = productName,
        batchNumber = batchNumber,
        expiryDate = expiredDate,
        currentQty = quantity,
        initialQty = quantity,
        buyPrice = purchasePrice ?: 0.0,
        branchId = branchId,
        isExpired = expired,
        isExpiringSoon = soon,
    )
}

// --- Transactions: sync ---

data class TransactionSyncRequest(
    val transactions: List<PosTransactionSyncDto>,
)

data class PosTransactionSyncDto(
    @SerializedName("local_transaction_id") val localTransactionId: String,
    @SerializedName("customer_id") val customerId: Int? = null,
    @SerializedName("prescription_id") val prescriptionId: Int? = null,
    @SerializedName("discount_id") val discountId: Int? = null,
    val subtotal: Double,
    @SerializedName("discount_amount") val discountAmount: Double = 0.0,
    @SerializedName("tax_amount") val taxAmount: Double = 0.0,
    @SerializedName("tusla_amount") val tuslaAmount: Double = 0.0,
    @SerializedName("embalse_amount") val embalseAmount: Double = 0.0,
    @SerializedName("grand_total") val grandTotal: Double,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("payment_status") val paymentStatus: String = "paid",
    val notes: String? = null,
    @SerializedName("completed_at") val completedAt: String? = null,
    val items: List<PosTransactionLineDto>,
    val payments: List<PosPaymentLineDto>? = null,
)
data class RacikanIngredientDto(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("quantity_needed") val quantityNeeded: Double,
)

data class PosTransactionLineDto(
    @SerializedName("product_id") val productId: Int? = null,
    @SerializedName("batch_id") val batchId: Int? = null,
    val quantity: Double,
    @SerializedName("unit_price") val unitPrice: Double,
    val discount: Double = 0.0,
    val subtotal: Double,
    @SerializedName("is_racikan") val isRacikan: Boolean? = false,
    @SerializedName("compound_recipe_id") val compoundRecipeId: Int? = null,
    @SerializedName("item_name") val itemName: String? = null,
    val ingredients: List<RacikanIngredientDto>? = null,
)

// --- Compound Recipes (Racikan) ---

data class CompoundRecipeItemDto(
    val id: Int,
    @SerializedName("product_id") val productId: Int? = null,
    @SerializedName("product_name") val productName: String,
    @SerializedName("quantity_needed") val quantityNeeded: Double,
    @SerializedName("unit_price") val unitPrice: Double,
    val subtotal: Double,
)

data class CompoundRecipeDto(
    val id: Int,
    val name: String,
    @SerializedName("aturan_pakai") val aturanPakai: String? = null,
    @SerializedName("quantity_produced") val quantityProduced: Int = 1,
    @SerializedName("total_price") val totalPrice: Double,
    @SerializedName("tusla_amount") val tuslaAmount: Double = 0.0,
    @SerializedName("embalse_amount") val embalseAmount: Double = 0.0,
    @SerializedName("is_active") val isActive: Boolean = true,
    val items: List<CompoundRecipeItemDto> = emptyList(),
)

data class CompoundRecipesEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: List<CompoundRecipeDto>? = null,
)

data class PosPaymentLineDto(
    val method: String,
    val amount: Double,
    val reference: String? = null,
)

data class TransactionSyncEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: TransactionSyncPayload? = null,
)

data class TransactionSyncPayload(
    val results: List<TransactionSyncResult>? = null,
    val summary: TransactionSyncSummary? = null,
    @SerializedName("server_time") val serverTime: String? = null,
)

data class TransactionSyncResult(
    @SerializedName("local_transaction_id") val localTransactionId: String? = null,
    @SerializedName("server_transaction_id") val serverTransactionId: Int? = null,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    val success: Boolean? = null,
    val message: String? = null,
    val status: String? = null,
)

data class TransactionSyncSummary(
    val total: Int? = null,
    val success: Int? = null,
    val failed: Int? = null,
)

// --- Cash expenses: sync ---

data class CashExpenseSyncRequest(
    val expenses: List<CashExpenseSyncDto> = emptyList(),
    val audits: List<CashExpenseAuditSyncDto> = emptyList(),
)

data class CashExpenseSyncDto(
    @SerializedName("local_cash_expense_id") val localCashExpenseId: String,
    @SerializedName("server_cash_expense_id") val serverCashExpenseId: Int? = null,
    @SerializedName("shift_id") val shiftId: String,
    @SerializedName("branch_id") val branchId: String,
    @SerializedName("cashier_id") val cashierId: String,
    val amount: Double,
    val category: String,
    val note: String? = null,
    @SerializedName("receipt_photo_path") val receiptPhotoPath: String? = null,
    @SerializedName("created_at") val createdAt: String,
)

data class CashExpenseAuditSyncDto(
    @SerializedName("local_audit_id") val localAuditId: String,
    @SerializedName("server_audit_id") val serverAuditId: Int? = null,
    @SerializedName("cash_expense_id") val cashExpenseId: String,
    @SerializedName("shift_id") val shiftId: String,
    val action: String,
    @SerializedName("actor_cashier_id") val actorCashierId: String,
    @SerializedName("old_amount") val oldAmount: Double,
    @SerializedName("new_amount") val newAmount: Double? = null,
    @SerializedName("old_category") val oldCategory: String,
    @SerializedName("new_category") val newCategory: String? = null,
    @SerializedName("old_note") val oldNote: String? = null,
    @SerializedName("new_note") val newNote: String? = null,
    @SerializedName("old_receipt_photo_path") val oldReceiptPhotoPath: String? = null,
    @SerializedName("new_receipt_photo_path") val newReceiptPhotoPath: String? = null,
    @SerializedName("created_at") val createdAt: String,
)

data class CashExpenseSyncEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: CashExpenseSyncPayload? = null,
)

data class CashExpenseSyncPayload(
    @SerializedName("expense_results") val expenseResults: List<CashExpenseSyncResult>? = null,
    @SerializedName("audit_results") val auditResults: List<CashExpenseAuditSyncResult>? = null,
)

data class CashExpenseSyncResult(
    @SerializedName("local_cash_expense_id") val localCashExpenseId: String? = null,
    @SerializedName("server_cash_expense_id") val serverCashExpenseId: Int? = null,
    val success: Boolean? = null,
    val message: String? = null,
)

data class CashExpenseAuditSyncResult(
    @SerializedName("local_audit_id") val localAuditId: String? = null,
    @SerializedName("server_audit_id") val serverAuditId: Int? = null,
    val success: Boolean? = null,
    val message: String? = null,
)

// --- Transactions: history ---

data class TransactionHistoryEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: TransactionHistoryPayload? = null,
)

data class TransactionHistoryPayload(
    val transactions: List<TransactionHistoryRowDto>? = emptyList(),
    val pagination: PosPagination? = null,
)

data class TransactionHistoryRowDto(
    val id: Int,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    @SerializedName("local_transaction_id") val localTransactionId: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("grand_total") val grandTotal: Double? = 0.0,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("items_count") val itemsCount: Int? = 0,
    @SerializedName("completed_at") val completedAt: String? = null,
    @SerializedName("synced_at") val syncedAt: String? = null,
)

fun TransactionHistoryRowDto.toHistoryRowTransaction(): Transaction = Transaction(
    id = id.toString(),
    transactionNumber = invoiceNumber.orEmpty(),
    branchId = "",
    branchName = "",
    cashierName = customerName ?: "-",
    items = emptyList(),
    subtotal = grandTotal ?: 0.0,
    discount = 0.0,
    totalAmount = grandTotal ?: 0.0,
    paymentDetails = emptyList(),
    totalPaid = grandTotal ?: 0.0,
    change = 0.0,
    notes = "",
    createdAt = completedAt ?: syncedAt ?: "",
    listItemsCount = itemsCount ?: 0,
)


// --- Transaction detail ---

data class TransactionDetailEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: TransactionDetailDto? = null,
)

data class TransactionDetailDto(
    val id: Int,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    @SerializedName("local_transaction_id") val localTransactionId: String? = null,
    @SerializedName("subtotal") val subtotal: Double? = 0.0,
    @SerializedName("discount_amount") val discountAmount: Double? = 0.0,
    @SerializedName("tax_amount") val taxAmount: Double? = 0.0,
    @SerializedName("grand_total") val grandTotal: Double? = 0.0,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    val items: List<TransactionDetailItemDto>? = emptyList(),
    val payments: List<PosPaymentLineDto>? = emptyList(),
    @SerializedName("completed_at") val completedAt: String? = null,
    val notes: String? = null,
)

data class TransactionDetailItemDto(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String,
    val sku: String? = null,
    @SerializedName("batch_number") val batchNumber: String? = null,
    @SerializedName("expired_date") val expiredDate: String? = null,
    val quantity: Double,
    @SerializedName("unit_price") val unitPrice: Double,
    val discount: Double? = 0.0,
    val subtotal: Double,
)

fun TransactionDetailDto.toDetailTransaction(): Transaction {
    val itemsUi = items.orEmpty().map {
        TransactionItem(
            productId = it.productId.toString(),
            productName = it.productName,
            qty = it.quantity.toInt().coerceAtLeast(1),
            unit = "pcs",
            sellPrice = it.unitPrice,
            subtotal = it.subtotal,
        )
    }
    val pay = payments.orEmpty().map {
        PaymentDetail(method = it.method, amount = it.amount, reference = it.reference.orEmpty())
    }
    val totalPaid = pay.sumOf { it.amount }
    return Transaction(
        id = id.toString(),
        transactionNumber = invoiceNumber.orEmpty(),
        branchId = "",
        branchName = "",
        cashierName = "-",
        items = itemsUi,
        subtotal = subtotal ?: 0.0,
        discount = discountAmount ?: 0.0,
        totalAmount = grandTotal ?: 0.0,
        paymentDetails = pay,
        totalPaid = totalPaid,
        change = (totalPaid - (grandTotal ?: 0.0)).coerceAtLeast(0.0),
        notes = notes.orEmpty(),
        createdAt = completedAt.orEmpty(),
    )
}

data class AlertsSummaryEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: AlertsSummaryData? = null,
)

data class AlertsSummaryData(
    val expiry: ExpirySummaryCounts? = null,
    val stock: StockSummaryCounts? = null,
)

data class ExpirySummaryCounts(
    val total: Int? = 0,
    val critical: Int? = 0,
    val warning: Int? = 0,
    val notice: Int? = 0,
    val unacknowledged: Int? = 0,
)

data class StockSummaryCounts(
    val total: Int? = 0,
    @SerializedName("out_of_stock") val outOfStock: Int? = 0,
    @SerializedName("low_stock") val lowStock: Int? = 0,
    val unacknowledged: Int? = 0,
)

data class ExpiryAlertsEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: ExpiryAlertsPage? = null,
)

data class ExpiryAlertsPage(
    @SerializedName("current_page") val currentPage: Int? = null,
    val data: List<ExpiryAlertRow>? = null,
    @SerializedName("per_page") val perPage: Int? = null,
    val total: Int? = null,
)

data class ExpiryAlertRow(
    val id: Int,
    @SerializedName("batch_number") val batchNumber: String? = null,
    @SerializedName("expired_date") val expiredDate: String? = null,
    val quantity: Int? = 0,
    @SerializedName("alert_level") val alertLevel: String? = null,
    @SerializedName("is_expired") val isExpired: Boolean? = false,
    val product: ExpiryAlertProduct? = null,
)

data class ExpiryAlertProduct(
    val id: Int,
    val name: String? = null,
)

data class StockAlertsEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: StockAlertsPage? = null,
)

data class StockAlertsPage(
    @SerializedName("current_page") val currentPage: Int? = null,
    val data: List<StockAlertRow>? = null,
    val total: Int? = null,
)

data class StockAlertRow(
    val id: Int,
    @SerializedName("product_id") val productId: Int? = null,
    @SerializedName("current_stock") val currentStock: Int? = 0,
    @SerializedName("min_stock") val minStock: Int? = 0,
    val product: ExpiryAlertProduct? = null,
)

data class AcknowledgeMultipleRequest(
    @SerializedName("alert_ids") val alertIds: List<Int>,
)

// --- POS discounts / promotions ---

data class PosDiscountsEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: List<PosDiscountDto>? = emptyList(),
)

data class PosDiscountDto(
    val id: Int,
    val name: String? = null,
    val code: String? = null,
    val type: String? = null,
    val value: Double? = 0.0,
    @SerializedName("min_purchase") val minPurchase: Double? = 0.0,
    @SerializedName("max_discount") val maxDiscount: Double? = null,
    @SerializedName("valid_from") val validFrom: String? = null,
    @SerializedName("valid_until") val validUntil: String? = null,
    @SerializedName("usage_limit") val usageLimit: Int? = null,
    @SerializedName("usage_count") val usageCount: Int? = 0,
)

data class PosPromotionsEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: List<PosPromotionDto>? = emptyList(),
)

data class PosPromotionDto(
    val id: Int,
    val name: String? = null,
    val description: String? = null,
    val type: String? = null,
    @SerializedName("valid_from") val validFrom: String? = null,
    @SerializedName("valid_until") val validUntil: String? = null,
    @SerializedName("promotion_products") val promotionProducts: List<PosPromotionProductDto>? = emptyList(),
)

data class PosPromotionProductDto(
    @SerializedName("product_id") val productId: Int,
    val name: String? = null,
    val sku: String? = null,
    @SerializedName("qty_required") val qtyRequired: Int? = null,
    @SerializedName("special_price") val specialPrice: Double? = null,
)

data class PosCategoriesEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: List<PosCategoryDto>? = emptyList(),
)

data class PosCategoryDto(
    val id: Int,
    val name: String,
)

data class PosUnitsEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: List<PosUnitDto>? = emptyList(),
)

data class PosUnitDto(
    val id: Int,
    val name: String,
    val abbreviation: String? = null,
)

fun ExpiryAlertRow.toAlertBatch(branchId: String): Batch {
    val expStr = expiredDate?.take(10).orEmpty()
    val today = java.time.LocalDate.now()
    val expDate = runCatching { java.time.LocalDate.parse(expStr) }.getOrNull()
    val expiredFlag = isExpired == true || (expDate != null && expDate.isBefore(today))
    val soon = expDate != null && !expiredFlag &&
        java.time.temporal.ChronoUnit.DAYS.between(today, expDate) <= 90
    return Batch(
        id = id.toString(),
        productId = product?.id?.toString() ?: "",
        productName = product?.name ?: "",
        batchNumber = batchNumber.orEmpty(),
        expiryDate = expiredDate.orEmpty(),
        currentQty = quantity ?: 0,
        initialQty = quantity ?: 0,
        buyPrice = 0.0,
        branchId = branchId,
        isExpired = expiredFlag,
        isExpiringSoon = soon,
    )
}

fun StockAlertRow.toLowStockProduct(): LowStockProduct = LowStockProduct(
    id = (productId ?: product?.id ?: 0).toString(),
    alertId = id.toString(),
    name = product?.name ?: "-",
    currentStock = currentStock ?: 0,
    minStock = minStock ?: 0,
)

// --- POS returns ---

data class PosReturnsListEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: PosReturnsListPayload? = null,
)

data class PosReturnsListPayload(
    val returns: List<PosReturnRowDto>? = emptyList(),
    val pagination: PosPagination? = null,
)

data class PosReturnRowDto(
    val id: Int,
    @SerializedName("return_number") val returnNumber: String? = null,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    @SerializedName("total_amount") val totalAmount: Double? = null,
    @SerializedName("refund_method") val refundMethod: String? = null,
    val reason: String? = null,
    val status: String? = null,
    @SerializedName("items_count") val itemsCount: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class PosReturnCreateRequest(
    @SerializedName("transaction_id") val transactionId: Int,
    val reason: String,
    @SerializedName("refund_method") val refundMethod: String? = null,
    val notes: String? = null,
    val items: List<PosReturnItemRequest>,
)

data class PosReturnItemRequest(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("batch_id") val batchId: Int? = null,
    val quantity: Int,
    val subtotal: Double,
    val condition: String? = "good",
    val restock: Boolean? = true,
)

data class PosReturnCreateEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: PosReturnCreateResult? = null,
)

data class PosReturnCreateResult(
    val id: Int,
    @SerializedName("return_number") val returnNumber: String? = null,
    @SerializedName("total_amount") val totalAmount: Double? = null,
    @SerializedName("refund_method") val refundMethod: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class PosReturnDetailEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: PosReturnDetailDto? = null,
)

data class PosReturnDetailDto(
    val id: Int,
    @SerializedName("return_number") val returnNumber: String? = null,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    @SerializedName("total_amount") val totalAmount: Double? = null,
    @SerializedName("refund_method") val refundMethod: String? = null,
    val reason: String? = null,
    val status: String? = null,
    val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

// --- POS customers ---

data class PosCustomersEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: PosCustomersPayload? = null,
)

data class PosCustomersPayload(
    val customers: List<PosCustomerDto>? = emptyList(),
    val pagination: PosPagination? = null,
)

data class PosCustomerDto(
    val id: Int,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
)

data class PosCustomerDetailEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: PosCustomerDto? = null,
)

data class PosCustomerUpsertRequest(
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
)

// --- POS doctors ---

data class PosDoctorsEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: PosDoctorsPayload? = null,
)

data class PosDoctorsPayload(
    val doctors: List<PosDoctorDto>? = emptyList(),
    val pagination: PosPagination? = null,
)

data class PosDoctorDto(
    val id: Int,
    val name: String? = null,
    @SerializedName("sip_number") val sipNumber: String? = null,
    val phone: String? = null,
    @SerializedName("clinic_name") val clinicName: String? = null,
)

data class PosDoctorDetailEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: PosDoctorDto? = null,
)

data class PosDoctorCreateRequest(
    val name: String,
    @SerializedName("sip_number") val sipNumber: String? = null,
    val phone: String? = null,
    @SerializedName("clinic_name") val clinicName: String? = null,
)

// --- POS prescriptions ---

data class PosPrescriptionsEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: PosPrescriptionsPayload? = null,
)

data class PosPrescriptionsPayload(
    val prescriptions: List<PosPrescriptionDto>? = emptyList(),
    val pagination: PosPagination? = null,
)

data class PosPrescriptionDto(
    val id: Int,
    @SerializedName("prescription_number") val prescriptionNumber: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("doctor_name") val doctorName: String? = null,
    val status: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class PosPrescriptionDetailEnvelope(
    val success: Boolean = false,
    val message: String? = null,
    val data: PosPrescriptionDetailDto? = null,
)

data class PosPrescriptionDetailDto(
    val id: Int,
    @SerializedName("prescription_number") val prescriptionNumber: String? = null,
    @SerializedName("customer_id") val customerId: Int? = null,
    @SerializedName("doctor_id") val doctorId: Int? = null,
    val notes: String? = null,
    val status: String? = null,
    val items: List<PosPrescriptionItemDto>? = emptyList(),
)

data class PosPrescriptionItemDto(
    @SerializedName("product_id") val productId: Int,
    val quantity: Double,
    val dosage: String? = null,
    val instruction: String? = null,
)

data class PosPrescriptionCreateRequest(
    @SerializedName("customer_id") val customerId: Int,
    @SerializedName("doctor_id") val doctorId: Int? = null,
    val notes: String? = null,
    val items: List<PosPrescriptionItemCreateRequest>,
)

data class PosPrescriptionItemCreateRequest(
    @SerializedName("product_id") val productId: Int,
    val quantity: Double,
    val dosage: String? = null,
    val instruction: String? = null,
)
