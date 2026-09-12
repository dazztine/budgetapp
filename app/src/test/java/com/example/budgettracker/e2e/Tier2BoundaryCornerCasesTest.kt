package com.example.budgettracker.e2e

import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.util.DueDateStatus
import com.example.budgettracker.util.LoanDateUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Tier 2: Boundary & Corner Cases Test Suite
 * Covers edge cases, boundary conditions, and error handling (>= 5 tests per area):
 * 1. Date Clamping (Feb 28/29, April 30, same-day, year rollover)
 * 2. Soft-Cap 10 & Lifecycle Limits
 * 3. Transfer Neutrality & Invariance Boundaries
 * 4. Extreme, Zero & Negative Balances
 * 5. Text Parser Typo Tolerance & Acronym Collision Guard
 * 6. 7-Day Warning Status Boundaries
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier2BoundaryCornerCasesTest {

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
    // 1. Date Clamping Edge Cases (5 tests)
    // =========================================================================

    @Test
    fun testT2_1_1_FebruaryLeapYearClampsTo29() {
        // 2024 is a leap year (February has 29 days)
        val today = LocalDate.of(2024, 2, 10)
        val clamped = LoanDateUtils.calculateNextDueDate(today = today, cycleDay1 = 31)
        assertEquals(LocalDate.of(2024, 2, 29), clamped)
    }

    @Test
    fun testT2_1_2_FebruaryNonLeapYearClampsTo28() {
        // 2025 is NOT a leap year (February has 28 days)
        val today = LocalDate.of(2025, 2, 10)
        val clamped = LoanDateUtils.calculateNextDueDate(today = today, cycleDay1 = 31)
        assertEquals(LocalDate.of(2025, 2, 28), clamped)
    }

    @Test
    fun testT2_1_3_April30ClampsFor31stTarget() {
        // April has 30 days
        val today = LocalDate.of(2026, 4, 1)
        val clamped = LoanDateUtils.calculateNextDueDate(today = today, cycleDay1 = 31)
        assertEquals(LocalDate.of(2026, 4, 30), clamped)
    }

    @Test
    fun testT2_1_4_SameDayEvaluationMatchesToday() {
        // When today is the 15th and cycle day is 15th, next due date is TODAY
        val today = LocalDate.of(2026, 7, 15)
        val nextDue = LoanDateUtils.calculateNextDueDate(today = today, cycleDay1 = 15)
        assertEquals(today, nextDue)
    }

    @Test
    fun testT2_1_5_YearBoundaryRolloverDecemberToJanuary() {
        // December 31 -> cycle day 15 must roll over to next year January 15
        val today = LocalDate.of(2025, 12, 31)
        val nextDue = LoanDateUtils.calculateNextDueDate(today = today, cycleDay1 = 15)
        assertEquals(LocalDate.of(2026, 1, 15), nextDue)
    }

    // =========================================================================
    // 2. Soft-Cap 10 & Lifecycle Boundaries (5 tests)
    // =========================================================================

    @Test
    fun testT2_2_1_ExactlyTenActiveAccountsAllowed() {
        for (i in 1..10) {
            harness.createAccount("Account $i", AccountType.BANK, 1_000L)
        }
        assertEquals(10, harness.getActiveAccountCount())
        assertEquals(10, harness.getAllActiveAccounts().size)
    }

    @Test
    fun testT2_2_2_SoftDeletedAccountsExcludedFromTenLimit() {
        val ids = (1..10).map { harness.createAccount("Acc $it", AccountType.BANK, 1_000L) }
        assertEquals(10, harness.getActiveAccountCount())

        // Soft-delete 2 accounts
        harness.softDeleteAccount(ids[0])
        harness.softDeleteAccount(ids[1])

        assertEquals(8, harness.getActiveAccountCount())
    }

    @Test
    fun testT2_2_3_EleventhAccountSoftCapDetected() {
        for (i in 1..10) {
            harness.createAccount("Acc $i", AccountType.BANK, 1_000L)
        }
        assertEquals(10, harness.getActiveAccountCount())

        // Creating 11th account
        harness.createAccount("Acc 11", AccountType.BANK, 1_000L)
        val totalActive = harness.getActiveAccountCount()
        assertEquals(11, totalActive)
        // Domain rule: soft-cap check triggers when count > 10
        val isSoftCapExceeded = totalActive > 10
        assertTrue(isSoftCapExceeded)
    }

    @Test
    fun testT2_2_4_RestoringAccountRespectsCapBoundary() {
        val ids = (1..10).map { harness.createAccount("Acc $it", AccountType.BANK, 100L) }
        harness.softDeleteAccount(ids[0])
        assertEquals(9, harness.getActiveAccountCount())

        // Create another account to reach 10 again
        harness.createAccount("New Acc", AccountType.BANK, 100L)
        assertEquals(10, harness.getActiveAccountCount())

        // Restoring previously deleted account pushes count to 11
        harness.restoreAccount(ids[0])
        assertEquals(11, harness.getActiveAccountCount())
    }

    @Test
    fun testT2_2_5_SoftDeleteThenCreateMaintainsTenCap() {
        val ids = (1..10).map { harness.createAccount("Acc $it", AccountType.BANK, 100L) }
        assertEquals(10, harness.getActiveAccountCount())

        // Delete one, add one -> count stays at exactly 10
        harness.softDeleteAccount(ids[4])
        assertEquals(9, harness.getActiveAccountCount())

        harness.createAccount("Replacement Acc", AccountType.BANK, 100L)
        assertEquals(10, harness.getActiveAccountCount())
    }

    // =========================================================================
    // 3. Transfer Neutrality & Boundary Invariants (5 tests)
    // =========================================================================

    @Test
    fun testT2_3_1_TransferFromZeroBalanceAccountProducesNegativeWithoutNetWorthChange() {
        val zeroAcc = harness.createAccount("Zero Acc", AccountType.CASH, 0L)
        val bank = harness.createAccount("Bank", AccountType.BANK, 50_000L)
        val initialNetWorth = harness.getTotalNetWorth()
        assertEquals(50_000L, initialNetWorth)

        // Transfer 10,000 from zeroAcc to bank
        harness.recordTransfer(zeroAcc, bank, 10_000L)

        assertEquals(-10_000L, harness.getAccountBalance(zeroAcc))
        assertEquals(60_000L, harness.getAccountBalance(bank))
        assertEquals(initialNetWorth, harness.getTotalNetWorth())
    }

    @Test
    fun testT2_3_2_TransferExactTotalBalanceDepletesSourceToZero() {
        val acc1 = harness.createAccount("Acc1", AccountType.BANK, 25_000L)
        val acc2 = harness.createAccount("Acc2", AccountType.E_WALLET, 10_000L)

        harness.recordTransfer(acc1, acc2, 25_000L)

        assertEquals(0L, harness.getAccountBalance(acc1))
        assertEquals(35_000L, harness.getAccountBalance(acc2))
        assertEquals(35_000L, harness.getTotalNetWorth())
    }

    @Test
    fun testT2_3_3_HighValueMultiMillionTransferConservesNetWorth() {
        val largeInitial = 500_000_000_00L // ₱500,000,000.00
        val acc1 = harness.createAccount("Corp Treasury", AccountType.BANK, largeInitial)
        val acc2 = harness.createAccount("Payroll Reserve", AccountType.BANK, 0L)

        val transferAmount = 250_000_000_00L // ₱250,000,000.00
        harness.recordTransfer(acc1, acc2, transferAmount)

        assertEquals(250_000_000_00L, harness.getAccountBalance(acc1))
        assertEquals(250_000_000_00L, harness.getAccountBalance(acc2))
        assertEquals(largeInitial, harness.getTotalNetWorth())
    }

    @Test
    fun testT2_3_4_TransfersAcrossDifferentAccountTypes() {
        val bank = harness.createAccount("BDO Bank", AccountType.BANK, 50_000L)
        val ewallet = harness.createAccount("GCash", AccountType.E_WALLET, 10_000L)
        val cash = harness.createAccount("Physical Cash", AccountType.CASH, 5_000L)

        // Bank -> E-Wallet -> Cash
        harness.recordTransfer(bank, ewallet, 15_000L)
        harness.recordTransfer(ewallet, cash, 8_000L)

        assertEquals(35_000L, harness.getAccountBalance(bank))
        assertEquals(17_000L, harness.getAccountBalance(ewallet))
        assertEquals(13_000L, harness.getAccountBalance(cash))
        assertEquals(65_000L, harness.getTotalNetWorth())
    }

    @Test
    fun testT2_3_5_CircularSymmetricTransferRestoresAllBalances() {
        val a = harness.createAccount("Account A", AccountType.BANK, 10_000L)
        val b = harness.createAccount("Account B", AccountType.BANK, 20_000L)
        val c = harness.createAccount("Account C", AccountType.BANK, 30_000L)

        val transferAmt = 5_000L
        // A -> B -> C -> A
        harness.recordTransfer(a, b, transferAmt)
        harness.recordTransfer(b, c, transferAmt)
        harness.recordTransfer(c, a, transferAmt)

        assertEquals(10_000L, harness.getAccountBalance(a))
        assertEquals(20_000L, harness.getAccountBalance(b))
        assertEquals(30_000L, harness.getAccountBalance(c))
        assertEquals(60_000L, harness.getTotalNetWorth())
    }

    // =========================================================================
    // 4. Extreme, Zero & Negative Balances (5 tests)
    // =========================================================================

    @Test
    fun testT2_4_1_NegativeAccountBalanceReducesNetWorth() {
        val positiveAcc = harness.createAccount("Savings", AccountType.BANK, 50_000L)
        val loanAcc = harness.createAccount("Personal Loan", AccountType.LOAN, 0L)

        // Expense charged against loan -> becomes negative balance
        harness.recordExpense(loanAcc, 30_000L, "Debt", "Borrowed capital")

        assertEquals(-30_000L, harness.getAccountBalance(loanAcc))
        assertEquals(50_000L, harness.getAccountBalance(positiveAcc))
        // Net worth: 50,000 + (-30,000) = 20,000 centavos
        assertEquals(20_000L, harness.getTotalNetWorth())
    }

    @Test
    fun testT2_4_2_ZeroCentavoTransactionHandled() {
        val acc = harness.createAccount("Cash", AccountType.CASH, 10_000L)
        harness.recordExpense(acc, 0L, "General", "Free sample")
        assertEquals(10_000L, harness.getAccountBalance(acc))
        assertEquals(10_000L, harness.getTotalNetWorth())
    }

    @Test
    fun testT2_4_3_ExactOneCentavoTransactionPrecision() {
        val acc = harness.createAccount("BPI", AccountType.BANK, 0L)
        harness.recordIncome(acc, 1L, "Interest", "Micro interest credit")
        assertEquals(1L, harness.getAccountBalance(acc))
        assertEquals(1L, harness.getTotalNetWorth())

        harness.recordExpense(acc, 1L, "Fee", "Micro fee debit")
        assertEquals(0L, harness.getAccountBalance(acc))
    }

    @Test
    fun testT2_4_4_LargeValuesBoundaryWithout64BitOverflow() {
        val maxSafePesosCentavos = 90_000_000_000_000_00L // 90 trillion centavos, well within Long.MAX_VALUE
        val acc = harness.createAccount("Sovereign Fund", AccountType.ASSET, maxSafePesosCentavos)
        assertEquals(maxSafePesosCentavos, harness.getTotalNetWorth())

        harness.recordIncome(acc, 1_000_000_00L, "Yield", "Dividends")
        assertEquals(maxSafePesosCentavos + 1_000_000_00L, harness.getTotalNetWorth())
    }

    @Test
    fun testT2_4_5_EmptyDatabaseReturnsZeroNetWorth() {
        assertEquals(0, harness.getActiveAccountCount())
        assertEquals(0L, harness.getTotalNetWorth())
        assertTrue(harness.getAllActiveAccounts().isEmpty())
    }

    // =========================================================================
    // 5. Parser Typo Tolerance & Acronym Collision Guard (5 tests)
    // =========================================================================

    @Test
    fun testT2_5_1_AcronymCollisionGuardBdoVsBpiRejected() {
        // Critical requirement: Levenshtein distance between "BDO" and "BPI" is 1,
        // but short acronyms (<= 3 chars) MUST NOT match!
        val distance = OpaqueTestHarness.LevenshteinOracle.computeDistance("bdo", "bpi")
        assertEquals(2, distance)

        val isMatched = OpaqueTestHarness.LevenshteinOracle.isFuzzyMatch("bdo", "bpi")
        assertFalse("Acronym collision guard must strictly reject BDO matching to BPI!", isMatched)
    }

    @Test
    fun testT2_5_2_TypoToleranceFourLetterTokenMatched() {
        // "gcas" -> "gcash" (length 5, distance 1 -> valid fuzzy match)
        val isMatched = OpaqueTestHarness.LevenshteinOracle.isFuzzyMatch("gcas", "gcash")
        assertTrue(isMatched)
    }

    @Test
    fun testT2_5_3_TypoToleranceLongTokenMatched() {
        // "spaylatr" -> "spaylater" (distance 1, length 9 -> valid match)
        val isMatched = OpaqueTestHarness.LevenshteinOracle.isFuzzyMatch("spaylatr", "spaylater")
        assertTrue(isMatched)
    }

    @Test
    fun testT2_5_4_EmptyAndWhitespaceInputYieldsZeroResults() {
        val emptyResult = OpaqueTestHarness.BatchSetupParserOracle.parse("")
        assertEquals(0, emptyResult.accounts.size)
        assertEquals(0L, emptyResult.totalBalanceCentavos)

        val whitespaceResult = OpaqueTestHarness.BatchSetupParserOracle.parse("   \n\t   ")
        assertEquals(0, whitespaceResult.accounts.size)
    }

    @Test
    fun testT2_5_5_SpecialCharactersAndPunctuationNoiseStripped() {
        val text = "₱1,500.50! Maya???"
        val result = OpaqueTestHarness.BatchSetupParserOracle.parse(text)
        assertEquals(1, result.accounts.size)
        assertEquals(150_050L, result.accounts[0].initialBalanceCentavos)
        assertEquals("Maya", result.accounts[0].name)
    }

    // =========================================================================
    // 6. 7-Day Warning Boundary Statuses (5 tests)
    // =========================================================================

    @Test
    fun testT2_6_1_OverdueDueDateStatus() {
        val today = LocalDate.of(2026, 5, 15)
        val yesterday = LocalDate.of(2026, 5, 14)
        val status = LoanDateUtils.getDueDateStatus(dueDate = yesterday, today = today)
        assertEquals(DueDateStatus.OVERDUE, status)
    }

    @Test
    fun testT2_6_2_DueTodayStatusIsDueSoon() {
        val today = LocalDate.of(2026, 5, 15)
        val status = LoanDateUtils.getDueDateStatus(dueDate = today, today = today)
        assertEquals(DueDateStatus.DUE_SOON, status)
    }

    @Test
    fun testT2_6_3_ExactlySevenDaysAwayIsDueSoon() {
        val today = LocalDate.of(2026, 5, 15)
        val inSevenDays = LocalDate.of(2026, 5, 22)
        val status = LoanDateUtils.getDueDateStatus(dueDate = inSevenDays, today = today)
        assertEquals(DueDateStatus.DUE_SOON, status)
    }

    @Test
    fun testT2_6_4_ExactlyEightDaysAwayIsUpcoming() {
        val today = LocalDate.of(2026, 5, 15)
        val inEightDays = LocalDate.of(2026, 5, 23)
        val status = LoanDateUtils.getDueDateStatus(dueDate = inEightDays, today = today)
        assertEquals(DueDateStatus.UPCOMING, status)
    }

    @Test
    fun testT2_6_5_TwentyDaysAwayIsUpcoming() {
        val today = LocalDate.of(2026, 5, 10)
        val inTwentyDays = LocalDate.of(2026, 5, 30)
        val status = LoanDateUtils.getDueDateStatus(dueDate = inTwentyDays, today = today)
        assertEquals(DueDateStatus.UPCOMING, status)
    }
}
