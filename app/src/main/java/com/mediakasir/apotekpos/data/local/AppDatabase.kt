package com.mediakasir.apotekpos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE local_transaction_items ADD COLUMN compoundRecipeId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE local_transaction_items ADD COLUMN itemName TEXT DEFAULT NULL")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_cash_expenses (
                id TEXT NOT NULL,
                shiftId TEXT NOT NULL,
                branchId TEXT NOT NULL,
                cashierId TEXT NOT NULL,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                note TEXT,
                receiptPhotoPath TEXT,
                createdAt TEXT NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS local_cash_expense_audits (
                id TEXT NOT NULL,
                cashExpenseId TEXT NOT NULL,
                shiftId TEXT NOT NULL,
                action TEXT NOT NULL,
                actorCashierId TEXT NOT NULL,
                oldAmount REAL NOT NULL,
                newAmount REAL,
                oldCategory TEXT NOT NULL,
                newCategory TEXT,
                oldNote TEXT,
                newNote TEXT,
                oldReceiptPhotoPath TEXT,
                newReceiptPhotoPath TEXT,
                createdAt TEXT NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE local_cash_expenses ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'pending'")
        db.execSQL("ALTER TABLE local_cash_expenses ADD COLUMN serverCashExpenseId INTEGER")
        db.execSQL("ALTER TABLE local_cash_expenses ADD COLUMN syncAttempts INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE local_cash_expenses ADD COLUMN lastSyncError TEXT")

        db.execSQL("ALTER TABLE local_cash_expense_audits ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'pending'")
        db.execSQL("ALTER TABLE local_cash_expense_audits ADD COLUMN serverAuditId INTEGER")
        db.execSQL("ALTER TABLE local_cash_expense_audits ADD COLUMN syncAttempts INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE local_cash_expense_audits ADD COLUMN lastSyncError TEXT")
    }
}

@Database(
    entities = [
        CachedProductEntity::class,
        PendingSyncEntity::class,
        LocalTransactionEntity::class,
        LocalTransactionItemEntity::class,
        LocalPaymentEntity::class,
        LocalShiftEntity::class,
        LocalCashExpenseEntity::class,
        LocalCashExpenseAuditEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productCacheDao(): ProductCacheDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun localTransactionDao(): LocalTransactionDao
    abstract fun localShiftDao(): LocalShiftDao
    abstract fun localCashExpenseDao(): LocalCashExpenseDao
    abstract fun localCashExpenseAuditDao(): LocalCashExpenseAuditDao
}
