package com.example.budgettracker.data.repository

import com.example.budgettracker.data.local.dao.AccountDao
import com.example.budgettracker.data.local.dao.BillDetailsDao
import com.example.budgettracker.data.local.dao.InstallmentPlanDao
import com.example.budgettracker.data.local.dao.LoanDetailsDao
import com.example.budgettracker.data.local.dao.MonthlyTotals
import com.example.budgettracker.data.local.dao.SavingsDetailsDao
import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.AccountWithBillDetails
import com.example.budgettracker.data.local.entity.AccountWithLoanDetails
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

class BudgetRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val loanDetailsDao: LoanDetailsDao,
    private val installmentPlanDao: InstallmentPlanDao,
    private val savingsDetailsDao: SavingsDetailsDao,
    private val billDetailsDao: BillDetailsDao
) {
    // Accounts
    val activeAccountsWithBalances: Flow<List<AccountWithBalance>> = accountDao.getAllActiveWithBalances()
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAll()
    val activeAccounts: Flow<List<AccountEntity>> = accountDao.getAllActive()
    val hiddenAccounts: Flow<List<AccountEntity>> = accountDao.getAllHiddenAccounts()
    val totalNetWorth: Flow<Long> = accountDao.getTotalNetWorth()
    val activeAccountCount: Flow<Int> = accountDao.getActiveAccountCount()

    fun getAccountBalance(accountId: Long): Flow<Long?> = accountDao.getAccountBalance(accountId)

    suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getById(id)

    suspend fun insertAccount(account: AccountEntity): Long = accountDao.insert(account)

    suspend fun insertAccounts(accounts: List<AccountEntity>): List<Long> = accountDao.insertAll(accounts)

    suspend fun updateAccount(account: AccountEntity): Int = accountDao.update(account)

    suspend fun updateNetWorthInclusion(accountId: Long, include: Boolean): Int =
        accountDao.updateNetWorthInclusion(accountId, include)

    suspend fun softDeleteAccount(id: Long): Int = accountDao.softDelete(id)

    suspend fun restoreAccount(id: Long): Int = accountDao.restoreAccount(id)

    suspend fun getActiveAccountCountDirect(): Int = accountDao.getActiveCountDirect()

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAll()

    suspend fun insertTransaction(transaction: TransactionEntity): Long = transactionDao.insert(transaction)

    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long> = transactionDao.insertAll(transactions)

    suspend fun updateTransaction(transaction: TransactionEntity): Int = transactionDao.update(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity): Int = transactionDao.delete(transaction)

    suspend fun getTransactionById(id: Long): TransactionEntity? = transactionDao.getById(id)

    fun getRecentTransactions(limit: Int = 20): Flow<List<TransactionEntity>> = transactionDao.getRecent(limit)

    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>> = transactionDao.getByAccount(accountId)
    suspend fun getTransactionCountByAccount(accountId: Long): Int = transactionDao.getCountByAccount(accountId)

    fun getMonthlyTotals(startTime: Long, endTime: Long): Flow<MonthlyTotals> = transactionDao.getMonthlyTotals(startTime, endTime)

    fun getFilteredTransactions(
        accountId: Long? = null,
        type: TransactionType? = null,
        category: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): Flow<List<TransactionEntity>> = transactionDao.getFilteredTransactions(accountId, type, category, startTime, endTime)

    // Autocomplete Stream Isolation by TransactionType
    fun getDistinctCategories(type: TransactionType, limit: Int = 20): Flow<List<String>> =
        transactionDao.getDistinctCategories(type, limit)

    fun getDistinctTitles(type: TransactionType, limit: Int = 20): Flow<List<String>> =
        transactionDao.getDistinctTitles(type, limit)

    fun getDistinctTitlesByCategory(type: TransactionType, category: String, limit: Int = 20): Flow<List<String>> =
        transactionDao.getDistinctTitlesByCategory(type, category, limit)

    // Loan Account Details
    suspend fun insertLoanDetails(details: LoanAccountDetailsEntity): Long = loanDetailsDao.insert(details)

    suspend fun updateLoanDetails(details: LoanAccountDetailsEntity): Int = loanDetailsDao.update(details)

    suspend fun deleteLoanDetails(details: LoanAccountDetailsEntity): Int = loanDetailsDao.delete(details)

    suspend fun getLoanDetailsByAccountId(accountId: Long): LoanAccountDetailsEntity? = loanDetailsDao.getByAccountId(accountId)

    fun getLoanDetailsByAccountIdFlow(accountId: Long): Flow<LoanAccountDetailsEntity?> = loanDetailsDao.getByAccountIdFlow(accountId)

    fun getAllActiveLoanAccounts(): Flow<List<AccountWithLoanDetails>> = accountDao.getAllActiveLoanAccounts()

    // Savings Account Details
    suspend fun insertSavingsDetails(details: SavingsAccountDetailsEntity): Long = savingsDetailsDao.insert(details)

    suspend fun updateSavingsDetails(details: SavingsAccountDetailsEntity): Int = savingsDetailsDao.update(details)

    suspend fun deleteSavingsDetails(details: SavingsAccountDetailsEntity): Int = savingsDetailsDao.delete(details)

    suspend fun getSavingsDetailsByAccountId(accountId: Long): SavingsAccountDetailsEntity? = savingsDetailsDao.getByAccountId(accountId)

    fun getSavingsDetailsByAccountIdFlow(accountId: Long): Flow<SavingsAccountDetailsEntity?> = savingsDetailsDao.getByAccountIdFlow(accountId)

    // Bill Account Details
    suspend fun insertBillDetails(details: BillAccountDetailsEntity): Long = billDetailsDao.insert(details)

    suspend fun updateBillDetails(details: BillAccountDetailsEntity): Int = billDetailsDao.update(details)

    suspend fun deleteBillDetails(details: BillAccountDetailsEntity): Int = billDetailsDao.delete(details)

    suspend fun getBillDetailsByAccountId(accountId: Long): BillAccountDetailsEntity? = billDetailsDao.getByAccountId(accountId)

    fun getBillDetailsByAccountIdFlow(accountId: Long): Flow<BillAccountDetailsEntity?> = billDetailsDao.getByAccountIdFlow(accountId)

    fun getAllActiveBillAccounts(): Flow<List<AccountWithBillDetails>> = accountDao.getAllActiveBillAccounts()

    // Installment Plans
    suspend fun insertInstallmentPlan(plan: InstallmentPlanEntity): Long = installmentPlanDao.insert(plan)

    suspend fun insertInstallmentPlans(plans: List<InstallmentPlanEntity>): List<Long> = installmentPlanDao.insertAll(plans)

    suspend fun updateInstallmentPlan(plan: InstallmentPlanEntity): Int = installmentPlanDao.update(plan)

    suspend fun deleteInstallmentPlan(plan: InstallmentPlanEntity): Int = installmentPlanDao.delete(plan)

    fun getInstallmentPlansByAccount(accountId: Long): Flow<List<InstallmentPlanEntity>> =
        installmentPlanDao.getByAccountId(accountId)

    fun getTransactionsByInstallmentPlan(planId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getByInstallmentPlan(planId)

    suspend fun recordInstallmentPayment(transaction: TransactionEntity, planId: Long): Long {
        val plan = installmentPlanDao.getById(planId)
        val insertedId = transactionDao.insert(transaction.copy(installmentPlanId = planId))
        if (plan != null) {
            val updated = plan.copy(
                installmentsPaid = plan.installmentsPaid + 1,
                remainingBalance = (plan.remainingBalance - transaction.amount).coerceAtLeast(0L),
                updatedAt = System.currentTimeMillis()
            )
            installmentPlanDao.update(updated)
        }
        return insertedId
    }

    val activeInstallmentPlans: Flow<List<InstallmentPlanEntity>> = installmentPlanDao.getAllActive()

    // Direct Data Extraction & Deletion for Backup/Restore
    suspend fun getAllAccountsDirect(): List<AccountEntity> = accountDao.getAllDirect()
    suspend fun getAllTransactionsDirect(): List<TransactionEntity> = transactionDao.getAllDirect()
    suspend fun getAllLoanDetailsDirect(): List<LoanAccountDetailsEntity> = loanDetailsDao.getAllDirect()
    suspend fun getAllSavingsDetailsDirect(): List<SavingsAccountDetailsEntity> = savingsDetailsDao.getAllDirect()
    suspend fun getAllBillDetailsDirect(): List<BillAccountDetailsEntity> = billDetailsDao.getAllDirect()
    suspend fun getAllInstallmentPlansDirect(): List<InstallmentPlanEntity> = installmentPlanDao.getAllDirect()

    suspend fun insertLoanDetailsList(detailsList: List<LoanAccountDetailsEntity>): List<Long> =
        loanDetailsDao.insertAll(detailsList)

    suspend fun insertSavingsDetailsList(detailsList: List<SavingsAccountDetailsEntity>): List<Long> =
        savingsDetailsDao.insertAll(detailsList)

    suspend fun insertBillDetailsList(detailsList: List<BillAccountDetailsEntity>): List<Long> =
        billDetailsDao.insertAll(detailsList)

    suspend fun clearAllData() {
        // Order matters for Foreign Key constraints
        transactionDao.deleteAll()
        installmentPlanDao.deleteAll()
        loanDetailsDao.deleteAll()
        savingsDetailsDao.deleteAll()
        billDetailsDao.deleteAll()
        accountDao.deleteAll()
    }
}
