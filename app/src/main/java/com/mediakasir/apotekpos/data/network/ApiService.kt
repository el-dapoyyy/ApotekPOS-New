package com.mediakasir.apotekpos.data.network

import com.mediakasir.apotekpos.data.model.*
import retrofit2.http.*

/**
 * Paths relatif ke [com.mediakasir.apotekpos.BuildConfig.BASE_URL].
 * Setelah login, **hak akses cabang** ditentukan backend dari Bearer token; endpoint POS/sync/history
 * tidak memerlukan `branch_id` di query. Query `branch_id` pada alerts hanya opsional — app mengirim `null`.
 */
interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body req: GoogleLoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun authMe(): AuthMeEnvelope

    /** Buka shift / clock-in. Sesuaikan path jika backend berbeda (mis. `pos/shifts/start`). */
    @POST("auth/shift/start")
    suspend fun startShift(@Body body: ShiftStartRequest): ShiftStartEnvelope

    // --- POS products (doc §5–7) ---

    @GET("pos/products/sync")
    suspend fun syncProducts(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100,
        @Query("updated_since") updatedSince: String? = null,
    ): ProductSyncEnvelope

    @GET("pos/products/search")
    suspend fun searchProducts(
        @Query("q") q: String,
        @Query("limit") limit: Int = 50,
    ): ProductSearchEnvelope

    @GET("pos/products/{id}")
    suspend fun getProductDetail(@Path("id") id: Int): ProductDetailEnvelope

    // --- POS transactions (doc §10–12) ---

    @POST("pos/transactions/sync")
    suspend fun syncTransactions(@Body body: TransactionSyncRequest): TransactionSyncEnvelope

    @GET("pos/transactions/history")
    suspend fun getTransactionHistory(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
    ): TransactionHistoryEnvelope

    @GET("pos/transactions/{id}")
    suspend fun getTransactionDetail(@Path("id") id: Int): TransactionDetailEnvelope

    // --- Alerts (doc §19+) ---

    @GET("alerts/summary")
    suspend fun getAlertsSummary(): AlertsSummaryEnvelope

    @GET("alerts/expiry")
    suspend fun getExpiryAlerts(
        @Query("branch_id") branchId: Int? = null,
        @Query("per_page") perPage: Int = 15,
        @Query("unacknowledged") unacknowledged: Boolean? = true,
    ): ExpiryAlertsEnvelope

    @GET("alerts/stock")
    suspend fun getStockAlerts(
        @Query("branch_id") branchId: Int? = null,
        @Query("per_page") perPage: Int = 15,
        @Query("unacknowledged") unacknowledged: Boolean? = true,
    ): StockAlertsEnvelope

    // --- POS returns (doc §13–15) ---

    @GET("pos/returns")
    suspend fun listReturns(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): PosReturnsListEnvelope

    @POST("pos/returns")
    suspend fun createReturn(@Body body: PosReturnCreateRequest): PosReturnCreateEnvelope
}
