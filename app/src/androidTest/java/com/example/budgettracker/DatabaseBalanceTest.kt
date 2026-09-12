package com.example.budgettracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.AdjustmentDirection
import com.example.budgettracker.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseBalanceTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testBalanceCalculationsWithTransfersAndAdjustments() {
        runBlocking {
        val accountDao = database.accountDao()
        val transactionDao = database.transactionDao()

        // 1. Create two accounts
        val gcashId = accountDao.insert(
            AccountEntity(
                name = "GCash",
                type = AccountType.E_WALLET,
                presetId = "preset_gcash",
                initialBalance = 10_000L // ₱100.00
            )
        )

        val bpiId = accountDao.insert(
            AccountEntity(
                name = "BPI",
                type = AccountType.BANK,
                presetId = "preset_bpi",
                initialBalance = 50_000L // ₱500.00
            )
        )

        val now = System.currentTimeMillis()

        // 2. Add Income to GCash (+₱200.00 = 20_000 centavos)
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 20_000L,
                accountId = gcashId,
                category = "Salary",
                title = "Part-time paycheck",
                timestamp = now
            )
        )

        // 3. Add Expense from GCash (-₱50.00 = 5_000 centavos)
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 5_000L,
                accountId = gcashId,
                category = "Food",
                title = "Lunch",
                timestamp = now + 1
            )
        )

        // 4. Transfer from GCash to BPI (₱30.00 = 3_000 centavos)
        // Subtracted from GCash, Added to BPI
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = 3_000L,
                accountId = gcashId,
                toAccountId = bpiId,
                category = "Transfer",
                title = "Fund Transfer",
                timestamp = now + 2
            )
        )

        // 5. Adjustment INCREASE on BPI (+₱10.00 = 1_000 centavos)
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.INCOME,
                isAdjustment = true,
                amount = 1_000L,
                accountId = bpiId,
                category = "Adjustment",
                title = "Interest credit",
                timestamp = now + 3
            )
        )

        // 6. Adjustment DECREASE on GCash (-₱5.00 = 500 centavos)
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                isAdjustment = true,
                amount = 500L,
                accountId = gcashId,
                category = "Adjustment",
                title = "Convenience fee reconciliation",
                timestamp = now + 4
            )
        )

        // --- Verifications ---

        // GCash: 10,000 (initial) + 20,000 (income) - 5,000 (expense) - 3,000 (transfer out) - 500 (adjustment decrease)
        // = 21,500 centavos (₱215.00)
        val gcashBalance = accountDao.getAccountBalance(gcashId).first()
        assertEquals(21_500L, gcashBalance)

        // BPI: 50,000 (initial) + 3,000 (transfer in) + 1,000 (adjustment increase)
        // = 54,000 centavos (₱540.00)
        val bpiBalance = accountDao.getAccountBalance(bpiId).first()
        assertEquals(54_000L, bpiBalance)

        // Total Net Worth: 21,500 + 54,000 = 75,500 centavos
        val totalNetWorth = accountDao.getTotalNetWorth().first()
        assertEquals(75_500L, totalNetWorth)

        // Verify with getAllActiveWithBalances()
        val activeWithBalances = accountDao.getAllActiveWithBalances().first()
        assertEquals(2, activeWithBalances.size)
        val gcashItem = activeWithBalances.first { it.id == gcashId }
        val bpiItem = activeWithBalances.first { it.id == bpiId }
        assertEquals(21_500L, gcashItem.currentBalance)
        assertEquals(54_000L, bpiItem.currentBalance)
        }
    }

    @Test
    fun testTransferDoesNotAffectNetWorth() {
        runBlocking {
        val accountDao = database.accountDao()
        val transactionDao = database.transactionDao()

        val acc1 = accountDao.insert(
            AccountEntity(name = "Wallet", type = AccountType.CASH, initialBalance = 10_000L)
        )
        val acc2 = accountDao.insert(
            AccountEntity(name = "Bank", type = AccountType.BANK, initialBalance = 20_000L)
        )

        val netWorthBefore = accountDao.getTotalNetWorth().first()
        assertEquals(30_000L, netWorthBefore)

        // Transfer 5,000 from Wallet to Bank
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = 5_000L,
                accountId = acc1,
                toAccountId = acc2,
                category = "Transfer",
                title = "Deposit",
                timestamp = System.currentTimeMillis()
            )
        )

        val netWorthAfter = accountDao.getTotalNetWorth().first()
        assertEquals(30_000L, netWorthAfter)

        val acc1Balance = accountDao.getAccountBalance(acc1).first()
        val acc2Balance = accountDao.getAccountBalance(acc2).first()
        assertEquals(5_000L, acc1Balance)
        assertEquals(25_000L, acc2Balance)
        }
    }

    @Test
    fun testAutocompleteScopedPerTransactionType() {
        runBlocking {
        val accountDao = database.accountDao()
        val transactionDao = database.transactionDao()

        val accId = accountDao.insert(
            AccountEntity(name = "Cash", type = AccountType.CASH, initialBalance = 5_000L)
        )

        val now = System.currentTimeMillis()

        // Expense entries
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 100L,
                accountId = accId,
                category = "Groceries",
                title = "Supermarket Milk",
                timestamp = now
            )
        )

        // Income entries
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 5_000L,
                accountId = accId,
                category = "Freelance",
                title = "Design Project",
                timestamp = now + 1
            )
        )

        // Autocomplete for EXPENSE should only return Groceries, not Freelance
        val expenseCategories = transactionDao.getDistinctCategories(TransactionType.EXPENSE).first()
        assertEquals(listOf("Groceries"), expenseCategories)

        // Autocomplete for INCOME should only return Freelance, not Groceries
        val incomeCategories = transactionDao.getDistinctCategories(TransactionType.INCOME).first()
        assertEquals(listOf("Freelance"), incomeCategories)

        // Autocomplete for titles scoped by type
        val expenseTitles = transactionDao.getDistinctTitles(TransactionType.EXPENSE).first()
        assertEquals(listOf("Supermarket Milk"), expenseTitles)

        val incomeTitles = transactionDao.getDistinctTitles(TransactionType.INCOME).first()
        assertEquals(listOf("Design Project"), incomeTitles)
        }
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun testForeignKeyRestrictPreventsHardDeleteOfAccountWithTransactions() {
        runBlocking {
            val accountDao = database.accountDao()
            val transactionDao = database.transactionDao()

            val accId = accountDao.insert(
                AccountEntity(name = "Locked Account", type = AccountType.BANK, initialBalance = 1_000L)
            )

            transactionDao.insert(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 100L,
                    accountId = accId,
                    category = "Fees",
                    title = "Monthly fee",
                    timestamp = System.currentTimeMillis()
                )
            )

            // Attempting to hard-delete the account directly must fail due to ForeignKey.RESTRICT
            val account = accountDao.getById(accId)!!
            accountDao.delete(account)
        }
    }
}
