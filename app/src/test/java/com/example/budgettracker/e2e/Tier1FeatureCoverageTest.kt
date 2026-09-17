package com.example.budgettracker.e2e

import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.util.LoanDateUtils
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
import java.time.LocalDate

/**
 * Tier 1: Feature Coverage Test Suite
 * Covers primary behavior (happy paths) with >= 5 test cases per core capability:
 * 1. Accounts
 * 2. Live Balances & Net Worth
 * 3. Transfers & Neutrality
 * 4. Loans & Due Date Tracking
 * 5. Offline Parsers & Zero Auto-Save Invariant
 * 6. Data Ownership (Export & Import)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier1FeatureCoverageTest {

    private lateinit var harness: OpaqueTestHarness

    @Before
    fun setUp() {
        harness = OpaqueTestHarness()
    }

    @After
    fun tearDown() {
        harness.close()
    }

    // =========================================================================
    // 1. Account Lifecycle & Integrity (5 tests)
    // =========================================================================

    @Test
    fun testT1_1_1_AccountCreationWithAttributes() {
        val id = harness.createAccount(
            name = "BDO Checking",
            type = AccountType.BANK,
            initialBalanceCentavos = 250_000L,
            presetId = "preset_bdo",
            displayOrder = 1
        )
        assertTrue(id > 0)
        val acc = harness.getAccount(id)
        assertNotNull(acc)
        assertEquals("BDO Checking", acc!!.name)
        assertEquals(AccountType.BANK, acc.type)
        assertEquals(250_000L, acc.initialBalance)
        assertEquals("preset_bdo", acc.presetId)
        assertTrue(acc.isActive)
        assertEquals(1, acc.displayOrder)
    }

    @Test
    fun testT1_1_2_AccountSoftDeleteMarksInactive() {
        val id = harness.createAccount("Cash Wallet", AccountType.CASH, 50_000L)
        val countBefore = harness.getActiveAccountCount()
        assertEquals(1, countBefore)

        val updated = harness.softDeleteAccount(id)
        assertEquals(1, updated)

        val countAfter = harness.getActiveAccountCount()
        assertEquals(0, countAfter)

        val acc = harness.getAccount(id)
        assertNotNull(acc)
        assertFalse(acc!!.isActive)
    }

    @Test
    fun testT1_1_3_AccountRestorationRestoresActive() {
        val id = harness.createAccount("Maya Wallet", AccountType.E_WALLET, 10_000L)
        harness.softDeleteAccount(id)
        assertEquals(0, harness.getActiveAccountCount())

        val restored = harness.restoreAccount(id)
        assertEquals(1, restored)
        assertEquals(1, harness.getActiveAccountCount())

        val acc = harness.getAccount(id)
        assertTrue(acc!!.isActive)
    }

    @Test
    fun testT1_1_4_AccountDisplayOrderResequencing() {
        val id1 = harness.createAccount("First Acc", AccountType.BANK, 100L, displayOrder = 10)
        val id2 = harness.createAccount("Second Acc", AccountType.BANK, 100L, displayOrder = 20)

        // Resequence: swap order
        harness.updateDisplayOrder(id1, 25)
        harness.updateDisplayOrder(id2, 5)

        val active = harness.getAllActiveAccounts()
        assertEquals(2, active.size)
        assertEquals("Second Acc", active[0].name)
        assertEquals("First Acc", active[1].name)
    }

    @Test
    fun testT1_1_5_AccountPresetAssociation() {
        val gcashId = harness.createAccount("GCash", AccountType.E_WALLET, 100_000L, presetId = "preset_gcash")
        val bpiId = harness.createAccount("BPI Savings", AccountType.BANK, 500_000L, presetId = "preset_bpi")

        val gcash = harness.getAccount(gcashId)
        val bpi = harness.getAccount(bpiId)

        assertEquals("preset_gcash", gcash!!.presetId)
        assertEquals("preset_bpi", bpi!!.presetId)
    }

    // =========================================================================
    // 2. Live Balances & Net Worth (5 tests)
    // =========================================================================

    @Test
    fun testT1_2_1_IncomeCreditIncreasesBalance() {
        val accId = harness.createAccount("Salary Acc", AccountType.BANK, 10_000L)
        assertEquals(10_000L, harness.getAccountBalance(accId))

        harness.recordIncome(accId, 25_000L, "Salary", "Bi-monthly Paycheck")
        assertEquals(35_000L, harness.getAccountBalance(accId))
    }

    @Test
    fun testT1_2_2_ExpenseDebitDecreasesBalance() {
        val accId = harness.createAccount("Daily Cash", AccountType.CASH, 20_000L)
        harness.recordExpense(accId, 5_500L, "Food & Dining", "Jollibee meal")
        assertEquals(14_500L, harness.getAccountBalance(accId))
    }

    @Test
    fun testT1_2_3_AdjustmentIncreaseCredited() {
        val accId = harness.createAccount("High Yield Savings", AccountType.BANK, 100_000L)
        harness.recordAdjustment(accId, 1_250L, true, "Bank Interest")
        assertEquals(101_250L, harness.getAccountBalance(accId))
    }

    @Test
    fun testT1_2_4_AdjustmentDecreaseDebited() {
        val accId = harness.createAccount("E-Wallet", AccountType.E_WALLET, 50_000L)
        harness.recordAdjustment(accId, 1500L, false, "Monthly Maintenance Fee")
        assertEquals(48_500L, harness.getAccountBalance(accId))
    }

    @Test
    fun testT1_2_5_MultiAccountNetWorthAggregation() {
        val a1 = harness.createAccount("Bank", AccountType.BANK, 100_000L)
        val a2 = harness.createAccount("Wallet", AccountType.E_WALLET, 50_000L)
        val a3 = harness.createAccount("Cash", AccountType.CASH, 25_000L)

        assertEquals(175_000L, harness.getTotalNetWorth())

        // Income to a1 (+₱50.00), Expense from a2 (-₱20.00), Adjustment on a3 (+₱5.00)
        harness.recordIncome(a1, 5_000L, "Salary", "Bonus")
        harness.recordExpense(a2, 2_000L, "Food", "Dinner")
        harness.recordAdjustment(a3, 500L, true, "Found coin")

        // 175,000 + 5,000 - 2,000 + 500 = 178,500 centavos
        assertEquals(178_500L, harness.getTotalNetWorth())
    }

    // =========================================================================
    // 3. Transfers & Neutrality (5 tests)
    // =========================================================================

    @Test
    fun testT1_3_1_TransferDebitsSourceCreditsTargetSymmetrically() {
        val source = harness.createAccount("Source Bank", AccountType.BANK, 100_000L)
        val target = harness.createAccount("Target Wallet", AccountType.E_WALLET, 20_000L)

        harness.recordTransfer(source, target, 30_000L, "Top-up Wallet")

        assertEquals(70_000L, harness.getAccountBalance(source))
        assertEquals(50_000L, harness.getAccountBalance(target))
    }

    @Test
    fun testT1_3_2_TransferLeavesGlobalNetWorthStrictlyUnchanged() {
        val a1 = harness.createAccount("Account 1", AccountType.BANK, 80_000L)
        val a2 = harness.createAccount("Account 2", AccountType.E_WALLET, 40_000L)
        val initialNetWorth = harness.getTotalNetWorth()
        assertEquals(120_000L, initialNetWorth)

        harness.recordTransfer(a1, a2, 35_000L, "Inter-account move")

        assertEquals(initialNetWorth, harness.getTotalNetWorth())
    }

    @Test
    fun testT1_3_3_TransfersExcludedFromMonthlyIncomeAndExpense() {
        val a1 = harness.createAccount("Checking", AccountType.BANK, 50_000L)
        val a2 = harness.createAccount("Savings", AccountType.BANK, 50_000L)
        val now = System.currentTimeMillis()

        harness.recordIncome(a1, 10_000L, "Salary", "Paycheck", now)
        harness.recordExpense(a1, 3_000L, "Food", "Groceries", now + 1)
        harness.recordTransfer(a1, a2, 20_000L, "Transfer to savings", now + 2)

        val totals = harness.getMonthlyTotals(now - 1000, now + 10_000)
        assertEquals(10_000L, totals.totalIncome)
        assertEquals(3_000L, totals.totalExpense)
        assertEquals(7_000L, totals.netSavings)
    }

    @Test
    fun testT1_3_4_BidirectionalTransfersMaintainBalanceIntegrity() {
        val accA = harness.createAccount("A", AccountType.BANK, 100_000L)
        val accB = harness.createAccount("B", AccountType.E_WALLET, 50_000L)

        harness.recordTransfer(accA, accB, 20_000L)
        harness.recordTransfer(accB, accA, 15_000L)
        harness.recordTransfer(accA, accB, 5_000L)

        // A: 100,000 - 20,000 + 15,000 - 5,000 = 90,000
        // B: 50,000 + 20,000 - 15,000 + 5,000 = 60,000
        assertEquals(90_000L, harness.getAccountBalance(accA))
        assertEquals(60_000L, harness.getAccountBalance(accB))
        assertEquals(150_000L, harness.getTotalNetWorth())
    }

    @Test
    fun testT1_3_5_ChainedMultiHopTransfersPreserveFunds() {
        val acc1 = harness.createAccount("Node1", AccountType.BANK, 100_000L)
        val acc2 = harness.createAccount("Node2", AccountType.E_WALLET, 0L)
        val acc3 = harness.createAccount("Node3", AccountType.CASH, 0L)

        // Hop 1: Node1 -> Node2 (₱500.00)
        harness.recordTransfer(acc1, acc2, 50_000L)
        // Hop 2: Node2 -> Node3 (₱300.00)
        harness.recordTransfer(acc2, acc3, 30_000L)

        assertEquals(50_000L, harness.getAccountBalance(acc1))
        assertEquals(20_000L, harness.getAccountBalance(acc2))
        assertEquals(30_000L, harness.getAccountBalance(acc3))
        assertEquals(100_000L, harness.getTotalNetWorth())
    }

    // =========================================================================
    // 4. Loans & Due Date Tracking (5 tests)
    // =========================================================================

    @Test
    fun testT1_4_1_UpcomingSingleCycleDayResolved() {
        val today = LocalDate.of(2026, 4, 10)
        val nextDue = LoanDateUtils.calculateNextDueDate(today = today, cycleDay1 = 15)
        assertEquals(LocalDate.of(2026, 4, 15), nextDue)
    }

    @Test
    fun testT1_4_2_PassedSingleCycleDayRollsOverToNextMonth() {
        val today = LocalDate.of(2026, 4, 16)
        val nextDue = LoanDateUtils.calculateNextDueDate(today = today, cycleDay1 = 15)
        assertEquals(LocalDate.of(2026, 5, 15), nextDue)
    }

    @Test
    fun testT1_4_3_DualCycleDaysSelectsEarliestCandidate() {
        val today = LocalDate.of(2026, 4, 12)
        val nextDue = LoanDateUtils.calculateNextDueDate(today = today, cycleDay1 = 15, cycleDay2 = 30)
        assertEquals(LocalDate.of(2026, 4, 15), nextDue)

        val todayPastFirst = LocalDate.of(2026, 4, 16)
        val nextDue2 = LoanDateUtils.calculateNextDueDate(today = todayPastFirst, cycleDay1 = 15, cycleDay2 = 30)
        assertEquals(LocalDate.of(2026, 4, 30), nextDue2)
    }

    @Test
    fun testT1_4_4_LoanAccountDetailsPersistence() {
        val loanAccId = harness.createAccount("SPayLater", AccountType.BNPL, 0L)
        harness.setupLoanDetails(
            accountId = loanAccId,
            cycleDay1 = 15,
            cycleDay2 = 30,
            minimumDueCentavos = 150_000L,
            totalRemainingCentavos = 1_200_000L,
            reminderDaysBefore = 7
        )

        val details = harness.getLoanDetails(loanAccId)
        assertNotNull(details)
        assertEquals(listOf(15, 30), details!!.parseDueDays())
        assertEquals(150_000L, details.minimumAmountDue)
        assertEquals(1_200_000L, details.totalRemainingBalance)
        assertEquals(7, details.reminderDaysBefore)
    }

    @Test
    fun testT1_4_5_CascadeDeletionOfLoanDetailsOnAccountDelete() {
        val loanAccId = harness.createAccount("GLoan", AccountType.LOAN, 0L)
        harness.setupLoanDetails(loanAccId, cycleDay1 = 20, minimumDueCentavos = 50_000L)
        assertNotNull(harness.getLoanDetails(loanAccId))

        // Hard delete account without transactions -> triggers CASCADE
        val acc = harness.getAccount(loanAccId)!!
        kotlinx.coroutines.runBlocking { harness.accountDao.delete(acc) }

        assertNull(harness.getAccount(loanAccId))
        assertNull(harness.getLoanDetails(loanAccId))
    }

    // =========================================================================
    // 5. Offline Parsers & Zero Auto-Save Invariant (5 tests)
    // =========================================================================

    @Test
    fun testT1_5_1_BatchParserExtractsMultipleAccounts() {
        val text = "GCash 3000, BDO savings 15000, SPayLater 2000 due on the 15th"
        val result = OpaqueTestHarness.BatchSetupParserOracle.parse(text)

        assertEquals(3, result.accounts.size)
        assertEquals("GCash", result.accounts[0].name)
        assertEquals(AccountType.E_WALLET, result.accounts[0].type)

        assertEquals("BDO Savings", result.accounts[1].name)
        assertEquals(AccountType.BANK, result.accounts[1].type)

        assertEquals("SPayLater", result.accounts[2].name)
        assertEquals(AccountType.BNPL, result.accounts[2].type)
        assertEquals(15, result.accounts[2].cycleDay1)
    }

    @Test
    fun testT1_5_2_BatchParserConvertsToCentavosAccurately() {
        val text = "GCash 3000, BDO 15k, Maya 250.50"
        val result = OpaqueTestHarness.BatchSetupParserOracle.parse(text)

        assertEquals(300_000L, result.accounts[0].initialBalanceCentavos)
        assertEquals(1_500_000L, result.accounts[1].initialBalanceCentavos)
        assertEquals(25_050L, result.accounts[2].initialBalanceCentavos)
        assertEquals(1_825_050L, result.totalBalanceCentavos)
    }

    @Test
    fun testT1_5_3_SingleTransactionParserExtractsComponents() {
        val known = listOf(
            AccountEntity(id = 1, name = "GCash", type = AccountType.E_WALLET)
        )
        val result = OpaqueTestHarness.SingleTransactionParserOracle.parse("150 gcash lunch", known)

        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(15_000L, result.amountCentavos)
        assertEquals("GCash", result.accountName)
        assertEquals("Food & Dining", result.category)
    }

    @Test
    fun testT1_5_4_TagalogColloquialismCategoryInference() {
        val known = listOf(
            AccountEntity(id = 1, name = "BPI", type = AccountType.BANK),
            AccountEntity(id = 2, name = "Cash", type = AccountType.CASH)
        )

        val salaryResult = OpaqueTestHarness.SingleTransactionParserOracle.parse("sahod 25000 bpi", known)
        assertEquals(TransactionType.INCOME, salaryResult.type)
        assertEquals(2_500_000L, salaryResult.amountCentavos)
        assertEquals("Salary", salaryResult.category)
        assertEquals("BPI", salaryResult.accountName)

        val jeepResult = OpaqueTestHarness.SingleTransactionParserOracle.parse("pamasahe 45 cash", known)
        assertEquals(TransactionType.EXPENSE, jeepResult.type)
        assertEquals(4_500L, jeepResult.amountCentavos)
        assertEquals("Transportation", jeepResult.category)
        assertEquals("Cash", jeepResult.accountName)
    }

    @Test
    fun testT1_5_5_ZeroAutoSaveInvariantMemoryOnly() {
        // Guarantee: running parsers produces ZERO database writes
        val countAccountsBefore = harness.getActiveAccountCount()
        val netWorthBefore = harness.getTotalNetWorth()

        val parseResult = OpaqueTestHarness.BatchSetupParserOracle.parse("GCash 5000, Maya 2000")
        assertEquals(2, parseResult.accounts.size)

        val txResult = OpaqueTestHarness.SingleTransactionParserOracle.parse("150 lunch", emptyList())
        assertEquals(15_000L, txResult.amountCentavos)

        // Verifying invariant: No mutations occurred in Room
        assertEquals(countAccountsBefore, harness.getActiveAccountCount())
        assertEquals(netWorthBefore, harness.getTotalNetWorth())
    }

    // =========================================================================
    // 6. Data Ownership: Export & Import (5 tests)
    // =========================================================================

    @Test
    fun testT1_6_1_JsonSnapshotExportStructure() {
        val aId = harness.createAccount("GCash", AccountType.E_WALLET, 10_000L, presetId = "preset_gcash")
        harness.recordIncome(aId, 5_000L, "Salary", "Freelance")
        val json = harness.exportJsonSnapshot()

        assertTrue(json.contains("\"accounts\""))
        assertTrue(json.contains("\"transactions\""))
        assertTrue(json.contains("\"GCash\""))
        assertTrue(json.contains("\"Freelance\""))
    }

    @Test
    fun testT1_6_2_JsonSnapshotImportRoundtripPreservesData() {
        val a1 = harness.createAccount("GCash", AccountType.E_WALLET, 15_000L)
        val a2 = harness.createAccount("BPI", AccountType.BANK, 50_000L)
        harness.recordTransfer(a1, a2, 5_000L, "Savings Deposit")

        val snapshot = harness.exportJsonSnapshot()

        // Create fresh second harness to simulate restore on a clean slate
        val freshHarness = OpaqueTestHarness()
        try {
            assertEquals(0, freshHarness.getActiveAccountCount())
            freshHarness.importJsonSnapshot(snapshot)

            assertEquals(2, freshHarness.getActiveAccountCount())
            assertEquals(10_000L, freshHarness.getAccountBalance(a1))
            assertEquals(55_000L, freshHarness.getAccountBalance(a2))
            assertEquals(65_000L, freshHarness.getTotalNetWorth())
        } finally {
            freshHarness.close()
        }
    }

    @Test
    fun testT1_6_3_JsonSnapshotAtomicImportOnSuccess() {
        val aId = harness.createAccount("Tonik Bank", AccountType.BANK, 250_000L)
        harness.setupLoanDetails(aId, cycleDay1 = 15, minimumDueCentavos = 10_000L)

        val json = harness.exportJsonSnapshot()

        val testHarness = OpaqueTestHarness()
        try {
            testHarness.importJsonSnapshot(json)
            val restoredLoan = testHarness.getLoanDetails(aId)
            assertNotNull(restoredLoan)
            assertEquals(listOf(15), restoredLoan!!.parseDueDays())
            assertEquals(10_000L, restoredLoan.minimumAmountDue)
        } finally {
            testHarness.close()
        }
    }

    @Test
    fun testT1_6_4_CsvExportHeaderAndRowStructure() {
        val aId = harness.createAccount("Cash", AccountType.CASH, 100_000L)
        harness.recordExpense(aId, 15_000L, "Food & Dining", "Restaurant Meal", timestamp = 1713000000000L)

        val csv = harness.exportCsv()
        val lines = csv.trim().lines()

        assertTrue(lines.size >= 2)
        assertEquals("Date,Type,Amount,Account,ToAccount,Category,Title", lines[0])
        assertTrue(lines[1].contains("EXPENSE,150.00,\"Cash\",\"\",\"Food & Dining\",\"Restaurant Meal\""))
    }

    @Test
    fun testT1_6_5_CsvExportFormattedCurrencyDecimals() {
        val aId = harness.createAccount("Maya", AccountType.E_WALLET, 50_000L)
        // 12345 centavos = ₱123.45
        harness.recordExpense(aId, 12_345L, "Shopping", "Office Supplies", timestamp = 1713000000000L)

        val csv = harness.exportCsv()
        assertTrue(csv.contains("123.45"))
    }
}
