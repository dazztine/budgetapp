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
import com.example.budgettracker.util.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    @Test
    fun testPemdasOrderOfOperationsEvaluation() {
        // Test addition only: 5+5+5+5 = 20
        viewModel.onClear()
        viewModel.onDigitInput("5")
        viewModel.onOperatorClick("+")
        viewModel.onDigitInput("5")
        viewModel.onOperatorClick("+")
        viewModel.onDigitInput("5")
        viewModel.onOperatorClick("+")
        viewModel.onDigitInput("5")
        assertEquals(2000L, viewModel.amountCentavos)
        val success1 = viewModel.onEqualClick()
        assertTrue(success1)
        assertEquals("20", viewModel.amountInput.value)

        // Test PEMDAS: 2 + 3 * 4 = 14 (not (2+3)*4 = 20)
        viewModel.onClear()
        viewModel.onDigitInput("2")
        viewModel.onOperatorClick("+")
        viewModel.onDigitInput("3")
        viewModel.onOperatorClick("×")
        viewModel.onDigitInput("4")
        assertEquals(1400L, viewModel.amountCentavos)
        val success2 = viewModel.onEqualClick()
        assertTrue(success2)
        assertEquals("14", viewModel.amountInput.value)

        // Test division precedence: 10 - 6 / 2 + 1 = 8
        viewModel.onClear()
        viewModel.onDigitInput("10")
        viewModel.onOperatorClick("−")
        viewModel.onDigitInput("6")
        viewModel.onOperatorClick("÷")
        viewModel.onDigitInput("2")
        viewModel.onOperatorClick("+")
        viewModel.onDigitInput("1")
        assertEquals(800L, viewModel.amountCentavos)
        val success3 = viewModel.onEqualClick()
        assertTrue(success3)
        assertEquals("8", viewModel.amountInput.value)
    }

    @Test
    fun testMultipleTokenDecimalsAndTrailingOperators() {
        viewModel.onClear()
        viewModel.onDigitInput("5")
        viewModel.onDotInput()
        viewModel.onDigitInput("5")
        viewModel.onOperatorClick("+")
        viewModel.onDigitInput("2")
        viewModel.onDotInput()
        viewModel.onDigitInput("5")
        viewModel.onOperatorClick("×")
        viewModel.onDigitInput("2")
        // 5.5 + (2.5 * 2) = 5.5 + 5.0 = 10.5
        assertEquals(1050L, viewModel.amountCentavos)
        viewModel.onEqualClick()
        assertEquals("10.50", viewModel.amountInput.value)
    }

    @Test
    fun testNumericOnlyPasteFiltering() {
        viewModel.onClear()
        // Paste currency string with symbols, commas, and text
        viewModel.onPasteInput("Total: ₱1,250.75 php")
        assertEquals("1250.75", viewModel.amountInput.value)
        assertEquals(125075L, viewModel.amountCentavos)

        // Paste multiple decimals strips secondary decimals
        viewModel.onPasteInput("99.50.25")
        assertEquals("99.5025", viewModel.amountInput.value)
    }

    @Test
    fun testConfirmAmountResolvesExpression() {
        viewModel.onClear()
        viewModel.onDigitInput("100")
        viewModel.onOperatorClick("+")
        viewModel.onDigitInput("50")
        val success = viewModel.onConfirmAmount()
        assertTrue(success)
        assertEquals("150", viewModel.amountInput.value)
        assertEquals(15000L, viewModel.amountCentavos)

        // Blank confirm sets to 0
        viewModel.onClear()
        viewModel.onConfirmAmount()
        assertEquals("0", viewModel.amountInput.value)
    }

    @Test
    fun testExpressionCharacterLimit_blocksDigitsAndOperatorsAt20Chars() = runBlocking {
        viewModel.onClear()
        val emittedToasts = mutableListOf<String>()
        val job = launch(testDispatcher) {
            viewModel.toastMessage.collect { emittedToasts.add(it) }
        }

        // Type 10 digits
        repeat(9) { viewModel.onDigitInput("1") }
        viewModel.onOperatorClick("+")
        // Now length is 10: "111111111+"
        assertEquals(10, viewModel.amountInput.value.length)

        // Type 9 more digits + 1 operator = 20 chars total
        repeat(9) { viewModel.onDigitInput("2") }
        viewModel.onOperatorClick("+")
        // "111111111+222222222+" is 20 chars
        assertEquals(20, viewModel.amountInput.value.length)
        assertEquals(0, emittedToasts.size)

        // Attempt to type a 21st character
        viewModel.onDigitInput("5")
        assertEquals(20, viewModel.amountInput.value.length)
        assertEquals(listOf("Amount limit reached"), emittedToasts)

        // Attempt to append another operator (blocked)
        viewModel.onOperatorClick("+")
        assertEquals(20, viewModel.amountInput.value.length)
        // Note: replacing trailing operator is allowed because length does not increase
        viewModel.onOperatorClick("−")
        assertEquals(20, viewModel.amountInput.value.length)
        assertEquals("111111111+222222222−", viewModel.amountInput.value)

        // Dot input when at 20 chars is also blocked
        viewModel.onDotInput()
        assertEquals(20, viewModel.amountInput.value.length)
        assertTrue(emittedToasts.contains("Amount limit reached"))

        job.cancel()
    }

    @Test
    fun testPasteInput_truncatesAt20Chars() = runBlocking {
        viewModel.onClear()
        val emittedToasts = mutableListOf<String>()
        val job = launch(testDispatcher) {
            viewModel.toastMessage.collect { emittedToasts.add(it) }
        }

        // Paste 25 numeric characters
        viewModel.onPasteInput("1234567890123456789099999")
        assertEquals(20, viewModel.amountInput.value.length)
        assertEquals("12345678901234567890", viewModel.amountInput.value)
        assertEquals(listOf("Amount limit reached"), emittedToasts)

        job.cancel()
    }

    @Test
    fun testAmountFormattingUnified_previewMatchesCalculatorDisplay() {
        viewModel.onClear()
        // Type 9 fives: 555555555
        repeat(9) { viewModel.onDigitInput("5") }
        assertEquals("555555555", viewModel.amountInput.value)

        // Verify top-preview-equivalent formatted output matches calculator formatted output
        val topPreviewFormatted = CurrencyUtils.formatExpressionForDisplay(viewModel.amountInput.value)
        val calculatorFormatted = CurrencyUtils.formatExpressionForDisplay(viewModel.amountInput.value)

        assertEquals("₱555,555,555", topPreviewFormatted)
        assertEquals("₱555,555,555", calculatorFormatted)
        assertEquals(topPreviewFormatted, calculatorFormatted)

        // Verify centavos calculation does not suffer from Double.toString scientific notation (5.55555555E8)
        assertEquals(55555555500L, viewModel.amountCentavos)

        // Test with explicit decimal
        viewModel.onClear()
        viewModel.onDigitInput("1")
        viewModel.onDigitInput("5")
        viewModel.onDigitInput("0")
        viewModel.onDotInput()
        viewModel.onDigitInput("5")
        viewModel.onDigitInput("0")
        val decimalPreview = CurrencyUtils.formatExpressionForDisplay(viewModel.amountInput.value)
        val decimalCalc = CurrencyUtils.formatExpressionForDisplay(viewModel.amountInput.value)
        assertEquals("₱150.50", decimalPreview)
        assertEquals("₱150.50", decimalCalc)
        assertEquals(15050L, viewModel.amountCentavos)

        // Test mid-expression with operator
        viewModel.onClear()
        viewModel.onDigitInput("1000")
        viewModel.onOperatorClick("+")
        viewModel.onDigitInput("250")
        val exprPreview = CurrencyUtils.formatExpressionForDisplay(viewModel.amountInput.value)
        val exprCalc = CurrencyUtils.formatExpressionForDisplay(viewModel.amountInput.value)
        assertEquals("₱1000+250", exprPreview)
        assertEquals("₱1000+250", exprCalc)

        // Once calculated / confirmed, displays formatted with commas
        viewModel.onEqualClick()
        val resolvedPreview = CurrencyUtils.formatExpressionForDisplay(viewModel.amountInput.value)
        val resolvedCalc = CurrencyUtils.formatExpressionForDisplay(viewModel.amountInput.value)
        assertEquals("₱1,250", resolvedPreview)
        assertEquals("₱1,250", resolvedCalc)
        assertEquals(125000L, viewModel.amountCentavos)
    }
}
