package com.mediakasir.apotekpos.di

import android.content.Context
import androidx.room.Room
import com.mediakasir.apotekpos.data.local.AppDatabase
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
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProductCacheDao(db: AppDatabase): ProductCacheDao = db.productCacheDao()

    @Provides
    fun providePendingSyncDao(db: AppDatabase): PendingSyncDao = db.pendingSyncDao()
}
