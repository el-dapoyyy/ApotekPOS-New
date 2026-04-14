package com.mediakasir.apotekpos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductCacheDao {

    @Query("DELETE FROM cached_products WHERE branchId = :branchId")
    suspend fun clearBranch(branchId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CachedProductEntity>)

    @Query("SELECT * FROM cached_products WHERE branchId = :branchId ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(branchId: String): List<CachedProductEntity>

    @Query(
        """
        SELECT * FROM cached_products WHERE branchId = :branchId
        AND (name LIKE '%' || :q || '%' OR barcode LIKE '%' || :q || '%')
        ORDER BY name COLLATE NOCASE ASC
        """,
    )
    suspend fun search(branchId: String, q: String): List<CachedProductEntity>

    @Query("UPDATE cached_products SET currentStock = MAX(0, currentStock - :qty) WHERE id = CAST(:productId AS INTEGER)")
    suspend fun deductStock(productId: String, qty: Int)

    /** Overwrite stock to a specific server-authoritative value, but never above the provided value. */
    @Query("UPDATE cached_products SET currentStock = :stock WHERE id = :productId AND branchId = :branchId")
    suspend fun updateStock(productId: Int, branchId: String, stock: Int)

    @Query("SELECT MAX(syncedAtEpochMs) FROM cached_products WHERE branchId = :branchId")
    suspend fun getLastSyncTime(branchId: String): Long?
}
