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
import com.example.budgettracker.data.local.dao.CreditDetailsDao
import com.example.budgettracker.data.local.dao.CustomCategoryDao
import com.example.budgettracker.data.local.dao.InstallmentPlanDao
import com.example.budgettracker.data.local.dao.LoanBillingCycleDao
import com.example.budgettracker.data.local.dao.LoanDetailsDao
import com.example.budgettracker.data.local.dao.SavingsDetailsDao
import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity
import com.example.budgettracker.data.local.entity.CustomCategoryEntity
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.LoanBillingCycleEntity
import com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity

import com.example.budgettracker.data.local.dao.RecurringBillDao
import com.example.budgettracker.data.local.entity.RecurringBillEntity

@Database(
    entities = [
        AccountEntity::class,
        LoanAccountDetailsEntity::class,
        SavingsAccountDetailsEntity::class,
        BillAccountDetailsEntity::class,
        InstallmentPlanEntity::class,
        TransactionEntity::class,
        LoanBillingCycleEntity::class,
        CustomCategoryEntity::class,
        RecurringBillEntity::class,
        CreditAccountDetailsEntity::class
    ],
    version = 7,
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
    abstract fun loanBillingCycleDao(): LoanBillingCycleDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun recurringBillDao(): RecurringBillDao
    abstract fun creditDetailsDao(): CreditDetailsDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add new columns to loan_details
                db.execSQL("ALTER TABLE `loan_details` ADD COLUMN `creditLimit` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `loan_details` ADD COLUMN `currentCycleDueDate` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `loan_details` ADD COLUMN `currentCycleAmountDue` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `loan_details` ADD COLUMN `isCurrentCyclePaid` INTEGER NOT NULL DEFAULT 0")

                // 2. Create loan_billing_cycles table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `loan_billing_cycles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `cycleDueDate` INTEGER NOT NULL,
                        `amountDue` INTEGER NOT NULL,
                        `isPaid` INTEGER NOT NULL DEFAULT 0,
                        `paidDate` INTEGER DEFAULT NULL,
                        `paidAmount` INTEGER DEFAULT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loan_billing_cycles_accountId` ON `loan_billing_cycles` (`accountId`)")

                // 3. Create custom_categories table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `iconName` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_categories_transactionType` ON `custom_categories` (`transactionType`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Backfill existing loan_details current cycle into loan_billing_cycles before dropping column
                db.execSQL("""
                    INSERT INTO `loan_billing_cycles` (`accountId`, `cycleDueDate`, `amountDue`, `isPaid`, `paidDate`, `paidAmount`, `createdAt`)
                    SELECT 
                        `accountId`,
                        COALESCE(`currentCycleDueDate`, 0),
                        COALESCE(`currentCycleAmountDue`, `minimumAmountDue`),
                        `isCurrentCyclePaid`,
                        CASE WHEN `isCurrentCyclePaid` = 1 THEN strftime('%s','now') * 1000 ELSE NULL END,
                        CASE WHEN `isCurrentCyclePaid` = 1 THEN COALESCE(`currentCycleAmountDue`, `minimumAmountDue`) ELSE NULL END,
                        strftime('%s','now') * 1000
                    FROM `loan_details`
                    WHERE `currentCycleDueDate` IS NOT NULL AND `currentCycleDueDate` > 0
                """)

                // 2. Recreate loan_details without cycleDay1, cycleDay2, currentCycleDueDate, currentCycleAmountDue, isCurrentCyclePaid
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `loan_details_new` (
                        `accountId` INTEGER PRIMARY KEY NOT NULL,
                        `dueDays` TEXT NOT NULL,
                        `minimumAmountDue` INTEGER NOT NULL,
                        `totalRemainingBalance` INTEGER NOT NULL,
                        `reminderEnabled` INTEGER NOT NULL,
                        `reminderDaysBefore` INTEGER NOT NULL,
                        `creditLimit` INTEGER,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `loan_details_new` (`accountId`, `dueDays`, `minimumAmountDue`, `totalRemainingBalance`, `reminderEnabled`, `reminderDaysBefore`, `creditLimit`)
                    SELECT 
                        `accountId`,
                        CASE 
                            WHEN `cycleDay2` IS NOT NULL THEN (CAST(`cycleDay1` AS TEXT) || ',' || CAST(`cycleDay2` AS TEXT))
                            ELSE CAST(`cycleDay1` AS TEXT)
                        END,
                        `minimumAmountDue`,
                        `totalRemainingBalance`,
                        `reminderEnabled`,
                        `reminderDaysBefore`,
                        `creditLimit`
                    FROM `loan_details`
                """.trimIndent())

                db.execSQL("DROP TABLE `loan_details`")
                db.execSQL("ALTER TABLE `loan_details_new` RENAME TO `loan_details`")

                // 3. Recreate bill_account_details without dueDay, adding dueDays
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `bill_account_details_new` (
                        `accountId` INTEGER PRIMARY KEY NOT NULL,
                        `dueDays` TEXT NOT NULL,
                        `amountDue` INTEGER,
                        `amountType` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `bill_account_details_new` (`accountId`, `dueDays`, `amountDue`, `amountType`, `createdAt`, `updatedAt`)
                    SELECT 
                        `accountId`,
                        CAST(`dueDay` AS TEXT),
                        `amountDue`,
                        `amountType`,
                        `createdAt`,
                        `updatedAt`
                    FROM `bill_account_details`
                """.trimIndent())

                db.execSQL("DROP TABLE `bill_account_details`")
                db.execSQL("ALTER TABLE `bill_account_details_new` RENAME TO `bill_account_details`")

                // 4. Create recurring_bills table and indices
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recurring_bills` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `dueDay` INTEGER NOT NULL,
                        `accountId` INTEGER,
                        `category` TEXT NOT NULL,
                        `isAutoPay` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_bills_accountId` ON `recurring_bills` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_bills_isActive` ON `recurring_bills` (`isActive`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loan_billing_cycles_accountId_isPaid` ON `loan_billing_cycles` (`accountId`, `isPaid`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `credit_account_details` (
                        `accountId` INTEGER PRIMARY KEY NOT NULL,
                        `creditLimit` INTEGER NOT NULL,
                        `statementDueDay` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `loan_billing_cycles` ADD COLUMN `isManualOverride` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_tracker.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
