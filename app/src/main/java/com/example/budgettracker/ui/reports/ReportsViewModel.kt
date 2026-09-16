package com.example.budgettracker.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.preference.AppPreferences
import com.example.budgettracker.data.repository.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class ReportsViewModel(
    private val repository: BudgetRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod.asStateFlow()

    val uiState: StateFlow<ReportsUiState> = combine(
        repository.allTransactions,
        repository.activeAccountsWithBalances,
        repository.getAllActiveLoanAccounts(),
        repository.getAllActiveBillAccounts(),
        repository.activeInstallmentPlans,
        appPreferences.isBalanceVisible,
        _selectedPeriod
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val transactions = args[0] as List<com.example.budgettracker.data.local.entity.TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val accounts = args[1] as List<com.example.budgettracker.data.local.entity.AccountWithBalance>
        @Suppress("UNCHECKED_CAST")
        val loans = args[2] as List<com.example.budgettracker.data.local.entity.AccountWithLoanDetails>
        @Suppress("UNCHECKED_CAST")
        val bills = args[3] as List<com.example.budgettracker.data.local.entity.AccountWithBillDetails>
        @Suppress("UNCHECKED_CAST")
        val installments = args[4] as List<com.example.budgettracker.data.local.entity.InstallmentPlanEntity>
        val isBalanceVisible = args[5] as Boolean
        val period = args[6] as ReportPeriod

        val today = LocalDate.now()
        val (startTime, endTime) = period.getTimeRange(today)

        val (totalExpense, spendingByCategory) = ReportsAnalyticsCalculator.calculateSpendingByCategory(
            transactions, startTime, endTime
        )

        val incomeExpense = ReportsAnalyticsCalculator.calculateIncomeVsExpense(
            transactions, period, today
        )

        val netWorthHistory = ReportsAnalyticsCalculator.calculateNetWorthHistory(
            accounts, transactions, today
        )

        val currentNetWorth = accounts
            .filter { it.isActive && it.includeInNetWorth }
            .sumOf { it.currentBalance }

        val upcomingObligations = ReportsAnalyticsCalculator.calculateUpcomingObligations(
            loans, bills, installments, today
        )
        val totalObligationsDue = upcomingObligations.sumOf { it.amount }

        val topMerchants = ReportsAnalyticsCalculator.calculateTopMerchants(
            transactions, startTime, endTime
        )

        val (trendMonths, categoryTrends) = ReportsAnalyticsCalculator.calculateCategoryTrends(
            transactions, today
        )

        val dailyBurnRate = ReportsAnalyticsCalculator.calculateDailyBurnRate(
            transactions, period, today
        )

        ReportsUiState(
            selectedPeriod = period,
            isBalanceVisible = isBalanceVisible,
            isLoading = false,
            spendingByCategory = spendingByCategory,
            totalExpenseInPeriod = totalExpense,
            incomeExpenseComparison = incomeExpense,
            netWorthHistory = netWorthHistory,
            currentNetWorth = currentNetWorth,
            upcomingObligations = upcomingObligations,
            totalObligationsDue = totalObligationsDue,
            topMerchants = topMerchants,
            categoryTrends = categoryTrends,
            trendMonths = trendMonths,
            dailyBurnRate = dailyBurnRate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(isLoading = true)
    )

    fun selectPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun toggleBalanceVisibility() {
        appPreferences.toggleBalanceVisibility()
    }
}
