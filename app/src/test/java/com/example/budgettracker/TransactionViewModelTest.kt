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
import org.junit.Assert.assertNotNull
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
            database.billDetailsDao(),
            database.loanBillingCycleDao()
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
        val bnplId = repository.insertAccount(
            AccountEntity(name = "SPayLater", type = AccountType.BNPL, initialBalance = 0L)
        )
        viewModel.setAccountId(bnplId)
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

    @Test
    fun testPayBillTransferLocksDestinationAndLeavesSourceEmpty() = runBlocking {
        viewModel.prepareForTransfer(toAccountId = 42L, amountCentavos = 2_500_00L)
        assertEquals(TransactionType.TRANSFER, viewModel.selectedType.value)
        assertEquals(42L, viewModel.selectedToAccountId.value)
        assertNull(viewModel.selectedAccountId.value)
        assertTrue(viewModel.isToAccountLocked.value)
        assertEquals(250000L, viewModel.amountCentavos)

        viewModel.resetFormForNextEntry()
        assertFalse(viewModel.isToAccountLocked.value)
    }

    @Test
    fun testSaveRecurringExpenseMonthlyCreatesBillAccountAndDetails() = runBlocking {
        viewModel.prepareForNewTransaction(accountId = account1Id, type = TransactionType.EXPENSE)
        viewModel.onDigitInput("1899")
        viewModel.setTitle("PLDT Home Fiber")
        viewModel.setCategory("Utilities")

        // Configure as monthly recurring bill on the 15th
        viewModel.configureRecurring(
            billName = "PLDT Home Fiber",
            frequency = "MONTHLY",
            dueDay = 15,
            dueMonth = 1
        )

        val latch = CountDownLatch(1)
        viewModel.saveTransaction { latch.countDown() }
        assertTrue(latch.await(2, TimeUnit.SECONDS))

        // 1. Verify expense transaction was created
        val txs = repository.allTransactions.first()
        assertEquals(1, txs.size)
        assertEquals(TransactionType.EXPENSE, txs[0].type)
        assertEquals(189900L, txs[0].amount)
        assertEquals(account1Id, txs[0].accountId)

        // 2. Verify new BILL account was created with preset 'pldt'
        val accounts = repository.allAccounts.first()
        val billAccount = accounts.find { it.type == AccountType.BILL }
        assertNotNull(billAccount)
        assertEquals("PLDT Home Fiber", billAccount!!.name)
        assertEquals("pldt", billAccount.presetId)

        // 3. Verify BillAccountDetailsEntity was created with recurrence MONTHLY
        val billDetails = repository.getBillDetailsByAccountId(billAccount.id)
        assertNotNull(billDetails)
        assertEquals("15", billDetails!!.dueDays)
        assertEquals(189900L, billDetails.amountDue)
        assertEquals("MONTHLY", billDetails.recurrence)

        // 4. Verify billing cycle is scheduled
        val cycles = database.loanBillingCycleDao().getPendingCyclesDirect(billAccount.id)
        assertEquals(1, cycles.size)
        assertEquals(189900L, cycles[0].amountDue)
        assertFalse(cycles[0].isPaid)
    }

    @Test
    fun testSaveRecurringExpenseDailyAndYearlyFrequencies() = runBlocking {
        // Test Daily
        viewModel.prepareForNewTransaction(accountId = account1Id, type = TransactionType.EXPENSE)
        viewModel.onDigitInput("50")
        viewModel.setTitle("Daily Coffee")
        viewModel.configureRecurring(
            billName = "Daily Coffee",
            frequency = "DAILY",
            dueDay = 1,
            dueMonth = 1
        )
        val latch1 = CountDownLatch(1)
        viewModel.saveTransaction { latch1.countDown() }
        assertTrue(latch1.await(2, TimeUnit.SECONDS))

        val accounts1 = repository.allAccounts.first()
        val dailyBill = accounts1.find { it.name == "Daily Coffee" }
        assertNotNull(dailyBill)
        val dailyDetails = repository.getBillDetailsByAccountId(dailyBill!!.id)
        assertNotNull(dailyDetails)
        assertEquals("DAILY", dailyDetails!!.recurrence)

        val dailyCycles = database.loanBillingCycleDao().getPendingCyclesDirect(dailyBill.id)
        assertEquals(1, dailyCycles.size)
        assertEquals(5000L, dailyCycles[0].amountDue)

        // Test Yearly
        viewModel.prepareForNewTransaction(accountId = account1Id, type = TransactionType.EXPENSE)
        viewModel.onDigitInput("4500")
        viewModel.setTitle("Car Insurance")
        viewModel.configureRecurring(
            billName = "Car Insurance",
            frequency = "YEARLY",
            dueDay = 25,
            dueMonth = 11
        )
        val latch2 = CountDownLatch(1)
        viewModel.saveTransaction { latch2.countDown() }
        assertTrue(latch2.await(2, TimeUnit.SECONDS))

        val accounts2 = repository.allAccounts.first()
        val yearlyBill = accounts2.find { it.name == "Car Insurance" }
        assertNotNull(yearlyBill)
        val yearlyDetails = repository.getBillDetailsByAccountId(yearlyBill!!.id)
        assertNotNull(yearlyDetails)
        assertEquals("YEARLY", yearlyDetails!!.recurrence)
        assertEquals("11-25", yearlyDetails.dueDays)

        val yearlyCycles = database.loanBillingCycleDao().getPendingCyclesDirect(yearlyBill.id)
        assertEquals(1, yearlyCycles.size)
        assertEquals(450000L, yearlyCycles[0].amountDue)
    }

    @Test
    fun testSaveRecurringExpenseUpdatesExistingBillAccountWithoutDuplicate() = runBlocking {
        // Pre-create existing Meralco bill account
        val billId = repository.insertAccount(
            AccountEntity(name = "Meralco", type = AccountType.BILL, presetId = "meralco")
        )
        repository.insertBillDetails(
            com.example.budgettracker.data.local.entity.BillAccountDetailsEntity(
                accountId = billId,
                dueDays = "10",
                amountDue = 200000L,
                recurrence = "MONTHLY"
            )
        )

        // User logs an expense of 2800 for "Meralco" with recurring due day 12
        viewModel.prepareForNewTransaction(accountId = account1Id, type = TransactionType.EXPENSE)
        viewModel.onDigitInput("2800")
        viewModel.setTitle("Meralco")
        viewModel.configureRecurring(
            billName = "Meralco",
            frequency = "MONTHLY",
            dueDay = 12,
            dueMonth = 1
        )
        val latch = CountDownLatch(1)
        viewModel.saveTransaction { latch.countDown() }
        assertTrue(latch.await(2, TimeUnit.SECONDS))

        // Verify only 1 bill account exists (no duplicate)
        val billAccounts = repository.allAccounts.first().filter { it.type == AccountType.BILL }
        assertEquals(1, billAccounts.size)
        assertEquals(billId, billAccounts[0].id)

        // Verify bill details were updated to 2800.00 and day 12
        val updatedDetails = repository.getBillDetailsByAccountId(billId)
        assertNotNull(updatedDetails)
        assertEquals("12", updatedDetails!!.dueDays)
        assertEquals(280000L, updatedDetails.amountDue)
    }

    @Test
    fun testDoubleTapSaveTransactionCreatesOnlyOneTransaction() = runBlocking {
        val standardDispatcher = kotlinx.coroutines.test.StandardTestDispatcher()
        val asyncVm = TransactionViewModel(repository, standardDispatcher)

        asyncVm.prepareForNewTransaction(accountId = account1Id, type = TransactionType.EXPENSE)
        asyncVm.onDigitInput("1")
        asyncVm.setCategory("General")
        asyncVm.setTitle("Test Expense")

        // Rapid back-to-back invocation before dispatcher advances
        asyncVm.saveTransaction()
        asyncVm.saveTransaction()

        // Advance dispatcher to execute scheduled coroutines
        standardDispatcher.scheduler.advanceUntilIdle()

        val transactions = repository.allTransactions.first()
        assertEquals(1, transactions.size)
        val balance = database.accountDao().getAccountBalanceDirect(account1Id)
        assertEquals(9_900L, balance)
    }

    @Test
    fun testFailurePathReleasesGuardAllowingSubsequentSave() = runBlocking {
        viewModel.prepareForNewTransaction(accountId = account1Id, type = TransactionType.EXPENSE)
        viewModel.onClear()

        // Validation failure (amount <= 0) should not lock the guard
        viewModel.saveTransaction()
        assertTrue(viewModel.saveState.value is SaveResult.Error)
        assertFalse(viewModel.isSaving.value)

        // Enter valid transaction and verify it can be saved successfully
        viewModel.onDigitInput("100")
        viewModel.setCategory("Food & Dining")
        val latch = CountDownLatch(1)
        viewModel.saveTransaction { latch.countDown() }
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals(SaveResult.Success, viewModel.saveState.value)
        assertFalse(viewModel.isSaving.value)

        val txs = repository.allTransactions.first()
        assertEquals(1, txs.size)
    }

    @Test
    fun testDoubleTapSaveTransferCreatesOnlyOneTransferAndUpdatesBalancesOnce() = runBlocking {
        val standardDispatcher = kotlinx.coroutines.test.StandardTestDispatcher()
        val asyncVm = TransactionViewModel(repository, standardDispatcher)

        asyncVm.prepareForTransfer(
            toAccountId = account2Id,
            amountCentavos = 50000L,
            category = "Bills & Utilities",
            title = "Deposit to Goal"
        )
        asyncVm.setAccountId(account1Id)

        // Rapid back-to-back save on transfer
        asyncVm.saveTransaction()
        asyncVm.saveTransaction()

        standardDispatcher.scheduler.advanceUntilIdle()

        val transactions = repository.allTransactions.first()
        assertEquals(1, transactions.size)
        assertEquals(TransactionType.TRANSFER, transactions[0].type)
        assertEquals(50000L, transactions[0].amount)

        // account1: initial 10_000 - 50_000 = -40_000
        val balance1 = database.accountDao().getAccountBalanceDirect(account1Id)
        assertEquals(-40_000L, balance1)
        // account2: initial 50_000 + 50_000 = 100_000
        val balance2 = database.accountDao().getAccountBalanceDirect(account2Id)
        assertEquals(100_000L, balance2)
    }

    @Test
    fun testSequentialSavesSucceedAfterResetForm() = runBlocking {
        viewModel.prepareForNewTransaction(accountId = account1Id, type = TransactionType.EXPENSE)
        viewModel.onDigitInput("50")
        viewModel.setCategory("General")
        viewModel.setTitle("First Expense")

        val latch1 = CountDownLatch(1)
        viewModel.saveTransaction { latch1.countDown() }
        assertTrue(latch1.await(2, TimeUnit.SECONDS))
        assertFalse(viewModel.isSaving.value)

        // Reset form for next entry
        viewModel.resetFormForNextEntry()
        assertFalse(viewModel.isSaving.value)

        viewModel.prepareForNewTransaction(accountId = account1Id, type = TransactionType.EXPENSE)
        viewModel.onDigitInput("75")
        viewModel.setCategory("General")
        viewModel.setTitle("Second Expense")

        val latch2 = CountDownLatch(1)
        viewModel.saveTransaction { latch2.countDown() }
        assertTrue(latch2.await(2, TimeUnit.SECONDS))
        assertFalse(viewModel.isSaving.value)

        val transactions = repository.allTransactions.first()
        assertEquals(2, transactions.size)
        val balance = database.accountDao().getAccountBalanceDirect(account1Id)
        // initial 10_000 - 5,000 (50.00) - 7,500 (75.00) = -2,500L
        assertEquals(-2_500L, balance)
    }
}
