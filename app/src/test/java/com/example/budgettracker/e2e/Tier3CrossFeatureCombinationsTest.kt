package com.example.budgettracker.e2e

import android.database.sqlite.SQLiteConstraintException
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.util.DueDateStatus
import com.example.budgettracker.util.LoanDateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Tier 3: Cross-Feature Combinations Test Suite
 * Tests pairwise interactions across subsystem boundaries:
 * 1. Batch Parse -> Preview Adjustment -> Room Live Balances
 * 2. Transfers & Live Balances -> Monthly Health Summary Exclusion
 * 3. Loan Setup -> Date Clamping -> 7-Day Warning -> Status Resolution
 * 4. Scoped Category Autocomplete -> Transaction Type Partitioning
 * 5. Account Soft-Deletion -> Balance Exclusion & Restore Re-inclusion
 * 6. ForeignKey.RESTRICT Guard -> Hard Delete Prevention with History
 * 7. Snapshot Export -> Database Wipe -> Import Roundtrip Fidelity
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier3CrossFeatureCombinationsTest {

    private lateinit var harness: OpaqueTestHarness

    @Before
    fun setUp() {
        harness = OpaqueTestHarness()
    }

    @After
    fun tearDown() {
        harness.close()
    }

    @Test
    fun testT3_1_BatchParseToPreviewToRoomLiveBalances() {
        // Step 1: Freeform text parse
        val text = "GCash 3000, BDO 15000"
        val parseResult = OpaqueTestHarness.BatchSetupParserOracle.parse(text)
        assertEquals(2, parseResult.accounts.size)

        // Step 2: User modifies BDO balance in confirmation preview card before commit
        val gcashDto = parseResult.accounts[0]
        val bdoDto = parseResult.accounts[1]
        bdoDto.initialBalanceCentavos = 1_600_000L // Adjusted from 15k to 16k in UI

        // Step 3: User confirms -> commit to Room database
        val gcashId = harness.createAccount(gcashDto.name, gcashDto.type, gcashDto.initialBalanceCentavos, gcashDto.presetId)
        val bdoId = harness.createAccount(bdoDto.name, bdoDto.type, bdoDto.initialBalanceCentavos, bdoDto.presetId)

        // Assertions: Live balances match confirmed inputs
        assertEquals(300_000L, harness.getAccountBalance(gcashId))
        assertEquals(1_600_000L, harness.getAccountBalance(bdoId))
        assertEquals(1_900_000L, harness.getTotalNetWorth())
    }

    @Test
    fun testT3_2_TransfersAndLiveBalancesToMonthlySummaryExclusion() {
        val checking = harness.createAccount("BPI Checking", AccountType.BANK, 100_000L)
        val savings = harness.createAccount("BPI Savings", AccountType.BANK, 50_000L)
        val now = System.currentTimeMillis()

        // 1. Income
        harness.recordIncome(checking, 30_000L, "Salary", "Paycheck", now)
        // 2. Expense
        harness.recordExpense(checking, 10_000L, "Food", "Groceries", now + 1)
        // 3. Transfer
        harness.recordTransfer(checking, savings, 15_000L, "Savings Deposit", now + 2)

        // Live balances:
        // Checking: 100,000 + 30,000 - 10,000 - 15,000 = 105,000
        // Savings: 50,000 + 15,000 = 65,000
        assertEquals(105_000L, harness.getAccountBalance(checking))
        assertEquals(65_000L, harness.getAccountBalance(savings))
        assertEquals(170_000L, harness.getTotalNetWorth())

        // Monthly Summary: Transfers MUST be completely excluded
        val totals = harness.getMonthlyTotals(now - 1000, now + 10_000)
        assertEquals(30_000L, totals.totalIncome)
        assertEquals(10_000L, totals.totalExpense)
        assertEquals(20_000L, totals.netSavings)
        assertEquals(66.666f, totals.savingsRate, 0.01f)
    }

    @Test
    fun testT3_3_LoanSetupToDateClampingToSevenDayWarning() {
        // Setup loan on April 24th with cycle day 31 (target month April has 30 days)
        val today = LocalDate.of(2026, 4, 24)
        val clampedDueDate = LoanDateUtils.calculateNextDueDate(today = today, cycleDay1 = 31)

        // April 31 clamps to April 30th
        assertEquals(LocalDate.of(2026, 4, 30), clampedDueDate)

        // 6 days between April 24 and April 30 -> within 7 days -> DUE_SOON
        val status = LoanDateUtils.getDueDateStatus(dueDate = clampedDueDate, today = today)
        assertEquals(DueDateStatus.DUE_SOON, status)
        assertTrue(LoanDateUtils.isDueSoon(dueDate = clampedDueDate, today = today, daysBefore = 7))
    }

    @Test
    fun testT3_4_ScopedCategoryAutocompletePartitioning() = runBlocking {
        val accId = harness.createAccount("Cash", AccountType.CASH, 10_000L)
        val now = System.currentTimeMillis()

        // Expense entries
        harness.recordExpense(accId, 100L, "Food & Dining", "Fast Food", now)
        harness.recordExpense(accId, 200L, "Transportation", "Jeepney", now + 1)

        // Income entries
        harness.recordIncome(accId, 500L, "Salary", "Main Job", now + 2)
        harness.recordIncome(accId, 300L, "Freelance", "Graphic Design", now + 3)

        // Verify EXPENSE suggestions only return Expense categories
        val expenseCats = harness.transactionDao.getDistinctCategories(TransactionType.EXPENSE).first()
        assertTrue(expenseCats.contains("Food & Dining"))
        assertTrue(expenseCats.contains("Transportation"))
        assertTrue(!expenseCats.contains("Salary"))
        assertTrue(!expenseCats.contains("Freelance"))

        // Verify INCOME suggestions only return Income categories
        val incomeCats = harness.transactionDao.getDistinctCategories(TransactionType.INCOME).first()
        assertTrue(incomeCats.contains("Salary"))
        assertTrue(incomeCats.contains("Freelance"))
        assertTrue(!incomeCats.contains("Food & Dining"))
        assertTrue(!incomeCats.contains("Transportation"))
    }

    @Test
    fun testT3_5_AccountSoftDeletionAndRestorationNetWorthImpact() {
        val a1 = harness.createAccount("Main Bank", AccountType.BANK, 50_000L)
        val a2 = harness.createAccount("Emergency Cash", AccountType.CASH, 25_000L)
        assertEquals(75_000L, harness.getTotalNetWorth())

        // Soft-delete Main Bank
        harness.softDeleteAccount(a1)
        val activeAccounts = harness.getAllActiveAccounts()
        assertEquals(1, activeAccounts.size)
        assertEquals("Emergency Cash", activeAccounts[0].name)
        // Net worth excludes soft-deleted accounts
        assertEquals(25_000L, harness.getTotalNetWorth())

        // Restore Main Bank
        harness.restoreAccount(a1)
        assertEquals(2, harness.getActiveAccountCount())
        assertEquals(75_000L, harness.getTotalNetWorth())
    }

    @Test
    fun testT3_6_ForeignKeyRestrictPreventsHardDeleteOfAccountWithTransactions() {
        val accId = harness.createAccount("Active Wallet", AccountType.E_WALLET, 10_000L)
        harness.recordExpense(accId, 1_000L, "Utilities", "Electricity Bill")

        val account = harness.getAccount(accId)
        assertNotNull(account)

        try {
            runBlocking { harness.accountDao.delete(account!!) }
            fail("Expected SQLiteConstraintException when hard-deleting account with transactions!")
        } catch (expected: SQLiteConstraintException) {
            // Success: ForeignKey.RESTRICT prevented cascading data loss
        }

        // Verify account is still preserved
        assertNotNull(harness.getAccount(accId))
    }

    @Test
    fun testT3_7_DatabaseSnapshotExportWipeRestoreRoundtrip() {
        // Seed database
        val gcashId = harness.createAccount("GCash", AccountType.E_WALLET, 50_000L, presetId = "preset_gcash")
        val loanId = harness.createAccount("SPayLater", AccountType.BNPL, 0L, presetId = "preset_spaylater")
        harness.setupLoanDetails(loanId, cycleDay1 = 15, cycleDay2 = 30, minimumDueCentavos = 100_000L, totalRemainingCentavos = 500_000L)

        val now = System.currentTimeMillis()
        harness.recordIncome(gcashId, 25_000L, "Salary", "Freelance Work", now)
        harness.recordExpense(gcashId, 5_000L, "Food", "Dinner", now + 1)
        harness.recordTransfer(gcashId, loanId, 10_000L, "Repayment", now + 2)

        val netWorthPre = harness.getTotalNetWorth()
        val gcashBalancePre = harness.getAccountBalance(gcashId)
        val loanBalancePre = harness.getAccountBalance(loanId)

        // Export Snapshot
        val snapshotJson = harness.exportJsonSnapshot()
        assertTrue(snapshotJson.isNotBlank())

        // Wipe / fresh harness
        val restoredHarness = OpaqueTestHarness()
        try {
            assertEquals(0, restoredHarness.getActiveAccountCount())
            restoredHarness.importJsonSnapshot(snapshotJson)

            // Full verification
            assertEquals(2, restoredHarness.getActiveAccountCount())
            assertEquals(gcashBalancePre, restoredHarness.getAccountBalance(gcashId))
            assertEquals(loanBalancePre, restoredHarness.getAccountBalance(loanId))
            assertEquals(netWorthPre, restoredHarness.getTotalNetWorth())

            val restoredLoanDetails = restoredHarness.getLoanDetails(loanId)
            assertNotNull(restoredLoanDetails)
            assertEquals(listOf(15, 30), restoredLoanDetails!!.parseDueDays())
            assertEquals(100_000L, restoredLoanDetails.minimumAmountDue)
        } finally {
            restoredHarness.close()
        }
    }
}
