package com.example.budgettracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.dao.AccountDao
import com.example.budgettracker.data.local.dao.LoanDetailsDao
import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class DatabaseBalanceUnitTest {

    private lateinit var database: AppDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var loanDetailsDao: LoanDetailsDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        accountDao = database.accountDao()
        transactionDao = database.transactionDao()
        loanDetailsDao = database.loanDetailsDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testBalanceCalculationsWithTransfersAndAdjustments() = runBlocking {
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

    @Test
    fun testTransferDoesNotAffectNetWorth() = runBlocking {
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

    @Test
    fun testMonthlyTotalsCorrectlyIncludesIncomeExpenseAndExcludesTransfersAdjustments() = runBlocking {
        val acc1 = accountDao.insert(
            AccountEntity(name = "Main Account", type = AccountType.BANK, initialBalance = 100_000L)
        )
        val acc2 = accountDao.insert(
            AccountEntity(name = "Second Account", type = AccountType.BANK, initialBalance = 50_000L)
        )

        val windowStart = 1_000_000L
        val windowEnd = 2_000_000L

        // 1. Income inside window (+25,000)
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 25_000L,
                accountId = acc1,
                category = "Salary",
                title = "Monthly Salary",
                timestamp = 1_200_000L
            )
        )

        // 2. Expense 1 inside window (-7,000)
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 7_000L,
                accountId = acc1,
                category = "Groceries",
                title = "Supermarket",
                timestamp = 1_300_000L
            )
        )

        // 3. Expense 2 inside window (-3,000)
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 3_000L,
                accountId = acc1,
                category = "Utilities",
                title = "Water Bill",
                timestamp = 1_400_000L
            )
        )

        // 4. Transfer inside window (5,000) - MUST BE EXCLUDED FROM MONTHLY TOTALS
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = 5_000L,
                accountId = acc1,
                toAccountId = acc2,
                category = "Transfer",
                title = "Internal transfer",
                timestamp = 1_500_000L
            )
        )

        // 5. Adjustment inside window (+2,000 Income)
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.INCOME,
                isAdjustment = true,
                amount = 2_000L,
                accountId = acc1,
                category = "Adjustment",
                title = "Correction",
                timestamp = 1_600_000L
            )
        )

        // 6. Income BEFORE window (+10,000 at 500_000L) - MUST BE EXCLUDED
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 10_000L,
                accountId = acc1,
                category = "Bonus",
                title = "Past Bonus",
                timestamp = 500_000L
            )
        )

        // 7. Expense AFTER window (-4,000 at 2_500_000L) - MUST BE EXCLUDED
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 4_000L,
                accountId = acc1,
                category = "Dining",
                title = "Future Dinner",
                timestamp = 2_500_000L
            )
        )

        val totals = transactionDao.getMonthlyTotals(windowStart, windowEnd).first()

        assertEquals(27_000L, totals.totalIncome)
        assertEquals(10_000L, totals.totalExpense)
        assertEquals(17_000L, totals.netSavings)
        assertEquals(62.96f, totals.savingsRate, 0.1f)
    }

    @Test
    fun testAutocompleteScopedPerTransactionType() = runBlocking {
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

        // Autocomplete for titles scoped by type and category
        val expenseTitlesForGroceries = transactionDao.getDistinctTitlesByCategory(
            TransactionType.EXPENSE,
            "Groceries"
        ).first()
        assertEquals(listOf("Supermarket Milk"), expenseTitlesForGroceries)

        val expenseTitlesForOther = transactionDao.getDistinctTitlesByCategory(
            TransactionType.EXPENSE,
            "Other"
        ).first()
        assertTrue(expenseTitlesForOther.isEmpty())
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun testForeignKeyRestrictPreventsHardDeleteOfAccountWithTransactions() = runBlocking {
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
        Unit
    }

    @Test
    fun testForeignKeyCascadeOnLoanDetails() = runBlocking {
        val loanAccId = accountDao.insert(
            AccountEntity(
                name = "SPayLater",
                type = AccountType.BNPL,
                presetId = "preset_spaylater",
                initialBalance = 0L
            )
        )

        val loanDetails = LoanAccountDetailsEntity(
            accountId = loanAccId,
            cycleDay1 = 15,
            cycleDay2 = 30,
            minimumAmountDue = 5_000L,
            totalRemainingBalance = 50_000L,
            reminderEnabled = true,
            reminderDaysBefore = 7
        )
        loanDetailsDao.insert(loanDetails)

        val retrievedLoanDetails = loanDetailsDao.getByAccountId(loanAccId)
        assertNotNull(retrievedLoanDetails)
        assertEquals(15, retrievedLoanDetails?.cycleDay1)

        // Hard-delete the account (no transactions exist)
        val account = accountDao.getById(loanAccId)!!
        accountDao.delete(account)

        // Account is gone
        assertNull(accountDao.getById(loanAccId))

        // Loan details must be cascaded and deleted automatically
        val cascadedLoanDetails = loanDetailsDao.getByAccountId(loanAccId)
        assertNull(cascadedLoanDetails)
    }

    @Test
    fun testSoftDeleteAndRestoreFunctionality() = runBlocking {
        val acc1Id = accountDao.insert(
            AccountEntity(name = "Active 1", type = AccountType.E_WALLET, initialBalance = 20_000L)
        )
        val acc2Id = accountDao.insert(
            AccountEntity(name = "Active 2", type = AccountType.BANK, initialBalance = 30_000L)
        )

        // Add a transaction to Account 1
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 5_000L,
                accountId = acc1Id,
                category = "Transport",
                title = "Commute",
                timestamp = System.currentTimeMillis()
            )
        )

        // Initial checks
        assertEquals(2, accountDao.getActiveCountDirect())
        assertEquals(2, accountDao.getActiveAccountCount().first())
        assertEquals(45_000L, accountDao.getTotalNetWorth().first())

        val initialActiveAccounts = accountDao.getAllActiveWithBalances().first()
        assertEquals(2, initialActiveAccounts.size)

        // Soft delete Account 1
        val deleteTimestamp = 999_888L
        val affectedRows = accountDao.softDelete(acc1Id, deleteTimestamp)
        assertEquals(1, affectedRows)

        // Check account entity state
        val softDeletedAccount = accountDao.getById(acc1Id)
        assertNotNull(softDeletedAccount)
        assertFalse(softDeletedAccount!!.isActive)
        assertEquals(deleteTimestamp, softDeletedAccount.updatedAt)

        // Check active counts and queries
        assertEquals(1, accountDao.getActiveCountDirect())
        assertEquals(1, accountDao.getActiveAccountCount().first())

        val activeAfterDelete = accountDao.getAllActiveWithBalances().first()
        assertEquals(1, activeAfterDelete.size)
        assertEquals(acc2Id, activeAfterDelete[0].id)

        // Net worth excludes soft-deleted account: only Account 2 (30_000L)
        assertEquals(30_000L, accountDao.getTotalNetWorth().first())

        // Restore Account 1
        val restoreTimestamp = 1_111_222L
        val restoreRows = accountDao.restoreAccount(acc1Id, restoreTimestamp)
        assertEquals(1, restoreRows)

        val restoredAccount = accountDao.getById(acc1Id)
        assertNotNull(restoredAccount)
        assertTrue(restoredAccount!!.isActive)
        assertEquals(restoreTimestamp, restoredAccount.updatedAt)

        // Active counts and net worth restored
        assertEquals(2, accountDao.getActiveCountDirect())
        assertEquals(2, accountDao.getActiveAccountCount().first())
        assertEquals(45_000L, accountDao.getTotalNetWorth().first())
    }

    @Test
    fun testUpdateDisplayOrder() = runBlocking {
        val acc1Id = accountDao.insert(
            AccountEntity(name = "Account A", type = AccountType.BANK, initialBalance = 10_000L, displayOrder = 1)
        )
        val acc2Id = accountDao.insert(
            AccountEntity(name = "Account B", type = AccountType.BANK, initialBalance = 10_000L, displayOrder = 2)
        )

        var list = accountDao.getAllActiveWithBalances().first()
        assertEquals("Account A", list[0].name)
        assertEquals("Account B", list[1].name)

        // Swap order: Account A -> 10, Account B -> 5
        accountDao.updateDisplayOrder(acc1Id, 10)
        accountDao.updateDisplayOrder(acc2Id, 5)

        list = accountDao.getAllActiveWithBalances().first()
        assertEquals("Account B", list[0].name)
        assertEquals("Account A", list[1].name)
    }

    @Test
    fun testFilteredTransactionsMultiFilter() = runBlocking {
        val acc1Id = accountDao.insert(
            AccountEntity(name = "Account 1", type = AccountType.BANK, initialBalance = 10_000L)
        )
        val acc2Id = accountDao.insert(
            AccountEntity(name = "Account 2", type = AccountType.BANK, initialBalance = 10_000L)
        )

        // T1: Income, acc1, category=Salary, time=100
        val t1 = transactionDao.insert(
            TransactionEntity(type = TransactionType.INCOME, amount = 1000L, accountId = acc1Id, category = "Salary", title = "Pay", timestamp = 100L)
        )
        // T2: Expense, acc1, category=Food, time=200
        val t2 = transactionDao.insert(
            TransactionEntity(type = TransactionType.EXPENSE, amount = 200L, accountId = acc1Id, category = "Food", title = "Lunch", timestamp = 200L)
        )
        // T3: Transfer, acc1 -> acc2, category=Transfer, time=300
        val t3 = transactionDao.insert(
            TransactionEntity(type = TransactionType.TRANSFER, amount = 300L, accountId = acc1Id, toAccountId = acc2Id, category = "Transfer", title = "Send", timestamp = 300L)
        )
        // T4: Expense, acc2, category=Food, time=400
        val t4 = transactionDao.insert(
            TransactionEntity(type = TransactionType.EXPENSE, amount = 400L, accountId = acc2Id, category = "Food", title = "Dinner", timestamp = 400L)
        )

        // 1. No filters: all 4 transactions sorted timestamp DESC
        val all = transactionDao.getFilteredTransactions().first()
        assertEquals(4, all.size)
        assertEquals(listOf(t4, t3, t2, t1), all.map { it.id })

        // 2. Filter by account: acc2 (appears in T3 as toAccountId and T4 as accountId)
        val acc2Tx = transactionDao.getFilteredTransactions(accountId = acc2Id).first()
        assertEquals(2, acc2Tx.size)
        assertEquals(listOf(t4, t3), acc2Tx.map { it.id })

        // 3. Filter by type: EXPENSE (T2, T4)
        val expenses = transactionDao.getFilteredTransactions(type = TransactionType.EXPENSE).first()
        assertEquals(2, expenses.size)
        assertEquals(listOf(t4, t2), expenses.map { it.id })

        // 4. Filter by category: Food (T2, T4)
        val food = transactionDao.getFilteredTransactions(category = "Food").first()
        assertEquals(2, food.size)
        assertEquals(listOf(t4, t2), food.map { it.id })

        // 5. Filter by time range: 150L to 350L (T2, T3)
        val timeRange = transactionDao.getFilteredTransactions(startTime = 150L, endTime = 350L).first()
        assertEquals(2, timeRange.size)
        assertEquals(listOf(t3, t2), timeRange.map { it.id })

        // 6. Combined filter: acc1 AND EXPENSE AND Food
        val combined = transactionDao.getFilteredTransactions(
            accountId = acc1Id,
            type = TransactionType.EXPENSE,
            category = "Food"
        ).first()
        assertEquals(1, combined.size)
        assertEquals(t2, combined[0].id)
    }
}
