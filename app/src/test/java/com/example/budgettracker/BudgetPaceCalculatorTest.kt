package com.example.budgettracker

import com.example.budgettracker.data.local.entity.BudgetEntity
import com.example.budgettracker.data.local.entity.CustomCategoryEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.domain.BudgetPaceCalculator
import com.example.budgettracker.domain.CategoryBudgetStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetPaceCalculatorTest {

    private val billAccountIds = setOf(100L, 101L)

    private fun createTx(
        type: TransactionType,
        amount: Long,
        accountId: Long = 1L,
        toAccountId: Long? = null,
        isAdjustment: Boolean = false,
        category: String = "General",
        title: String = "Test tx",
        timestamp: Long = 1700000000000L
    ): TransactionEntity {
        return TransactionEntity(
            id = 0,
            type = type,
            amount = amount,
            isAdjustment = isAdjustment,
            accountId = accountId,
            toAccountId = toAccountId,
            category = category,
            title = title,
            timestamp = timestamp
        )
    }

    private fun expense(amount: Long, category: String, isAdjustment: Boolean = false): TransactionEntity {
        return TransactionEntity(
            id = 0,
            type = TransactionType.EXPENSE,
            amount = amount,
            isAdjustment = isAdjustment,
            accountId = 1L,
            category = category,
            title = "Expense item",
            timestamp = 1700000000000L
        )
    }

    private fun installment(amount: Long, category: String): TransactionEntity {
        return TransactionEntity(
            id = 0,
            type = TransactionType.INSTALLMENT,
            amount = amount,
            accountId = 1L,
            installmentPlanId = 10L,
            category = category,
            title = "Installment item",
            timestamp = 1700000000000L
        )
    }

    private fun billTransfer(amount: Long, toAccountId: Long, category: String = "Bill Payment"): TransactionEntity {
        return TransactionEntity(
            id = 0,
            type = TransactionType.TRANSFER,
            amount = amount,
            accountId = 1L,
            toAccountId = toAccountId,
            category = category,
            title = "Bill transfer item",
            timestamp = 1700000000000L
        )
    }

    private fun normalTransfer(amount: Long, toAccountId: Long): TransactionEntity {
        return TransactionEntity(
            id = 0,
            type = TransactionType.TRANSFER,
            amount = amount,
            accountId = 1L,
            toAccountId = toAccountId,
            category = "Transfer",
            title = "Savings transfer",
            timestamp = 1700000000000L
        )
    }

    private fun income(amount: Long): TransactionEntity {
        return TransactionEntity(
            id = 0,
            type = TransactionType.INCOME,
            amount = amount,
            accountId = 1L,
            category = "Salary",
            title = "Salary",
            timestamp = 1700000000000L
        )
    }

    @Test
    fun testDayOne_standardMonth() {
        val today = LocalDate.of(2026, 10, 1) // October has 31 days. Day 1: daysLeft = 31
        val monthlyBudget = 3100000L // ₱31,000.00
        val txs = listOf(expense(100000L, "Food & Dining")) // Spent ₱1,000.00

        val result = BudgetPaceCalculator.calculate(
            monthlyBudget = monthlyBudget,
            transactions = txs,
            today = today,
            billAccountIds = billAccountIds
        )

        assertEquals(31, result.daysLeft)
        assertEquals(3000000L, result.leftThisMonth)
        assertFalse(result.isOverBudget)
        assertEquals(0L, result.overBy)
        // 30,000.00 / 31 = 967.7419... -> 96774 centavos
        // 3000000 / 31 = 96774L, remainder = 3000000 - (96774 * 31) = 6L
        assertEquals(96774L, result.dailyAllowance)
        assertEquals(6L, result.remainderCentavos)
        assertEquals(3000000L, result.dailyAllowance * result.daysLeft + result.remainderCentavos)
    }

    @Test
    fun testLastDayOfMonth() {
        val today = LocalDate.of(2026, 10, 31) // October 31: daysLeft = 1
        val monthlyBudget = 1000000L // ₱10,000.00
        val txs = listOf(expense(400000L, "Groceries")) // Spent ₱4,000.00

        val result = BudgetPaceCalculator.calculate(
            monthlyBudget = monthlyBudget,
            transactions = txs,
            today = today,
            billAccountIds = billAccountIds
        )

        assertEquals(1, result.daysLeft)
        assertEquals(600000L, result.leftThisMonth)
        assertFalse(result.isOverBudget)
        assertEquals(0L, result.overBy)
        assertEquals(600000L, result.dailyAllowance)
        assertEquals(0L, result.remainderCentavos)
    }

    @Test
    fun testLeapYearFebruary() {
        // 2028 is a leap year (Feb 29 days)
        val today = LocalDate.of(2028, 2, 1)
        val monthlyBudget = 2900000L // ₱29,000.00
        val txs = emptyList<TransactionEntity>()

        val result = BudgetPaceCalculator.calculate(
            monthlyBudget = monthlyBudget,
            transactions = txs,
            today = today,
            billAccountIds = billAccountIds
        )

        assertEquals(29, result.daysLeft)
        assertEquals(2900000L, result.leftThisMonth)
        assertEquals(100000L, result.dailyAllowance)
        assertEquals(0L, result.remainderCentavos)
    }

    @Test
    fun testNonLeapYearFebruary() {
        // 2026 is non-leap (Feb 28 days)
        val today = LocalDate.of(2026, 2, 14) // Day 14: daysLeft = 28 - 14 + 1 = 15
        val monthlyBudget = 1500000L
        val txs = listOf(expense(300000L, "Food"))

        val result = BudgetPaceCalculator.calculate(
            monthlyBudget = monthlyBudget,
            transactions = txs,
            today = today,
            billAccountIds = billAccountIds
        )

        assertEquals(15, result.daysLeft)
        assertEquals(1200000L, result.leftThisMonth)
        assertEquals(80000L, result.dailyAllowance)
        assertEquals(0L, result.remainderCentavos)
    }

    @Test
    fun testZeroBudget() {
        val today = LocalDate.of(2026, 10, 10)
        val monthlyBudget = 0L
        val txs = listOf(expense(50000L, "Coffee")) // Spent ₱500.00

        val result = BudgetPaceCalculator.calculate(
            monthlyBudget = monthlyBudget,
            transactions = txs,
            today = today,
            billAccountIds = billAccountIds
        )

        assertEquals(-50000L, result.leftThisMonth)
        assertTrue(result.isOverBudget)
        assertEquals(50000L, result.overBy)
        assertEquals(0L, result.dailyAllowance)
        assertEquals(0L, result.remainderCentavos)
    }

    @Test
    fun testNullBudget_noBudgetSet() {
        val today = LocalDate.of(2026, 10, 10)
        val txs = listOf(expense(50000L, "Coffee"))

        val result = BudgetPaceCalculator.calculate(
            monthlyBudget = null,
            transactions = txs,
            today = today,
            billAccountIds = billAccountIds
        )

        assertNull(result.totalBudget)
        assertNull(result.leftThisMonth)
        assertFalse(result.isOverBudget)
        assertEquals(0L, result.overBy)
        assertEquals(0L, result.dailyAllowance)
        assertEquals(0L, result.remainderCentavos)
        assertEquals(50000L, result.totalSpent)
    }

    @Test
    fun testSpentExactlyEqualsBudget() {
        val today = LocalDate.of(2026, 10, 15)
        val monthlyBudget = 500000L
        val txs = listOf(expense(500000L, "Shopping"))

        val result = BudgetPaceCalculator.calculate(
            monthlyBudget = monthlyBudget,
            transactions = txs,
            today = today,
            billAccountIds = billAccountIds
        )

        assertEquals(0L, result.leftThisMonth)
        assertFalse(result.isOverBudget)
        assertEquals(0L, result.overBy)
        assertEquals(0L, result.dailyAllowance)
        assertEquals(0L, result.remainderCentavos)
    }

    @Test
    fun testOverspending_distinctStateNotClamped() {
        val today = LocalDate.of(2026, 10, 15) // daysLeft = 17
        val monthlyBudget = 200000L // ₱2,000.00
        val txs = listOf(expense(350000L, "Dining")) // Spent ₱3,500.00

        val result = BudgetPaceCalculator.calculate(
            monthlyBudget = monthlyBudget,
            transactions = txs,
            today = today,
            billAccountIds = billAccountIds
        )

        assertEquals(-150000L, result.leftThisMonth)
        assertTrue(result.isOverBudget)
        assertEquals(150000L, result.overBy) // Distinct overBy
        assertEquals(0L, result.dailyAllowance) // max(0, left) prevents negative daily allowance
        assertEquals(0L, result.remainderCentavos)
    }

    @Test
    fun testExclusionOfNonExpenseTransactions() {
        val today = LocalDate.of(2026, 10, 10)
        val monthlyBudget = 1000000L

        val txs = listOf(
            expense(200000L, "Food"), // Included
            installment(150000L, "Gadgets"), // Included
            billTransfer(100000L, toAccountId = 100L, category = "Bill Payment"), // Included (to bill account)
            normalTransfer(500000L, toAccountId = 2L), // Excluded (asset-to-asset transfer)
            income(1000000L), // Excluded
            expense(90000L, "Adjustment", isAdjustment = true) // Excluded (reconciliation adjustment)
        )

        val result = BudgetPaceCalculator.calculate(
            monthlyBudget = monthlyBudget,
            transactions = txs,
            today = today,
            billAccountIds = billAccountIds
        )

        // totalSpent should be 200,000 + 150,000 + 100,000 = 450,000
        assertEquals(450000L, result.totalSpent)
        assertEquals(550000L, result.leftThisMonth)
    }

    /**
     * Requirement 4: Add a calculator test proving that the sum of per-category spent amounts
     * plus uncategorized spent equals total spent (no double counting, no leakage).
     */
    @Test
    fun testConservationOfSpending_sumOfCategorySpentPlusUncategorizedEqualsTotalSpent() {
        val today = LocalDate.of(2026, 10, 10)
        val txs = listOf(
            expense(120050L, "Food & Dining"),
            expense(85000L, "Transportation"),
            installment(340000L, "Gadgets & Tech"),
            billTransfer(150000L, toAccountId = 100L, category = "Bill Payment"),
            expense(45000L, ""), // Empty category -> Uncategorized
            expense(25000L, "   "), // Whitespace category -> Uncategorized
            expense(70000L, "Uncategorized"), // Explicit Uncategorized
            expense(99999L, "Custom Hobbies")
        )

        val categoryBudgets = listOf(
            BudgetEntity(id = 1, category = "Food & Dining", amount = 200000L),
            BudgetEntity(id = 2, category = "Transportation", amount = 100000L),
            BudgetEntity(id = 3, category = "Gadgets & Tech", amount = 50000L),
            BudgetEntity(id = 4, category = "Bills & Utilities", amount = 200000L),
            BudgetEntity(id = 5, category = "Custom Hobbies", amount = 150000L),
            BudgetEntity(id = 6, category = "Entertainment", amount = 50000L) // 0 spent
        )

        val result = BudgetPaceCalculator.calculate(
            monthlyBudget = 2000000L,
            transactions = txs,
            today = today,
            billAccountIds = billAccountIds,
            categoryBudgets = categoryBudgets
        )

        // Calculate sum across all distinct categories in result
        val categorySpentSum = result.categoryPacings.sumOf { it.spentAmount }
        val uncategorizedSpent = result.uncategorizedSpent

        // Total expected = 120050 + 85000 + 340000 + 150000 + 45000 + 25000 + 70000 + 99999 = 935049L
        assertEquals(935049L, result.totalSpent)
        assertEquals(140000L, uncategorizedSpent) // 45000 + 25000 + 70000
        assertEquals(795049L, categorySpentSum) // 120050 + 85000 + 340000 + 150000 + 99999 + 0

        // Conservation equality: totalSpent == categorySpentSum + uncategorizedSpent
        assertEquals(result.totalSpent, categorySpentSum + uncategorizedSpent)
    }

    /**
     * Requirement 3: Budgets for deleted or renamed custom categories must not crash or be
     * silently dropped. Keep the row, resolve display names defensively, and add a test.
     */
    @Test
    fun testDeletedOrRenamedCustomCategory_resolvedDefensivelyAndRowKept() {
        val today = LocalDate.of(2026, 10, 10)
        val categoryBudgets = listOf(
            BudgetEntity(id = 1, category = "Old Deleted Category", amount = 500000L),
            BudgetEntity(id = 2, category = "Active Custom", amount = 300000L)
        )
        val activeCustomCategories = listOf(
            CustomCategoryEntity(id = 10, name = "Active Custom", transactionType = TransactionType.EXPENSE, iconName = "star")
            // "Old Deleted Category" is NOT present in customCategories (was deleted/renamed)
        )

        val txs = listOf(
            expense(150000L, "Old Deleted Category")
        )

        val result = BudgetPaceCalculator.calculate(
            monthlyBudget = 1000000L,
            transactions = txs,
            today = today,
            billAccountIds = billAccountIds,
            categoryBudgets = categoryBudgets,
            customCategories = activeCustomCategories
        )

        // Both rows are kept
        assertEquals(2, result.categoryPacings.size)

        val deletedPacing = result.categoryPacings.first { it.categoryKey == "Old Deleted Category" }
        // Displays defensively as "Old Deleted Category" without crashing
        assertEquals("Old Deleted Category", deletedPacing.displayName)
        assertEquals(500000L, deletedPacing.budgetAmount)
        assertEquals(150000L, deletedPacing.spentAmount)
        assertEquals(350000L, deletedPacing.remainingCentavos)
        assertEquals(CategoryBudgetStatus.OK, deletedPacing.status)

        val activePacing = result.categoryPacings.first { it.categoryKey == "Active Custom" }
        assertEquals("Active Custom", activePacing.displayName)
    }

    @Test
    fun testCategoryPacingStatuses_overNearLimitAndOk() {
        val today = LocalDate.of(2026, 10, 1)
        val categoryBudgets = listOf(
            BudgetEntity(id = 1, category = "CatOver", amount = 100000L), // Spent 120,000 -> OVER
            BudgetEntity(id = 2, category = "CatNear", amount = 100000L), // Spent 80,000 (80%) -> NEAR_LIMIT
            BudgetEntity(id = 3, category = "CatOk", amount = 100000L)    // Spent 50,000 (50%) -> OK
        )

        val txs = listOf(
            expense(120000L, "CatOver"),
            expense(80000L, "CatNear"),
            expense(50000L, "CatOk")
        )

        val result = BudgetPaceCalculator.calculate(
            monthlyBudget = 500000L,
            transactions = txs,
            today = today,
            billAccountIds = billAccountIds,
            categoryBudgets = categoryBudgets
        )

        val over = result.categoryPacings.first { it.categoryKey == "CatOver" }
        assertEquals(CategoryBudgetStatus.OVER, over.status)
        assertTrue(over.isOverBudget)
        assertEquals(20000L, over.overBy)

        val near = result.categoryPacings.first { it.categoryKey == "CatNear" }
        assertEquals(CategoryBudgetStatus.NEAR_LIMIT, near.status)
        assertFalse(near.isOverBudget)
        assertEquals(0L, near.overBy)

        val ok = result.categoryPacings.first { it.categoryKey == "CatOk" }
        assertEquals(CategoryBudgetStatus.OK, ok.status)
        assertFalse(ok.isOverBudget)
    }

    @Test
    fun testBillPaymentSentinelCategoriesMapToCanonicalBillsAndUtilities() {
        // Test sentinel categories: "Bill Payment", "Transfer", and blank/whitespace
        val sentinels = listOf("Bill Payment", "Transfer", "   ", "")
        for (sentinel in sentinels) {
            val tx = createTx(
                type = TransactionType.TRANSFER,
                amount = 250000L,
                toAccountId = 10L,
                category = sentinel
            )
            val effective = BudgetPaceCalculator.effectiveSpendingCategory(tx, AccountType.BILL)
            assertEquals("Bills & Utilities", effective)

            val effectiveWithIds = BudgetPaceCalculator.effectiveSpendingCategory(tx, setOf(10L))
            assertEquals("Bills & Utilities", effectiveWithIds)
        }
    }

    @Test
    fun testBillPaymentCustomCategoryIsPreservedUnchanged() {
        val tx = createTx(
            type = TransactionType.TRANSFER,
            amount = 350000L,
            toAccountId = 10L,
            category = "Utilities"
        )
        val effective = BudgetPaceCalculator.effectiveSpendingCategory(tx, AccountType.BILL)
        assertEquals("Utilities", effective)

        val effectiveWithIds = BudgetPaceCalculator.effectiveSpendingCategory(tx, setOf(10L))
        assertEquals("Utilities", effectiveWithIds)
    }

    @Test
    fun testCreditCardAndLoanPaymentsAreNotCountedAsSpending() {
        val ccPayment = createTx(
            type = TransactionType.TRANSFER,
            amount = 1500000L,
            toAccountId = 20L,
            category = "Credit Card Payment"
        )
        val loanPayment = createTx(
            type = TransactionType.TRANSFER,
            amount = 800000L,
            toAccountId = 30L,
            category = "Loan Payment"
        )

        assertFalse("Credit card payment transfer must NOT be spending",
            BudgetPaceCalculator.isExpenseTransaction(ccPayment, destinationAccountType = AccountType.CREDIT))
        assertFalse("Loan payment transfer must NOT be spending",
            BudgetPaceCalculator.isExpenseTransaction(loanPayment, destinationAccountType = AccountType.LOAN))

        // When evaluating against billAccountIds where 20L and 30L are not BILL accounts
        assertFalse("Credit card payment must NOT be spending in set evaluation",
            BudgetPaceCalculator.isExpenseTransaction(ccPayment, billAccountIds = setOf(10L)))
        assertFalse("Loan payment must NOT be spending in set evaluation",
            BudgetPaceCalculator.isExpenseTransaction(loanPayment, billAccountIds = setOf(10L)))
    }

    @Test
    fun testConservationOfSpendingWithMixedTransactions() {
        val today = LocalDate.of(2026, 10, 15)
        val billId = 10L
        val txs = listOf(
            expense(100000L, "Food & Dining"),
            expense(50000L, "   "), // blank category -> Uncategorized
            createTx(TransactionType.TRANSFER, 200000L, toAccountId = billId, category = "Bill Payment"), // sentinel -> Bills & Utilities
            createTx(TransactionType.TRANSFER, 150000L, toAccountId = billId, category = "Internet"), // custom bill -> Internet
            createTx(TransactionType.INSTALLMENT, 300000L, category = "Shopping"), // installment -> Shopping
            // Excluded transactions
            createTx(TransactionType.TRANSFER, 500000L, toAccountId = 99L, category = "Transfer"), // non-bill transfer
            createTx(TransactionType.INCOME, 5000000L, category = "Salary"),
            createTx(TransactionType.EXPENSE, 100000L, isAdjustment = true, category = "Adjustment") // adjustment
        )

        val categoryBudgets = listOf(
            BudgetEntity(id = 1, category = "Food & Dining", amount = 150000L),
            BudgetEntity(id = 2, category = "Bills & Utilities", amount = 300000L),
            BudgetEntity(id = 3, category = "Internet", amount = 200000L),
            BudgetEntity(id = 4, category = "Shopping", amount = 400000L)
        )

        val result = BudgetPaceCalculator.calculate(
            monthlyBudget = 1000000L,
            transactions = txs,
            today = today,
            billAccountIds = setOf(billId),
            categoryBudgets = categoryBudgets
        )

        // Expected spending = 100,000 + 50,000 + 200,000 + 150,000 + 300,000 = 800,000
        assertEquals(800000L, result.totalSpent)

        val perCatSpentSum = result.categoryPacings.sumOf { it.spentAmount }
        val uncategorized = result.uncategorizedSpent
        assertEquals(50000L, uncategorized)
        assertEquals("Sum of configured category pacings + uncategorized spent must equal total spent",
            result.totalSpent, perCatSpentSum + uncategorized)
    }

    @Test
    fun testUnifiedClassificationAcrossCalculatorAndReportsForLegacyBillPayment() {
        val billId = 10L
        val legacyBillTx = createTx(
            type = TransactionType.TRANSFER,
            amount = 280000L,
            toAccountId = billId,
            category = "Bill Payment",
            timestamp = 1000L
        )
        val txs = listOf(legacyBillTx)
        val billAccountIds = setOf(billId)

        // 1. BudgetPaceCalculator
        val paceResult = BudgetPaceCalculator.calculate(
            monthlyBudget = 500000L,
            transactions = txs,
            today = LocalDate.of(2026, 10, 15),
            billAccountIds = billAccountIds,
            categoryBudgets = listOf(BudgetEntity(id = 1, category = "Bills & Utilities", amount = 300000L))
        )
        val paceCategory = paceResult.categoryPacings.first { it.categoryKey == "Bills & Utilities" }
        assertEquals(280000L, paceCategory.spentAmount)
        assertEquals(280000L, paceResult.totalSpent)
        assertEquals(0L, paceResult.uncategorizedSpent)

        // 2. ReportsAnalyticsCalculator
        val (reportsTotal, reportsBreakdown) = com.example.budgettracker.ui.reports.ReportsAnalyticsCalculator.calculateSpendingByCategory(
            transactions = txs,
            startTime = 0L,
            endTime = 5000L,
            billAccountIds = billAccountIds
        )
        assertEquals(280000L, reportsTotal)
        assertEquals(1, reportsBreakdown.size)
        assertEquals("Bills & Utilities", reportsBreakdown[0].category)
        assertEquals(280000L, reportsBreakdown[0].amount)
    }

    @Test
    fun testStatusThresholds_exactly100PercentAndBoundaries() {
        val today = LocalDate.of(2026, 10, 15)
        val budgetAmount = 10_000_00L // ₱10,000

        // Case 1: 79% spent (₱7,900) -> OK (< 80%)
        val tx79 = listOf(expense(7_900_00L, "Food & Dining"))
        val catBudget = listOf(BudgetEntity(id = 1, category = "Food & Dining", amount = budgetAmount))
        val result79 = BudgetPaceCalculator.calculate(
            monthlyBudget = budgetAmount,
            transactions = tx79,
            today = today,
            categoryBudgets = catBudget
        )
        assertEquals(CategoryBudgetStatus.OK, result79.status)
        assertEquals(CategoryBudgetStatus.OK, result79.categoryPacings[0].status)

        // Case 2: 80% spent (₱8,000) -> NEAR_LIMIT (>= 80%)
        val tx80 = listOf(expense(8_000_00L, "Food & Dining"))
        val result80 = BudgetPaceCalculator.calculate(
            monthlyBudget = budgetAmount,
            transactions = tx80,
            today = today,
            categoryBudgets = catBudget
        )
        assertEquals(CategoryBudgetStatus.NEAR_LIMIT, result80.status)
        assertEquals(CategoryBudgetStatus.NEAR_LIMIT, result80.categoryPacings[0].status)

        // Case 3: Exactly 100% spent (₱10,000) -> NEAR_LIMIT (spent == budget, 0 left, not exceeded)
        val tx100 = listOf(expense(10_000_00L, "Food & Dining"))
        val result100 = BudgetPaceCalculator.calculate(
            monthlyBudget = budgetAmount,
            transactions = tx100,
            today = today,
            categoryBudgets = catBudget
        )
        assertFalse("At exactly 100%, isOverBudget must be false", result100.isOverBudget)
        assertEquals(0L, result100.leftThisMonth)
        assertEquals(0L, result100.overBy)
        assertEquals("At exactly 100%, status must be NEAR_LIMIT", CategoryBudgetStatus.NEAR_LIMIT, result100.status)
        assertEquals("At exactly 100%, category status must be NEAR_LIMIT", CategoryBudgetStatus.NEAR_LIMIT, result100.categoryPacings[0].status)

        // Case 4: 100.01% spent (₱10,000.01) -> OVER (> 100%)
        val tx100Plus = listOf(expense(10_000_01L, "Food & Dining"))
        val result100Plus = BudgetPaceCalculator.calculate(
            monthlyBudget = budgetAmount,
            transactions = tx100Plus,
            today = today,
            categoryBudgets = catBudget
        )
        assertTrue("At >100%, isOverBudget must be true", result100Plus.isOverBudget)
        assertEquals(1L, result100Plus.overBy)
        assertEquals(-1L, result100Plus.leftThisMonth)
        assertEquals(CategoryBudgetStatus.OVER, result100Plus.status)
        assertEquals(CategoryBudgetStatus.OVER, result100Plus.categoryPacings[0].status)
    }

    @Test
    fun testParseBudgetPesoInput_validPlainAndFormatted() {
        val cases = listOf(
            "1500" to 1500_00L,
            "1,500" to 1500_00L,
            "1,500.50" to 1500_50L,
            "0.50" to 50L,
            "100.5" to 100_50L,
            "100." to 100_00L,
            "50,000.25" to 50000_25L,
            "100,000,000" to 100_000_000_00L
        )

        for ((input, expectedCentavos) in cases) {
            val result = com.example.budgettracker.domain.PesoInput.parseBudgetPesoInput(input)
            assertTrue("Expected Valid for '$input'", result is com.example.budgettracker.domain.BudgetInputResult.Valid)
            assertEquals(expectedCentavos, (result as com.example.budgettracker.domain.BudgetInputResult.Valid).centavos)
        }
    }

    @Test
    fun testParseBudgetPesoInput_invalidFormatsAndErrors() {
        val emptyResult = com.example.budgettracker.domain.PesoInput.parseBudgetPesoInput("")
        assertTrue(emptyResult is com.example.budgettracker.domain.BudgetInputResult.Error)
        assertEquals("Amount cannot be empty", (emptyResult as com.example.budgettracker.domain.BudgetInputResult.Error).message)

        val whitespaceResult = com.example.budgettracker.domain.PesoInput.parseBudgetPesoInput("   ")
        assertTrue(whitespaceResult is com.example.budgettracker.domain.BudgetInputResult.Error)

        val negativeResult = com.example.budgettracker.domain.PesoInput.parseBudgetPesoInput("-100")
        assertTrue(negativeResult is com.example.budgettracker.domain.BudgetInputResult.Error)
        assertEquals("Amount cannot be negative", (negativeResult as com.example.budgettracker.domain.BudgetInputResult.Error).message)

        val invalidCharsResult = com.example.budgettracker.domain.PesoInput.parseBudgetPesoInput("₱1500")
        assertTrue(invalidCharsResult is com.example.budgettracker.domain.BudgetInputResult.Error)
        assertEquals("Amount contains invalid characters", (invalidCharsResult as com.example.budgettracker.domain.BudgetInputResult.Error).message)

        val multiDotResult = com.example.budgettracker.domain.PesoInput.parseBudgetPesoInput("100.50.25")
        assertTrue(multiDotResult is com.example.budgettracker.domain.BudgetInputResult.Error)
        assertEquals("Amount cannot have multiple decimal points", (multiDotResult as com.example.budgettracker.domain.BudgetInputResult.Error).message)

        val commaAtStart = com.example.budgettracker.domain.PesoInput.parseBudgetPesoInput(",100")
        assertTrue(commaAtStart is com.example.budgettracker.domain.BudgetInputResult.Error)

        val commaAtEnd = com.example.budgettracker.domain.PesoInput.parseBudgetPesoInput("100,")
        assertTrue(commaAtEnd is com.example.budgettracker.domain.BudgetInputResult.Error)

        val doubleComma = com.example.budgettracker.domain.PesoInput.parseBudgetPesoInput("100,,000")
        assertTrue(doubleComma is com.example.budgettracker.domain.BudgetInputResult.Error)

        val threeDecimals = com.example.budgettracker.domain.PesoInput.parseBudgetPesoInput("100.123")
        assertTrue(threeDecimals is com.example.budgettracker.domain.BudgetInputResult.Error)
        assertEquals("Maximum 2 decimal places allowed", (threeDecimals as com.example.budgettracker.domain.BudgetInputResult.Error).message)

        val zeroResult = com.example.budgettracker.domain.PesoInput.parseBudgetPesoInput("0")
        assertTrue(zeroResult is com.example.budgettracker.domain.BudgetInputResult.Error)
        assertEquals("Budget amount must be greater than ₱0", (zeroResult as com.example.budgettracker.domain.BudgetInputResult.Error).message)

        val zeroCentsResult = com.example.budgettracker.domain.PesoInput.parseBudgetPesoInput("0.00")
        assertTrue(zeroCentsResult is com.example.budgettracker.domain.BudgetInputResult.Error)
        assertEquals("Budget amount must be greater than ₱0", (zeroCentsResult as com.example.budgettracker.domain.BudgetInputResult.Error).message)

        val exceedMaxResult = com.example.budgettracker.domain.PesoInput.parseBudgetPesoInput("100,000,000.01")
        assertTrue(exceedMaxResult is com.example.budgettracker.domain.BudgetInputResult.Error)
        assertEquals("Amount exceeds maximum allowed budget (₱100,000,000)", (exceedMaxResult as com.example.budgettracker.domain.BudgetInputResult.Error).message)
    }

    @Test
    fun testFormatPesoInput_preservesTypingFlow() {
        assertEquals("1,500", com.example.budgettracker.domain.PesoInput.formatPesoInput("1500"))
        assertEquals("1,500.", com.example.budgettracker.domain.PesoInput.formatPesoInput("1500."))
        assertEquals("1,500.5", com.example.budgettracker.domain.PesoInput.formatPesoInput("1500.5"))
        assertEquals("1,500.50", com.example.budgettracker.domain.PesoInput.formatPesoInput("1500.50"))
        assertEquals("1,500.50", com.example.budgettracker.domain.PesoInput.formatPesoInput("1500.509"))
        assertEquals("0", com.example.budgettracker.domain.PesoInput.formatPesoInput("0"))
    }
}
