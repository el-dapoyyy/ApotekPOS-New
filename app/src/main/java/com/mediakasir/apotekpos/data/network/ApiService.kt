package com.mediakasir.apotekpos.data.network

import com.mediakasir.apotekpos.data.model.*
import retrofit2.http.*

interface ApiService {

    // License
    @POST("license/validate")
    suspend fun validateLicense(@Body req: LicenseValidateRequest): LicenseInfo

    // Auth
    @POST("auth/login")
    suspend fun login(@Body req: LoginRequest): UserInfo

    @POST("auth/change-pin")
    suspend fun changePin(@Body req: ChangePinRequest): MessageResponse

    // Products
    @GET("products")
    suspend fun getProducts(
        @Query("branch_id") branchId: String,
        @Query("search") search: String = "",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): ProductsResponse

    @POST("products")
    suspend fun createProduct(@Body data: ProductCreate): Product

    @PUT("products/{id}")
    suspend fun updateProduct(@Path("id") id: String, @Body data: ProductCreate): MessageResponse

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: String): MessageResponse

    // Batches
    @GET("batches")
    suspend fun getBatches(
        @Query("branch_id") branchId: String,
        @Query("product_id") productId: String? = null
    ): List<Batch>

    @POST("batches")
    suspend fun createBatch(@Body data: BatchCreate): Batch

    @PUT("batches/{id}")
    suspend fun updateBatch(@Path("id") id: String, @Body data: BatchCreate): MessageResponse

    @DELETE("batches/{id}")
    suspend fun deleteBatch(@Path("id") id: String): MessageResponse

    // Transactions
    @POST("transactions")
    suspend fun createTransaction(@Body data: TransactionCreate): Transaction

    @GET("transactions")
    suspend fun getTransactions(
        @Query("branch_id") branchId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): TransactionsResponse

    @GET("transactions/{id}")
    suspend fun getTransaction(@Path("id") id: String): Transaction

    // Dashboard
    @GET("dashboard")
    suspend fun getDashboard(@Query("branch_id") branchId: String): DashboardData

    @GET("alerts")
    suspend fun getAlerts(@Query("branch_id") branchId: String): AlertData

    // Seed
    @POST("seed")
    suspend fun seed(): Map<String, Any>
}
