package com.example.budgettracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.budgettracker.data.local.dao.AccountDao
import com.example.budgettracker.data.local.dao.BillDetailsDao
import com.example.budgettracker.data.local.dao.InstallmentPlanDao
import com.example.budgettracker.data.local.dao.LoanDetailsDao
import com.example.budgettracker.data.local.dao.SavingsDetailsDao
import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        LoanAccountDetailsEntity::class,
        SavingsAccountDetailsEntity::class,
        BillAccountDetailsEntity::class,
        InstallmentPlanEntity::class,
        TransactionEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun loanDetailsDao(): LoanDetailsDao
    abstract fun savingsDetailsDao(): SavingsDetailsDao
    abstract fun billDetailsDao(): BillDetailsDao
    abstract fun installmentPlanDao(): InstallmentPlanDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_account_details` (
                        `accountId` INTEGER PRIMARY KEY NOT NULL,
                        `interestRate` REAL,
                        `goalAmount` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bill_account_details` (
                        `accountId` INTEGER PRIMARY KEY NOT NULL,
                        `dueDay` INTEGER NOT NULL,
                        `amountDue` INTEGER,
                        `amountType` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `installmentPlanId` INTEGER DEFAULT NULL REFERENCES `installment_plans`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_installmentPlanId` ON `transactions` (`installmentPlanId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Backfill misclassified Bill accounts
                db.execSQL("UPDATE `accounts` SET `type` = 'BILL' WHERE `id` IN (SELECT `accountId` FROM `bill_account_details`) AND `type` != 'BILL'")
                // 2. Add includeInNetWorth column (default 1)
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `includeInNetWorth` INTEGER NOT NULL DEFAULT 1")
                // 3. Set includeInNetWorth = 0 for BILL accounts
                db.execSQL("UPDATE `accounts` SET `includeInNetWorth` = 0 WHERE `type` = 'BILL'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_tracker.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
