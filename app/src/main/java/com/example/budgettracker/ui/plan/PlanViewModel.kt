package com.example.budgettracker.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.BudgetEntity
import com.example.budgettracker.data.local.entity.CustomCategoryEntity
import com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.domain.BudgetPaceCalculator
import com.example.budgettracker.domain.BudgetPaceResult
import com.example.budgettracker.ui.dashboard.UpcomingBillItem
import com.example.budgettracker.util.LoanDateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Representation of a Savings Goal card item in the Plan tab.
 */
data class GoalItem(
    val account: AccountWithBalance,
    val details: SavingsAccountDetailsEntity,
    val savedAmount: Long, // derived from ledger balance
    val goalAmount: Long,
    val percent: Int, // derived integer percentage: (savedAmount * 100) / goalAmount
    val isCompleted: Boolean,
    val targetDate: Long?
)

/**
 * Representation of a Debt Account card item in the Debts sub-tab.
 */
data class DebtItem(
    val account: AccountWithBalance,
    val totalDebtAmount: Long, // For LOAN/BNPL, absolute debt
    val availableCredit: Long?, // For CREDIT: creditLimit + currentBalance
    val creditLimit: Long?
)

/**
 * Complete immutable UI state for the Plan tab.
 */
sealed class PlanUiState {
    object Loading : PlanUiState()

    data class Success(
        val paceResult: BudgetPaceResult,
        val overallBudget: BudgetEntity?,
        val categoryBudgets: List<BudgetEntity>,
        val goals: List<GoalItem>,
        val upcomingObligations: List<UpcomingBillItem>,
        val debts: List<DebtItem>,
        val availableCategories: List<String>
    ) : PlanUiState()

    data class Error(val message: String) : PlanUiState()
}

/**
 * Supported sub-tabs for the Plan tab screen.
 */
enum class PlanSubTab(val title: String) {
    BUDGETS("Budgets"),
    UPCOMING("Upcoming"),
    GOALS("Goals"),
    DEBTS("Debts")
}

class PlanViewModel(
    private val repository: BudgetRepository,
    private val clock: Clock = Clock.system(ZoneId.of("Asia/Manila"))
) : ViewModel() {

    private val _selectedSubTab = MutableStateFlow(PlanSubTab.BUDGETS)
    val selectedSubTab: StateFlow<PlanSubTab> = _selectedSubTab.asStateFlow()

    fun selectSubTab(tab: PlanSubTab) {
        _selectedSubTab.value = tab
    }

    val uiState: StateFlow<PlanUiState> = combine(
        repository.observeAllBudgets(),
        repository.allTransactions,
        repository.activeAccountsWithBalances,
        repository.getAllPendingBillingCycles(),
        repository.activeRecurringBills,
        repository.getCustomCategories(com.example.budgettracker.data.model.TransactionType.EXPENSE),
        repository.getAllActiveBillAccounts(),
        repository.allCreditDetails
    ) { args: Array<Any?> ->
        try {
            @Suppress("UNCHECKED_CAST")
            val budgets = args[0] as List<BudgetEntity>
            @Suppress("UNCHECKED_CAST")
            val allTxs = args[1] as List<TransactionEntity>
            @Suppress("UNCHECKED_CAST")
            val accounts = args[2] as List<AccountWithBalance>
            @Suppress("UNCHECKED_CAST")
            val pendingCycles = args[3] as List<com.example.budgettracker.data.local.entity.LoanBillingCycleEntity>
            @Suppress("UNCHECKED_CAST")
            val recurringBills = args[4] as List<com.example.budgettracker.data.local.entity.RecurringBillEntity>
            @Suppress("UNCHECKED_CAST")
            val customCategories = args[5] as List<CustomCategoryEntity>
            @Suppress("UNCHECKED_CAST")
            val billAccounts = args[6] as List<com.example.budgettracker.data.local.entity.AccountWithBillDetails>
            @Suppress("UNCHECKED_CAST")
            val creditDetailsList = args[7] as List<com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity>

            val today = LocalDate.now(clock)
            val zoneId = clock.zone
            val currentYm = YearMonth.from(today)
            val monthStartEpoch = currentYm.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val monthEndEpoch = currentYm.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()

            val currentMonthTxs = allTxs.filter { it.timestamp in monthStartEpoch..monthEndEpoch }
            val billAccountIds = billAccounts.map { it.account.id }.toSet()

            val overallBudget = budgets.find { it.category == null && it.period == "MONTHLY" }
            val categoryBudgets = budgets.filter { it.category != null && it.period == "MONTHLY" }

            // 1. Budget Pace Calculation
            val paceResult = BudgetPaceCalculator.calculate(
                monthlyBudget = overallBudget?.amount,
                transactions = currentMonthTxs,
                today = today,
                billAccountIds = billAccountIds,
                categoryBudgets = categoryBudgets,
                customCategories = customCategories
            )

            // 2. Goals (SAVINGS accounts with isGoal == true)
            val allSavingsDetails: List<SavingsAccountDetailsEntity> = repository.getAllSavingsDetailsDirect()
            val goals = accounts
                .filter { it.type == AccountType.SAVINGS }
                .mapNotNull { acc ->
                    val details: SavingsAccountDetailsEntity? = allSavingsDetails.find { it.accountId == acc.id }
                    if (details != null && details.isGoal) {
                        val target = details.goalAmount ?: 0L
                        val saved = acc.currentBalance.coerceAtLeast(0L)
                        val percent = if (target > 0L) ((saved * 100L) / target).toInt() else 0
                        GoalItem(
                            account = acc,
                            details = details,
                            savedAmount = saved,
                            goalAmount = target,
                            percent = percent,
                            isCompleted = target > 0L && saved >= target,
                            targetDate = details.targetDate
                        )
                    } else null
                }

            // 3. Upcoming Obligations
            val upcoming = mutableListOf<UpcomingBillItem>()
            pendingCycles.forEach { cycle ->
                val account = accounts.find { it.id == cycle.accountId }
                if (account != null && account.isActive) {
                    val isDebtAccount = account.type == AccountType.LOAN || account.type == AccountType.BNPL || account.type == AccountType.CREDIT
                    if (isDebtAccount && (account.currentBalance >= 0L || cycle.amountDue <= 0L)) {
                        return@forEach
                    }

                    val dueDate = Instant.ofEpochMilli(cycle.cycleDueDate).atZone(zoneId).toLocalDate()
                    val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
                    val status = LoanDateUtils.getDueDateStatus(dueDate, today)
                    upcoming.add(
                        UpcomingBillItem(
                            id = cycle.id,
                            accountId = account.id,
                            cycleId = cycle.id,
                            recurringBillId = null,
                            name = account.name,
                            amountCentavos = cycle.amountDue,
                            dueDate = dueDate,
                            daysUntilDue = daysUntilDue,
                            status = status,
                            subtitle = if (daysUntilDue < 0) "Overdue by ${-daysUntilDue}d" else if (daysUntilDue == 0L) "Due today" else "Due in ${daysUntilDue}d",
                            presetId = account.presetId,
                            accountType = account.type
                        )
                    )
                }
            }
            upcoming.sortBy { it.dueDate }

            // 4. Debts
            val debts = accounts
                .filter { it.type == AccountType.LOAN || it.type == AccountType.BNPL || it.type == AccountType.CREDIT }
                .map { acc ->
                    val creditDetail = creditDetailsList.find { it.accountId == acc.id }
                    val availableCredit = if (acc.type == AccountType.CREDIT && creditDetail != null) {
                        creditDetail.creditLimit + acc.currentBalance
                    } else null

                    DebtItem(
                        account = acc,
                        totalDebtAmount = if (acc.currentBalance < 0) -acc.currentBalance else 0L,
                        availableCredit = availableCredit,
                        creditLimit = creditDetail?.creditLimit
                    )
                }

            // 5. Available Categories for Picker (Union of standard, custom, used, and in budgets)
            val standardKeys = listOf(
                "Food & Dining",
                "Transportation",
                "Bills & Utilities",
                "Groceries",
                "Shopping",
                "Health & Medical",
                "Entertainment",
                "General"
            )
            val customKeys = customCategories.map { it.name }
            val usedKeys = allTxs.map { it.category }
            val budgetKeys = budgets.mapNotNull { it.category }

            val rawUnion = standardKeys + customKeys + usedKeys + budgetKeys
            val distinctCategories = mutableListOf<String>()
            val seenLower = mutableSetOf<String>()
            for (key in rawUnion) {
                val trimmed = key.trim()
                if (trimmed.isNotEmpty() && !trimmed.equals("Uncategorized", ignoreCase = true)) {
                    val lower = trimmed.lowercase()
                    if (seenLower.add(lower)) {
                        distinctCategories.add(trimmed)
                    }
                }
            }

            PlanUiState.Success(
                paceResult = paceResult,
                overallBudget = overallBudget,
                categoryBudgets = categoryBudgets,
                goals = goals,
                upcomingObligations = upcoming,
                debts = debts,
                availableCategories = distinctCategories
            )
        } catch (e: Exception) {
            PlanUiState.Error(e.message ?: "Failed to load plan state")
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PlanUiState.Loading
    )

    fun upsertBudget(category: String?, amountCentavos: Long, period: String = "MONTHLY") {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.upsertBudget(
                BudgetEntity(
                    category = category,
                    amount = amountCentavos,
                    period = period,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }
}
