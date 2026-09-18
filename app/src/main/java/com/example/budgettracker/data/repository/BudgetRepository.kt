package com.example.budgettracker.data.repository

import com.example.budgettracker.data.local.dao.AccountDao
import com.example.budgettracker.data.local.dao.BillDetailsDao
import com.example.budgettracker.data.local.dao.CustomCategoryDao
import com.example.budgettracker.data.local.dao.InstallmentPlanDao
import com.example.budgettracker.data.local.dao.LoanBillingCycleDao
import com.example.budgettracker.data.local.dao.LoanDetailsDao
import com.example.budgettracker.data.local.dao.MonthlyTotals
import com.example.budgettracker.data.local.dao.RecurringBillDao
import com.example.budgettracker.data.local.dao.SavingsDetailsDao
import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.AccountWithBillDetails
import com.example.budgettracker.data.local.entity.AccountWithLoanDetails
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import com.example.budgettracker.data.local.entity.CustomCategoryEntity
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.LoanBillingCycleEntity
import com.example.budgettracker.data.local.entity.RecurringBillEntity
import com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.local.dao.CreditDetailsDao
import com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

sealed class CyclePaymentResult {
    data class Partial(
        val cycleId: Long,
        val amountPaid: Long,
        val remainingDue: Long,
        val dueDate: Long
    ) : CyclePaymentResult()

    data class PaidInFull(
        val cycleId: Long,
        val amountPaid: Long,
        val cycle: LoanBillingCycleEntity,
        val overpaymentAmount: Long = 0L
    ) : CyclePaymentResult()

    data class NotFound(val cycleId: Long) : CyclePaymentResult()
}

class BudgetRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val loanDetailsDao: LoanDetailsDao,
    private val installmentPlanDao: InstallmentPlanDao,
    private val savingsDetailsDao: SavingsDetailsDao,
    private val billDetailsDao: BillDetailsDao,
    private val loanBillingCycleDao: LoanBillingCycleDao? = null,
    private val customCategoryDao: CustomCategoryDao? = null,
    private val recurringBillDao: RecurringBillDao? = null,
    private val creditDetailsDao: CreditDetailsDao? = null
) {
    // Accounts
    val activeAccountsWithBalances: Flow<List<AccountWithBalance>> = accountDao.getAllActiveWithBalances()
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAll()
    val activeAccounts: Flow<List<AccountEntity>> = accountDao.getAllActive()
    val hiddenAccounts: Flow<List<AccountEntity>> = accountDao.getAllHiddenAccounts()
    val totalNetWorth: Flow<Long> = accountDao.getTotalNetWorth()
    val activeAccountCount: Flow<Int> = accountDao.getActiveAccountCount()

    fun getComputedBalance(accountId: Long): Flow<Long?> = accountDao.getAccountBalance(accountId)
    fun getAccountBalance(accountId: Long): Flow<Long?> = getComputedBalance(accountId)

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
    suspend fun insertLoanDetails(details: LoanAccountDetailsEntity): Long {
        val id = loanDetailsDao.insert(details)
        ensurePendingCyclesForAccount(details.accountId)
        return id
    }

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
    suspend fun insertBillDetails(details: BillAccountDetailsEntity): Long {
        val id = billDetailsDao.insert(details)
        ensurePendingCyclesForAccount(details.accountId, details.parseDueDays(), details.amountDue ?: 0L)
        return id
    }

    suspend fun updateBillDetails(details: BillAccountDetailsEntity): Int = billDetailsDao.update(details)

    suspend fun deleteBillDetails(details: BillAccountDetailsEntity): Int = billDetailsDao.delete(details)

    suspend fun getBillDetailsByAccountId(accountId: Long): BillAccountDetailsEntity? = billDetailsDao.getByAccountId(accountId)

    fun getBillDetailsByAccountIdFlow(accountId: Long): Flow<BillAccountDetailsEntity?> = billDetailsDao.getByAccountIdFlow(accountId)

    fun getAllActiveBillAccounts(): Flow<List<AccountWithBillDetails>> = accountDao.getAllActiveBillAccounts()

    // Installment Plans
    suspend fun insertInstallmentPlan(plan: InstallmentPlanEntity): Long {
        val planId = installmentPlanDao.insert(plan)
        val existingTxs = transactionDao.getByInstallmentPlanDirect(planId)
        if (existingTxs.isEmpty()) {
            val tx = TransactionEntity(
                type = TransactionType.INSTALLMENT,
                amount = plan.totalPurchaseAmount,
                accountId = plan.accountId,
                installmentPlanId = planId,
                category = plan.category,
                title = plan.title,
                timestamp = plan.purchaseDate
            )
            transactionDao.insert(tx)
        }
        ensurePendingCyclesForAccount(plan.accountId)
        return planId
    }

    suspend fun insertInstallmentPlans(plans: List<InstallmentPlanEntity>): List<Long> {
        val ids = installmentPlanDao.insertAll(plans)
        plans.forEachIndexed { index, plan ->
            val planId = ids.getOrNull(index) ?: plan.id
            val existingTxs = transactionDao.getByInstallmentPlanDirect(planId)
            if (existingTxs.isEmpty()) {
                val tx = TransactionEntity(
                    type = TransactionType.INSTALLMENT,
                    amount = plan.totalPurchaseAmount,
                    accountId = plan.accountId,
                    installmentPlanId = planId,
                    category = plan.category,
                    title = plan.title,
                    timestamp = plan.purchaseDate
                )
                transactionDao.insert(tx)
            }
        }
        plans.map { it.accountId }.distinct().forEach { accId ->
            ensurePendingCyclesForAccount(accId)
        }
        return ids
    }

    suspend fun updateInstallmentPlan(plan: InstallmentPlanEntity): Int {
        val count = installmentPlanDao.update(plan)
        val linkedTxs = transactionDao.getByInstallmentPlanDirect(plan.id)
        val purchaseTx = linkedTxs.find { it.type == TransactionType.INSTALLMENT }
        if (purchaseTx != null && purchaseTx.amount != plan.totalPurchaseAmount) {
            transactionDao.update(purchaseTx.copy(amount = plan.totalPurchaseAmount))
        }
        ensurePendingCyclesForAccount(plan.accountId)
        return count
    }

    suspend fun deleteInstallmentPlan(plan: InstallmentPlanEntity): Int {
        transactionDao.deleteByInstallmentPlan(plan.id)
        val count = installmentPlanDao.delete(plan)
        ensurePendingCyclesForAccount(plan.accountId)
        return count
    }

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
            ensurePendingCyclesForAccount(plan.accountId)
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
    suspend fun getAllRecurringBillsDirect(): List<RecurringBillEntity> = recurringBillDao?.getAllDirect() ?: emptyList()

    suspend fun insertLoanDetailsList(detailsList: List<LoanAccountDetailsEntity>): List<Long> =
        loanDetailsDao.insertAll(detailsList)

    suspend fun insertSavingsDetailsList(detailsList: List<SavingsAccountDetailsEntity>): List<Long> =
        savingsDetailsDao.insertAll(detailsList)

    suspend fun insertBillDetailsList(detailsList: List<BillAccountDetailsEntity>): List<Long> =
        billDetailsDao.insertAll(detailsList)

    suspend fun insertCreditDetails(details: CreditAccountDetailsEntity): Long {
        val id = creditDetailsDao?.insert(details) ?: 0L
        ensurePendingCyclesForAccount(details.accountId)
        return id
    }

    suspend fun insertCreditDetailsList(detailsList: List<CreditAccountDetailsEntity>): List<Long> =
        creditDetailsDao?.insertAll(detailsList) ?: emptyList()

    suspend fun updateCreditDetails(details: CreditAccountDetailsEntity): Int =
        creditDetailsDao?.update(details) ?: 0

    suspend fun deleteCreditDetails(details: CreditAccountDetailsEntity): Int =
        creditDetailsDao?.delete(details) ?: 0

    fun getCreditDetails(accountId: Long): Flow<CreditAccountDetailsEntity?> =
        creditDetailsDao?.getByAccountIdFlow(accountId) ?: kotlinx.coroutines.flow.flowOf(null)

    suspend fun getCreditDetailsDirect(accountId: Long): CreditAccountDetailsEntity? =
        creditDetailsDao?.getByAccountId(accountId)

    val allCreditDetails: Flow<List<CreditAccountDetailsEntity>> =
        creditDetailsDao?.getAll() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun getAllCreditDetailsDirect(): List<CreditAccountDetailsEntity> =
        creditDetailsDao?.getAllDirect() ?: emptyList()

    suspend fun clearAllData() {
        // Order matters for Foreign Key constraints
        transactionDao.deleteAll()
        installmentPlanDao.deleteAll()
        recurringBillDao?.deleteAll()
        loanDetailsDao.deleteAll()
        savingsDetailsDao.deleteAll()
        billDetailsDao.deleteAll()
        creditDetailsDao?.deleteAll()
        accountDao.deleteAll()
    }

    // Billing Cycles (Single Source of Truth)
    fun getPendingBillingCycles(accountId: Long): Flow<List<LoanBillingCycleEntity>> =
        loanBillingCycleDao?.getPendingCycles(accountId) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getAllPendingBillingCycles(): Flow<List<LoanBillingCycleEntity>> =
        loanBillingCycleDao?.getAllPendingCycles() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getPaidBillingCycles(accountId: Long): Flow<List<LoanBillingCycleEntity>> =
        loanBillingCycleDao?.getPaidCycles(accountId) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getAllBillingCycles(accountId: Long): Flow<List<LoanBillingCycleEntity>> =
        loanBillingCycleDao?.getAllCycles(accountId) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertBillingCycle(cycle: LoanBillingCycleEntity): Long =
        loanBillingCycleDao?.insert(cycle) ?: 0L

    suspend fun updateBillingCycle(cycle: LoanBillingCycleEntity): Int =
        loanBillingCycleDao?.update(cycle) ?: 0

    suspend fun updateCycleAmountDue(cycleId: Long, amountDueCentavos: Long, isManualOverride: Boolean = true) {
        val cycle = loanBillingCycleDao?.getById(cycleId) ?: return
        loanBillingCycleDao.update(cycle.copy(amountDue = amountDueCentavos, isManualOverride = isManualOverride))
    }

    suspend fun deleteBillingCycle(cycle: LoanBillingCycleEntity): Int =
        loanBillingCycleDao?.delete(cycle) ?: 0

    suspend fun getPendingCyclesDirect(accountId: Long): List<LoanBillingCycleEntity> =
        loanBillingCycleDao?.getPendingCyclesDirect(accountId) ?: emptyList()

    suspend fun getBillingCycleById(cycleId: Long): LoanBillingCycleEntity? =
        loanBillingCycleDao?.getById(cycleId)

    suspend fun recordBillingCyclePayment(cycleId: Long, paymentAmount: Long): CyclePaymentResult {
        val cycle = loanBillingCycleDao?.getById(cycleId) ?: return CyclePaymentResult.NotFound(cycleId)
        val now = System.currentTimeMillis()
        val totalPaid = (cycle.paidAmount ?: 0L) + paymentAmount

        return if (paymentAmount < cycle.amountDue) {
            val remaining = cycle.amountDue - paymentAmount
            val updated = cycle.copy(
                amountDue = remaining,
                paidAmount = totalPaid,
                isPaid = false
            )
            loanBillingCycleDao?.update(updated)
            CyclePaymentResult.Partial(
                cycleId = cycleId,
                amountPaid = paymentAmount,
                remainingDue = remaining,
                dueDate = cycle.cycleDueDate
            )
        } else {
            val updated = cycle.copy(
                isPaid = true,
                paidDate = now,
                paidAmount = totalPaid
            )
            loanBillingCycleDao?.update(updated)

            val loanDetails = loanDetailsDao?.getByAccountId(cycle.accountId)
            if (loanDetails != null) {
                val newRemaining = maxOf(0L, loanDetails.totalRemainingBalance - paymentAmount)
                loanDetailsDao.update(loanDetails.copy(totalRemainingBalance = newRemaining))
            }

            ensurePendingCyclesForAccount(cycle.accountId)
            val overpayment = maxOf(0L, paymentAmount - cycle.amountDue)
            CyclePaymentResult.PaidInFull(
                cycleId = cycleId,
                amountPaid = paymentAmount,
                cycle = updated,
                overpaymentAmount = overpayment
            )
        }
    }

    suspend fun markBillingCyclePaid(cycleId: Long, amountPaid: Long) {
        recordBillingCyclePayment(cycleId, amountPaid)
    }

    suspend fun ensurePendingCyclesForAccount(
        accountId: Long,
        today: java.time.LocalDate = java.time.LocalDate.now()
    ) {
        val account = accountDao.getById(accountId) ?: return
        val dao = loanBillingCycleDao ?: return
        val zoneId = java.time.ZoneId.systemDefault()
        val currentYm = java.time.YearMonth.from(today)

        when (account.type) {
            AccountType.CREDIT -> {
                val creditDetails = creditDetailsDao?.getByAccountId(accountId) ?: return
                val currentBalance = accountDao.getAccountBalanceDirect(accountId) ?: 0L
                val pendingCycles = dao.getPendingCyclesDirect(accountId)
                if (currentBalance >= 0) {
                    // Card has no debt - clean up any existing pending cycle
                    pendingCycles.forEach { dao.delete(it) }
                    return
                }

                if (pendingCycles.isNotEmpty()) {
                    val cycle = pendingCycles.first()
                    if (!cycle.isManualOverride) {
                        val computedDue = -currentBalance
                        if (cycle.amountDue != computedDue) {
                            dao.update(cycle.copy(amountDue = computedDue))
                        }
                    }
                    return
                }

                val day = creditDetails.statementDueDay
                val paidCycles = dao.getPaidCyclesDirect(accountId)

                val clampedDay = day.coerceIn(1, currentYm.lengthOfMonth())
                val candidateDate = currentYm.atDay(clampedDay)
                val hasPaidCandidate = paidCycles.any {
                    val paidDate = java.time.Instant.ofEpochMilli(it.cycleDueDate).atZone(zoneId).toLocalDate()
                    paidDate.year == candidateDate.year && paidDate.month == candidateDate.month && paidDate.dayOfMonth == candidateDate.dayOfMonth
                }

                val dueDate = if (candidateDate.isBefore(today) || hasPaidCandidate) {
                    val nextYm = currentYm.plusMonths(1)
                    nextYm.atDay(day.coerceIn(1, nextYm.lengthOfMonth()))
                } else {
                    candidateDate
                }

                val defaultAmount = -currentBalance
                val dueEpoch = dueDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

                dao.insert(
                    LoanBillingCycleEntity(
                        accountId = accountId,
                        cycleDueDate = dueEpoch,
                        amountDue = defaultAmount,
                        isPaid = false,
                        isManualOverride = false
                    )
                )
            }
            AccountType.LOAN, AccountType.BNPL -> {
                val loanDetails = loanDetailsDao?.getByAccountId(accountId) ?: return
                val currentBalance = accountDao.getAccountBalanceDirect(accountId) ?: 0L
                var pendingCycles = dao.getPendingCyclesDirect(accountId)
                val activePlans = installmentPlanDao.getByAccountIdDirect(accountId).filter { it.remainingBalance > 0 }

                if (loanDetails.totalRemainingBalance <= 0 && currentBalance >= 0 && activePlans.isEmpty()) {
                    pendingCycles.forEach { dao.delete(it) }
                    return
                }

                // Compute default amount due: sum of active installment monthly payments if any exist; otherwise fallback to minimumAmountDue
                val computedDue = if (activePlans.isNotEmpty()) {
                    calculateActiveInstallmentsMonthlySum(activePlans)
                } else {
                    loanDetails.minimumAmountDue
                }

                val dueDays = loanDetails.parseDueDays()
                val paidCycles = dao.getPaidCyclesDirect(accountId)

                for (day in dueDays) {
                    val hasPendingForDay = pendingCycles.any {
                        val pDate = java.time.Instant.ofEpochMilli(it.cycleDueDate).atZone(zoneId).toLocalDate()
                        pDate.dayOfMonth == day || (day > 28 && pDate.dayOfMonth == pDate.lengthOfMonth())
                    }
                    if (hasPendingForDay) continue

                    val clampedDay = day.coerceIn(1, currentYm.lengthOfMonth())
                    val candidateDate = currentYm.atDay(clampedDay)
                    val hasPaidCandidate = paidCycles.any {
                        val paidDate = java.time.Instant.ofEpochMilli(it.cycleDueDate).atZone(zoneId).toLocalDate()
                        paidDate.year == candidateDate.year && paidDate.month == candidateDate.month && paidDate.dayOfMonth == candidateDate.dayOfMonth
                    }

                    val dueDate = if (candidateDate.isBefore(today) || hasPaidCandidate) {
                        val nextYm = currentYm.plusMonths(1)
                        nextYm.atDay(day.coerceIn(1, nextYm.lengthOfMonth()))
                    } else {
                        candidateDate
                    }

                    val dueEpoch = dueDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    dao.insert(
                        LoanBillingCycleEntity(
                            accountId = accountId,
                            cycleDueDate = dueEpoch,
                            amountDue = computedDue,
                            isPaid = false,
                            isManualOverride = false
                        )
                    )
                }

                // Update existing pending cycles that haven't been manually overridden
                pendingCycles = dao.getPendingCyclesDirect(accountId)
                for (cycle in pendingCycles) {
                    if (!cycle.isManualOverride && cycle.amountDue != computedDue) {
                        dao.update(cycle.copy(amountDue = computedDue))
                    }
                }
            }
            AccountType.BILL -> {
                val billDetails = billDetailsDao.getByAccountId(accountId) ?: return
                val dueDays = billDetails.parseDueDays()
                val pendingCycles = dao.getPendingCyclesDirect(accountId)
                val paidCycles = dao.getPaidCyclesDirect(accountId)

                for (day in dueDays) {
                    val hasPendingForDay = pendingCycles.any {
                        val pDate = java.time.Instant.ofEpochMilli(it.cycleDueDate).atZone(zoneId).toLocalDate()
                        pDate.dayOfMonth == day || (day > 28 && pDate.dayOfMonth == pDate.lengthOfMonth())
                    }
                    if (hasPendingForDay) continue

                    val clampedDay = day.coerceIn(1, currentYm.lengthOfMonth())
                    val candidateDate = currentYm.atDay(clampedDay)
                    val hasPaidCandidate = paidCycles.any {
                        val paidDate = java.time.Instant.ofEpochMilli(it.cycleDueDate).atZone(zoneId).toLocalDate()
                        paidDate.year == candidateDate.year && paidDate.month == candidateDate.month && paidDate.dayOfMonth == candidateDate.dayOfMonth
                    }

                    val dueDate = if (candidateDate.isBefore(today) || hasPaidCandidate) {
                        val nextYm = currentYm.plusMonths(1)
                        nextYm.atDay(day.coerceIn(1, nextYm.lengthOfMonth()))
                    } else {
                        candidateDate
                    }

                    val dueEpoch = dueDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    dao.insert(
                        LoanBillingCycleEntity(
                            accountId = accountId,
                            cycleDueDate = dueEpoch,
                            amountDue = billDetails.amountDue ?: 0L,
                            isPaid = false,
                            isManualOverride = false
                        )
                    )
                }
            }
            else -> {}
        }
    }

    suspend fun ensurePendingCyclesForAllAccounts(today: java.time.LocalDate = java.time.LocalDate.now()) {
        val activeAccounts = accountDao.getAllDirect().filter {
            it.isActive && (it.type == AccountType.CREDIT || it.type == AccountType.LOAN || it.type == AccountType.BNPL || it.type == AccountType.BILL)
        }
        for (acc in activeAccounts) {
            ensurePendingCyclesForAccount(acc.id, today)
        }
    }

    suspend fun ensurePendingCyclesForAccount(
        accountId: Long,
        dueDaysList: List<Int>,
        defaultAmountDue: Long = 0L,
        today: java.time.LocalDate = java.time.LocalDate.now()
    ) {
        val dao = loanBillingCycleDao ?: return
        val existingPending = dao.getPendingCyclesDirect(accountId)
        val paidCycles = dao.getPaidCyclesDirect(accountId)
        val currentYm = java.time.YearMonth.from(today)
        val zoneId = java.time.ZoneId.systemDefault()

        for (day in dueDaysList) {
            val clampedDay = day.coerceIn(1, currentYm.lengthOfMonth())
            val candidateDate = currentYm.atDay(clampedDay)
            val hasPaidCandidate = paidCycles.any {
                val paidDate = java.time.Instant.ofEpochMilli(it.cycleDueDate).atZone(zoneId).toLocalDate()
                paidDate.year == candidateDate.year && paidDate.month == candidateDate.month && paidDate.dayOfMonth == candidateDate.dayOfMonth
            }

            val dueDate = if (candidateDate.isBefore(today) || hasPaidCandidate) {
                val nextYm = currentYm.plusMonths(1)
                nextYm.atDay(day.coerceIn(1, nextYm.lengthOfMonth()))
            } else {
                candidateDate
            }
            val dueEpoch = dueDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val alreadyExists = existingPending.any {
                val existingDate = java.time.Instant.ofEpochMilli(it.cycleDueDate).atZone(zoneId).toLocalDate()
                existingDate.month == dueDate.month && existingDate.dayOfMonth == dueDate.dayOfMonth
            }
            if (!alreadyExists) {
                dao.insert(
                    LoanBillingCycleEntity(
                        accountId = accountId,
                        cycleDueDate = dueEpoch,
                        amountDue = defaultAmountDue,
                        isPaid = false,
                        isManualOverride = false
                    )
                )
            }
        }
    }

    // Recurring Bills
    val activeRecurringBills: Flow<List<RecurringBillEntity>> =
        recurringBillDao?.getAllActive() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertRecurringBill(bill: RecurringBillEntity): Long =
        recurringBillDao?.insert(bill) ?: 0L

    suspend fun updateRecurringBill(bill: RecurringBillEntity): Int =
        recurringBillDao?.update(bill) ?: 0

    suspend fun deleteRecurringBill(bill: RecurringBillEntity): Int =
        recurringBillDao?.delete(bill) ?: 0

    suspend fun getRecurringBillById(id: Long): RecurringBillEntity? =
        recurringBillDao?.getById(id)

    // Custom Categories
    fun getCustomCategories(type: TransactionType): Flow<List<CustomCategoryEntity>> =
        customCategoryDao?.getCategoriesByType(type) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertCustomCategory(category: CustomCategoryEntity): Long {
        val trimmedName = category.name.trim()
        if (trimmedName.isBlank()) {
            throw IllegalArgumentException("Category name cannot be empty")
        }
        val existing = customCategoryDao?.getCategoriesByTypeDirect(category.transactionType) ?: emptyList()
        val isDuplicate = existing.any { it.name.trim().equals(trimmedName, ignoreCase = true) }
        if (isDuplicate) {
            throw IllegalArgumentException("Category '$trimmedName' already exists for ${category.transactionType.name}")
        }
        return customCategoryDao?.insert(category.copy(name = trimmedName)) ?: 0L
    }

    companion object {
        fun calculateAvailableCredit(creditLimit: Long?, currentBalance: Long): Long? {
            return creditLimit?.let { it + currentBalance }
        }

        fun calculateInstallmentPlansTotal(plans: List<InstallmentPlanEntity>): Long {
            return plans.filter { it.remainingBalance > 0 }.sumOf { it.remainingBalance }
        }

        fun calculateActiveInstallmentsMonthlySum(plans: List<InstallmentPlanEntity>): Long {
            return plans.filter { it.remainingBalance > 0 }.sumOf { it.monthlyPaymentAmount }
        }
    }
}
