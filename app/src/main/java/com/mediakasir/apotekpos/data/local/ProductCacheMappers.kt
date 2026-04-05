package com.mediakasir.apotekpos.data.local

import com.mediakasir.apotekpos.data.model.PosProductDto
import com.mediakasir.apotekpos.data.model.Product

fun PosProductDto.toCachedEntity(branchId: String, syncedAtEpochMs: Long): CachedProductEntity {
    val code = sku?.takeIf { it.isNotBlank() }
        ?: barcode?.takeIf { it.isNotBlank() }
        ?: "OBT-$id"
    return CachedProductEntity(
        id = id,
        branchId = branchId,
        sku = code,
        barcode = barcode.orEmpty(),
        name = name,
        category = categoryName ?: "Umum",
        unit = unitName ?: unitAbbreviation ?: "unit",
        sellPrice = sellingPrice,
        buyPrice = purchasePrice ?: 0.0,
        minStock = minStock,
        currentStock = stock?.total ?: 0,
        syncedAtEpochMs = syncedAtEpochMs,
    )
}

fun CachedProductEntity.toProductModel(): Product = Product(
    id = id.toString(),
    sku = sku,
    barcode = barcode,
    name = name,
    category = category,
    unit = unit,
    sellPrice = sellPrice,
    buyPrice = buyPrice,
    minStock = minStock,
    branchId = branchId,
    currentStock = currentStock,
    isActive = true,
)
