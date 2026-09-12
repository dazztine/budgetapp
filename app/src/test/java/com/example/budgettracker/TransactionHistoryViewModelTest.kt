package com.example.budgettracker

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.ui.history.TransactionHistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TransactionHistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetRepository
    private lateinit var viewModel: TransactionHistoryViewModel

    private var acc1Id: Long = 0
    private var acc2Id: Long = 0

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = BudgetRepository(
            accountDao = database.accountDao(),
            transactionDao = database.transactionDao(),
            loanDetailsDao = database.loanDetailsDao(),
            installmentPlanDao = database.installmentPlanDao()
        )

        runBlocking {
            acc1Id = repository.insertAccount(AccountEntity(name = "BDO", type = AccountType.BANK))
            acc2Id = repository.insertAccount(AccountEntity(name = "GCash", type = AccountType.E_WALLET))

            repository.insertTransaction(
                TransactionEntity(
                    accountId = acc1Id,
                    type = TransactionType.EXPENSE,
                    amount = 15000L,
                    category = "Food & Dining",
                    title = "Jollibee Lunch",
                    timestamp = 1000L
                )
            )

            repository.insertTransaction(
                TransactionEntity(
                    accountId = acc2Id,
                    type = TransactionType.INCOME,
                    amount = 500000L,
                    category = "Salary",
                    title = "Freelance Payout",
                    timestamp = 2000L
                )
            )

            repository.insertTransaction(
                TransactionEntity(
                    accountId = acc1Id,
                    type = TransactionType.EXPENSE,
                    amount = 20000L,
                    category = "Shopping",
                    title = "SM Department Store",
                    timestamp = 3000L
                )
            )
        }

        viewModel = TransactionHistoryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testInitialStateLoadsAllTransactions() = runBlocking {
        val result = viewModel.filteredTransactions.first { it.isNotEmpty() }
        assertEquals(3, result.size)
    }

    @Test
    fun testFilterByAccount() = runBlocking {
        viewModel.setSelectedAccount(acc2Id)
        val result = viewModel.filteredTransactions.first { it.size == 1 }
        assertEquals(1, result.size)
        assertEquals("Freelance Payout", result.first().title)
    }

    @Test
    fun testFilterByType() = runBlocking {
        viewModel.setSelectedType(TransactionType.EXPENSE)
        val result = viewModel.filteredTransactions.first { it.size == 2 }
        assertEquals(2, result.size)
        assertEquals(true, result.all { it.type == TransactionType.EXPENSE })
    }

    @Test
    fun testSearchQueryFilter() = runBlocking {
        viewModel.setSearchQuery("Jollibee")
        val result = viewModel.filteredTransactions.first { it.size == 1 }
        assertEquals("Jollibee Lunch", result.first().title)
    }

    @Test
    fun testClearFiltersResetsAll() = runBlocking {
        viewModel.setSelectedAccount(acc1Id)
        viewModel.setSelectedType(TransactionType.EXPENSE)
        viewModel.setSearchQuery("SM")

        viewModel.clearFilters()

        val result = viewModel.filteredTransactions.first { it.size == 3 }
        assertEquals(3, result.size)
    }
}
