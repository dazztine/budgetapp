package com.example.budgettracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.dao.MonthlyTotals
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.AccountWithBillDetails
import com.example.budgettracker.data.local.entity.AccountWithLoanDetails
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.repository.BudgetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.LoanBillingCycleEntity
import com.example.budgettracker.data.preference.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.example.budgettracker.data.local.entity.RecurringBillEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.util.DueDateStatus
import com.example.budgettracker.util.LoanDateUtils
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class UpcomingBillItem(
    val id: Long,
    val accountId: Long? = null,
    val cycleId: Long? = null,
    val recurringBillId: Long? = null,
    val name: String,
    val amountCentavos: Long,
    val dueDate: LocalDate,
    val daysUntilDue: Long,
    val status: DueDateStatus,
    val subtitle: String,
    val presetId: String? = null,
    val accountType: AccountType? = null
)

class DashboardViewModel(
    private val repository: BudgetRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val appPreferences: AppPreferences? = null
) : ViewModel() {

    private val _isBalanceVisible = MutableStateFlow(appPreferences?.isBalanceVisible?.value ?: true)
    val isBalanceVisible: StateFlow<Boolean> = appPreferences?.isBalanceVisible ?: _isBalanceVisible.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            repository.ensurePendingCyclesForAllAccounts()
        }
    }

    private val _upcomingBillsViewMode = MutableStateFlow(appPreferences?.upcomingBillsViewMode?.value ?: "VERTICAL")
    val upcomingBillsViewMode: StateFlow<String> = appPreferences?.upcomingBillsViewMode ?: _upcomingBillsViewMode.asStateFlow()

    fun toggleUpcomingBillsViewMode() {
        if (appPreferences != null) {
            appPreferences.toggleUpcomingBillsViewMode()
        } else {
            _upcomingBillsViewMode.value = if (_upcomingBillsViewMode.value == "VERTICAL") "HORIZONTAL" else "VERTICAL"
        }
    }

    fun toggleBalanceVisibility() {
        if (appPreferences != null) {
            appPreferences.toggleBalanceVisibility()
        } else {
            _isBalanceVisible.value = !_isBalanceVisible.value
        }
    }

    val netWorth: StateFlow<Long> = repository.totalNetWorth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val activeAccountsWithBalances: StateFlow<List<AccountWithBalance>> = repository.activeAccountsWithBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenAccounts: StateFlow<List<AccountEntity>> = repository.hiddenAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.getRecentTransactions(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loanAccounts: StateFlow<List<AccountWithLoanDetails>> = repository.getAllActiveLoanAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val billAccounts: StateFlow<List<AccountWithBillDetails>> = repository.getAllActiveBillAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditDetailsList: StateFlow<List<com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity>> = repository.allCreditDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeInstallmentPlans: StateFlow<List<InstallmentPlanEntity>> = repository.activeInstallmentPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTotals: StateFlow<MonthlyTotals> = run {
        val (start, end) = getCurrentMonthBounds()
        repository.getMonthlyTotals(start, end)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyTotals(0L, 0L))
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(ioDispatcher) {
            repository.deleteTransaction(transaction)
        }
    }

    fun softDeleteAccount(accountId: Long) {
        viewModelScope.launch(ioDispatcher) {
            repository.softDeleteAccount(accountId)
        }
    }

    fun restoreAccount(accountId: Long) {
        viewModelScope.launch(ioDispatcher) {
            repository.restoreAccount(accountId)
        }
    }

    fun updateNetWorthInclusion(accountId: Long, include: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            repository.updateNetWorthInclusion(accountId, include)
        }
    }

    fun getTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>> =
        repository.getTransactionsByAccount(accountId)

    suspend fun getAccountById(accountId: Long): AccountEntity? =
        repository.getAccountById(accountId)

    suspend fun getLoanDetailsForAccount(accountId: Long): LoanAccountDetailsEntity? =
        repository.getLoanDetailsByAccountId(accountId)

    suspend fun getTransactionCountForAccount(accountId: Long): Int = repository.getTransactionCountByAccount(accountId)

    suspend fun getSavingsDetailsForAccount(accountId: Long): com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity? =
        repository.getSavingsDetailsByAccountId(accountId)

    suspend fun getBillDetailsForAccount(accountId: Long): com.example.budgettracker.data.local.entity.BillAccountDetailsEntity? =
        repository.getBillDetailsByAccountId(accountId)

    suspend fun getCreditDetailsForAccount(accountId: Long): com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity? =
        repository.getCreditDetailsDirect(accountId)

    fun saveAccount(
        account: AccountEntity,
        loanDetails: LoanAccountDetailsEntity? = null,
        savingsDetails: com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity? = null,
        billDetails: com.example.budgettracker.data.local.entity.BillAccountDetailsEntity? = null,
        creditDetails: com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(ioDispatcher) {
            val accountId = if (account.id == 0L) {
                repository.insertAccount(account)
            } else {
                repository.updateAccount(account)
                account.id
            }

            if (loanDetails != null) {
                val detailsToSave = loanDetails.copy(accountId = accountId)
                repository.insertLoanDetails(detailsToSave)
            }
            if (savingsDetails != null) {
                val detailsToSave = savingsDetails.copy(accountId = accountId)
                repository.insertSavingsDetails(detailsToSave)
            }
            if (billDetails != null) {
                val detailsToSave = billDetails.copy(accountId = accountId)
                repository.insertBillDetails(detailsToSave)
            }
            if (creditDetails != null) {
                val detailsToSave = creditDetails.copy(accountId = accountId)
                repository.insertCreditDetails(detailsToSave)
            }
            if (account.type == AccountType.CREDIT || account.type == AccountType.LOAN || account.type == AccountType.BNPL || account.type == AccountType.BILL) {
                repository.ensurePendingCyclesForAccount(accountId)
            }
            onComplete()
        }
    }

    fun logBalanceAdjustment(
        accountId: Long,
        type: com.example.budgettracker.data.model.TransactionType,
        amountCentavos: Long,
        title: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(ioDispatcher) {
            val transaction = TransactionEntity(
                type = type,
                amount = amountCentavos,
                isAdjustment = true,
                accountId = accountId,
                category = "Adjustment",
                title = title,
                timestamp = System.currentTimeMillis()
            )
            repository.insertTransaction(transaction)
            onComplete()
        }
    }

    fun getInstallmentPlansForAccount(accountId: Long): Flow<List<InstallmentPlanEntity>> =
        repository.getInstallmentPlansByAccount(accountId)

    fun getPaidBillingCyclesForAccount(accountId: Long): Flow<List<LoanBillingCycleEntity>> =
        repository.getPaidBillingCycles(accountId)

    val upcomingObligations: StateFlow<List<UpcomingBillItem>> = combine(
        repository.getAllPendingBillingCycles(),
        repository.activeAccountsWithBalances,
        repository.activeRecurringBills
    ) { pendingCycles, accounts, recurringBills ->
        val today = LocalDate.now()
        val list = mutableListOf<UpcomingBillItem>()

        // 1. Pending billing cycles from accounts (Loans, BNPL, Bills, Credit)
        pendingCycles.forEach { cycle ->
            val account = accounts.find { it.id == cycle.accountId }
            if (account != null && account.isActive) {
                val isDebtAccount = account.type == AccountType.LOAN || account.type == AccountType.BNPL || account.type == AccountType.CREDIT
                if (isDebtAccount && (account.currentBalance >= 0L || cycle.amountDue <= 0L)) {
                    return@forEach
                }

                val dueDate = Instant.ofEpochMilli(cycle.cycleDueDate).atZone(ZoneId.systemDefault()).toLocalDate()
                val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
                val status = LoanDateUtils.getDueDateStatus(dueDate, today)
                list.add(
                    UpcomingBillItem(
                        id = cycle.id,
                        accountId = account.id,
                        cycleId = cycle.id,
                        name = account.name,
                        amountCentavos = cycle.amountDue,
                        dueDate = dueDate,
                        daysUntilDue = daysUntilDue,
                        status = status,
                        subtitle = "${account.name} • ${account.type.toDisplayLabel()}",
                        presetId = account.presetId,
                        accountType = account.type
                    )
                )
            }
        }

        // 2. Standalone recurring bills
        recurringBills.filter { it.isActive }.forEach { bill ->
            val dueDate = LoanDateUtils.calculateNextDueDate(today, listOf(bill.dueDay))
            val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
            val status = LoanDateUtils.getDueDateStatus(dueDate, today)
            list.add(
                UpcomingBillItem(
                    id = bill.id,
                    accountId = bill.accountId,
                    recurringBillId = bill.id,
                    name = bill.name,
                    amountCentavos = bill.amount,
                    dueDate = dueDate,
                    daysUntilDue = daysUntilDue,
                    status = status,
                    subtitle = "${bill.category} • Recurring Bill",
                    presetId = null,
                    accountType = AccountType.BILL
                )
            )
        }

        list.sortedBy { it.dueDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getPendingBillingCyclesForAccount(accountId: Long): Flow<List<LoanBillingCycleEntity>> =
        repository.getPendingBillingCycles(accountId)

    fun markBillingCyclePaid(cycleId: Long, paidAmount: Long) {
        viewModelScope.launch(ioDispatcher) {
            repository.markBillingCyclePaid(cycleId, paidAmount)
        }
    }

    fun updateBillingCycle(cycle: LoanBillingCycleEntity) {
        viewModelScope.launch(ioDispatcher) {
            repository.updateBillingCycle(cycle)
        }
    }

    fun updateCycleDueDate(cycleId: Long, dueDateMillis: Long) {
        viewModelScope.launch(ioDispatcher) {
            val cycle = repository.getBillingCycleById(cycleId)
            if (cycle != null) {
                repository.updateBillingCycle(cycle.copy(cycleDueDate = dueDateMillis))
            }
        }
    }

    fun updateCycleAmountDue(cycleId: Long, amountDueCentavos: Long) {
        viewModelScope.launch(ioDispatcher) {
            repository.updateCycleAmountDue(cycleId, amountDueCentavos, isManualOverride = true)
        }
    }

    fun saveRecurringBill(bill: RecurringBillEntity) {
        viewModelScope.launch(ioDispatcher) {
            if (bill.id == 0L) {
                repository.insertRecurringBill(bill)
            } else {
                repository.updateRecurringBill(bill)
            }
        }
    }

    fun deleteRecurringBill(bill: RecurringBillEntity) {
        viewModelScope.launch(ioDispatcher) {
            repository.deleteRecurringBill(bill)
        }
    }

    fun updateLoanCycle(accountId: Long, dueDate: Long?, amountDue: Long?) {
        viewModelScope.launch(ioDispatcher) {
            val pending = repository.getPendingCyclesDirect(accountId)
            if (pending.isNotEmpty()) {
                val first = pending.first()
                val updated = first.copy(
                    cycleDueDate = dueDate ?: first.cycleDueDate,
                    amountDue = amountDue ?: first.amountDue
                )
                repository.updateBillingCycle(updated)
            }
        }
    }

    fun recordCyclePayment(
        accountId: Long,
        cycleId: Long?,
        actualAmount: Long,
        onResult: ((com.example.budgettracker.data.repository.CyclePaymentResult) -> Unit)? = null
    ) {
        viewModelScope.launch(ioDispatcher) {
            val targetCycleId = cycleId ?: repository.getPendingCyclesDirect(accountId).firstOrNull()?.id
            if (targetCycleId != null) {
                val result = repository.recordBillingCyclePayment(targetCycleId, actualAmount)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onResult?.invoke(result)
                }
            } else {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onResult?.invoke(com.example.budgettracker.data.repository.CyclePaymentResult.NotFound(-1L))
                }
            }
        }
    }

    fun markLoanCyclePaid(accountId: Long, paidAmount: Long) {
        recordCyclePayment(accountId, null, paidAmount)
    }

    private fun getCurrentMonthBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val endTime = cal.timeInMillis

        return Pair(startTime, endTime)
    }
}
