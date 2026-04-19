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

    @POST("auth/logout")
    suspend fun logout(): ApiMessageEnvelope

    @POST("auth/logout-all")
    suspend fun logoutAll(): ApiMessageEnvelope

    /** GET shift aktif saat ini dari server. */
    @GET("pos/shifts/current")
    suspend fun getCurrentShift(): ShiftStartEnvelope

    /** Buka shift — POST ke backend agar terbaca di web. */
    @POST("pos/shifts/open")
    suspend fun startShift(@Body body: ShiftStartRequest): ShiftStartEnvelope

    /** Tutup shift di server. */
    @POST("pos/shifts/{id}/close")
    suspend fun closeShiftOnServer(
        @Path("id") id: Int,
        @Body body: ShiftCloseRequest,
    ): ShiftCloseEnvelope

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

    @GET("pos/products/categories")
    suspend fun getProductCategories(): PosCategoriesEnvelope

    @GET("pos/products/units")
    suspend fun getProductUnits(): PosUnitsEnvelope

    @GET("pos/discounts")
    suspend fun getDiscounts(): PosDiscountsEnvelope

    @GET("pos/promotions")
    suspend fun getPromotions(): PosPromotionsEnvelope

    // --- POS transactions (doc §10–12) ---

    @POST("pos/transactions/sync")
    suspend fun syncTransactions(@Body body: TransactionSyncRequest): TransactionSyncEnvelope

    @POST("pos/cash-expenses/sync")
    suspend fun syncCashExpenses(@Body body: CashExpenseSyncRequest): CashExpenseSyncEnvelope

    @GET("pos/compound-recipes")
    suspend fun getCompoundRecipes(): CompoundRecipesEnvelope

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

    @GET("pos/returns/{id}")
    suspend fun getReturnDetail(@Path("id") id: Int): PosReturnDetailEnvelope

    @GET("pos/customers")
    suspend fun listCustomers(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("search") search: String? = null,
    ): PosCustomersEnvelope

    @GET("pos/customers/{id}")
    suspend fun getCustomerDetail(@Path("id") id: Int): PosCustomerDetailEnvelope

    @POST("pos/customers")
    suspend fun createCustomer(@Body body: PosCustomerUpsertRequest): PosCustomerDetailEnvelope

    @PUT("pos/customers/{id}")
    suspend fun updateCustomer(
        @Path("id") id: Int,
        @Body body: PosCustomerUpsertRequest,
    ): PosCustomerDetailEnvelope

    @GET("pos/doctors")
    suspend fun listDoctors(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("search") search: String? = null,
    ): PosDoctorsEnvelope

    @GET("pos/doctors/{id}")
    suspend fun getDoctorDetail(@Path("id") id: Int): PosDoctorDetailEnvelope

    @POST("pos/doctors")
    suspend fun createDoctor(@Body body: PosDoctorCreateRequest): PosDoctorDetailEnvelope

    @GET("pos/prescriptions")
    suspend fun listPrescriptions(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
    ): PosPrescriptionsEnvelope

    @GET("pos/prescriptions/{id}")
    suspend fun getPrescriptionDetail(@Path("id") id: Int): PosPrescriptionDetailEnvelope

    @POST("pos/prescriptions")
    suspend fun createPrescription(@Body body: PosPrescriptionCreateRequest): PosPrescriptionDetailEnvelope

    @POST("alerts/expiry/{id}/acknowledge")
    suspend fun acknowledgeExpiryAlert(@Path("id") id: Int): ApiMessageEnvelope

    @POST("alerts/stock/{id}/acknowledge")
    suspend fun acknowledgeStockAlert(@Path("id") id: Int): ApiMessageEnvelope

    @POST("alerts/expiry/acknowledge-multiple")
    suspend fun acknowledgeExpiryMultiple(@Body body: AcknowledgeMultipleRequest): ApiMessageEnvelope

    @POST("alerts/stock/acknowledge-multiple")
    suspend fun acknowledgeStockMultiple(@Body body: AcknowledgeMultipleRequest): ApiMessageEnvelope
}
