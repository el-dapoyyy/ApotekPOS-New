package com.mediakasir.apotekpos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedProductEntity::class,
        PendingSyncEntity::class,
        LocalTransactionEntity::class,
        LocalTransactionItemEntity::class,
        LocalPaymentEntity::class,
        LocalShiftEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productCacheDao(): ProductCacheDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun localTransactionDao(): LocalTransactionDao
    abstract fun localShiftDao(): LocalShiftDao
}
