package com.example.budgettracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetRepository
    private lateinit var viewModel: DashboardViewModel

    private var gcashId: Long = 0L
    private var bpiId: Long = 0L

    @Before
    fun setup() = runBlocking {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BudgetRepository(
            database.accountDao(),
            database.transactionDao(),
            database.loanDetailsDao(),
            database.installmentPlanDao()
        )
        viewModel = DashboardViewModel(repository, testDispatcher)

        gcashId = repository.insertAccount(
            AccountEntity(name = "GCash", type = AccountType.E_WALLET, initialBalance = 15_000L)
        )
        bpiId = repository.insertAccount(
            AccountEntity(name = "BPI", type = AccountType.BANK, initialBalance = 50_000L)
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testNetWorthAndAccountBalancesFlow() = runBlocking {
        val activeAccounts = viewModel.activeAccountsWithBalances.first { it.isNotEmpty() }
        assertEquals(2, activeAccounts.size)

        val netWorthVal = viewModel.netWorth.first { it > 0L }
        assertEquals(65_000L, netWorthVal)
    }

    @Test
    fun testSoftDeleteAndRestoreAccountFlow() = runBlocking {
        val initialActive = viewModel.activeAccountsWithBalances.first { it.size == 2 }
        assertEquals(2, initialActive.size)

        viewModel.softDeleteAccount(gcashId)

        val activeAccountsAfterDelete = viewModel.activeAccountsWithBalances.first { it.size == 1 }
        assertEquals(1, activeAccountsAfterDelete.size)
        assertEquals("BPI", activeAccountsAfterDelete[0].name)

        val netWorthAfterDelete = viewModel.netWorth.first { it == 50_000L }
        assertEquals(50_000L, netWorthAfterDelete)

        // Restore account
        viewModel.restoreAccount(gcashId)

        val activeAccountsAfterRestore = viewModel.activeAccountsWithBalances.first { it.size == 2 }
        assertEquals(2, activeAccountsAfterRestore.size)

        val netWorthAfterRestore = viewModel.netWorth.first { it == 65_000L }
        assertEquals(65_000L, netWorthAfterRestore)
    }

    @Test
    fun testSaveAccountWithLoanDetails() = runBlocking {
        val newAccount = AccountEntity(
            name = "SPayLater",
            type = AccountType.BNPL,
            initialBalance = 0L
        )
        val loanDetails = LoanAccountDetailsEntity(
            accountId = 0L,
            cycleDay1 = 15,
            cycleDay2 = 30,
            minimumAmountDue = 2_000L,
            totalRemainingBalance = 10_000L,
            reminderEnabled = true,
            reminderDaysBefore = 7
        )

        val latch = CountDownLatch(1)
        viewModel.saveAccount(newAccount, loanDetails) {
            latch.countDown()
        }

        val completed = latch.await(5, TimeUnit.SECONDS)
        assertTrue(completed)

        val activeAccounts = viewModel.activeAccountsWithBalances.first { it.size == 3 }
        assertEquals(3, activeAccounts.size)

        val loanAccs = viewModel.loanAccounts.first { it.isNotEmpty() }
        assertEquals(1, loanAccs.size)
        assertEquals("SPayLater", loanAccs[0].account.name)
        assertEquals(15, loanAccs[0].loanDetails?.cycleDay1)
    }

    @Test
    fun testDeleteTransactionUpdatesRecentTransactionsAndNetWorth() = runBlocking {
        val now = System.currentTimeMillis()
        val txId = repository.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 5_000L,
                accountId = gcashId,
                category = "Food",
                title = "Dinner",
                timestamp = now
            )
        )

        val recentBefore = viewModel.recentTransactions.first { it.isNotEmpty() }
        assertEquals(1, recentBefore.size)

        val netWorthWithTx = viewModel.netWorth.first { it == 60_000L }
        assertEquals(60_000L, netWorthWithTx)

        val txToDelete = TransactionEntity(
            id = txId,
            type = TransactionType.EXPENSE,
            amount = 5_000L,
            accountId = gcashId,
            category = "Food",
            title = "Dinner",
            timestamp = now
        )
        viewModel.deleteTransaction(txToDelete)

        val recentAfter = viewModel.recentTransactions.first { it.isEmpty() }
        assertTrue(recentAfter.isEmpty())

        val netWorthRestored = viewModel.netWorth.first { it == 65_000L }
        assertEquals(65_000L, netWorthRestored)
    }
}
