package com.example.budgettracker

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import com.example.budgettracker.data.local.entity.BillAmountType
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationTest {

    private val dbName = "migration_test_budget_tracker.db"
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun teardown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun testMigrationFromV2ToV3WithPreExistingData() = runBlocking {
        // Step 1: Create a real SQLite database at Version 2
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create v2 tables
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `accounts` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `presetId` TEXT,
                            `initialBalance` INTEGER NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `displayOrder` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_isActive_displayOrder` ON `accounts` (`isActive`, `displayOrder`)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `loan_details` (
                            `accountId` INTEGER PRIMARY KEY NOT NULL,
                            `cycleDay1` INTEGER NOT NULL,
                            `cycleDay2` INTEGER,
                            `minimumAmountDue` INTEGER NOT NULL,
                            `totalRemainingBalance` INTEGER NOT NULL,
                            `reminderEnabled` INTEGER NOT NULL,
                            `reminderDaysBefore` INTEGER NOT NULL,
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `installment_plans` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `accountId` INTEGER NOT NULL,
                            `title` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `totalPurchaseAmount` INTEGER NOT NULL,
                            `totalInstallments` INTEGER NOT NULL,
                            `installmentsPaid` INTEGER NOT NULL,
                            `remainingBalance` INTEGER NOT NULL,
                            `monthlyPaymentAmount` INTEGER NOT NULL,
                            `purchaseDate` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_plans_accountId` ON `installment_plans` (`accountId`)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `transactions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `type` TEXT NOT NULL,
                            `amount` INTEGER NOT NULL,
                            `isAdjustment` INTEGER NOT NULL,
                            `accountId` INTEGER NOT NULL,
                            `toAccountId` INTEGER,
                            `category` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            `note` TEXT,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                            FOREIGN KEY(`toAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_toAccountId` ON `transactions` (`toAccountId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type_category` ON `transactions` (`type`, `category`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type_title` ON `transactions` (`type`, `title`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v2Db = helper.writableDatabase

        // Step 2: Insert real pre-existing data into Version 2 database
        v2Db.execSQL("""
            INSERT INTO `accounts` (`id`, `name`, `type`, `presetId`, `initialBalance`, `isActive`, `displayOrder`, `createdAt`, `updatedAt`)
            VALUES (1, 'BPI Savings Account', 'BANK', 'bpi', 15000000, 1, 0, 1700000000000, 1700000000000)
        """.trimIndent())

        v2Db.execSQL("""
            INSERT INTO `loan_details` (`accountId`, `cycleDay1`, `cycleDay2`, `minimumAmountDue`, `totalRemainingBalance`, `reminderEnabled`, `reminderDaysBefore`)
            VALUES (1, 15, 30, 250000, 1200000, 1, 3)
        """.trimIndent())

        v2Db.execSQL("""
            INSERT INTO `transactions` (`id`, `type`, `amount`, `isAdjustment`, `accountId`, `toAccountId`, `category`, `title`, `timestamp`, `note`, `createdAt`)
            VALUES (1, 'EXPENSE', 125000, 0, 1, NULL, 'Groceries', 'Supermarket Run', 1700005000000, 'Weekly food supplies', 1700005000000)
        """.trimIndent())

        v2Db.close()
        helper.close()

        // Step 3: Open database with Room v3 builder specifying MIGRATION_2_3
        // Room will execute MIGRATION_2_3 and validate the resulting schema against Room's v3 entity definitions
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        // Trigger open and migration validation
        val accountDao = roomDb.accountDao()
        val loanDao = roomDb.loanDetailsDao()
        val transactionDao = roomDb.transactionDao()
        val savingsDao = roomDb.savingsDetailsDao()
        val billDao = roomDb.billDetailsDao()

        // Step 4: Validate pre-existing data is 100% intact
        val accounts = accountDao.getAll().first()
        assertEquals(1, accounts.size)
        val preAccount = accounts[0]
        assertEquals(1L, preAccount.id)
        assertEquals("BPI Savings Account", preAccount.name)
        assertEquals(AccountType.BANK, preAccount.type)
        assertEquals("bpi", preAccount.presetId)
        assertEquals(15000000L, preAccount.initialBalance)
        assertEquals(true, preAccount.isActive)

        val preLoan = loanDao.getByAccountId(1L)
        assertNotNull(preLoan)
        assertEquals(1L, preLoan?.accountId)
        assertEquals(15, preLoan?.cycleDay1)
        assertEquals(30, preLoan?.cycleDay2)
        assertEquals(250000L, preLoan?.minimumAmountDue)
        assertEquals(1200000L, preLoan?.totalRemainingBalance)

        val preTx = transactionDao.getById(1L)
        assertNotNull(preTx)
        assertEquals(1L, preTx?.id)
        assertEquals(TransactionType.EXPENSE, preTx?.type)
        assertEquals(125000L, preTx?.amount)
        assertEquals(false, preTx?.isAdjustment)
        assertEquals(1L, preTx?.accountId)
        assertEquals(null, preTx?.toAccountId)
        assertEquals(null, preTx?.installmentPlanId) // new column defaults to null
        assertEquals("Groceries", preTx?.category)
        assertEquals("Supermarket Run", preTx?.title)
        assertEquals("Weekly food supplies", preTx?.note)

        // Step 5: Validate new v3 schema functionality
        // 5a. Insert savings account details
        val savingsDetails = SavingsAccountDetailsEntity(
            accountId = 1L,
            interestRate = 4.5,
            goalAmount = 50000000L
        )
        savingsDao.insert(savingsDetails)
        val savedSavings = savingsDao.getByAccountId(1L)
        assertNotNull(savedSavings)
        assertEquals(4.5, savedSavings?.interestRate ?: 0.0, 0.001)
        assertEquals(50000000L, savedSavings?.goalAmount)

        // 5b. Create a bill account and insert bill account details
        val billAccountId = accountDao.insert(
            AccountEntity(
                name = "PLDT Fiber",
                type = AccountType.BILL,
                initialBalance = 0L
            )
        )
        val billDetails = BillAccountDetailsEntity(
            accountId = billAccountId,
            dueDay = 24,
            amountDue = 189900L,
            amountType = BillAmountType.FIXED
        )
        billDao.insert(billDetails)
        val savedBill = billDao.getByAccountId(billAccountId)
        assertNotNull(savedBill)
        assertEquals(24, savedBill?.dueDay)
        assertEquals(189900L, savedBill?.amountDue)
        assertEquals(BillAmountType.FIXED, savedBill?.amountType)

        // 5c. Insert transaction with installmentPlanId
        val plan = InstallmentPlanEntity(
            accountId = 1L,
            title = "iPad Air",
            category = "Electronics",
            totalPurchaseAmount = 3600000L,
            totalInstallments = 6,
            monthlyPaymentAmount = 600000L,
            remainingBalance = 3600000L
        )
        val planId = roomDb.installmentPlanDao().insert(plan)
        val newTxId = transactionDao.insert(
            TransactionEntity(
                type = TransactionType.INSTALLMENT,
                amount = 3600000L,
                accountId = 1L,
                installmentPlanId = planId,
                category = "Electronics",
                title = "iPad Air Purchase",
                timestamp = System.currentTimeMillis()
            )
        )
        val newTx = transactionDao.getById(newTxId)
        assertNotNull(newTx)
        assertEquals(planId, newTx?.installmentPlanId)

        roomDb.close()
    }

    @Test
    fun testMigrationFromV3ToV4WithPreExistingData() = runBlocking {
        // Step 1: Create a real SQLite database at Version 3
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `accounts` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `presetId` TEXT,
                            `initialBalance` INTEGER NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `displayOrder` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_isActive_displayOrder` ON `accounts` (`isActive`, `displayOrder`)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `loan_details` (
                            `accountId` INTEGER PRIMARY KEY NOT NULL,
                            `cycleDay1` INTEGER NOT NULL,
                            `cycleDay2` INTEGER,
                            `minimumAmountDue` INTEGER NOT NULL,
                            `totalRemainingBalance` INTEGER NOT NULL,
                            `reminderEnabled` INTEGER NOT NULL,
                            `reminderDaysBefore` INTEGER NOT NULL,
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `savings_account_details` (
                            `accountId` INTEGER PRIMARY KEY NOT NULL,
                            `interestRate` REAL,
                            `goalAmount` INTEGER,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `bill_account_details` (
                            `accountId` INTEGER PRIMARY KEY NOT NULL,
                            `dueDay` INTEGER NOT NULL,
                            `amountDue` INTEGER,
                            `amountType` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `installment_plans` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `accountId` INTEGER NOT NULL,
                            `title` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `totalPurchaseAmount` INTEGER NOT NULL,
                            `totalInstallments` INTEGER NOT NULL,
                            `installmentsPaid` INTEGER NOT NULL,
                            `remainingBalance` INTEGER NOT NULL,
                            `monthlyPaymentAmount` INTEGER NOT NULL,
                            `purchaseDate` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_plans_accountId` ON `installment_plans` (`accountId`)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `transactions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `type` TEXT NOT NULL,
                            `amount` INTEGER NOT NULL,
                            `isAdjustment` INTEGER NOT NULL,
                            `accountId` INTEGER NOT NULL,
                            `toAccountId` INTEGER,
                            `installmentPlanId` INTEGER,
                            `category` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            `note` TEXT,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                            FOREIGN KEY(`toAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                            FOREIGN KEY(`installmentPlanId`) REFERENCES `installment_plans`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_toAccountId` ON `transactions` (`toAccountId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_installmentPlanId` ON `transactions` (`installmentPlanId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type_category` ON `transactions` (`type`, `category`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type_title` ON `transactions` (`type`, `title`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v3Db = helper.writableDatabase

        // Step 2: Insert real pre-existing v3 data:
        // Account 1: Regular Bank Account (500.00 PHP = 50,000 centavos)
        v3Db.execSQL("""
            INSERT INTO `accounts` (`id`, `name`, `type`, `presetId`, `initialBalance`, `isActive`, `displayOrder`, `createdAt`, `updatedAt`)
            VALUES (1, 'BPI Bank', 'BANK', 'bpi', 50000, 1, 0, 1700000000000, 1700000000000)
        """.trimIndent())

        // Account 2: Meralco misclassified as BNPL in v3 (has bill_account_details row!)
        v3Db.execSQL("""
            INSERT INTO `accounts` (`id`, `name`, `type`, `presetId`, `initialBalance`, `isActive`, `displayOrder`, `createdAt`, `updatedAt`)
            VALUES (2, 'Meralco Electric', 'BNPL', 'meralco', 0, 1, 1, 1700000000000, 1700000000000)
        """.trimIndent())
        v3Db.execSQL("""
            INSERT INTO `bill_account_details` (`accountId`, `dueDay`, `amountDue`, `amountType`, `createdAt`, `updatedAt`)
            VALUES (2, 15, 350000, 'FIXED', 1700000000000, 1700000000000)
        """.trimIndent())

        // Account 3: E-Wallet (150.00 PHP = 15,000 centavos)
        v3Db.execSQL("""
            INSERT INTO `accounts` (`id`, `name`, `type`, `presetId`, `initialBalance`, `isActive`, `displayOrder`, `createdAt`, `updatedAt`)
            VALUES (3, 'GCash Wallet', 'E_WALLET', 'gcash', 15000, 1, 2, 1700000000000, 1700000000000)
        """.trimIndent())

        v3Db.close()
        helper.close()

        // Step 3: Run migration to v4 using Room
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        val accountDao = roomDb.accountDao()

        // Step 4: Verify migration results
        val accounts = accountDao.getAll().first()
        assertEquals(3, accounts.size)

        val bpi = accounts.find { it.id == 1L }!!
        assertEquals(AccountType.BANK, bpi.type)
        assertEquals(true, bpi.includeInNetWorth)

        // Verify Account 2 was automatically backfilled to BILL and includeInNetWorth = false
        val meralco = accounts.find { it.id == 2L }!!
        assertEquals(AccountType.BILL, meralco.type)
        assertEquals(false, meralco.includeInNetWorth)

        val gcash = accounts.find { it.id == 3L }!!
        assertEquals(AccountType.E_WALLET, gcash.type)
        assertEquals(true, gcash.includeInNetWorth)

        // Verify Net Worth calculation: 50,000 (BPI) + 15,000 (GCash) = 65,000 centavos
        val netWorth = accountDao.getTotalNetWorth().first()
        assertEquals(65000L, netWorth)

        // Step 5: Test net worth toggle on Account 2
        accountDao.updateNetWorthInclusion(2L, true)
        val meralcoUpdated = accountDao.getById(2L)!!
        assertEquals(true, meralcoUpdated.includeInNetWorth)

        // Step 6: Test hidden accounts functionality
        accountDao.softDelete(3L)
        val hiddenList = accountDao.getAllHiddenAccounts().first()
        assertEquals(1, hiddenList.size)
        assertEquals(3L, hiddenList[0].id)

        // Active count direct should now be 2
        assertEquals(2, accountDao.getActiveCountDirect())

        // Net worth after hiding GCash (15,000) should now be 50,000 (Account 1: 50,000 + Account 2: 0)
        val netWorthAfterHide = accountDao.getTotalNetWorth().first()
        assertEquals(50000L, netWorthAfterHide)

        // Restore GCash
        accountDao.restoreAccount(3L)
        val hiddenAfterRestore = accountDao.getAllHiddenAccounts().first()
        assertEquals(0, hiddenAfterRestore.size)
        assertEquals(3, accountDao.getActiveCountDirect())

        roomDb.close()
    }
}
