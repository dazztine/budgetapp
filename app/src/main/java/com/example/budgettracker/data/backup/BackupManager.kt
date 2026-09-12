package com.example.budgettracker.data.backup

import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.data.repository.BudgetRepository
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val version: Int = CURRENT_SCHEMA_VERSION,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity>,
    val loanDetails: List<LoanAccountDetailsEntity>,
    val installmentPlans: List<InstallmentPlanEntity> = emptyList(),
    val transactions: List<TransactionEntity>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

sealed class RestoreResult {
    data class Success(
        val accountsCount: Int,
        val loanDetailsCount: Int,
        val installmentPlansCount: Int,
        val transactionsCount: Int
    ) : RestoreResult()

    data class Error(val message: String) : RestoreResult()
}

object BackupManager {

    fun exportToJson(
        accounts: List<AccountEntity>,
        loanDetails: List<LoanAccountDetailsEntity>,
        installmentPlans: List<InstallmentPlanEntity>,
        transactions: List<TransactionEntity>
    ): String {
        val rootJson = JSONObject()
        rootJson.put("version", BackupData.CURRENT_SCHEMA_VERSION)
        rootJson.put("exportTimestamp", System.currentTimeMillis())

        val accountsArray = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("type", acc.type.name)
            obj.put("presetId", acc.presetId ?: JSONObject.NULL)
            obj.put("initialBalance", acc.initialBalance)
            obj.put("isActive", acc.isActive)
            obj.put("displayOrder", acc.displayOrder)
            obj.put("createdAt", acc.createdAt)
            obj.put("updatedAt", acc.updatedAt)
            accountsArray.put(obj)
        }
        rootJson.put("accounts", accountsArray)

        val loanDetailsArray = JSONArray()
        loanDetails.forEach { ld ->
            val obj = JSONObject()
            obj.put("accountId", ld.accountId)
            obj.put("cycleDay1", ld.cycleDay1)
            obj.put("cycleDay2", ld.cycleDay2 ?: JSONObject.NULL)
            obj.put("minimumAmountDue", ld.minimumAmountDue)
            obj.put("totalRemainingBalance", ld.totalRemainingBalance)
            obj.put("reminderEnabled", ld.reminderEnabled)
            obj.put("reminderDaysBefore", ld.reminderDaysBefore)
            loanDetailsArray.put(obj)
        }
        rootJson.put("loanDetails", loanDetailsArray)

        val installmentPlansArray = JSONArray()
        installmentPlans.forEach { ip ->
            val obj = JSONObject()
            obj.put("id", ip.id)
            obj.put("accountId", ip.accountId)
            obj.put("title", ip.title)
            obj.put("category", ip.category)
            obj.put("totalPurchaseAmount", ip.totalPurchaseAmount)
            obj.put("totalInstallments", ip.totalInstallments)
            obj.put("installmentsPaid", ip.installmentsPaid)
            obj.put("remainingBalance", ip.remainingBalance)
            obj.put("monthlyPaymentAmount", ip.monthlyPaymentAmount)
            obj.put("purchaseDate", ip.purchaseDate)
            obj.put("createdAt", ip.createdAt)
            obj.put("updatedAt", ip.updatedAt)
            installmentPlansArray.put(obj)
        }
        rootJson.put("installmentPlans", installmentPlansArray)

        val transactionsArray = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("accountId", tx.accountId)
            obj.put("toAccountId", tx.toAccountId ?: JSONObject.NULL)
            obj.put("type", tx.type.name)
            obj.put("amount", tx.amount)
            obj.put("isAdjustment", tx.isAdjustment)
            obj.put("category", tx.category)
            obj.put("title", tx.title)
            obj.put("note", tx.note ?: JSONObject.NULL)
            obj.put("timestamp", tx.timestamp)
            obj.put("createdAt", tx.createdAt)
            transactionsArray.put(obj)
        }
        rootJson.put("transactions", transactionsArray)

        return rootJson.toString(2)
    }

    fun importFromJson(jsonString: String): BackupData {
        val rootJson = JSONObject(jsonString)

        if (!rootJson.has("version")) {
            throw IllegalArgumentException("Invalid backup JSON: Missing 'version' field.")
        }

        val version = rootJson.getInt("version")
        if (version < 1 || version > BackupData.CURRENT_SCHEMA_VERSION) {
            throw IllegalArgumentException("Unsupported backup version: $version. Expected version up to ${BackupData.CURRENT_SCHEMA_VERSION}.")
        }

        val exportTimestamp = rootJson.optLong("exportTimestamp", System.currentTimeMillis())

        // Accounts
        val accounts = mutableListOf<AccountEntity>()
        val accountsArray = rootJson.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until accountsArray.length()) {
            val obj = accountsArray.getJSONObject(i)
            accounts.add(
                AccountEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    type = AccountType.valueOf(obj.getString("type")),
                    presetId = if (obj.isNull("presetId")) null else obj.getString("presetId"),
                    initialBalance = obj.getLong("initialBalance"),
                    isActive = obj.getBoolean("isActive"),
                    displayOrder = obj.getInt("displayOrder"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // Loan Details
        val loanDetails = mutableListOf<LoanAccountDetailsEntity>()
        val loanDetailsArray = rootJson.optJSONArray("loanDetails") ?: JSONArray()
        for (i in 0 until loanDetailsArray.length()) {
            val obj = loanDetailsArray.getJSONObject(i)
            loanDetails.add(
                LoanAccountDetailsEntity(
                    accountId = obj.getLong("accountId"),
                    cycleDay1 = obj.getInt("cycleDay1"),
                    cycleDay2 = if (obj.isNull("cycleDay2")) null else obj.getInt("cycleDay2"),
                    minimumAmountDue = obj.optLong("minimumAmountDue", 0L),
                    totalRemainingBalance = obj.optLong("totalRemainingBalance", 0L),
                    reminderEnabled = obj.optBoolean("reminderEnabled", true),
                    reminderDaysBefore = obj.optInt("reminderDaysBefore", 3)
                )
            )
        }

        // Installment Plans
        val installmentPlans = mutableListOf<InstallmentPlanEntity>()
        val installmentPlansArray = rootJson.optJSONArray("installmentPlans") ?: JSONArray()
        for (i in 0 until installmentPlansArray.length()) {
            val obj = installmentPlansArray.getJSONObject(i)
            installmentPlans.add(
                InstallmentPlanEntity(
                    id = obj.getLong("id"),
                    accountId = obj.getLong("accountId"),
                    title = obj.getString("title"),
                    category = obj.getString("category"),
                    totalPurchaseAmount = obj.getLong("totalPurchaseAmount"),
                    totalInstallments = obj.getInt("totalInstallments"),
                    installmentsPaid = obj.optInt("installmentsPaid", 0),
                    remainingBalance = obj.getLong("remainingBalance"),
                    monthlyPaymentAmount = obj.getLong("monthlyPaymentAmount"),
                    purchaseDate = obj.optLong("purchaseDate", System.currentTimeMillis()),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // Transactions
        val transactions = mutableListOf<TransactionEntity>()
        val transactionsArray = rootJson.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until transactionsArray.length()) {
            val obj = transactionsArray.getJSONObject(i)
            val typeStr = obj.getString("type")
            // Backward compatibility for v1 ADJUSTMENT type
            val (type, isAdjustment) = if (typeStr == "ADJUSTMENT") {
                val dir = obj.optString("adjustmentDirection", "INCREASE")
                val t = if (dir == "DECREASE") TransactionType.EXPENSE else TransactionType.INCOME
                Pair(t, true)
            } else {
                Pair(TransactionType.valueOf(typeStr), obj.optBoolean("isAdjustment", false))
            }

            transactions.add(
                TransactionEntity(
                    id = obj.getLong("id"),
                    accountId = obj.getLong("accountId"),
                    toAccountId = if (obj.isNull("toAccountId")) null else obj.getLong("toAccountId"),
                    type = type,
                    amount = obj.getLong("amount"),
                    isAdjustment = isAdjustment,
                    category = obj.getString("category"),
                    title = obj.getString("title"),
                    note = if (obj.isNull("note")) null else obj.getString("note"),
                    timestamp = obj.getLong("timestamp"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        return BackupData(
            version = version,
            exportTimestamp = exportTimestamp,
            accounts = accounts,
            loanDetails = loanDetails,
            installmentPlans = installmentPlans,
            transactions = transactions
        )
    }

    suspend fun performRestore(
        repository: BudgetRepository,
        backupData: BackupData
    ): RestoreResult {
        return try {
            repository.clearAllData()
            if (backupData.accounts.isNotEmpty()) {
                repository.insertAccounts(backupData.accounts)
            }
            if (backupData.loanDetails.isNotEmpty()) {
                repository.insertLoanDetailsList(backupData.loanDetails)
            }
            if (backupData.installmentPlans.isNotEmpty()) {
                repository.insertInstallmentPlans(backupData.installmentPlans)
            }
            if (backupData.transactions.isNotEmpty()) {
                repository.insertTransactions(backupData.transactions)
            }

            RestoreResult.Success(
                accountsCount = backupData.accounts.size,
                loanDetailsCount = backupData.loanDetails.size,
                installmentPlansCount = backupData.installmentPlans.size,
                transactionsCount = backupData.transactions.size
            )
        } catch (e: Exception) {
            RestoreResult.Error(e.message ?: "Failed to restore database backup.")
        }
    }
}
