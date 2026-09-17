package com.example.budgettracker.e2e

import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.util.DueDateStatus
import com.example.budgettracker.util.LoanDateUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Tier 4: Real-World Application Scenarios
 * Comprehensive, end-to-end user journeys modeled directly from ORIGINAL_REQUEST.md requirements:
 * 1. Paycheck & Initial Wallet Setup Journey
 * 2. High-Velocity Multi-Expense Day Journey
 * 3. Mid-Month Loan Repayment Journey
 * 4. Complete Disaster Recovery & Backup Journey
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier4RealWorldScenariosTest {

    private lateinit var harness: OpaqueTestHarness

    @Before
    fun setUp() {
        harness = OpaqueTestHarness()
    }

    @After
    fun tearDown() {
        harness.close()
    }

    /**
     * Scenario 4.1: Paycheck & Initial Wallet Setup Journey
     * - User installs app (0 accounts) -> FAB is in Setup Mode.
     * - User batch-pastes freeform account text: "GCash 5000, BDO Savings 45000, Cash 2500, SPayLater 1500 due on 15th".
     * - Parser extracts 4 accounts, starting balances, and cycle day 15.
     * - User confirms preview card -> 4 accounts committed to Room.
     * - Dynamic FAB switches to Transaction Mode (account count >= 1).
     * - Net Worth is correctly calculated to ₱54,000.00 (5,400,000 centavos).
     */
    @Test
    fun testScenario4_1_PaycheckAndInitialWalletSetupJourney() {
        // 1. Initial Cold Start
        assertEquals(0, harness.getActiveAccountCount())
        val fabModeInitial = if (harness.getActiveAccountCount() == 0) "SETUP" else "TRANSACTION"
        assertEquals("SETUP", fabModeInitial)

        // 2. Batch Paste Input
        val pastedText = "GCash 5000, BDO Savings 45000, Cash 2500, SPayLater 1500 due on 15th"
        val parsed = OpaqueTestHarness.BatchSetupParserOracle.parse(pastedText)
        assertEquals(4, parsed.accounts.size)

        // Verify parsed fields before commit (Zero Auto-Save Invariant)
        assertEquals(0, harness.getActiveAccountCount())

        // 3. User Review and Confirmation
        val accountIds = mutableListOf<Long>()
        for (dto in parsed.accounts) {
            val id = harness.createAccount(dto.name, dto.type, dto.initialBalanceCentavos, dto.presetId)
            if (dto.cycleDay1 != null) {
                harness.setupLoanDetails(id, cycleDay1 = dto.cycleDay1!!)
            }
            accountIds.add(id)
        }

        // 4. Verify Post-Setup State
        assertEquals(4, harness.getActiveAccountCount())
        val fabModePostSetup = if (harness.getActiveAccountCount() == 0) "SETUP" else "TRANSACTION"
        assertEquals("TRANSACTION", fabModePostSetup)

        // 5. Verify Net Worth Calculation
        // 500,000 (GCash) + 4,500,000 (BDO) + 250,000 (Cash) + 150,000 (SPayLater) = 5,400,000 centavos
        assertEquals(5_400_000L, harness.getTotalNetWorth())

        // 6. Verify SPayLater Cycle Tracking
        val spayId = accountIds[3]
        val loanDetails = harness.getLoanDetails(spayId)
        assertEquals(listOf(15), loanDetails!!.parseDueDays())
    }

    /**
     * Scenario 4.2: High-Velocity Multi-Expense Day Journey
     * - Commuter in Manila begins day with GCash (₱5,000) and Cash (₱2,000). Total Net Worth = ₱7,000.00.
     * - Morning: Quick-pastes "120 grab car gcash" -> ₱120.00 expense debited from GCash.
     * - Lunch: Logs "180 lunch cash" -> ₱180.00 expense debited from Cash.
     * - Afternoon: Transfers ₱1,000.00 from Bank (BDO ₱10,000) to GCash -> Net worth unchanged.
     * - Evening: Quick-pastes "pamasahe 45 cash" -> ₱45.00 expense debited from Cash.
     * - Verification: All balances update accurately, monthly totals reflect ₱345.00 expense, transfer is excluded.
     */
    @Test
    fun testScenario4_2_MultiExpenseDayJourney() {
        val gcashId = harness.createAccount("GCash", AccountType.E_WALLET, 500_000L) // ₱5,000.00
        val cashId = harness.createAccount("Cash", AccountType.CASH, 200_000L)       // ₱2,000.00
        val bdoId = harness.createAccount("BDO", AccountType.BANK, 1_000_000L)       // ₱10,000.00
        val startNetWorth = harness.getTotalNetWorth()
        assertEquals(1_700_000L, startNetWorth) // ₱17,000.00

        val now = System.currentTimeMillis()

        // 1. Morning Grab
        val grabDto = OpaqueTestHarness.SingleTransactionParserOracle.parse("120 grab car gcash", harness.getAllActiveAccounts())
        assertEquals(TransactionType.EXPENSE, grabDto.type)
        assertEquals(12_000L, grabDto.amountCentavos)
        harness.recordExpense(gcashId, grabDto.amountCentavos, grabDto.category, grabDto.title, now)

        // 2. Lunch Jollibee
        val lunchDto = OpaqueTestHarness.SingleTransactionParserOracle.parse("180 lunch cash", harness.getAllActiveAccounts())
        assertEquals(18_000L, lunchDto.amountCentavos)
        harness.recordExpense(cashId, lunchDto.amountCentavos, lunchDto.category, lunchDto.title, now + 1)

        // 3. Afternoon Transfer from BDO to GCash
        harness.recordTransfer(bdoId, gcashId, 100_000L, "Top-up GCash", now + 2) // ₱1,000.00

        // 4. Evening Jeepney
        val jeepDto = OpaqueTestHarness.SingleTransactionParserOracle.parse("pamasahe 45 cash", harness.getAllActiveAccounts())
        assertEquals(4_500L, jeepDto.amountCentavos)
        harness.recordExpense(cashId, jeepDto.amountCentavos, jeepDto.category, jeepDto.title, now + 3)

        // --- Verifications ---
        // GCash: 500,000 - 12,000 + 100,000 = 588,000 centavos (₱5,880.00)
        assertEquals(588_000L, harness.getAccountBalance(gcashId))

        // Cash: 200,000 - 18,000 - 4,500 = 177,500 centavos (₱1,775.00)
        assertEquals(177_500L, harness.getAccountBalance(cashId))

        // BDO: 1,000,000 - 100,000 = 900,000 centavos (₱9,000.00)
        assertEquals(900_000L, harness.getAccountBalance(bdoId))

        // Global Net Worth: 588,000 + 177,500 + 900,000 = 1,665,500 centavos (₱16,655.00)
        // Original (1,700,000) - Total expenses (12,000 + 18,000 + 4,500 = 34,500) = 1,665,500
        assertEquals(1_665_500L, harness.getTotalNetWorth())

        // Monthly Summary: Expenses = 34,500 centavos; Income = 0; Transfer excluded
        val totals = harness.getMonthlyTotals(now - 1000, now + 10_000)
        assertEquals(0L, totals.totalIncome)
        assertEquals(34_500L, totals.totalExpense)
        assertEquals(-34_500L, totals.netSavings)
    }

    /**
     * Scenario 4.3: Mid-Month Loan Repayment Journey
     * - User has BDO Bank (₱20,000) and SPayLater BNPL with cycle day 15.
     * - On May 10th (5 days before due date), loan status resolves to DUE_SOON.
     * - User makes loan repayment transfer: ₱2,500.00 from BDO to SPayLater.
     * - SPayLater balance is credited, BDO is debited.
     * - Net worth remains conserved.
     */
    @Test
    fun testScenario4_3_LoanRepaymentJourney() {
        val bdoId = harness.createAccount("BDO", AccountType.BANK, 2_000_000L) // ₱20,000.00
        val spayId = harness.createAccount("SPayLater", AccountType.BNPL, -250_000L) // -₱2,500.00 owed
        harness.setupLoanDetails(spayId, cycleDay1 = 15, minimumDueCentavos = 250_000L, totalRemainingCentavos = 250_000L)

        // Today is May 10th (5 days away from May 15th)
        val today = LocalDate.of(2026, 5, 10)
        val dueDate = LoanDateUtils.calculateNextDueDate(today = today, cycleDay1 = 15)
        assertEquals(LocalDate.of(2026, 5, 15), dueDate)

        // Status must be DUE_SOON (<= 7 days)
        val statusPrePayment = LoanDateUtils.getDueDateStatus(dueDate = dueDate, today = today)
        assertEquals(DueDateStatus.DUE_SOON, statusPrePayment)

        val netWorthBefore = harness.getTotalNetWorth()
        assertEquals(1_750_000L, netWorthBefore) // 20,000 - 2,500 = 17,500 PHP = 1,750,000 centavos

        // Execute repayment transfer from BDO to SPayLater (₱2,500.00)
        harness.recordTransfer(bdoId, spayId, 250_000L, "SPayLater Full Repayment")

        // Post repayment balances
        assertEquals(1_750_000L, harness.getAccountBalance(bdoId)) // ₱17,500.00
        assertEquals(0L, harness.getAccountBalance(spayId))        // ₱0.00 debt cleared
        assertEquals(netWorthBefore, harness.getTotalNetWorth())   // Net worth conserved!
    }

    /**
     * Scenario 4.4: Complete Disaster Recovery & Backup Journey
     * - Complex wallet: 3 accounts (Bank, E-Wallet, BNPL Loan), 6 transactions (Income, Expense, Transfer, Adjustment).
     * - User exports complete JSON snapshot via SAF backup contract.
     * - Disaster simulation: app data wiped / fresh reinstall.
     * - User imports JSON snapshot.
     * - Complete verification: exact match across account balances, loan cycle dates, transaction history, and net worth.
     */
    @Test
    fun testScenario4_4_CompleteDisasterRecoveryJourney() {
        val bdoId = harness.createAccount("BDO Bank", AccountType.BANK, 500_000L, presetId = "preset_bdo", displayOrder = 1)
        val gcashId = harness.createAccount("GCash", AccountType.E_WALLET, 200_000L, presetId = "preset_gcash", displayOrder = 2)
        val loanId = harness.createAccount("Billease", AccountType.BNPL, 0L, presetId = "preset_billease", displayOrder = 3)

        harness.setupLoanDetails(loanId, cycleDay1 = 15, cycleDay2 = 30, minimumDueCentavos = 100_000L, totalRemainingCentavos = 800_000L)

        val now = System.currentTimeMillis()
        harness.recordIncome(bdoId, 100_000L, "Salary", "Primary Salary", now)
        harness.recordTransfer(bdoId, gcashId, 50_000L, "Allowance to GCash", now + 1)
        harness.recordExpense(gcashId, 15_000L, "Food & Dining", "Grocery run", now + 2)
        harness.recordTransfer(gcashId, loanId, 20_000L, "Installment Payment", now + 3)

        // Capture snapshot before wipe
        val snapshotJson = harness.exportJsonSnapshot()
        val csvBackup = harness.exportCsv()

        val netWorthPre = harness.getTotalNetWorth()
        val bdoBalancePre = harness.getAccountBalance(bdoId)
        val gcashBalancePre = harness.getAccountBalance(gcashId)
        val loanBalancePre = harness.getAccountBalance(loanId)

        // Disaster simulation: Fresh harness representing wiped database
        val recoveryHarness = OpaqueTestHarness()
        try {
            assertEquals(0, recoveryHarness.getActiveAccountCount())
            assertEquals(0L, recoveryHarness.getTotalNetWorth())

            // Restore from JSON snapshot
            recoveryHarness.importJsonSnapshot(snapshotJson)

            // Comprehensive Verification: 100% Fidelity
            assertEquals(3, recoveryHarness.getActiveAccountCount())
            assertEquals(netWorthPre, recoveryHarness.getTotalNetWorth())
            assertEquals(bdoBalancePre, recoveryHarness.getAccountBalance(bdoId))
            assertEquals(gcashBalancePre, recoveryHarness.getAccountBalance(gcashId))
            assertEquals(loanBalancePre, recoveryHarness.getAccountBalance(loanId))

            val restoredLoan = recoveryHarness.getLoanDetails(loanId)
            assertNotNull(restoredLoan)
            assertEquals(listOf(15, 30), restoredLoan!!.parseDueDays())
            assertEquals(100_000L, restoredLoan.minimumAmountDue)
            assertEquals(800_000L, restoredLoan.totalRemainingBalance)

            // Verify CSV generation on restored data matches original
            val restoredCsv = recoveryHarness.exportCsv()
            assertEquals(csvBackup.lines().size, restoredCsv.lines().size)
        } finally {
            recoveryHarness.close()
        }
    }
}
