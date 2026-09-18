package com.example.budgettracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.dao.AccountDao
import com.example.budgettracker.data.local.dao.BillDetailsDao
import com.example.budgettracker.data.local.dao.CreditDetailsDao
import com.example.budgettracker.data.local.dao.InstallmentPlanDao
import com.example.budgettracker.data.local.dao.LoanDetailsDao
import com.example.budgettracker.data.local.dao.SavingsDetailsDao
import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import com.example.budgettracker.data.local.entity.BillAmountType
import com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity
import com.example.budgettracker.data.local.entity.CustomCategoryEntity
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.LoanBillingCycleEntity
import com.example.budgettracker.data.local.entity.RecurringBillEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.data.repository.CyclePaymentResult
import com.example.budgettracker.util.CurrencyUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    private lateinit var billDetailsDao: BillDetailsDao
    private lateinit var installmentPlanDao: InstallmentPlanDao
    private lateinit var savingsDetailsDao: SavingsDetailsDao
    private lateinit var creditDetailsDao: CreditDetailsDao
    private lateinit var repository: BudgetRepository

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
        billDetailsDao = database.billDetailsDao()
        installmentPlanDao = database.installmentPlanDao()
        savingsDetailsDao = database.savingsDetailsDao()
        creditDetailsDao = database.creditDetailsDao()
        repository = BudgetRepository(
            accountDao = accountDao,
            transactionDao = transactionDao,
            loanDetailsDao = loanDetailsDao,
            installmentPlanDao = installmentPlanDao,
            savingsDetailsDao = savingsDetailsDao,
            billDetailsDao = billDetailsDao,
            loanBillingCycleDao = database.loanBillingCycleDao(),
            customCategoryDao = database.customCategoryDao(),
            recurringBillDao = database.recurringBillDao(),
            creditDetailsDao = creditDetailsDao
        )
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
            dueDays = "15,30",
            minimumAmountDue = 5_000L,
            totalRemainingBalance = 50_000L,
            reminderEnabled = true,
            reminderDaysBefore = 7
        )
        loanDetailsDao.insert(loanDetails)

        val retrievedLoanDetails = loanDetailsDao.getByAccountId(loanAccId)
        assertNotNull(retrievedLoanDetails)
        assertEquals(listOf(15, 30), retrievedLoanDetails?.parseDueDays())

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

    @Test
    fun testBillAccountWithLargeAmountDueDoesNotAffectNetWorth() = runBlocking {
        // 1. Create a bank account with positive balance of 50,000 PHP (5,000,000 centavos)
        val bankId = accountDao.insert(
            AccountEntity(
                name = "BDO Savings",
                type = AccountType.BANK,
                initialBalance = 5_000_000L
            )
        )
        val initialNetWorth = accountDao.getTotalNetWorth().first()
        assertEquals(5_000_000L, initialNetWorth)

        // 2. Create a BILL account with a large amount due (100,000 PHP / 10,000,000 centavos)
        val billId = accountDao.insert(
            AccountEntity(
                name = "Meralco Electric",
                type = AccountType.BILL,
                initialBalance = 0L
            )
        )
        billDetailsDao.insert(
            BillAccountDetailsEntity(
                accountId = billId,
                dueDays = "15",
                amountDue = 10_000_000L,
                amountType = BillAmountType.FIXED
            )
        )

        // 3. Assert net worth is strictly unchanged (Bill amountDue is an upcoming expense, NOT debt/liability)
        val netWorthAfterBill = accountDao.getTotalNetWorth().first()
        assertEquals(5_000_000L, netWorthAfterBill)

        // 4. Even with an initialBalance or balance on the bill account, it must NOT add or subtract from Net Worth
        val billWithBalanceId = accountDao.insert(
            AccountEntity(
                name = "PLDT Fiber",
                type = AccountType.BILL,
                initialBalance = 250_000L
            )
        )
        val netWorthAfterBillWithBalance = accountDao.getTotalNetWorth().first()
        assertEquals(5_000_000L, netWorthAfterBillWithBalance)

        // 5. Assert that when loan accounts are queried, BILL accounts are NOT listed
        val loanAccounts = accountDao.getAllActiveLoanAccounts().first()
        assertTrue(loanAccounts.none { it.account.id == billId })
        assertTrue(loanAccounts.none { it.account.id == billWithBalanceId })
    }

    @Test
    fun testSumOfComputedBalancesEqualsTotalNetWorthAcrossAllTransactionTypes() = runBlocking {
        // Invariant helper:
        // 1. Every active account's repository.getComputedBalance(id) MUST equal account.currentBalance in activeAccountsWithBalances.
        // 2. The sum of currentBalance across all active accounts with includeInNetWorth = true MUST strictly equal repository.totalNetWorth.
        suspend fun assertBalanceAndNetWorthInvariant(stepDescription: String) {
            val activeAccounts = repository.activeAccountsWithBalances.first()
            var calculatedNetWorth = 0L

            for (acc in activeAccounts) {
                val computedBalance = repository.getComputedBalance(acc.id).first()
                assertEquals(
                    "[$stepDescription] Account ${acc.name} getComputedBalance() should match currentBalance",
                    computedBalance,
                    acc.currentBalance
                )
                if (acc.includeInNetWorth) {
                    calculatedNetWorth += acc.currentBalance
                }
            }

            val totalNetWorth = repository.totalNetWorth.first()
            assertEquals(
                "[$stepDescription] Sum of included active accounts' currentBalance must equal totalNetWorth",
                calculatedNetWorth,
                totalNetWorth
            )
        }

        // 1. Initial State: Create multiple accounts
        // - acc1: Bank, included in net worth, initialBalance = 100,000 centavos (₱1,000.00)
        // - acc2: E-Wallet, included in net worth, initialBalance = 50,000 centavos (₱500.00)
        // - acc3: Cash, EXCLUDED from net worth, initialBalance = 20,000 centavos (₱200.00)
        // - acc4: Loan/Credit, included in net worth (liability/negative initialBalance = -30,000 centavos (-₱300.00))
        val acc1Id = repository.insertAccount(
            AccountEntity(
                name = "BDO Bank",
                type = AccountType.BANK,
                initialBalance = 100_000L,
                includeInNetWorth = true
            )
        )
        val acc2Id = repository.insertAccount(
            AccountEntity(
                name = "GCash",
                type = AccountType.E_WALLET,
                initialBalance = 50_000L,
                includeInNetWorth = true
            )
        )
        val acc3Id = repository.insertAccount(
            AccountEntity(
                name = "Physical Cash",
                type = AccountType.CASH,
                initialBalance = 20_000L,
                includeInNetWorth = false
            )
        )
        val acc4Id = repository.insertAccount(
            AccountEntity(
                name = "Credit Card",
                type = AccountType.LOAN,
                initialBalance = -30_000L,
                includeInNetWorth = true
            )
        )

        // Expected Net Worth: 100,000 + 50,000 - 30,000 = 120,000 centavos (acc3 excluded)
        assertBalanceAndNetWorthInvariant("Initial state")
        assertEquals(120_000L, repository.totalNetWorth.first())

        val now = System.currentTimeMillis()

        // 2. INCOME transaction (+25,000 centavos to acc1)
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 25_000L,
                accountId = acc1Id,
                category = "Salary",
                title = "Paycheck",
                timestamp = now + 1
            )
        )
        assertBalanceAndNetWorthInvariant("After Income on acc1")
        assertEquals(145_000L, repository.totalNetWorth.first())

        // 3. EXPENSE transaction (-10,000 centavos from acc2)
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 10_000L,
                accountId = acc2Id,
                category = "Food",
                title = "Dinner",
                timestamp = now + 2
            )
        )
        assertBalanceAndNetWorthInvariant("After Expense on acc2")
        assertEquals(135_000L, repository.totalNetWorth.first())

        // 4. TRANSFER transaction between two included accounts (acc1 -> acc2, 15,000 centavos)
        // Net worth should NOT change
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = 15_000L,
                accountId = acc1Id,
                toAccountId = acc2Id,
                category = "Transfer",
                title = "Fund E-wallet",
                timestamp = now + 3
            )
        )
        assertBalanceAndNetWorthInvariant("After Transfer acc1 -> acc2")
        assertEquals(135_000L, repository.totalNetWorth.first())

        // 5. TRANSFER from included account to excluded account (acc1 -> acc3, 5,000 centavos)
        // Net worth should drop by 5,000
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = 5_000L,
                accountId = acc1Id,
                toAccountId = acc3Id,
                category = "ATM Withdrawal",
                title = "Cash Out",
                timestamp = now + 4
            )
        )
        assertBalanceAndNetWorthInvariant("After Transfer acc1 -> acc3 (included to excluded)")
        assertEquals(130_000L, repository.totalNetWorth.first())

        // 6. TRANSFER from excluded account to included account (acc3 -> acc2, 2,000 centavos)
        // Net worth should increase by 2,000
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = 2_000L,
                accountId = acc3Id,
                toAccountId = acc2Id,
                category = "Deposit",
                title = "Deposit Cash",
                timestamp = now + 5
            )
        )
        assertBalanceAndNetWorthInvariant("After Transfer acc3 -> acc2 (excluded to included)")
        assertEquals(132_000L, repository.totalNetWorth.first())

        // 7. ADJUSTMENT transaction (logged balance adjustment, isAdjustment = true, INCOME on acc4 +10,000)
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 10_000L,
                accountId = acc4Id,
                isAdjustment = true,
                category = "Adjustment",
                title = "Balance Adjustment: Credit Card",
                timestamp = now + 6
            )
        )
        assertBalanceAndNetWorthInvariant("After Adjustment on acc4")
        assertEquals(142_000L, repository.totalNetWorth.first())

        // 8. INSTALLMENT transaction (-6,000 centavos from acc1)
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.INSTALLMENT,
                amount = 6_000L,
                accountId = acc1Id,
                category = "Electronics",
                title = "Gadget purchase",
                timestamp = now + 7
            )
        )
        assertBalanceAndNetWorthInvariant("After Installment on acc1")
        assertEquals(136_000L, repository.totalNetWorth.first())

        // 9. SOFT DELETE an account (acc2)
        // acc2 balance: initial 50,000 - 10,000 (expense) + 15,000 (transfer in) + 2,000 (transfer in) = 57,000 centavos
        repository.softDeleteAccount(acc2Id)
        assertBalanceAndNetWorthInvariant("After Soft Delete acc2")
        // Net worth should no longer include acc2: 136,000 - 57,000 = 79,000 centavos
        assertEquals(79_000L, repository.totalNetWorth.first())
        val activeAccountsAfterDelete = repository.activeAccountsWithBalances.first()
        assertTrue(activeAccountsAfterDelete.none { it.id == acc2Id })

        // 10. RESTORE the account (acc2)
        repository.restoreAccount(acc2Id)
        assertBalanceAndNetWorthInvariant("After Restore acc2")
        assertEquals(136_000L, repository.totalNetWorth.first())
        val activeAccountsAfterRestore = repository.activeAccountsWithBalances.first()
        assertTrue(activeAccountsAfterRestore.any { it.id == acc2Id })
    }

    @Test
    fun testPayBillTransferReducesLoanDebtTowardZero() = runBlocking {
        // Source Bank account: ₱10,000.00 = 1,000,000 centavos
        val bankId = repository.insertAccount(
            AccountEntity(
                name = "BPI Checking",
                type = AccountType.BANK,
                initialBalance = 1_000_000L
            )
        )

        // Loan / BNPL account: ₱0 initial balance
        val loanId = repository.insertAccount(
            AccountEntity(
                name = "SPayLater",
                type = AccountType.LOAN,
                initialBalance = 0L
            )
        )

        // Log an Expense of ₱3,000.00 (300,000 centavos) on Loan account
        val expenseId = repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 300_000L,
                accountId = loanId,
                category = "Electronics",
                title = "Gadget purchase",
                timestamp = System.currentTimeMillis()
            )
        )
        assertTrue(expenseId > 0)

        // Computed balance of Loan account is now -300,000 centavos (-₱3,000.00)
        assertEquals(-300_000L, repository.getComputedBalance(loanId).first())
        assertEquals(1_000_000L, repository.getComputedBalance(bankId).first())
        // Net worth: 1,000,000 + (-300,000) = 700,000
        assertEquals(700_000L, repository.totalNetWorth.first())

        // "Pay Bill" transfer: ₱1,000.00 (100,000 centavos) from Bank into Loan
        val transferId = repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = 100_000L,
                accountId = bankId,
                toAccountId = loanId,
                category = "Bill Payment",
                title = "Loan Bill Payment",
                timestamp = System.currentTimeMillis() + 1000
            )
        )
        assertTrue(transferId > 0)

        // Assert Loan balance increased (moved toward zero / debt reduced) to -200,000 centavos
        assertEquals(-200_000L, repository.getComputedBalance(loanId).first())
        // Assert Bank balance decreased to 900,000 centavos
        assertEquals(900_000L, repository.getComputedBalance(bankId).first())
        // Assert Net worth conserved (900,000 + (-200,000) = 700,000)
        assertEquals(700_000L, repository.totalNetWorth.first())
    }

    @Test
    fun testLoanTotalOwedStaysAccurateWithPlainExpensesOutsideInstallments() = runBlocking {
        val loanId = repository.insertAccount(
            AccountEntity(
                name = "Atome",
                type = AccountType.BNPL,
                initialBalance = 0L
            )
        )

        // Plain direct expense 1 (no installment plan linked)
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 150_000L, // ₱1,500.00
                accountId = loanId,
                category = "Dining",
                title = "Dinner with Friends",
                timestamp = System.currentTimeMillis()
            )
        )

        var balance = repository.getComputedBalance(loanId).first()
        assertEquals(-150_000L, balance)
        assertEquals("-₱1,500.00", CurrencyUtils.formatCentavosToPesos(balance!!))

        // Plain direct expense 2
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 85_000L, // ₱850.00
                accountId = loanId,
                category = "Shopping",
                title = "Shoes",
                timestamp = System.currentTimeMillis() + 1000
            )
        )

        balance = repository.getComputedBalance(loanId).first()
        assertEquals(-235_000L, balance)
        assertEquals("-₱2,350.00", CurrencyUtils.formatCentavosToPesos(balance!!))
    }

    @Test
    fun testAvailableCreditWithAndWithoutCreditLimit() {
        // With limit ₱20,000.00 and debt ₱3,484.82 (balance = -348,482 centavos)
        val limit = 2_000_000L
        val balance = -348_482L
        val available = BudgetRepository.calculateAvailableCredit(limit, balance)
        assertEquals(1_651_518L, available) // ₱16,515.18

        // Without credit limit (null)
        val availableWithoutLimit = BudgetRepository.calculateAvailableCredit(null, balance)
        assertNull(availableWithoutLimit)

        // When debt exceeds limit, available credit goes negative (not clamped to 0)
        val smallLimit = 100_000L
        val largeDebt = -150_000L
        val availableCapped = BudgetRepository.calculateAvailableCredit(smallLimit, largeDebt)
        assertEquals(-50_000L, availableCapped)
    }

    @Test
    fun testCyclePaidTransitionLogic() = runBlocking {
        val loanId = repository.insertAccount(
            AccountEntity(
                name = "Home Credit",
                type = AccountType.LOAN,
                initialBalance = 0L
            )
        )

        repository.insertLoanDetails(
            LoanAccountDetailsEntity(
                accountId = loanId,
                dueDays = "15",
                minimumAmountDue = 250_000L,
                totalRemainingBalance = 500_000L,
                reminderEnabled = true,
                reminderDaysBefore = 7
            )
        )

        // Ensure pending cycles exist
        repository.ensurePendingCyclesForAccount(loanId, listOf(15), 250_000L)

        val pendingCycles = repository.getPendingBillingCycles(loanId).first()
        assertEquals(1, pendingCycles.size)
        assertEquals(250_000L, pendingCycles[0].amountDue)
        assertFalse(pendingCycles[0].isPaid)

        // Mark cycle as paid
        repository.markBillingCyclePaid(pendingCycles[0].id, 250_000L)

        // Verify pending cycles contains the rolled-over next cycle (Bug 2 fix)
        val pendingAfter = repository.getPendingBillingCycles(loanId).first()
        assertEquals(1, pendingAfter.size)
        assertFalse(pendingAfter[0].id == pendingCycles[0].id)
        assertFalse(pendingAfter[0].isPaid)

        // Verify paid billing cycles contains 1 record
        val paidCycles = repository.getPaidBillingCycles(loanId).first()
        assertEquals(1, paidCycles.size)
        assertEquals(250_000L, paidCycles[0].paidAmount)
        assertTrue(paidCycles[0].isPaid)
        assertEquals(pendingCycles[0].id, paidCycles[0].id)
    }

    @Test
    fun testCustomCategoryDuplicateNamePrevention() = runBlocking {
        // Insert custom category "Shopping" for EXPENSE
        repository.insertCustomCategory(
            CustomCategoryEntity(
                name = "Shopping",
                transactionType = TransactionType.EXPENSE,
                iconName = "shopping_bag"
            )
        )

        // Attempting to insert lowercase "shopping" should fail
        var caughtException = false
        try {
            repository.insertCustomCategory(
                CustomCategoryEntity(
                    name = "shopping",
                    transactionType = TransactionType.EXPENSE,
                    iconName = "shopping_bag"
                )
            )
        } catch (e: IllegalArgumentException) {
            caughtException = true
        }
        assertTrue("Expected IllegalArgumentException for duplicate case-insensitive category name", caughtException)

        // Attempting to insert with padding "  Shopping  " should also fail
        var caughtWhitespace = false
        try {
            repository.insertCustomCategory(
                CustomCategoryEntity(
                    name = "  Shopping  ",
                    transactionType = TransactionType.EXPENSE,
                    iconName = "shopping_bag"
                )
            )
        } catch (e: IllegalArgumentException) {
            caughtWhitespace = true
        }
        assertTrue("Expected IllegalArgumentException for whitespace-padded category name", caughtWhitespace)

        // Inserting "Shopping" for INCOME (different transaction type) should succeed
        val incomeCatId = repository.insertCustomCategory(
            CustomCategoryEntity(
                name = "Shopping",
                transactionType = TransactionType.INCOME,
                iconName = "shopping_bag"
            )
        )
        assertTrue(incomeCatId > 0)

        val expenseCats = repository.getCustomCategories(TransactionType.EXPENSE).first()
        assertEquals(1, expenseCats.size)

        val incomeCats = repository.getCustomCategories(TransactionType.INCOME).first()
        assertEquals(1, incomeCats.size)
    }

    @Test
    fun testDashboardWidgetAndAccountCurrentTabAlwaysAgreeOnPendingCycles() = runBlocking {
        val mayaId = accountDao.insert(
            AccountEntity(name = "Maya Credit", type = AccountType.LOAN, initialBalance = 0L)
        )
        val spayId = accountDao.insert(
            AccountEntity(name = "SPayLater", type = AccountType.BNPL, initialBalance = 0L)
        )

        repository.insertLoanDetails(
            LoanAccountDetailsEntity(accountId = mayaId, dueDays = "15,30", minimumAmountDue = 100_000L, totalRemainingBalance = 500_000L)
        )
        repository.insertLoanDetails(
            LoanAccountDetailsEntity(accountId = spayId, dueDays = "5", minimumAmountDue = 50_000L, totalRemainingBalance = 200_000L)
        )

        repository.ensurePendingCyclesForAccount(mayaId, listOf(15, 30), 100_000L)
        repository.ensurePendingCyclesForAccount(spayId, listOf(5), 50_000L)

        val allPending = repository.getAllPendingBillingCycles().first()
        assertEquals(3, allPending.size)

        val mayaPending = repository.getPendingBillingCycles(mayaId).first()
        val spayPending = repository.getPendingBillingCycles(spayId).first()

        assertEquals(2, mayaPending.size)
        assertEquals(1, spayPending.size)

        val dashboardMayaCycles = allPending.filter { it.accountId == mayaId }
        assertEquals(mayaPending.map { it.id }.toSet(), dashboardMayaCycles.map { it.id }.toSet())
        assertEquals(mayaPending.map { it.amountDue }, dashboardMayaCycles.map { it.amountDue })

        repository.markBillingCyclePaid(mayaPending.first().id, 100_000L)

        val allPendingAfter = repository.getAllPendingBillingCycles().first()
        val mayaPendingAfter = repository.getPendingBillingCycles(mayaId).first()
        assertEquals(3, allPendingAfter.size)
        assertEquals(2, mayaPendingAfter.size)
        val dashboardMayaCyclesAfter = allPendingAfter.filter { it.accountId == mayaId }
        assertEquals(mayaPendingAfter.map { it.id }.toSet(), dashboardMayaCyclesAfter.map { it.id }.toSet())
    }

    @Test
    fun testMultipleDueDatesPerAccountIndependentCycles() = runBlocking {
        val loanId = accountDao.insert(
            AccountEntity(name = "Multi-Cycle Loan", type = AccountType.LOAN, initialBalance = 0L)
        )
        repository.insertLoanDetails(
            LoanAccountDetailsEntity(accountId = loanId, dueDays = "1,15,30", minimumAmountDue = 50_000L, totalRemainingBalance = 150_000L)
        )

        repository.ensurePendingCyclesForAccount(loanId, listOf(1, 15, 30), 50_000L)

        val pending = repository.getPendingBillingCycles(loanId).first()
        assertEquals(3, pending.size)

        val firstCycle = pending[0]
        repository.markBillingCyclePaid(firstCycle.id, 50_000L)

        val remainingPending = repository.getPendingBillingCycles(loanId).first()
        assertEquals(3, remainingPending.size)
        assertFalse(remainingPending.any { it.id == firstCycle.id })

        val paid = repository.getPaidBillingCycles(loanId).first()
        assertEquals(1, paid.size)
        assertEquals(firstCycle.id, paid[0].id)
    }

    @Test
    fun testCaughtUpEmptyStateTriggerCondition() = runBlocking {
        val loanId = accountDao.insert(
            AccountEntity(name = "GLoan", type = AccountType.LOAN, initialBalance = 0L)
        )
        repository.insertLoanDetails(
            LoanAccountDetailsEntity(accountId = loanId, dueDays = "10", minimumAmountDue = 80_000L, totalRemainingBalance = 80_000L)
        )

        repository.ensurePendingCyclesForAccount(loanId, listOf(10), 80_000L)
        val pending1 = repository.getPendingBillingCycles(loanId).first()
        assertEquals(1, pending1.size)

        repository.markBillingCyclePaid(pending1[0].id, 80_000L)
        val pendingAfter = repository.getPendingBillingCycles(loanId).first()
        assertTrue(pendingAfter.isEmpty())
    }

    @Test
    fun testInstallmentToBalanceAutomaticSync() = runBlocking {
        val loanId = accountDao.insert(
            AccountEntity(name = "BPI Credit Card", type = AccountType.LOAN, initialBalance = 0L)
        )

        val initialBalance = repository.getComputedBalance(loanId).first() ?: 0L
        assertEquals(0L, initialBalance)

        val planId = repository.insertInstallmentPlan(
            InstallmentPlanEntity(
                accountId = loanId,
                title = "MacBook Pro",
                category = "Electronics",
                totalPurchaseAmount = 120_000_00L,
                totalInstallments = 12,
                installmentsPaid = 0,
                remainingBalance = 120_000_00L,
                monthlyPaymentAmount = 10_000_00L,
                purchaseDate = System.currentTimeMillis()
            )
        )
        assertTrue(planId > 0)

        val balanceAfterInsert = repository.getComputedBalance(loanId).first() ?: 0L
        assertEquals(-120_000_00L, balanceAfterInsert)

        val txs = transactionDao.getByInstallmentPlanDirect(planId)
        assertEquals(1, txs.size)
        assertEquals(TransactionType.INSTALLMENT, txs[0].type)
        assertEquals(120_000_00L, txs[0].amount)

        val plan = installmentPlanDao.getById(planId)!!
        repository.updateInstallmentPlan(plan.copy(totalPurchaseAmount = 100_000_00L, remainingBalance = 100_000_00L))

        val balanceAfterUpdate = repository.getComputedBalance(loanId).first() ?: 0L
        assertEquals(-100_000_00L, balanceAfterUpdate)

        repository.deleteInstallmentPlan(plan)
        val balanceAfterDelete = repository.getComputedBalance(loanId).first() ?: 0L
        assertEquals(0L, balanceAfterDelete)
        val txsAfterDelete = transactionDao.getByInstallmentPlanDirect(planId)
        assertTrue(txsAfterDelete.isEmpty())
    }

    @Test
    fun testUpcomingBillsNavigationRouting() = runBlocking {
        val loanId = accountDao.insert(
            AccountEntity(name = "LazPayLater", type = AccountType.BNPL, initialBalance = 0L)
        )
        repository.insertLoanDetails(
            LoanAccountDetailsEntity(accountId = loanId, dueDays = "15", minimumAmountDue = 30_000L, totalRemainingBalance = 60_000L)
        )
        repository.ensurePendingCyclesForAccount(loanId, listOf(15), 30_000L)

        val recurringBillId = repository.insertRecurringBill(
            RecurringBillEntity(name = "Converge Fiber", amount = 150_000L, dueDay = 20)
        )
        assertTrue(recurringBillId > 0)

        val pendingCycles = repository.getAllPendingBillingCycles().first()
        val recurringBills = repository.activeRecurringBills.first()

        val cycleItem = pendingCycles.first()
        assertEquals(loanId, cycleItem.accountId)

        val billItem = recurringBills.first()
        assertEquals(recurringBillId, billItem.id)
        assertNull(billItem.accountId)
    }

    @Test
    fun testCreditSingleCycleGeneration() = runBlocking {
        val creditId = accountDao.insert(
            AccountEntity(name = "BPI Credit Card", type = AccountType.CREDIT, initialBalance = -5_000_00L)
        )
        creditDetailsDao.insert(
            CreditAccountDetailsEntity(accountId = creditId, creditLimit = 50_000_00L, statementDueDay = 15)
        )

        val today = LocalDate.of(2026, 9, 17)
        repository.ensurePendingCyclesForAccount(creditId, today)

        val pendingCycles = repository.getPendingBillingCycles(creditId).first()
        assertEquals(1, pendingCycles.size)
        val cycle = pendingCycles[0]
        assertEquals(creditId, cycle.accountId)
        assertEquals(5_000_00L, cycle.amountDue)
        assertFalse(cycle.isPaid)
        assertFalse(cycle.isManualOverride)

        // Calling ensurePendingCyclesForAccount again does not duplicate
        repository.ensurePendingCyclesForAccount(creditId, today)
        val pendingAfter = repository.getPendingBillingCycles(creditId).first()
        assertEquals(1, pendingAfter.size)
    }

    @Test
    fun testCreditAutoComputedButOverridableStatementAmount() = runBlocking {
        val creditId = accountDao.insert(
            AccountEntity(name = "Citi Rewards", type = AccountType.CREDIT, initialBalance = -5_000_00L)
        )
        creditDetailsDao.insert(
            CreditAccountDetailsEntity(accountId = creditId, creditLimit = 100_000_00L, statementDueDay = 20)
        )
        val today = LocalDate.of(2026, 9, 17)
        repository.ensurePendingCyclesForAccount(creditId, today)

        var pending = repository.getPendingBillingCycles(creditId).first().first()
        assertEquals(5_000_00L, pending.amountDue)
        assertFalse(pending.isManualOverride)

        // Make an expense transaction on this card
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 1_500_00L,
                accountId = creditId,
                category = "Groceries",
                title = "Supermarket",
                timestamp = System.currentTimeMillis()
            )
        )
        val balance = repository.getComputedBalance(creditId).first() ?: 0L
        assertEquals(-6_500_00L, balance)

        // Re-running ensurePendingCycles should auto-sync statement amount
        repository.ensurePendingCyclesForAccount(creditId, today)
        pending = repository.getPendingBillingCycles(creditId).first().first()
        assertEquals(6_500_00L, pending.amountDue)
        assertFalse(pending.isManualOverride)

        // User overrides amount due manually to ₱4,000.00
        repository.updateCycleAmountDue(pending.id, 4_000_00L, isManualOverride = true)
        pending = repository.getPendingBillingCycles(creditId).first().first()
        assertEquals(4_000_00L, pending.amountDue)
        assertTrue(pending.isManualOverride)

        // Another purchase is made
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 500_00L,
                accountId = creditId,
                category = "Dining",
                title = "Coffee",
                timestamp = System.currentTimeMillis()
            )
        )
        // ensurePendingCycles should NOT overwrite user manual override
        repository.ensurePendingCyclesForAccount(creditId, today)
        pending = repository.getPendingBillingCycles(creditId).first().first()
        assertEquals(4_000_00L, pending.amountDue)
        assertTrue(pending.isManualOverride)
    }

    @Test
    fun testPartialPaymentReducesAmountDueAndKeepsPending() = runBlocking {
        val creditId = accountDao.insert(
            AccountEntity(name = "UnionBank Miles", type = AccountType.CREDIT, initialBalance = -10_000_00L)
        )
        creditDetailsDao.insert(
            CreditAccountDetailsEntity(accountId = creditId, creditLimit = 80_000_00L, statementDueDay = 25)
        )
        val today = LocalDate.of(2026, 9, 17)
        repository.ensurePendingCyclesForAccount(creditId, today)
        val cycle = repository.getPendingBillingCycles(creditId).first().first()
        assertEquals(10_000_00L, cycle.amountDue)

        // Pay partial amount: ₱4,000.00
        val result = repository.recordBillingCyclePayment(cycle.id, 4_000_00L)
        assertTrue(result is CyclePaymentResult.Partial)
        val partial = result as CyclePaymentResult.Partial
        assertEquals(4_000_00L, partial.amountPaid)
        assertEquals(6_000_00L, partial.remainingDue)

        // Verify cycle in database
        val pendingCycles = repository.getPendingBillingCycles(creditId).first()
        assertEquals(1, pendingCycles.size)
        val updatedCycle = pendingCycles.first()
        assertEquals(6_000_00L, updatedCycle.amountDue)
        assertEquals(4_000_00L, updatedCycle.paidAmount)
        assertFalse(updatedCycle.isPaid)
        assertNull(updatedCycle.paidDate)
    }

    @Test
    fun testOverpaymentMarksPaidInFullAndAbsorbsOverpayment() = runBlocking {
        val creditId = accountDao.insert(
            AccountEntity(name = "RCBC Hexagon", type = AccountType.CREDIT, initialBalance = -3_000_00L)
        )
        creditDetailsDao.insert(
            CreditAccountDetailsEntity(accountId = creditId, creditLimit = 100_000_00L, statementDueDay = 10)
        )
        val today = LocalDate.of(2026, 9, 17)
        repository.ensurePendingCyclesForAccount(creditId, today)
        val cycle = repository.getPendingBillingCycles(creditId).first().first()
        assertEquals(3_000_00L, cycle.amountDue)

        // Overpay: ₱5,000.00 on a ₱3,000.00 statement (recording payment transaction reflects balance)
        val checkingId = accountDao.insert(
            AccountEntity(name = "BDO Bank", type = AccountType.BANK, initialBalance = 10_000_00L)
        )
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = 5_000_00L,
                accountId = checkingId,
                toAccountId = creditId,
                category = "Payment",
                title = "Card Payment",
                timestamp = System.currentTimeMillis()
            )
        )
        val result = repository.recordBillingCyclePayment(cycle.id, 5_000_00L)
        assertTrue(result is CyclePaymentResult.PaidInFull)
        val paidInFull = result as CyclePaymentResult.PaidInFull
        assertEquals(5_000_00L, paidInFull.amountPaid)
        assertEquals(2_000_00L, paidInFull.overpaymentAmount)

        // Verify cycle in database is settled and no new cycle needed since balance is now positive (₱2,000 credit)
        val pendingCycles = repository.getPendingBillingCycles(creditId).first()
        assertTrue(pendingCycles.isEmpty())

        val paidCycles = repository.getPaidBillingCycles(creditId).first()
        assertEquals(1, paidCycles.size)
        val settledCycle = paidCycles.first()
        assertEquals(3_000_00L, settledCycle.amountDue)
        assertEquals(5_000_00L, settledCycle.paidAmount)
        assertTrue(settledCycle.isPaid)
        assertNotNull(settledCycle.paidDate)
    }

    @Test
    fun testAutomaticCycleRolloverOnPaidInFull() = runBlocking {
        val creditId = accountDao.insert(
            AccountEntity(name = "Metrobank Titanium", type = AccountType.CREDIT, initialBalance = -4_000_00L)
        )
        creditDetailsDao.insert(
            CreditAccountDetailsEntity(accountId = creditId, creditLimit = 50_000_00L, statementDueDay = 15)
        )
        val today = LocalDate.of(2026, 9, 17)
        repository.ensurePendingCyclesForAccount(creditId, today)
        val cycle = repository.getPendingBillingCycles(creditId).first().first()

        // Pay in full
        val result = repository.recordBillingCyclePayment(cycle.id, 4_000_00L)
        assertTrue(result is CyclePaymentResult.PaidInFull)

        // Make another purchase for next cycle
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 2_500_00L,
                accountId = creditId,
                category = "Shopping",
                title = "Shoes",
                timestamp = System.currentTimeMillis()
            )
        )

        // Rollover: generate next cycle
        repository.ensurePendingCyclesForAccount(creditId, today)
        val newPendingCycles = repository.getPendingBillingCycles(creditId).first()
        assertEquals(1, newPendingCycles.size)
        val nextCycle = newPendingCycles.first()
        assertFalse(nextCycle.isPaid)
        val nextDueDate = Instant.ofEpochMilli(nextCycle.cycleDueDate).atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(15, nextDueDate.dayOfMonth)
        assertEquals(10, nextDueDate.monthValue)
        assertEquals(2026, nextDueDate.year)
    }

    @Test
    fun testNoCycleGeneratedWhenBalanceIsZeroOrPositiveAtCreationForLoanBnplCredit() = runBlocking {
        // Loan with 0 balance and 0 remaining debt
        val loanId = accountDao.insert(
            AccountEntity(name = "Zero Loan", type = AccountType.LOAN, initialBalance = 0L)
        )
        repository.insertLoanDetails(
            LoanAccountDetailsEntity(accountId = loanId, dueDays = "15", minimumAmountDue = 100_000L, totalRemainingBalance = 0L)
        )
        val loanPending = repository.getPendingBillingCycles(loanId).first()
        assertTrue(loanPending.isEmpty())

        // BNPL with 0 balance and 0 remaining debt
        val bnplId = accountDao.insert(
            AccountEntity(name = "Zero BNPL", type = AccountType.BNPL, initialBalance = 0L)
        )
        repository.insertLoanDetails(
            LoanAccountDetailsEntity(accountId = bnplId, dueDays = "20", minimumAmountDue = 50_000L, totalRemainingBalance = 0L)
        )
        val bnplPending = repository.getPendingBillingCycles(bnplId).first()
        assertTrue(bnplPending.isEmpty())

        // Credit with 0 balance
        val creditId = accountDao.insert(
            AccountEntity(name = "Zero Credit", type = AccountType.CREDIT, initialBalance = 0L)
        )
        repository.insertCreditDetails(
            CreditAccountDetailsEntity(accountId = creditId, creditLimit = 50_000_00L, statementDueDay = 15)
        )
        val creditPending = repository.getPendingBillingCycles(creditId).first()
        assertTrue(creditPending.isEmpty())
    }

    @Test
    fun testStartingBalanceDefaultsToZeroForLoanBnplCredit() = runBlocking {
        val today = LocalDate.of(2026, 9, 17)

        val creditId = accountDao.insert(
            AccountEntity(name = "New Card", type = AccountType.CREDIT, initialBalance = 0L)
        )
        repository.insertCreditDetails(
            CreditAccountDetailsEntity(accountId = creditId, creditLimit = 30_000_00L, statementDueDay = 10)
        )
        assertEquals(0L, repository.getComputedBalance(creditId).first())
        repository.ensurePendingCyclesForAccount(creditId, today)
        assertTrue(repository.getPendingBillingCycles(creditId).first().isEmpty())

        // Once a debt is incurred (e.g., expense), cycle is created
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 2_000_00L,
                accountId = creditId,
                category = "Groceries",
                title = "Market",
                timestamp = System.currentTimeMillis()
            )
        )
        assertEquals(-2_000_00L, repository.getComputedBalance(creditId).first())
        repository.ensurePendingCyclesForAccount(creditId, today)
        val pendingAfterExpense = repository.getPendingBillingCycles(creditId).first()
        assertEquals(1, pendingAfterExpense.size)
        assertEquals(2_000_00L, pendingAfterExpense.first().amountDue)
    }

    @Test
    fun testNetWorthInclusionToggleForCreditLoanBnpl() = runBlocking {
        // 1. Create a Savings baseline account with ₱50,000 balance
        val savingsId = accountDao.insert(
            AccountEntity(name = "BPI Savings", type = AccountType.SAVINGS, initialBalance = 50_000_00L)
        )
        assertEquals(50_000_00L, repository.totalNetWorth.first())

        // 2. Create a Credit account with ₱10,000 expense debt (includeInNetWorth = true by default)
        val creditId = accountDao.insert(
            AccountEntity(name = "BDO Credit Card", type = AccountType.CREDIT, initialBalance = 0L)
        )
        repository.insertCreditDetails(
            CreditAccountDetailsEntity(accountId = creditId, creditLimit = 50_000_00L, statementDueDay = 15)
        )
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 10_000_00L,
                accountId = creditId,
                category = "Electronics",
                title = "Gadget",
                timestamp = System.currentTimeMillis()
            )
        )
        // Balance is -₱10,000
        assertEquals(-10_000_00L, repository.getComputedBalance(creditId).first())
        // Net worth should be ₱50,000 - ₱10,000 = ₱40,000
        assertEquals(40_000_00L, repository.totalNetWorth.first())

        // 3. Toggle Credit account includeInNetWorth = false -> Net worth restores to ₱50,000
        repository.updateNetWorthInclusion(creditId, false)
        assertEquals(50_000_00L, repository.totalNetWorth.first())

        // Toggle back to true -> Net worth reduces to ₱40,000
        repository.updateNetWorthInclusion(creditId, true)
        assertEquals(40_000_00L, repository.totalNetWorth.first())

        // 4. Create a Loan/BNPL account with ₱15,000 installment debt
        val loanId = accountDao.insert(
            AccountEntity(name = "SPayLater", type = AccountType.BNPL, initialBalance = 0L)
        )
        repository.insertLoanDetails(
            LoanAccountDetailsEntity(accountId = loanId, dueDays = "15", minimumAmountDue = 5_000_00L, totalRemainingBalance = 0L)
        )
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.INSTALLMENT,
                amount = 15_000_00L,
                accountId = loanId,
                category = "Shopping",
                title = "Phone Installment",
                timestamp = System.currentTimeMillis()
            )
        )
        // Loan balance is -₱15,000
        assertEquals(-15_000_00L, repository.getComputedBalance(loanId).first())
        // Net worth should now be ₱50,000 - ₱10,000 (credit) - ₱15,000 (loan) = ₱25,000
        assertEquals(25_000_00L, repository.totalNetWorth.first())

        // 5. Toggle Loan/BNPL account includeInNetWorth = false -> Net worth increases by ₱15,000 to ₱40,000
        repository.updateNetWorthInclusion(loanId, false)
        assertEquals(40_000_00L, repository.totalNetWorth.first())

        // Toggle Credit account includeInNetWorth = false -> Net worth becomes ₱50,000 (both excluded)
        repository.updateNetWorthInclusion(creditId, false)
        assertEquals(50_000_00L, repository.totalNetWorth.first())

        // Toggle both back to true -> Net worth becomes ₱25,000
        repository.updateNetWorthInclusion(creditId, true)
        repository.updateNetWorthInclusion(loanId, true)
        assertEquals(25_000_00L, repository.totalNetWorth.first())
    }

    @Test
    fun testCalculateAvailableCredit() {
        // 1. Under limit: limit ₱50,000, balance -₱10,000 -> available ₱40,000
        val underLimit = BudgetRepository.calculateAvailableCredit(
            creditLimit = 50_000_00L,
            currentBalance = -10_000_00L
        )
        assertEquals(40_000_00L, underLimit)

        // 2. Zero debt: limit ₱50,000, balance ₱0 -> available ₱50,000
        val zeroDebt = BudgetRepository.calculateAvailableCredit(
            creditLimit = 50_000_00L,
            currentBalance = 0L
        )
        assertEquals(50_000_00L, zeroDebt)

        // 3. Over limit (negative available credit): limit ₱10,000, balance -₱15,000 -> available -₱5,000 (not clamped to 0)
        val overLimit = BudgetRepository.calculateAvailableCredit(
            creditLimit = 10_000_00L,
            currentBalance = -15_000_00L
        )
        assertEquals(-5_000_00L, overLimit)

        // 4. No credit limit set: returns null for fallback
        val noLimit = BudgetRepository.calculateAvailableCredit(
            creditLimit = null,
            currentBalance = -5_000_00L
        )
        assertNull(noLimit)
    }

    @Test
    fun testLoanBillingCycleAutoComputedAmountDueFromActiveInstallments() = runBlocking {
        // 1. Create a brand-new BNPL account (starts at ₱0 debt, no installments yet)
        val accountId = repository.insertAccount(
            AccountEntity(name = "SPayLater", type = AccountType.BNPL, initialBalance = 0L)
        )
        repository.insertLoanDetails(
            LoanAccountDetailsEntity(
                accountId = accountId,
                dueDays = "15",
                minimumAmountDue = 0L,
                totalRemainingBalance = 0L,
                creditLimit = 10_000_00L
            )
        )

        // With 0 debt and 0 installments, there are no pending cycles yet
        var pending = repository.getPendingCyclesDirect(accountId)
        assertEquals(0, pending.size)

        // 2. Add first installment: Air Frier ₱780/mo (total ₱2,340, 3 mos)
        val plan1Id = repository.insertInstallmentPlan(
            InstallmentPlanEntity(
                accountId = accountId,
                title = "Air Frier",
                category = "Appliances",
                totalPurchaseAmount = 2_340_00L,
                totalInstallments = 3,
                installmentsPaid = 0,
                remainingBalance = 2_340_00L,
                monthlyPaymentAmount = 780_00L
            )
        )

        pending = repository.getPendingCyclesDirect(accountId)
        assertEquals(1, pending.size)
        assertEquals(780_00L, pending[0].amountDue)
        assertFalse(pending[0].isManualOverride)

        // 3. Add second installment: Phone ₱1,200/mo (total ₱7,200, 6 mos)
        val plan2Id = repository.insertInstallmentPlan(
            InstallmentPlanEntity(
                accountId = accountId,
                title = "Phone",
                category = "Electronics",
                totalPurchaseAmount = 7_200_00L,
                totalInstallments = 6,
                installmentsPaid = 0,
                remainingBalance = 7_200_00L,
                monthlyPaymentAmount = 1_200_00L
            )
        )

        // Sum should automatically update to ₱1,980.00 (780 + 1200)
        pending = repository.getPendingCyclesDirect(accountId)
        assertEquals(1, pending.size)
        assertEquals(1_980_00L, pending[0].amountDue)
        assertFalse(pending[0].isManualOverride)

        // 4. Test Manual Override persistence: User manually edits Amount Due to ₱2,100.00 (e.g. added bill fees)
        repository.updateCycleAmountDue(pending[0].id, 2_100_00L, isManualOverride = true)
        pending = repository.getPendingCyclesDirect(accountId)
        assertEquals(2_100_00L, pending[0].amountDue)
        assertTrue(pending[0].isManualOverride)

        // Re-running ensurePendingCyclesForAccount must NOT overwrite manual override
        repository.ensurePendingCyclesForAccount(accountId)
        pending = repository.getPendingCyclesDirect(accountId)
        assertEquals(2_100_00L, pending[0].amountDue)
        assertTrue(pending[0].isManualOverride)

        // 5. Test completion of an installment plan
        // Reset manual override so auto-compute takes effect
        repository.updateCycleAmountDue(pending[0].id, 1_980_00L, isManualOverride = false)

        // Pay off Air Frier (remainingBalance becomes 0)
        val airFrier = installmentPlanDao.getById(plan1Id)!!
        repository.updateInstallmentPlan(airFrier.copy(remainingBalance = 0L, installmentsPaid = 3))

        // Amount Due should now only include the active Phone installment: ₱1,200.00
        pending = repository.getPendingCyclesDirect(accountId)
        assertEquals(1, pending.size)
        assertEquals(1_200_00L, pending[0].amountDue)
        assertFalse(pending[0].isManualOverride)
    }
}

