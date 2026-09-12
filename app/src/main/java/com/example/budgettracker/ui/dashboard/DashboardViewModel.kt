package com.example.budgettracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.dao.MonthlyTotals
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.AccountWithLoanDetails
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.repository.BudgetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

import com.example.budgettracker.data.local.entity.InstallmentPlanEntity

class DashboardViewModel(
    private val repository: BudgetRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    val netWorth: StateFlow<Long> = repository.totalNetWorth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val activeAccountsWithBalances: StateFlow<List<AccountWithBalance>> = repository.activeAccountsWithBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.getRecentTransactions(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loanAccounts: StateFlow<List<AccountWithLoanDetails>> = repository.getAllActiveLoanAccounts()
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

    fun saveAccount(account: AccountEntity, loanDetails: LoanAccountDetailsEntity? = null, onComplete: () -> Unit = {}) {
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
