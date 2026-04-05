package com.mediakasir.apotekpos.data.local

import androidx.room.Entity

@Entity(tableName = "cached_products", primaryKeys = ["id", "branchId"])
data class CachedProductEntity(
    val id: Int,
    val branchId: String,
    val sku: String = "",
    val barcode: String,
    val name: String,
    val category: String,
    val unit: String,
    val sellPrice: Double,
    val buyPrice: Double,
    val minStock: Int,
    val currentStock: Int,
    val syncedAtEpochMs: Long,
)
