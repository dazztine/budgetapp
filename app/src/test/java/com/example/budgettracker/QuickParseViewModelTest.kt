package com.example.budgettracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.parser.ParsedAccountSetup
import com.example.budgettracker.parser.ParsedTransaction
import com.example.budgettracker.ui.parse.ParseUiResult
import com.example.budgettracker.ui.parse.QuickParseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class QuickParseViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetRepository
    private lateinit var viewModel: QuickParseViewModel

    private var gcashId: Long = 0L

    @Before
    fun setup() {
        runBlocking {
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
            viewModel = QuickParseViewModel(repository, testDispatcher)

            gcashId = repository.insertAccount(
                AccountEntity(name = "GCash", type = AccountType.E_WALLET, initialBalance = 10_000L)
            )
            // Wait for activeAccounts state flow to emit inserted account
            viewModel.activeAccounts.first { it.isNotEmpty() }
        }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testZeroAutoSaveInvariantOnParse() = runBlocking {
        viewModel.updateInputText("You paid P150.00 to Jollibee using GCash.")
        viewModel.parseText()

        val result = viewModel.parseResult.value
        assertTrue(result is ParseUiResult.SingleTransaction)

        val parsed = (result as ParseUiResult.SingleTransaction).parsed
        assertEquals(15000L, parsed.amountCentavos)
        assertEquals("GCash", parsed.accountName)

        // ZERO database writes must have occurred prior to explicit confirmation
        val transactionsInDb = repository.allTransactions.first()
        assertTrue(transactionsInDb.isEmpty())
    }

    @Test
    fun testModeASingleTransactionParseAndConfirmSavePipeline() = runBlocking {
        viewModel.updateInputText("Received PHP 5,000.00 in GCash for Freelance project")
        viewModel.parseText()

        val result = viewModel.parseResult.value
        assertTrue(result is ParseUiResult.SingleTransaction)
        val parsed = (result as ParseUiResult.SingleTransaction).parsed

        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(500000L, parsed.amountCentavos)

        val latch = CountDownLatch(1)
        var successCalled = false
        viewModel.confirmAndSaveSingleTransaction(parsed, gcashId) {
            successCalled = true
            latch.countDown()
        }

        val completed = latch.await(5, TimeUnit.SECONDS)
        assertTrue(completed)
        assertTrue(successCalled)

        // Database now has the single confirmed transaction
        val transactionsInDb = repository.allTransactions.first()
        assertEquals(1, transactionsInDb.size)

        val tx = transactionsInDb[0]
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals(500000L, tx.amount)
        assertEquals(gcashId, tx.accountId)
    }

    @Test
    fun testModeBBatchAccountSetupParseAndConfirmSavePipeline() = runBlocking {
        viewModel.updateInputText("BDO 50000, SPayLater 10000 due 15th and 30th")
        viewModel.parseText()

        val result = viewModel.parseResult.value
        assertTrue(result is ParseUiResult.BatchAccounts)
        val parsedAccounts = (result as ParseUiResult.BatchAccounts).accounts

        assertEquals(2, parsedAccounts.size)

        val latch = CountDownLatch(1)
        var successCalled = false
        viewModel.confirmAndSaveBatchAccounts(parsedAccounts) {
            successCalled = true
            latch.countDown()
        }

        val completed = latch.await(5, TimeUnit.SECONDS)
        assertTrue(completed)
        assertTrue(successCalled)

        // Verify accounts in DB
        val activeAccounts = repository.activeAccounts.first { it.size == 3 }
        // 1 initial GCash + 2 new accounts = 3
        assertEquals(3, activeAccounts.size)

        val spaylaterAcc = activeAccounts.find { it.name == "SPayLater" }
        assertNotNull(spaylaterAcc)

        val loanDetails = repository.getLoanDetailsByAccountId(spaylaterAcc!!.id)
        assertNotNull(loanDetails)
        assertEquals(15, loanDetails?.cycleDay1)
        assertEquals(30, loanDetails?.cycleDay2)
    }
}
