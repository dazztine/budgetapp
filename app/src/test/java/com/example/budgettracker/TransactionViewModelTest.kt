package com.example.budgettracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.ui.transaction.SaveResult
import com.example.budgettracker.ui.transaction.TransactionViewModel
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
import org.junit.Assert.assertNull
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
class TransactionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetRepository
    private lateinit var viewModel: TransactionViewModel

    private var account1Id: Long = 0L
    private var account2Id: Long = 0L

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
            database.installmentPlanDao(),
            database.savingsDetailsDao(),
            database.billDetailsDao()
        )
        viewModel = TransactionViewModel(repository, testDispatcher)

        account1Id = repository.insertAccount(
            AccountEntity(name = "GCash", type = AccountType.E_WALLET, initialBalance = 10_000L)
        )
        account2Id = repository.insertAccount(
            AccountEntity(name = "BPI", type = AccountType.BANK, initialBalance = 50_000L)
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testNumpadInputFormattingAndCentavosConversion() {
        assertEquals(0L, viewModel.amountCentavos)

        viewModel.onDigitInput("1")
        viewModel.onDigitInput("5")
        viewModel.onDigitInput("0")
        viewModel.onDotInput()
        viewModel.onDigitInput("5")

        assertEquals("150.5", viewModel.amountInput.value)
        assertEquals(15050L, viewModel.amountCentavos)

        viewModel.onDigitInput("0")
        assertEquals("150.50", viewModel.amountInput.value)
        assertEquals(15050L, viewModel.amountCentavos)

        viewModel.onDigitInput("5")
        assertEquals("150.50", viewModel.amountInput.value)

        viewModel.onBackspace()
        assertEquals("150.5", viewModel.amountInput.value)
        assertEquals(15050L, viewModel.amountCentavos)

        viewModel.onClear()
        assertEquals("", viewModel.amountInput.value)
        assertEquals(0L, viewModel.amountCentavos)
    }

    @Test
    fun testValidationPreventsInvalidTransactionSave() = runBlocking {
        viewModel.setAccountId(account1Id)
        viewModel.onClear()

        viewModel.saveTransaction()
        assertTrue(viewModel.saveState.value is SaveResult.Error)
        assertEquals("Amount must be greater than zero", (viewModel.saveState.value as SaveResult.Error).message)

        viewModel.onDigitInput("100")
        viewModel.setAccountId(null)
        viewModel.saveTransaction()
        assertTrue(viewModel.saveState.value is SaveResult.Error)
        assertEquals("Please select an account", (viewModel.saveState.value as SaveResult.Error).message)

        viewModel.setAccountId(account1Id)
        viewModel.setTransactionType(TransactionType.TRANSFER)
        viewModel.setToAccountId(account1Id)
        viewModel.saveTransaction()
        assertTrue(viewModel.saveState.value is SaveResult.Error)
        assertEquals("Destination account must be different from source account", (viewModel.saveState.value as SaveResult.Error).message)
    }

    @Test
    fun testSuccessfulTransactionSaveAndRepositoryInsertion() = runBlocking {
        viewModel.setAccountId(account1Id)
        viewModel.setTransactionType(TransactionType.EXPENSE)
        viewModel.onDigitInput("250")
        viewModel.setCategory("Food & Dining")
        viewModel.setTitle("Dinner")
        viewModel.setNote("Team meal")

        val latch = CountDownLatch(1)
        var successCalled = false
        viewModel.saveTransaction {
            successCalled = true
            latch.countDown()
        }

        val completed = latch.await(5, TimeUnit.SECONDS)
        assertTrue(completed)
        assertEquals(SaveResult.Success, viewModel.saveState.value)
        assertTrue(successCalled)

        val transactions = repository.allTransactions.first()
        assertEquals(1, transactions.size)

        val tx = transactions[0]
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(25000L, tx.amount)
        assertEquals(account1Id, tx.accountId)
        assertEquals("Food & Dining", tx.category)
        assertEquals("Dinner", tx.title)
        assertEquals("Team meal", tx.note)
    }

    @Test
    fun testInstallmentPlanCreation() = runBlocking {
        viewModel.setAccountId(account1Id)
        viewModel.setTransactionType(TransactionType.INSTALLMENT)
        viewModel.onDigitInput("6000")
        viewModel.setTotalInstallments("6")
        viewModel.setCategory("Electronics")
        viewModel.setTitle("SPayLater Phone")

        val latch = CountDownLatch(1)
        viewModel.saveTransaction { latch.countDown() }
        latch.await(5, TimeUnit.SECONDS)

        val plans = repository.activeInstallmentPlans.first()
        assertEquals(1, plans.size)
        val plan = plans[0]
        assertEquals("SPayLater Phone", plan.title)
        assertEquals(600_000L, plan.totalPurchaseAmount)
        assertEquals(6, plan.totalInstallments)
        assertEquals(100_000L, plan.monthlyPaymentAmount)
    }

    @Test
    fun testLoadTransactionForEditModifyAndSaveUpdatesBalanceAndHistory() = runBlocking {
        // 1. Initial State: Account 1 initial balance is 10,000L (PHP 100.00).
        val initialAccounts = repository.activeAccountsWithBalances.first()
        val acc1Before = initialAccounts.first { it.id == account1Id }
        assertEquals(10_000L, acc1Before.currentBalance)

        // 2. Insert initial transaction: Expense of 2,500L (PHP 25.00)
        viewModel.setAccountId(account1Id)
        viewModel.setTransactionType(TransactionType.EXPENSE)
        viewModel.onDigitInput("25")
        viewModel.setCategory("Food")
        viewModel.setTitle("Lunch")
        val insertLatch = CountDownLatch(1)
        viewModel.saveTransaction { insertLatch.countDown() }
        insertLatch.await(5, TimeUnit.SECONDS)

        val txListAfterInsert = repository.allTransactions.first()
        assertEquals(1, txListAfterInsert.size)
        val originalTx = txListAfterInsert[0]
        assertEquals("Lunch", originalTx.title)
        assertEquals("Food", originalTx.category)
        assertEquals(2_500L, originalTx.amount)

        // Balance should now be 10,000 - 2,500 = 7,500L
        val acc1AfterInsert = repository.activeAccountsWithBalances.first().first { it.id == account1Id }
        assertEquals(7_500L, acc1AfterInsert.currentBalance)

        // 3. Load transaction for edit
        viewModel.loadTransactionForEdit(originalTx)
        assertTrue(viewModel.isEditing.value)
        assertEquals(originalTx.id, viewModel.editingTransactionId.value)
        assertEquals("25", viewModel.amountInput.value)
        assertEquals("Lunch", viewModel.titleInput.value)
        assertEquals("Food", viewModel.categoryInput.value)

        // 4. Modify transaction: amount = 40.00 (4,000L), title = "Dinner Buffet", category = "Dining Out"
        viewModel.onClear()
        viewModel.onDigitInput("40")
        viewModel.setTitle("Dinner Buffet")
        viewModel.setCategory("Dining Out")

        val updateLatch = CountDownLatch(1)
        viewModel.saveTransaction { updateLatch.countDown() }
        updateLatch.await(5, TimeUnit.SECONDS)

        assertEquals(SaveResult.Success, viewModel.saveState.value)

        // 5. Verify transaction history: Still 1 transaction, with modified attributes
        val txListAfterUpdate = repository.allTransactions.first()
        assertEquals(1, txListAfterUpdate.size)
        val updatedTx = txListAfterUpdate[0]
        assertEquals(originalTx.id, updatedTx.id)
        assertEquals("Dinner Buffet", updatedTx.title)
        assertEquals("Dining Out", updatedTx.category)
        assertEquals(4_000L, updatedTx.amount)

        // 6. Verify account balance: 10,000 - 4,000 = 6,000L (properly recalculated)
        val acc1AfterUpdate = repository.activeAccountsWithBalances.first().first { it.id == account1Id }
        assertEquals(6_000L, acc1AfterUpdate.currentBalance)

        // 7. Reset form for next entry
        viewModel.resetFormForNextEntry()
        assertFalse(viewModel.isEditing.value)
        assertNull(viewModel.editingTransactionId.value)
        assertEquals("", viewModel.amountInput.value)
        assertEquals("", viewModel.titleInput.value)
        assertEquals("", viewModel.categoryInput.value)
    }
}
