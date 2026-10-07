package com.example.budgettracker.ui.plan

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.BudgetEntity
import com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.domain.BudgetPaceCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlanViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetRepository

    private val zoneId = ZoneId.of("Asia/Manila")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = BudgetRepository(
            accountDao = database.accountDao(),
            transactionDao = database.transactionDao(),
            loanDetailsDao = database.loanDetailsDao(),
            installmentPlanDao = database.installmentPlanDao(),
            savingsDetailsDao = database.savingsDetailsDao(),
            billDetailsDao = database.billDetailsDao(),
            loanBillingCycleDao = database.loanBillingCycleDao(),
            customCategoryDao = database.customCategoryDao(),
            recurringBillDao = database.recurringBillDao(),
            creditDetailsDao = database.creditDetailsDao(),
            budgetDao = database.budgetDao()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    private fun createFixedClock(date: LocalDate): Clock {
        val instant = date.atStartOfDay(zoneId).toInstant()
        return Clock.fixed(instant, zoneId)
    }

    @Test
    fun testPlanShellInitializationAndSubTabSwitching() {
        val clock = createFixedClock(LocalDate.of(2026, 10, 1))
        val viewModel = PlanViewModel(repository, clock)

        assertEquals(PlanSubTab.BUDGETS, viewModel.selectedSubTab.value)

        viewModel.selectSubTab(PlanSubTab.GOALS)
        assertEquals(PlanSubTab.GOALS, viewModel.selectedSubTab.value)

        viewModel.selectSubTab(PlanSubTab.UPCOMING)
        assertEquals(PlanSubTab.UPCOMING, viewModel.selectedSubTab.value)

        viewModel.selectSubTab(PlanSubTab.DEBTS)
        assertEquals(PlanSubTab.DEBTS, viewModel.selectedSubTab.value)
    }

    @Test
    fun testPlanUiStateLoadsEmptyStateSuccessfully() = runBlocking {
        val clock = createFixedClock(LocalDate.of(2026, 10, 1))
        val viewModel = PlanViewModel(repository, clock)

        val state = viewModel.uiState.first { it is PlanUiState.Success } as PlanUiState.Success
        assertNotNull(state)
        assertEquals(0L, state.paceResult.totalSpent)
        assertTrue(state.goals.isEmpty())
        assertTrue(state.upcomingObligations.isEmpty())
        assertTrue(state.debts.isEmpty())
    }

    @Test
    fun testDayOne_standardMonthPacing() = runBlocking {
        val clock = createFixedClock(LocalDate.of(2026, 10, 1))
        // ₱31,000 budget for 31 days -> ₱1,000/day
        repository.upsertBudget(BudgetEntity(category = null, amount = 31_000_00L, period = "MONTHLY"))
        val viewModel = PlanViewModel(repository, clock)

        val state = viewModel.uiState.first { it is PlanUiState.Success && (it as PlanUiState.Success).overallBudget != null } as PlanUiState.Success
        assertEquals(31, state.paceResult.daysLeft)
        assertEquals(31_000_00L, state.paceResult.leftThisMonth)
        assertEquals(1_000_00L, state.paceResult.dailyAllowance)
        assertFalse(state.paceResult.isOverBudget)
        assertEquals(0L, state.paceResult.overBy)
    }

    @Test
    fun testLastDayOfMonthPacing() = runBlocking {
        val clock = createFixedClock(LocalDate.of(2026, 10, 31))
        repository.upsertBudget(BudgetEntity(category = null, amount = 10_000_00L, period = "MONTHLY"))
        val viewModel = PlanViewModel(repository, clock)

        val state = viewModel.uiState.first { it is PlanUiState.Success && (it as PlanUiState.Success).overallBudget != null } as PlanUiState.Success
        assertEquals(1, state.paceResult.daysLeft)
        assertEquals(10_000_00L, state.paceResult.leftThisMonth)
        assertEquals(10_000_00L, state.paceResult.dailyAllowance)
    }

    @Test
    fun testOverBudgetState_distinctStateNotZeroClamped() = runBlocking {
        val clock = createFixedClock(LocalDate.of(2026, 10, 15))
        repository.upsertBudget(BudgetEntity(category = null, amount = 20_000_00L, period = "MONTHLY"))
        val accId = repository.insertAccount(AccountEntity(name = "Wallet", type = AccountType.CASH, initialBalance = 50_000_00L))

        // Spend ₱25,000 (over budget by ₱5,000)
        val epochMidMonth = LocalDate.of(2026, 10, 10).atStartOfDay(zoneId).toInstant().toEpochMilli()
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 25_000_00L,
                accountId = accId,
                category = "Food & Dining",
                title = "Dining out",
                timestamp = epochMidMonth
            )
        )

        val viewModel = PlanViewModel(repository, clock)
        val state = viewModel.uiState.first { it is PlanUiState.Success && (it as PlanUiState.Success).paceResult.totalSpent > 0L } as PlanUiState.Success
        assertTrue(state.paceResult.isOverBudget)
        assertEquals(5_000_00L, state.paceResult.overBy)
        assertEquals(-5_000_00L, state.paceResult.leftThisMonth)
        assertEquals(0L, state.paceResult.dailyAllowance) // Daily allowance clamped to 0 when over budget
        assertEquals(com.example.budgettracker.domain.CategoryBudgetStatus.OVER, state.paceResult.status)
    }

    @Test
    fun testNoBudgetSet_clearCallToActionState() = runBlocking {
        val clock = createFixedClock(LocalDate.of(2026, 10, 10))
        val viewModel = PlanViewModel(repository, clock)

        val state = viewModel.uiState.first { it is PlanUiState.Success } as PlanUiState.Success
        assertNull(state.overallBudget)
        assertNull(state.paceResult.totalBudget)
        assertNull(state.paceResult.leftThisMonth)
        assertFalse(state.paceResult.isOverBudget)
        assertEquals(0L, state.paceResult.dailyAllowance)
        assertEquals(com.example.budgettracker.domain.CategoryBudgetStatus.OK, state.paceResult.status)
    }

    @Test
    fun testCategoryWithoutBudget_spendingStillTrackedInTotalAndAvailableCategories() = runBlocking {
        val clock = createFixedClock(LocalDate.of(2026, 10, 10))
        val accId = repository.insertAccount(AccountEntity(name = "Wallet", type = AccountType.CASH, initialBalance = 50_000_00L))
        val epochMidMonth = LocalDate.of(2026, 10, 5).atStartOfDay(zoneId).toInstant().toEpochMilli()

        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 3_500_00L,
                accountId = accId,
                category = "Rare Hobby",
                title = "Board game",
                timestamp = epochMidMonth
            )
        )

        val viewModel = PlanViewModel(repository, clock)
        val state = viewModel.uiState.first { it is PlanUiState.Success && (it as PlanUiState.Success).paceResult.totalSpent > 0L } as PlanUiState.Success

        assertEquals(3_500_00L, state.paceResult.totalSpent)
        assertTrue(state.availableCategories.any { it.equals("Rare Hobby", ignoreCase = true) })
        assertTrue("No category pacing since no category budget configured for it", state.paceResult.categoryPacings.isEmpty())
    }

    @Test
    fun testBudgetForDeletedCustomCategory_rowKeptDefensivelyDisplayedAndCanBeDeleted() = runBlocking {
        val clock = createFixedClock(LocalDate.of(2026, 10, 10))
        repository.upsertBudget(
            BudgetEntity(
                category = "Deleted Custom Category",
                amount = 10_000_00L,
                period = "MONTHLY"
            )
        )

        val viewModel = PlanViewModel(repository, clock)
        val state = viewModel.uiState.first { it is PlanUiState.Success && (it as PlanUiState.Success).categoryBudgets.isNotEmpty() } as PlanUiState.Success

        assertEquals(1, state.paceResult.categoryPacings.size)
        val pacing = state.paceResult.categoryPacings[0]
        assertEquals("Deleted Custom Category", pacing.categoryKey)
        assertEquals("Deleted Custom Category", pacing.displayName)
        assertEquals(10_000_00L, pacing.budgetAmount)

        // Verify budget can be deleted cleanly even though its category no longer exists in category lists
        val budgetToDelete = state.categoryBudgets.first { it.category == "Deleted Custom Category" }
        viewModel.deleteBudget(budgetToDelete)

        val afterDeleteState = viewModel.uiState.first { it is PlanUiState.Success && (it as PlanUiState.Success).categoryBudgets.isEmpty() } as PlanUiState.Success
        assertTrue(afterDeleteState.categoryBudgets.isEmpty())
        assertTrue(afterDeleteState.paceResult.categoryPacings.isEmpty())
    }

    @Test
    fun testDepositToGoal_endToEndLedgerBalanceIncreaseAndReversibility() = runBlocking {
        // Setup source account (Maya) and Goal account (Emergency Fund)
        val sourceAccId = repository.insertAccount(
            AccountEntity(name = "Maya Wallet", type = AccountType.E_WALLET, initialBalance = 10_000_00L)
        )
        val goalAccId = repository.insertAccount(
            AccountEntity(name = "Emergency Fund", type = AccountType.SAVINGS, initialBalance = 2_000_00L)
        )
        repository.insertSavingsDetails(
            SavingsAccountDetailsEntity(
                accountId = goalAccId,
                goalAmount = 50_000_00L,
                isGoal = true,
                iconPreset = "emergency"
            )
        )

        val initialGoalBalance = repository.getComputedBalance(goalAccId).first() ?: 0L
        assertEquals(2_000_00L, initialGoalBalance)

        // Exercise the exact code path onDepositToGoal uses via TransactionViewModel
        val txViewModel = com.example.budgettracker.ui.transaction.TransactionViewModel(repository, ioDispatcher = testDispatcher)
        txViewModel.prepareForTransfer(
            toAccountId = goalAccId,
            category = "Savings Transfer",
            title = "Deposit to Emergency Fund"
        )
        txViewModel.setAccountId(sourceAccId)
        txViewModel.onPasteInput("8000")
        val latch = java.util.concurrent.CountDownLatch(1)
        var savedCalled = false
        txViewModel.saveTransaction(onSuccess = {
            savedCalled = true
            latch.countDown()
        })
        val completed = latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
        assertTrue(completed)
        assertTrue(savedCalled)

        val allTransactions = repository.getAllTransactionsDirect()
        assertEquals(1, allTransactions.size)
        val depositTx = allTransactions[0]

        // Assert that category is "Savings Transfer", not mapped to Bills & Utilities, and not counted as expense
        assertEquals("Savings Transfer", depositTx.category)
        assertEquals(TransactionType.TRANSFER, depositTx.type)
        assertEquals(8_000_00L, depositTx.amount)
        assertFalse(
            "Deposit to goal must NOT be counted as an expense",
            com.example.budgettracker.ui.reports.ReportsAnalyticsCalculator.isExpenseTransaction(depositTx, emptySet())
        )

        // Goal ledger balance must increase by 8,000.00: 2,000.00 + 8,000.00 = 10,000.00
        val afterDepositGoalBalance = repository.getComputedBalance(goalAccId).first() ?: 0L
        assertEquals(10_000_00L, afterDepositGoalBalance)

        // Source balance must decrease by 8,000.00: 10,000.00 - 8,000.00 = 2,000.00
        val afterDepositSourceBalance = repository.getComputedBalance(sourceAccId).first() ?: 0L
        assertEquals(2_000_00L, afterDepositSourceBalance)

        // Verify PlanViewModel computes goal percentage correctly: (10,000 * 100) / 50,000 = 20%
        val clock = createFixedClock(LocalDate.of(2026, 10, 10))
        val viewModel = PlanViewModel(repository, clock)
        val state = viewModel.uiState.first { it is PlanUiState.Success && (it as PlanUiState.Success).goals.isNotEmpty() } as PlanUiState.Success
        val goalItem = state.goals.first { it.account.id == goalAccId }
        assertEquals(10_000_00L, goalItem.savedAmount)
        assertEquals(20, goalItem.percent)
        assertFalse(goalItem.isCompleted)

        // Reversibility: Delete the transfer transaction
        repository.deleteTransaction(depositTx)

        // Goal balance and source balance must return strictly to initial amounts
        val restoredGoalBalance = repository.getComputedBalance(goalAccId).first() ?: 0L
        val restoredSourceBalance = repository.getComputedBalance(sourceAccId).first() ?: 0L
        assertEquals("Goal balance must return to initial balance on delete", initialGoalBalance, restoredGoalBalance)
        assertEquals("Source balance must return to initial balance on delete", 10_000_00L, restoredSourceBalance)
    }

    @Test
    fun testUpcomingObligationsSortedByDueDateAndEmptyState() = runBlocking {
        val clock = createFixedClock(LocalDate.of(2026, 10, 15))
        val viewModel = PlanViewModel(repository, clock)

        // Initially no cycles -> empty list
        var state = viewModel.uiState.first { it is PlanUiState.Success } as PlanUiState.Success
        assertTrue(state.upcomingObligations.isEmpty())

        // Create two bill accounts with pending cycles
        val bill1Id = repository.insertAccount(
            AccountEntity(name = "Internet", type = AccountType.BILL, initialBalance = 0L, includeInNetWorth = false)
        )
        val bill2Id = repository.insertAccount(
            AccountEntity(name = "Electric", type = AccountType.BILL, initialBalance = 0L, includeInNetWorth = false)
        )

        // Cycle 2: Due Oct 28
        val cycle2DueDate = LocalDate.of(2026, 10, 28).atStartOfDay(zoneId).toInstant().toEpochMilli()
        database.loanBillingCycleDao().insert(
            com.example.budgettracker.data.local.entity.LoanBillingCycleEntity(
                accountId = bill2Id,
                cycleDueDate = cycle2DueDate,
                amountDue = 2_000_00L,
                isPaid = false
            )
        )

        // Cycle 1: Due Oct 20 (earlier than Cycle 2)
        val cycle1DueDate = LocalDate.of(2026, 10, 20).atStartOfDay(zoneId).toInstant().toEpochMilli()
        database.loanBillingCycleDao().insert(
            com.example.budgettracker.data.local.entity.LoanBillingCycleEntity(
                accountId = bill1Id,
                cycleDueDate = cycle1DueDate,
                amountDue = 1_500_00L,
                isPaid = false
            )
        )

        state = viewModel.uiState.first { it is PlanUiState.Success && (it as PlanUiState.Success).upcomingObligations.size == 2 } as PlanUiState.Success
        val obligations = state.upcomingObligations
        assertEquals(2, obligations.size)
        // Must be sorted by dueDate ascending
        assertEquals("Internet", obligations[0].name)
        assertEquals(LocalDate.of(2026, 10, 20), obligations[0].dueDate)
        assertEquals(1_500_00L, obligations[0].amountCentavos)

        assertEquals("Electric", obligations[1].name)
        assertEquals(LocalDate.of(2026, 10, 28), obligations[1].dueDate)
        assertEquals(2_000_00L, obligations[1].amountCentavos)
    }

    @Test
    fun testBackdatedExpenseExcludedFromThisMonthTotalsAndBudgets() = runBlocking {
        // Oct 15, 2026 current time
        val clock = createFixedClock(LocalDate.of(2026, 10, 15))
        val accId = repository.insertAccount(AccountEntity(name = "Wallet", type = AccountType.CASH, initialBalance = 50_000_00L))
        repository.upsertBudget(BudgetEntity(category = null, amount = 20_000_00L, period = "MONTHLY"))
        repository.upsertBudget(BudgetEntity(category = "Food", amount = 10_000_00L, period = "MONTHLY"))

        // Backdated expense from previous month (Sept 20, 2026)
        val sept20Millis = LocalDate.of(2026, 9, 20).atStartOfDay(zoneId).toInstant().toEpochMilli()
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 5_000_00L,
                accountId = accId,
                category = "Food",
                title = "Sept Groceries",
                timestamp = sept20Millis
            )
        )

        // Expense from this month (Oct 10, 2026)
        val oct10Millis = LocalDate.of(2026, 10, 10).atStartOfDay(zoneId).toInstant().toEpochMilli()
        repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 2_000_00L,
                accountId = accId,
                category = "Food",
                title = "Oct Groceries",
                timestamp = oct10Millis
            )
        )

        val viewModel = PlanViewModel(repository, clock)
        val state = viewModel.uiState.first {
            it is PlanUiState.Success && (it as PlanUiState.Success).overallBudget != null
        } as PlanUiState.Success

        // Total spent in this month must only be 2_000_00L (the Oct 10 expense), excluding the backdated 5_000_00L expense
        assertEquals(2_000_00L, state.paceResult.totalSpent)
        val foodPacing = state.paceResult.categoryPacings.first { it.categoryKey == "Food" }
        assertEquals(2_000_00L, foodPacing.spentAmount)
    }
}
