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
}
