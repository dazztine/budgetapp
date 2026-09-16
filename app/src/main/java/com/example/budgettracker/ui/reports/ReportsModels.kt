package com.example.budgettracker.ui.reports

import androidx.compose.ui.graphics.Color
import com.example.budgettracker.util.DueDateStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class ReportPeriod(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_6_MONTHS("Last 6 Months");

    fun getTimeRange(referenceDate: LocalDate = LocalDate.now(), zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val currentYm = YearMonth.from(referenceDate)
        return when (this) {
            THIS_MONTH -> {
                val start = currentYm.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = currentYm.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
                Pair(start, end)
            }
            LAST_MONTH -> {
                val lastYm = currentYm.minusMonths(1)
                val start = lastYm.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = lastYm.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
                Pair(start, end)
            }
            LAST_3_MONTHS -> {
                val startYm = currentYm.minusMonths(2)
                val start = startYm.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = currentYm.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
                Pair(start, end)
            }
            LAST_6_MONTHS -> {
                val startYm = currentYm.minusMonths(5)
                val start = startYm.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = currentYm.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
                Pair(start, end)
            }
        }
    }
}

data class CategorySpending(
    val category: String,
    val amount: Long,
    val percentage: Float,
    val color: Color,
    val transactionCount: Int
)

data class MonthlyBarData(
    val monthLabel: String,
    val income: Long,
    val expense: Long
)

data class IncomeExpenseComparison(
    val totalIncome: Long,
    val totalExpense: Long,
    val netAmount: Long,
    val isSaved: Boolean,
    val monthlyBars: List<MonthlyBarData>
)

data class NetWorthPoint(
    val monthLabel: String,
    val netWorth: Long,
    val timestamp: Long
)

data class UpcomingObligation(
    val id: Long,
    val title: String,
    val amount: Long,
    val dueDate: LocalDate,
    val daysUntilDue: Long,
    val status: DueDateStatus,
    val typeLabel: String
)

data class MerchantSpend(
    val rank: Int,
    val name: String,
    val count: Int,
    val totalAmount: Long
)

data class MonthlyCategorySpend(
    val monthLabel: String,
    val amount: Long
)

data class CategoryTrend(
    val category: String,
    val color: Color,
    val monthlyData: List<MonthlyCategorySpend>
)

data class DailyBurnRate(
    val averageDailySpend: Long,
    val daysElapsed: Int,
    val totalDaysInPeriod: Int,
    val projectedMonthTotal: Long?
)

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val isBalanceVisible: Boolean = true,
    val isLoading: Boolean = false,
    val spendingByCategory: List<CategorySpending> = emptyList(),
    val totalExpenseInPeriod: Long = 0L,
    val incomeExpenseComparison: IncomeExpenseComparison = IncomeExpenseComparison(0L, 0L, 0L, true, emptyList()),
    val netWorthHistory: List<NetWorthPoint> = emptyList(),
    val currentNetWorth: Long = 0L,
    val upcomingObligations: List<UpcomingObligation> = emptyList(),
    val totalObligationsDue: Long = 0L,
    val topMerchants: List<MerchantSpend> = emptyList(),
    val categoryTrends: List<CategoryTrend> = emptyList(),
    val trendMonths: List<String> = emptyList(),
    val dailyBurnRate: DailyBurnRate? = null
)
