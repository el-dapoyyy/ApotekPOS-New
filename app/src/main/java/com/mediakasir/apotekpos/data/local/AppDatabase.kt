package com.mediakasir.apotekpos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CachedProductEntity::class, PendingSyncEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productCacheDao(): ProductCacheDao
    abstract fun pendingSyncDao(): PendingSyncDao
}
