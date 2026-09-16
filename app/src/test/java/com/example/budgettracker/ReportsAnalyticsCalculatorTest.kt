package com.example.budgettracker

import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.AccountWithBillDetails
import com.example.budgettracker.data.local.entity.AccountWithLoanDetails
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.ui.reports.ReportPeriod
import com.example.budgettracker.ui.reports.ReportsAnalyticsCalculator
import com.example.budgettracker.util.DueDateStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ReportsAnalyticsCalculatorTest {

    private val testZone = ZoneId.of("UTC")
    private val baseDate = LocalDate.of(2026, 9, 16)

    private fun localDateToMillis(date: LocalDate): Long {
        return date.atStartOfDay(testZone).toInstant().toEpochMilli()
    }

    @Test
    fun testSpendingByCategory_aggregatesAndGroupsOthers() {
        val (startTime, endTime) = ReportPeriod.THIS_MONTH.getTimeRange(baseDate, testZone)

        val txList = listOf(
            TransactionEntity(id = 1, type = TransactionType.EXPENSE, amount = 5000L, category = "Food", title = "Lunch", timestamp = startTime + 1000, accountId = 1),
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 4000L, category = "Rent", title = "Apartment", timestamp = startTime + 2000, accountId = 1),
            TransactionEntity(id = 3, type = TransactionType.EXPENSE, amount = 3000L, category = "Utilities", title = "Electric", timestamp = startTime + 3000, accountId = 1),
            TransactionEntity(id = 4, type = TransactionType.EXPENSE, amount = 2000L, category = "Transport", title = "Grab", timestamp = startTime + 4000, accountId = 1),
            TransactionEntity(id = 5, type = TransactionType.EXPENSE, amount = 1500L, category = "Entertainment", title = "Cinema", timestamp = startTime + 5000, accountId = 1),
            TransactionEntity(id = 6, type = TransactionType.EXPENSE, amount = 1000L, category = "Health", title = "Meds", timestamp = startTime + 6000, accountId = 1),
            TransactionEntity(id = 7, type = TransactionType.EXPENSE, amount = 500L, category = "Books", title = "Novel", timestamp = startTime + 7000, accountId = 1),
            // Out of range transaction - should be ignored
            TransactionEntity(id = 8, type = TransactionType.EXPENSE, amount = 9999L, category = "Food", title = "Old", timestamp = startTime - 5000, accountId = 1),
            // Income transaction - should be ignored
            TransactionEntity(id = 9, type = TransactionType.INCOME, amount = 50000L, category = "Salary", title = "Pay", timestamp = startTime + 8000, accountId = 1)
        )

        val (total, breakdown) = ReportsAnalyticsCalculator.calculateSpendingByCategory(txList, startTime, endTime)

        // Total = 5000 + 4000 + 3000 + 2000 + 1500 + 1000 + 500 = 17000L
        assertEquals(17000L, total)

        // Since there are 7 categories (> 6), top 5 remain separate, 6th and 7th grouped into "Others"
        assertEquals(6, breakdown.size)
        assertEquals("Food", breakdown[0].category)
        assertEquals(5000L, breakdown[0].amount)

        assertEquals("Rent", breakdown[1].category)
        assertEquals("Utilities", breakdown[2].category)
        assertEquals("Transport", breakdown[3].category)
        assertEquals("Entertainment", breakdown[4].category)

        val others = breakdown[5]
        assertEquals("Others", others.category)
        assertEquals(1500L, others.amount) // 1000 + 500
        assertEquals(2, others.transactionCount)

        // Sum of percentages should be approximately 100%
        val totalPct = breakdown.sumOf { it.percentage.toDouble() }
        assertEquals(100.0, totalPct, 0.5)
    }

    @Test
    fun testSpendingByCategory_emptyOrZero() {
        val (startTime, endTime) = ReportPeriod.THIS_MONTH.getTimeRange(baseDate, testZone)
        val (total, breakdown) = ReportsAnalyticsCalculator.calculateSpendingByCategory(emptyList(), startTime, endTime)
        assertEquals(0L, total)
        assertTrue(breakdown.isEmpty())
    }

    @Test
    fun testIncomeVsExpense_savedAndOverspent() {
        val (startTime, endTime) = ReportPeriod.THIS_MONTH.getTimeRange(baseDate, testZone)

        // Case 1: Saved
        val savedTx = listOf(
            TransactionEntity(id = 1, type = TransactionType.INCOME, amount = 25000L, category = "Salary", title = "Paycheck", timestamp = startTime + 1000, accountId = 1),
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 10000L, category = "Groceries", title = "Food", timestamp = startTime + 2000, accountId = 1)
        )
        val comparisonSaved = ReportsAnalyticsCalculator.calculateIncomeVsExpense(savedTx, ReportPeriod.THIS_MONTH, baseDate, testZone)
        assertEquals(25000L, comparisonSaved.totalIncome)
        assertEquals(10000L, comparisonSaved.totalExpense)
        assertEquals(15000L, comparisonSaved.netAmount)
        assertTrue(comparisonSaved.isSaved)

        // Case 2: Overspent
        val overspentTx = listOf(
            TransactionEntity(id = 1, type = TransactionType.INCOME, amount = 5000L, category = "Salary", title = "Paycheck", timestamp = startTime + 1000, accountId = 1),
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 12000L, category = "Shopping", title = "Gadget", timestamp = startTime + 2000, accountId = 1)
        )
        val comparisonOverspent = ReportsAnalyticsCalculator.calculateIncomeVsExpense(overspentTx, ReportPeriod.THIS_MONTH, baseDate, testZone)
        assertEquals(5000L, comparisonOverspent.totalIncome)
        assertEquals(12000L, comparisonOverspent.totalExpense)
        assertEquals(-7000L, comparisonOverspent.netAmount)
        assertFalse(comparisonOverspent.isSaved)

        // Case 3: Last 3 Months has 3 monthly bars
        val comparison3Months = ReportsAnalyticsCalculator.calculateIncomeVsExpense(overspentTx, ReportPeriod.LAST_3_MONTHS, baseDate, testZone)
        assertEquals(3, comparison3Months.monthlyBars.size)
    }

    @Test
    fun testNetWorthHistory_derivesHistoricalBalances() {
        val accounts = listOf(
            AccountWithBalance(
                id = 1L,
                name = "Bank",
                type = AccountType.BANK,
                presetId = null,
                initialBalance = 10000L,
                isActive = true,
                includeInNetWorth = true,
                displayOrder = 0,
                currentBalance = 10000L
            ),
            AccountWithBalance(
                id = 2L,
                name = "Cash",
                type = AccountType.CASH,
                presetId = null,
                initialBalance = 5000L,
                isActive = true,
                includeInNetWorth = true,
                displayOrder = 1,
                currentBalance = 5000L
            )
        )

        // July 2026: Bank receives +2000 income
        val julyDate = LocalDate.of(2026, 7, 15)
        // August 2026: Bank transfers 3000 to Cash, Cash spends 1000
        val augDate = LocalDate.of(2026, 8, 10)
        // September 2026: Bank pays 4000 expense
        val sepDate = LocalDate.of(2026, 9, 5)

        val txList = listOf(
            TransactionEntity(id = 1, type = TransactionType.INCOME, amount = 2000L, category = "Bonus", title = "July bonus", timestamp = localDateToMillis(julyDate), accountId = 1),
            TransactionEntity(id = 2, type = TransactionType.TRANSFER, amount = 3000L, category = "Transfer", title = "To Cash", timestamp = localDateToMillis(augDate), accountId = 1, toAccountId = 2),
            TransactionEntity(id = 3, type = TransactionType.EXPENSE, amount = 1000L, category = "Food", title = "Lunch", timestamp = localDateToMillis(augDate) + 1000, accountId = 2),
            TransactionEntity(id = 4, type = TransactionType.EXPENSE, amount = 4000L, category = "Bills", title = "Internet", timestamp = localDateToMillis(sepDate), accountId = 1)
        )

        val points = ReportsAnalyticsCalculator.calculateNetWorthHistory(accounts, txList, baseDate, testZone)

        // Initial net worth = 10000 + 5000 = 15000
        // At end of July: 15000 + 2000 = 17000
        // At end of August: 17000 - 1000 = 16000 (transfer is neutral to total NW, expense reduced NW by 1000)
        // At end of September: 16000 - 4000 = 12000
        val sepPoint = points.find { it.monthLabel == "Sep" }
        assertNotNull(sepPoint)
        assertEquals(12000L, sepPoint!!.netWorth)

        val augPoint = points.find { it.monthLabel == "Aug" }
        assertNotNull(augPoint)
        assertEquals(16000L, augPoint!!.netWorth)

        val julPoint = points.find { it.monthLabel == "Jul" }
        assertNotNull(julPoint)
        assertEquals(17000L, julPoint!!.netWorth)
    }

    @Test
    fun testUpcomingObligations_filtersNext30Days() {
        val today = LocalDate.of(2026, 9, 16)

        val loanAccount = AccountEntity(id = 10, name = "Maya Credit", type = AccountType.LOAN)
        val loanDetails = LoanAccountDetailsEntity(accountId = 10, cycleDay1 = 20, minimumAmountDue = 2500L, totalRemainingBalance = 10000L)
        val loans = listOf(AccountWithLoanDetails(loanAccount, loanDetails))

        val billAccount = AccountEntity(id = 20, name = "Meralco", type = AccountType.BILL)
        val billDetails = BillAccountDetailsEntity(accountId = 20, dueDay = 25, amountDue = 3200L)
        val bills = listOf(AccountWithBillDetails(billAccount, billDetails))

        val installments = listOf(
            InstallmentPlanEntity(
                id = 30,
                accountId = 10,
                title = "Phone BNPL",
                category = "Electronics",
                totalPurchaseAmount = 12000L,
                totalInstallments = 6,
                installmentsPaid = 2,
                remainingBalance = 8000L,
                monthlyPaymentAmount = 2000L,
                purchaseDate = localDateToMillis(LocalDate.of(2026, 7, 18))
            )
        )

        val obligations = ReportsAnalyticsCalculator.calculateUpcomingObligations(loans, bills, installments, today)

        // Today is Sep 16.
        // Maya Credit due date: Sep 20 (4 days away) -> DUE_SOON
        // Meralco due date: Sep 25 (9 days away) -> UPCOMING
        // Installment due date (cycleDay1 of Maya = 20): Sep 20 (4 days away) -> DUE_SOON
        assertEquals(3, obligations.size)

        val first = obligations[0]
        assertEquals(LocalDate.of(2026, 9, 20), first.dueDate)
        assertEquals(DueDateStatus.DUE_SOON, first.status)

        val bill = obligations.find { it.title == "Meralco" }
        assertNotNull(bill)
        assertEquals(3200L, bill!!.amount)
        assertEquals(DueDateStatus.UPCOMING, bill.status)
    }

    @Test
    fun testTopMerchants_ranksAndCounts() {
        val (startTime, endTime) = ReportPeriod.THIS_MONTH.getTimeRange(baseDate, testZone)

        val txList = listOf(
            TransactionEntity(id = 1, type = TransactionType.EXPENSE, amount = 1000L, category = "Food", title = "Jollibee", timestamp = startTime + 100, accountId = 1),
            TransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 1500L, category = "Food", title = "Jollibee", timestamp = startTime + 200, accountId = 1),
            TransactionEntity(id = 3, type = TransactionType.EXPENSE, amount = 4000L, category = "Groceries", title = "SM Supermarket", timestamp = startTime + 300, accountId = 1),
            TransactionEntity(id = 4, type = TransactionType.EXPENSE, amount = 500L, category = "Coffee", title = "Starbucks", timestamp = startTime + 400, accountId = 1),
            TransactionEntity(id = 5, type = TransactionType.EXPENSE, amount = 700L, category = "Coffee", title = "Starbucks", timestamp = startTime + 500, accountId = 1),
            TransactionEntity(id = 6, type = TransactionType.EXPENSE, amount = 200L, category = "Snack", title = "7-Eleven", timestamp = startTime + 600, accountId = 1),
            TransactionEntity(id = 7, type = TransactionType.EXPENSE, amount = 150L, category = "Transport", title = "MRT", timestamp = startTime + 700, accountId = 1),
            TransactionEntity(id = 8, type = TransactionType.EXPENSE, amount = 50L, category = "Misc", title = "Sari-Sari Store", timestamp = startTime + 800, accountId = 1)
        )

        val topMerchants = ReportsAnalyticsCalculator.calculateTopMerchants(txList, startTime, endTime)

        assertEquals(5, topMerchants.size)
        // 1. SM Supermarket (4000L, count 1)
        assertEquals(1, topMerchants[0].rank)
        assertEquals("SM Supermarket", topMerchants[0].name)
        assertEquals(4000L, topMerchants[0].totalAmount)
        assertEquals(1, topMerchants[0].count)

        // 2. Jollibee (2500L, count 2)
        assertEquals(2, topMerchants[1].rank)
        assertEquals("Jollibee", topMerchants[1].name)
        assertEquals(2500L, topMerchants[1].totalAmount)
        assertEquals(2, topMerchants[1].count)

        // 3. Starbucks (1200L, count 2)
        assertEquals(3, topMerchants[2].rank)
        assertEquals("Starbucks", topMerchants[2].name)
        assertEquals(1200L, topMerchants[2].totalAmount)
        assertEquals(2, topMerchants[2].count)
    }

    @Test
    fun testDailyBurnRate_thisMonthAndProjections() {
        val (startTime, endTime) = ReportPeriod.THIS_MONTH.getTimeRange(baseDate, testZone)

        // baseDate is Sep 16, 2026. September has 30 days. Days elapsed = 16.
        // Total expense = 16000L.
        // Average daily spend = 16000 / 16 = 1000L.
        // Projected month total = 1000 * 30 = 30000L.
        val txList = listOf(
            TransactionEntity(id = 1, type = TransactionType.EXPENSE, amount = 16000L, category = "Various", title = "Various", timestamp = startTime + 1000, accountId = 1)
        )

        val burnRate = ReportsAnalyticsCalculator.calculateDailyBurnRate(txList, ReportPeriod.THIS_MONTH, baseDate, testZone)
        assertEquals(1000L, burnRate.averageDailySpend)
        assertEquals(16, burnRate.daysElapsed)
        assertEquals(30, burnRate.totalDaysInPeriod)
        assertEquals(30000L, burnRate.projectedMonthTotal)

        // LAST_MONTH has no projected total
        val burnRateLastMonth = ReportsAnalyticsCalculator.calculateDailyBurnRate(txList, ReportPeriod.LAST_MONTH, baseDate, testZone)
        assertNull(burnRateLastMonth.projectedMonthTotal)
    }
}
