package com.example.budgettracker.domain

import com.example.budgettracker.data.local.entity.BudgetEntity
import com.example.budgettracker.data.local.entity.CustomCategoryEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Status indicator for per-category pacing.
 */
enum class CategoryBudgetStatus {
    OK,
    NEAR_LIMIT, // >= 80% spent
    OVER
}

/**
 * Calculation breakdown for an individual category's budget.
 *
 * @param categoryKey The raw category identifier stored in the database.
 * @param displayName The user-facing category name, defensively resolved if custom category was deleted/renamed.
 * @param budgetAmount Target budget in centavos (Long).
 * @param spentAmount Amount spent in this category so far in centavos (Long).
 * @param remainingCentavos Remaining budget (budgetAmount - spentAmount) in centavos. May be negative if overspent.
 * @param isOverBudget True if spentAmount > budgetAmount.
 * @param overBy Centavos spent exceeding budget (spentAmount - budgetAmount), or 0L if within budget.
 * @param dailyPace Suggested daily spending rate in centavos for the remainder of the month.
 * @param status Health status: OVER, NEAR_LIMIT (>= 80%), or OK.
 */
data class CategoryPacing(
    val categoryKey: String,
    val displayName: String,
    val budgetAmount: Long,
    val spentAmount: Long,
    val remainingCentavos: Long,
    val isOverBudget: Boolean,
    val overBy: Long,
    val dailyPace: Long,
    val status: CategoryBudgetStatus
)

/**
 * Result data class for monthly budget pacing analysis.
 * All monetary amounts are integer centavos (₱1.00 = 100L).
 *
 * Status thresholds:
 * - OK: spent < 80% of budget (or no budget set)
 * - NEAR_LIMIT: spent >= 80% and spent <= 100% of budget. Exactly 100% is NEAR_LIMIT (budget fully utilized, 0 remaining, not exceeded).
 * - OVER: spent > 100% of budget (isOverBudget = true, over by >= 1 centavo).
 */
data class BudgetPaceResult(
    val totalBudget: Long?,
    val totalSpent: Long,
    val leftThisMonth: Long?,
    val isOverBudget: Boolean,
    val overBy: Long,
    val daysLeft: Int,
    val dailyAllowance: Long,
    val remainderCentavos: Long,
    val categoryPacings: List<CategoryPacing>,
    val uncategorizedSpent: Long,
    val status: CategoryBudgetStatus = when {
        isOverBudget -> CategoryBudgetStatus.OVER
        totalBudget != null && totalBudget > 0L && (totalSpent * 100L) >= (totalBudget * 80L) -> CategoryBudgetStatus.NEAR_LIMIT
        else -> CategoryBudgetStatus.OK
    }
)

/**
 * Pure Kotlin calculator for budget pacing and daily allowance forecasting.
 * Contains zero Android dependencies and operates exclusively in integer centavos (Long).
 */
object BudgetPaceCalculator {

    val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Manila")

    /**
     * Single source of truth for qualifying a transaction as a budget expense.
     * Aligned exactly with ReportsAnalyticsCalculator:
     * - EXPENSE transactions
     * - INSTALLMENT transactions
     * - TRANSFER transactions where destination account is in [billAccountIds]
     *
     * Explicitly excludes asset-to-asset transfers and adjustment transactions (isAdjustment = true).
     */
    const val CANONICAL_BILLS_CATEGORY = "Bills & Utilities"
    const val FALLBACK_CATEGORY = "Uncategorized"

    /**
     * Single source of truth for whether a transaction counts as spending/expense.
     * Reused by both BudgetPaceCalculator and ReportsAnalyticsCalculator.
     *
     * Rules:
     * - Adjustments (isAdjustment = true) are NEVER spending.
     * - EXPENSE and INSTALLMENT transactions are ALWAYS spending.
     * - TRANSFER is ONLY spending if its destination is a BILL account (pass-through bill payment).
     * - TRANSFER to CREDIT (credit card bill), LOAN, SAVINGS, or other liquid accounts is NOT spending.
     * - INCOME is NOT spending.
     */
    fun isExpenseTransaction(tx: TransactionEntity, billAccountIds: Set<Long> = emptySet()): Boolean {
        if (tx.isAdjustment) return false
        return tx.type == TransactionType.EXPENSE ||
                tx.type == TransactionType.INSTALLMENT ||
                (tx.type == TransactionType.TRANSFER && tx.toAccountId != null && tx.toAccountId in billAccountIds)
    }

    fun isExpenseTransaction(tx: TransactionEntity, destinationAccountType: AccountType?): Boolean {
        if (tx.isAdjustment) return false
        return when (tx.type) {
            TransactionType.EXPENSE, TransactionType.INSTALLMENT -> true
            TransactionType.TRANSFER -> destinationAccountType == AccountType.BILL
            else -> false
        }
    }

    /**
     * Canonical category mapper for spending transactions.
     *
     * Rules:
     * - TRANSFER to a BILL account with a sentinel category ("Transfer", "Bill Payment", or blank)
     *   maps to the canonical key [CANONICAL_BILLS_CATEGORY] ("Bills & Utilities").
     * - Any non-sentinel category on a bill payment (e.g. "Utilities") is returned unchanged.
     * - For any other transaction, returns trimmed category if non-empty, otherwise [FALLBACK_CATEGORY] ("Uncategorized").
     */
    fun effectiveSpendingCategory(
        tx: TransactionEntity,
        destinationAccountType: AccountType? = null
    ): String {
        val trimmed = tx.category.trim()
        val isSentinel = trimmed.isEmpty() ||
                trimmed.equals("Transfer", ignoreCase = true) ||
                trimmed.equals("Bill Payment", ignoreCase = true)

        if (tx.type == TransactionType.TRANSFER &&
            destinationAccountType == AccountType.BILL &&
            isSentinel
        ) {
            return CANONICAL_BILLS_CATEGORY
        }
        return trimmed.ifEmpty { FALLBACK_CATEGORY }
    }

    fun effectiveSpendingCategory(
        tx: TransactionEntity,
        billAccountIds: Set<Long>
    ): String {
        val isBillDestination = tx.toAccountId != null && tx.toAccountId in billAccountIds
        return effectiveSpendingCategory(
            tx,
            if (isBillDestination) AccountType.BILL else null
        )
    }

    /**
     * Normalizes a raw category string from a transaction, grouping null or whitespace-only
     * strings under [FALLBACK_CATEGORY].
     */
    fun normalizeCategory(raw: String?): String {
        return raw?.trim()?.ifEmpty { FALLBACK_CATEGORY } ?: FALLBACK_CATEGORY
    }

    /**
     * Defensively resolves display name for a category key.
     * If a custom category was deleted, renamed, or is unrecognized, it falls back
     * cleanly to [categoryKey] instead of throwing or dropping the row.
     */
    fun resolveCategoryDisplayName(
        categoryKey: String,
        customCategories: List<CustomCategoryEntity> = emptyList()
    ): String {
        val trimmed = categoryKey.trim()
        if (trimmed.isEmpty() || trimmed.equals(FALLBACK_CATEGORY, ignoreCase = true)) {
            return FALLBACK_CATEGORY
        }
        val customMatch = customCategories.find { it.name.equals(trimmed, ignoreCase = true) }
        return customMatch?.name ?: trimmed
    }

    /**
     * Calculates the budget pace and daily allowance.
     *
     * @param monthlyBudget Overall monthly budget in centavos (null if not set).
     * @param transactions All transactions for the evaluation month.
     * @param today The reference date to evaluate against (defaults to current date in Manila zone).
     * @param billAccountIds Account IDs of BILL accounts (for identifying bill payment transfers).
     * @param categoryBudgets Optional per-category budget entities.
     * @param customCategories Optional list of custom categories for defensive name resolution.
     */
    fun calculate(
        monthlyBudget: Long?,
        transactions: List<TransactionEntity>,
        today: LocalDate,
        billAccountIds: Set<Long> = emptySet(),
        categoryBudgets: List<BudgetEntity> = emptyList(),
        customCategories: List<CustomCategoryEntity> = emptyList()
    ): BudgetPaceResult {
        // Filter strictly qualified expense transactions
        val qualifiedExpenses = transactions.filter { isExpenseTransaction(it, billAccountIds) }
        val totalSpent = qualifiedExpenses.sumOf { it.amount }

        // Calendar days calculation: today inclusive
        val yearMonth = YearMonth.from(today)
        val lengthOfMonth = yearMonth.lengthOfMonth()
        val dayOfMonth = today.dayOfMonth
        val daysLeft = (lengthOfMonth - dayOfMonth + 1).coerceAtLeast(1)

        // Overall budget metrics
        val leftThisMonth = monthlyBudget?.let { it - totalSpent }
        val isOverBudget = if (monthlyBudget != null) totalSpent > monthlyBudget else false
        val overBy = if (monthlyBudget != null && totalSpent > monthlyBudget) totalSpent - monthlyBudget else 0L

        // Daily allowance with integer floor division and documented remainder
        val positiveRemaining = (leftThisMonth ?: 0L).coerceAtLeast(0L)
        val dailyAllowance = if (monthlyBudget != null && daysLeft > 0) {
            positiveRemaining / daysLeft.toLong()
        } else {
            0L
        }
        val remainderCentavos = if (monthlyBudget != null && daysLeft > 0) {
            positiveRemaining % daysLeft.toLong()
        } else {
            0L
        }

        // Group spending by canonical effective category key
        val spendingByCategory = qualifiedExpenses.groupBy { effectiveSpendingCategory(it, billAccountIds) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val uncategorizedSpent = spendingByCategory[FALLBACK_CATEGORY] ?: 0L

        // Calculate pacing for each configured category budget
        val categoryPacings = categoryBudgets
            .filter { it.category != null }
            .map { budget ->
                val catKey = budget.category!!.trim()
                val spent = spendingByCategory[catKey] ?: 0L
                val remaining = budget.amount - spent
                val catOver = spent > budget.amount
                val catOverBy = if (catOver) spent - budget.amount else 0L

                val catPositiveRemaining = remaining.coerceAtLeast(0L)
                val catDailyPace = if (daysLeft > 0) catPositiveRemaining / daysLeft.toLong() else 0L

                // Status threshold: OVER if spent > budget; NEAR_LIMIT if spent >= 80% of budget; else OK
                val status = when {
                    catOver -> CategoryBudgetStatus.OVER
                    budget.amount > 0L && (spent * 100L) >= (budget.amount * 80L) -> CategoryBudgetStatus.NEAR_LIMIT
                    else -> CategoryBudgetStatus.OK
                }

                val displayName = resolveCategoryDisplayName(catKey, customCategories)

                CategoryPacing(
                    categoryKey = catKey,
                    displayName = displayName,
                    budgetAmount = budget.amount,
                    spentAmount = spent,
                    remainingCentavos = remaining,
                    isOverBudget = catOver,
                    overBy = catOverBy,
                    dailyPace = catDailyPace,
                    status = status
                )
            }

        return BudgetPaceResult(
            totalBudget = monthlyBudget,
            totalSpent = totalSpent,
            leftThisMonth = leftThisMonth,
            isOverBudget = isOverBudget,
            overBy = overBy,
            daysLeft = daysLeft,
            dailyAllowance = dailyAllowance,
            remainderCentavos = remainderCentavos,
            categoryPacings = categoryPacings,
            uncategorizedSpent = uncategorizedSpent
        )
    }
}
