package com.example.budgettracker.ui.reports

import androidx.compose.ui.graphics.Color
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.AccountWithBillDetails
import com.example.budgettracker.data.local.entity.AccountWithLoanDetails
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.ui.theme.AmberGlow
import com.example.budgettracker.ui.theme.MutedCoral
import com.example.budgettracker.ui.theme.MutedSage
import com.example.budgettracker.util.LoanDateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object ReportsAnalyticsCalculator {

    val CategoryCoralPalette = listOf(
        Color(0xFFF87171), // Base Muted Coral
        Color(0xFFE05A5A), // Deep Coral
        Color(0xFFFCA5A5), // Soft Coral
        Color(0xFFEF4444), // Red Coral
        Color(0xFFDC2626), // Darker Coral
        Color(0xFFFDA4AF)  // Rose Coral
    )
    val CategoryOthersColor = Color(0xFFA1A1AA) // Zinc 400

    val TrendLineColors = listOf(
        MutedCoral,            // #F87171
        AmberGlow,             // #F4B942
        MutedSage,             // #7FB88F
        Color(0xFF60A5FA)      // Blue 400
    )

    /**
     * Tier 1: Spending by Category.
     * Top 5-6 categories with amounts and percentages; remainder grouped as "Others".
     * Muted Coral tones for expense categories.
     */
    fun calculateSpendingByCategory(
        transactions: List<TransactionEntity>,
        startTime: Long,
        endTime: Long
    ): Pair<Long, List<CategorySpending>> {
        val expenseTx = transactions.filter {
            it.timestamp in startTime..endTime &&
                    (it.type == TransactionType.EXPENSE || it.type == TransactionType.INSTALLMENT)
        }

        val totalExpense = expenseTx.sumOf { it.amount }
        if (totalExpense == 0L || expenseTx.isEmpty()) {
            return Pair(0L, emptyList())
        }

        val grouped = expenseTx
            .groupBy { it.category.trim().ifEmpty { "Uncategorized" } }
            .mapValues { entry ->
                Pair(entry.value.sumOf { it.amount }, entry.value.size)
            }
            .toList()
            .sortedByDescending { it.second.first } // Sort by amount descending

        val result = mutableListOf<CategorySpending>()

        if (grouped.size <= 6) {
            grouped.forEachIndexed { index, (category, stats) ->
                val amount = stats.first
                val count = stats.second
                val percentage = (amount.toFloat() / totalExpense) * 100f
                val color = CategoryCoralPalette.getOrElse(index) { CategoryCoralPalette.last() }
                result.add(CategorySpending(category, amount, percentage, color, count))
            }
        } else {
            // Take top 5, group rest into Others
            val top5 = grouped.take(5)
            val others = grouped.drop(5)

            top5.forEachIndexed { index, (category, stats) ->
                val amount = stats.first
                val count = stats.second
                val percentage = (amount.toFloat() / totalExpense) * 100f
                val color = CategoryCoralPalette.getOrElse(index) { CategoryCoralPalette.last() }
                result.add(CategorySpending(category, amount, percentage, color, count))
            }

            val othersAmount = others.sumOf { it.second.first }
            val othersCount = others.sumOf { it.second.second }
            val othersPercentage = (othersAmount.toFloat() / totalExpense) * 100f
            result.add(CategorySpending("Others", othersAmount, othersPercentage, CategoryOthersColor, othersCount))
        }

        return Pair(totalExpense, result)
    }

    /**
     * Tier 1: Income vs Expense.
     * Grouped totals for selected period and monthly bar breakdown.
     */
    fun calculateIncomeVsExpense(
        transactions: List<TransactionEntity>,
        period: ReportPeriod,
        referenceDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): IncomeExpenseComparison {
        val (startTime, endTime) = period.getTimeRange(referenceDate, zoneId)
        val periodTx = transactions.filter { it.timestamp in startTime..endTime }

        val totalIncome = periodTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = periodTx.filter {
            it.type == TransactionType.EXPENSE || it.type == TransactionType.INSTALLMENT
        }.sumOf { it.amount }

        val netAmount = totalIncome - totalExpense
        val isSaved = netAmount >= 0

        val currentYm = YearMonth.from(referenceDate)
        val monthsCount = when (period) {
            ReportPeriod.THIS_MONTH, ReportPeriod.LAST_MONTH -> 1
            ReportPeriod.LAST_3_MONTHS -> 3
            ReportPeriod.LAST_6_MONTHS -> 6
        }

        val startYm = when (period) {
            ReportPeriod.THIS_MONTH -> currentYm
            ReportPeriod.LAST_MONTH -> currentYm.minusMonths(1)
            ReportPeriod.LAST_3_MONTHS -> currentYm.minusMonths(2)
            ReportPeriod.LAST_6_MONTHS -> currentYm.minusMonths(5)
        }

        val monthlyBars = mutableListOf<MonthlyBarData>()
        for (i in 0 until monthsCount) {
            val ym = startYm.plusMonths(i.toLong())
            val monthStart = ym.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val monthEnd = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()

            val monthTx = transactions.filter { it.timestamp in monthStart..monthEnd }
            val mIncome = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val mExpense = monthTx.filter {
                it.type == TransactionType.EXPENSE || it.type == TransactionType.INSTALLMENT
            }.sumOf { it.amount }

            val label = ym.month.getDisplayName(TextStyle.SHORT, Locale.US)
            monthlyBars.add(MonthlyBarData(label, mIncome, mExpense))
        }

        return IncomeExpenseComparison(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netAmount = netAmount,
            isSaved = isSaved,
            monthlyBars = monthlyBars
        )
    }

    /**
     * Tier 1: Net Worth Over Time.
     * Derived from account initial balance + historical transaction history.
     */
    fun calculateNetWorthHistory(
        accounts: List<AccountWithBalance>,
        transactions: List<TransactionEntity>,
        referenceDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<NetWorthPoint> {
        val netWorthAccounts = accounts.filter { it.isActive && it.includeInNetWorth }
        if (netWorthAccounts.isEmpty()) {
            return emptyList()
        }

        val currentYm = YearMonth.from(referenceDate)
        // Find earliest timestamp to determine how many months of data actually exist
        val earliestTxTime = transactions.minOfOrNull { it.timestamp }
        val earliestYm = if (earliestTxTime != null) {
            YearMonth.from(Instant.ofEpochMilli(earliestTxTime).atZone(zoneId))
        } else {
            currentYm
        }

        val sixMonthsAgoYm = currentYm.minusMonths(5)
        val startYm = if (earliestYm.isAfter(sixMonthsAgoYm)) earliestYm else sixMonthsAgoYm

        val points = mutableListOf<NetWorthPoint>()
        var ym = startYm
        while (!ym.isAfter(currentYm)) {
            val monthEnd = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()

            var totalNetWorthForMonth = 0L
            for (acc in netWorthAccounts) {
                var accBal = acc.initialBalance
                for (tx in transactions) {
                    if (tx.timestamp <= monthEnd) {
                        when (tx.type) {
                            TransactionType.INCOME -> {
                                if (tx.accountId == acc.id) accBal += tx.amount
                            }
                            TransactionType.EXPENSE, TransactionType.INSTALLMENT -> {
                                if (tx.accountId == acc.id) accBal -= tx.amount
                            }
                            TransactionType.TRANSFER -> {
                                if (tx.accountId == acc.id) accBal -= tx.amount
                                if (tx.toAccountId == acc.id) accBal += tx.amount
                            }
                        }
                    }
                }
                totalNetWorthForMonth += accBal
            }

            val label = ym.month.getDisplayName(TextStyle.SHORT, Locale.US)
            points.add(NetWorthPoint(label, totalNetWorthForMonth, monthEnd))
            ym = ym.plusMonths(1)
        }

        return points
    }

    /**
     * Tier 1: Upcoming Obligations.
     * Loans, bills, BNPL installments due in next 30 days.
     */
    fun calculateUpcomingObligations(
        loans: List<AccountWithLoanDetails>,
        bills: List<AccountWithBillDetails>,
        installments: List<InstallmentPlanEntity>,
        today: LocalDate = LocalDate.now()
    ): List<UpcomingObligation> {
        val obligations = mutableListOf<UpcomingObligation>()
        val maxDueLimit = today.plusDays(30)

        // 1. Loans & BNPL accounts
        loans.filter { it.account.isActive && it.loanDetails != null }.forEach { item ->
            val details = item.loanDetails!!
            if (details.cycleDay1 in 1..31) {
                val dueDate = LoanDateUtils.calculateNextDueDate(today, details.cycleDay1, details.cycleDay2)
                if (!dueDate.isAfter(maxDueLimit)) {
                    val amount = if (details.minimumAmountDue > 0) details.minimumAmountDue else details.totalRemainingBalance
                    val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
                    val status = LoanDateUtils.getDueDateStatus(dueDate, today)
                    val typeLabel = item.account.type.toDisplayLabel()
                    obligations.add(
                        UpcomingObligation(
                            id = item.account.id,
                            title = item.account.name,
                            amount = amount,
                            dueDate = dueDate,
                            daysUntilDue = daysUntilDue,
                            status = status,
                            typeLabel = typeLabel
                        )
                    )
                }
            }
        }

        // 2. Bills
        bills.filter { it.account.isActive && it.billDetails != null }.forEach { item ->
            val details = item.billDetails!!
            if (details.dueDay in 1..31) {
                val dueDate = LoanDateUtils.calculateNextDueDate(today, details.dueDay, null)
                if (!dueDate.isAfter(maxDueLimit)) {
                    val amount = details.amountDue ?: 0L
                    val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
                    val status = LoanDateUtils.getDueDateStatus(dueDate, today)
                    obligations.add(
                        UpcomingObligation(
                            id = item.account.id,
                            title = item.account.name,
                            amount = amount,
                            dueDate = dueDate,
                            daysUntilDue = daysUntilDue,
                            status = status,
                            typeLabel = "Bill"
                        )
                    )
                }
            }
        }

        // 3. Active Installment Plans
        val loanMap = loans.associateBy { it.account.id }
        installments.filter { it.remainingBalance > 0 }.forEach { plan ->
            val parentLoan = loanMap[plan.accountId]
            val cycleDay = parentLoan?.loanDetails?.cycleDay1 ?: run {
                val purchaseDay = Instant.ofEpochMilli(plan.purchaseDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .dayOfMonth
                cycleDayClamp(purchaseDay)
            }
            val dueDate = LoanDateUtils.calculateNextDueDate(today, cycleDay, null)
            if (!dueDate.isAfter(maxDueLimit)) {
                val amount = plan.monthlyPaymentAmount.coerceAtMost(plan.remainingBalance)
                val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
                val status = LoanDateUtils.getDueDateStatus(dueDate, today)
                obligations.add(
                    UpcomingObligation(
                        id = plan.id,
                        title = plan.title,
                        amount = amount,
                        dueDate = dueDate,
                        daysUntilDue = daysUntilDue,
                        status = status,
                        typeLabel = "Installment"
                    )
                )
            }
        }

        return obligations.sortedBy { it.dueDate }
    }

    private fun cycleDayClamp(day: Int): Int = day.coerceIn(1, 31)

    /**
     * Tier 2: Top Merchants/Titles.
     * Ranked list of top 5 transaction titles by spend with counts.
     */
    fun calculateTopMerchants(
        transactions: List<TransactionEntity>,
        startTime: Long,
        endTime: Long
    ): List<MerchantSpend> {
        val expenseTx = transactions.filter {
            it.timestamp in startTime..endTime &&
                    (it.type == TransactionType.EXPENSE || it.type == TransactionType.INSTALLMENT) &&
                    it.title.isNotBlank()
        }

        if (expenseTx.isEmpty()) return emptyList()

        return expenseTx
            .groupBy { it.title.trim() }
            .map { (title, list) ->
                val totalAmount = list.sumOf { it.amount }
                val count = list.size
                title to (totalAmount to count)
            }
            .sortedByDescending { it.second.first }
            .take(5)
            .mapIndexed { index, (title, stats) ->
                MerchantSpend(
                    rank = index + 1,
                    name = title,
                    count = stats.second,
                    totalAmount = stats.first
                )
            }
    }

    /**
     * Tier 2: Category Trend Over Time.
     * Traces top 3-4 spending categories month-over-month over last 6 months.
     */
    fun calculateCategoryTrends(
        transactions: List<TransactionEntity>,
        referenceDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Pair<List<String>, List<CategoryTrend>> {
        val currentYm = YearMonth.from(referenceDate)
        val startYm = currentYm.minusMonths(5)
        val startTime = startYm.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endTime = currentYm.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()

        val periodExpenses = transactions.filter {
            it.timestamp in startTime..endTime &&
                    (it.type == TransactionType.EXPENSE || it.type == TransactionType.INSTALLMENT)
        }

        if (periodExpenses.isEmpty()) return Pair(emptyList(), emptyList())

        // Top 4 categories overall
        val topCategories = periodExpenses
            .groupBy { it.category.trim().ifEmpty { "Uncategorized" } }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(4)
            .map { it.first }

        val months = (0..5).map { i -> startYm.plusMonths(i.toLong()) }
        val monthLabels = months.map { it.month.getDisplayName(TextStyle.SHORT, Locale.US) }

        val trends = topCategories.mapIndexed { index, category ->
            val color = TrendLineColors.getOrElse(index) { TrendLineColors.last() }
            val monthlyData = months.map { ym ->
                val mStart = ym.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val mEnd = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
                val sum = transactions.filter {
                    it.timestamp in mStart..mEnd &&
                            (it.type == TransactionType.EXPENSE || it.type == TransactionType.INSTALLMENT) &&
                            it.category.trim().ifEmpty { "Uncategorized" }.equals(category, ignoreCase = true)
                }.sumOf { it.amount }

                MonthlyCategorySpend(
                    monthLabel = ym.month.getDisplayName(TextStyle.SHORT, Locale.US),
                    amount = sum
                )
            }
            CategoryTrend(category = category, color = color, monthlyData = monthlyData)
        }

        return Pair(monthLabels, trends)
    }

    /**
     * Tier 2: Daily Burn Rate.
     * Average daily spend for period + factual projection for the active month.
     */
    fun calculateDailyBurnRate(
        transactions: List<TransactionEntity>,
        period: ReportPeriod,
        referenceDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailyBurnRate {
        val (startTime, endTime) = period.getTimeRange(referenceDate, zoneId)
        val periodExpenses = transactions.filter {
            it.timestamp in startTime..endTime &&
                    (it.type == TransactionType.EXPENSE || it.type == TransactionType.INSTALLMENT)
        }

        val totalExpense = periodExpenses.sumOf { it.amount }

        return when (period) {
            ReportPeriod.THIS_MONTH -> {
                val currentYm = YearMonth.from(referenceDate)
                val daysElapsed = referenceDate.dayOfMonth.coerceAtLeast(1)
                val totalDays = currentYm.lengthOfMonth()
                val avgDaily = totalExpense / daysElapsed
                val projected = avgDaily * totalDays
                DailyBurnRate(
                    averageDailySpend = avgDaily,
                    daysElapsed = daysElapsed,
                    totalDaysInPeriod = totalDays,
                    projectedMonthTotal = projected
                )
            }
            ReportPeriod.LAST_MONTH -> {
                val lastYm = YearMonth.from(referenceDate).minusMonths(1)
                val totalDays = lastYm.lengthOfMonth()
                val avgDaily = totalExpense / totalDays.coerceAtLeast(1)
                DailyBurnRate(
                    averageDailySpend = avgDaily,
                    daysElapsed = totalDays,
                    totalDaysInPeriod = totalDays,
                    projectedMonthTotal = null
                )
            }
            ReportPeriod.LAST_3_MONTHS -> {
                val startYm = YearMonth.from(referenceDate).minusMonths(2)
                val startDate = startYm.atDay(1)
                val daysElapsed = (ChronoUnit.DAYS.between(startDate, referenceDate) + 1).toInt().coerceAtLeast(1)
                val endDate = YearMonth.from(referenceDate).atEndOfMonth()
                val totalDays = (ChronoUnit.DAYS.between(startDate, endDate) + 1).toInt().coerceAtLeast(1)
                val avgDaily = totalExpense / daysElapsed
                DailyBurnRate(
                    averageDailySpend = avgDaily,
                    daysElapsed = daysElapsed,
                    totalDaysInPeriod = totalDays,
                    projectedMonthTotal = null
                )
            }
            ReportPeriod.LAST_6_MONTHS -> {
                val startYm = YearMonth.from(referenceDate).minusMonths(5)
                val startDate = startYm.atDay(1)
                val daysElapsed = (ChronoUnit.DAYS.between(startDate, referenceDate) + 1).toInt().coerceAtLeast(1)
                val endDate = YearMonth.from(referenceDate).atEndOfMonth()
                val totalDays = (ChronoUnit.DAYS.between(startDate, endDate) + 1).toInt().coerceAtLeast(1)
                val avgDaily = totalExpense / daysElapsed
                DailyBurnRate(
                    averageDailySpend = avgDaily,
                    daysElapsed = daysElapsed,
                    totalDaysInPeriod = totalDays,
                    projectedMonthTotal = null
                )
            }
        }
    }
}
