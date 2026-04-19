package com.mediakasir.apotekpos.di

import android.content.Context
import androidx.room.Room
import com.mediakasir.apotekpos.data.local.AppDatabase
import com.mediakasir.apotekpos.data.local.LocalCashExpenseAuditDao
import com.mediakasir.apotekpos.data.local.LocalCashExpenseDao
import com.mediakasir.apotekpos.data.local.LocalShiftDao
import com.mediakasir.apotekpos.data.local.MIGRATION_4_5
import com.mediakasir.apotekpos.data.local.MIGRATION_3_4
import com.mediakasir.apotekpos.data.local.MIGRATION_5_6
import com.mediakasir.apotekpos.data.local.MIGRATION_6_7
import com.mediakasir.apotekpos.data.local.LocalTransactionDao
import com.mediakasir.apotekpos.data.local.PendingSyncDao
import com.mediakasir.apotekpos.data.local.ProductCacheDao
import dagger.Module as DaggerModule
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@DaggerModule
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "apo_apps_pos.db")
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideProductCacheDao(db: AppDatabase): ProductCacheDao = db.productCacheDao()

    @Provides
    fun providePendingSyncDao(db: AppDatabase): PendingSyncDao = db.pendingSyncDao()

    @Provides
    fun provideLocalTransactionDao(db: AppDatabase): LocalTransactionDao = db.localTransactionDao()

    @Provides
    fun provideLocalShiftDao(db: AppDatabase): LocalShiftDao = db.localShiftDao()

    @Provides
    fun provideLocalCashExpenseDao(db: AppDatabase): LocalCashExpenseDao = db.localCashExpenseDao()

    @Provides
    fun provideLocalCashExpenseAuditDao(db: AppDatabase): LocalCashExpenseAuditDao = db.localCashExpenseAuditDao()
}
