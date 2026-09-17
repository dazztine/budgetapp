package com.example.budgettracker.data.backup

import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import com.example.budgettracker.data.local.entity.BillAmountType
import com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity
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
    val savingsDetails: List<SavingsAccountDetailsEntity> = emptyList(),
    val billDetails: List<BillAccountDetailsEntity> = emptyList(),
    val creditDetails: List<CreditAccountDetailsEntity> = emptyList(),
    val installmentPlans: List<InstallmentPlanEntity> = emptyList(),
    val transactions: List<TransactionEntity>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 5
    }
}

sealed class RestoreResult {
    data class Success(
        val accountsCount: Int,
        val loanDetailsCount: Int,
        val savingsDetailsCount: Int = 0,
        val billDetailsCount: Int = 0,
        val creditDetailsCount: Int = 0,
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
        transactions: List<TransactionEntity>,
        savingsDetails: List<SavingsAccountDetailsEntity> = emptyList(),
        billDetails: List<BillAccountDetailsEntity> = emptyList(),
        creditDetails: List<CreditAccountDetailsEntity> = emptyList()
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
            obj.put("includeInNetWorth", acc.includeInNetWorth)
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
            obj.put("dueDays", ld.dueDays)
            obj.put("minimumAmountDue", ld.minimumAmountDue)
            obj.put("totalRemainingBalance", ld.totalRemainingBalance)
            obj.put("reminderEnabled", ld.reminderEnabled)
            obj.put("reminderDaysBefore", ld.reminderDaysBefore)
            obj.put("creditLimit", ld.creditLimit ?: JSONObject.NULL)
            loanDetailsArray.put(obj)
        }
        rootJson.put("loanDetails", loanDetailsArray)

        val savingsDetailsArray = JSONArray()
        savingsDetails.forEach { sd ->
            val obj = JSONObject()
            obj.put("accountId", sd.accountId)
            obj.put("interestRate", sd.interestRate ?: JSONObject.NULL)
            obj.put("goalAmount", sd.goalAmount ?: JSONObject.NULL)
            obj.put("createdAt", sd.createdAt)
            obj.put("updatedAt", sd.updatedAt)
            savingsDetailsArray.put(obj)
        }
        rootJson.put("savingsDetails", savingsDetailsArray)

        val billDetailsArray = JSONArray()
        billDetails.forEach { bd ->
            val obj = JSONObject()
            obj.put("accountId", bd.accountId)
            obj.put("dueDays", bd.dueDays)
            obj.put("amountDue", bd.amountDue ?: JSONObject.NULL)
            obj.put("amountType", bd.amountType.name)
            obj.put("createdAt", bd.createdAt)
            obj.put("updatedAt", bd.updatedAt)
            billDetailsArray.put(obj)
        }
        rootJson.put("billDetails", billDetailsArray)

        val creditDetailsArray = JSONArray()
        creditDetails.forEach { cd ->
            val obj = JSONObject()
            obj.put("accountId", cd.accountId)
            obj.put("creditLimit", cd.creditLimit)
            obj.put("statementDueDay", cd.statementDueDay)
            obj.put("createdAt", cd.createdAt)
            obj.put("updatedAt", cd.updatedAt)
            creditDetailsArray.put(obj)
        }
        rootJson.put("creditDetails", creditDetailsArray)

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
            obj.put("installmentPlanId", tx.installmentPlanId ?: JSONObject.NULL)
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
            val type = AccountType.valueOf(obj.getString("type"))
            val defaultInclude = (type != AccountType.BILL)
            val includeInNetWorth = obj.optBoolean("includeInNetWorth", defaultInclude)
            accounts.add(
                AccountEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    type = type,
                    presetId = if (obj.isNull("presetId")) null else obj.getString("presetId"),
                    initialBalance = obj.getLong("initialBalance"),
                    isActive = obj.getBoolean("isActive"),
                    includeInNetWorth = includeInNetWorth,
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
            val dueDays = if (obj.has("dueDays")) {
                obj.getString("dueDays")
            } else {
                val day1 = obj.optInt("cycleDay1", 15)
                val day2 = if (obj.isNull("cycleDay2")) null else obj.optInt("cycleDay2")
                if (day2 != null) "$day1,$day2" else "$day1"
            }
            loanDetails.add(
                LoanAccountDetailsEntity(
                    accountId = obj.getLong("accountId"),
                    dueDays = dueDays,
                    minimumAmountDue = obj.optLong("minimumAmountDue", 0L),
                    totalRemainingBalance = obj.optLong("totalRemainingBalance", 0L),
                    reminderEnabled = obj.optBoolean("reminderEnabled", true),
                    reminderDaysBefore = obj.optInt("reminderDaysBefore", 3),
                    creditLimit = if (obj.isNull("creditLimit")) null else obj.optLong("creditLimit")
                )
            )
        }

        // Savings Details
        val savingsDetails = mutableListOf<SavingsAccountDetailsEntity>()
        val savingsDetailsArray = rootJson.optJSONArray("savingsDetails") ?: JSONArray()
        for (i in 0 until savingsDetailsArray.length()) {
            val obj = savingsDetailsArray.getJSONObject(i)
            savingsDetails.add(
                SavingsAccountDetailsEntity(
                    accountId = obj.getLong("accountId"),
                    interestRate = if (obj.isNull("interestRate")) null else obj.getDouble("interestRate"),
                    goalAmount = if (obj.isNull("goalAmount")) null else obj.getLong("goalAmount"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // Bill Details
        val billDetails = mutableListOf<BillAccountDetailsEntity>()
        val billDetailsArray = rootJson.optJSONArray("billDetails") ?: JSONArray()
        for (i in 0 until billDetailsArray.length()) {
            val obj = billDetailsArray.getJSONObject(i)
            val dueDays = if (obj.has("dueDays")) {
                obj.getString("dueDays")
            } else {
                obj.optInt("dueDay", 1).toString()
            }
            billDetails.add(
                BillAccountDetailsEntity(
                    accountId = obj.getLong("accountId"),
                    dueDays = dueDays,
                    amountDue = if (obj.isNull("amountDue")) null else obj.getLong("amountDue"),
                    amountType = if (obj.has("amountType")) BillAmountType.valueOf(obj.getString("amountType")) else BillAmountType.FIXED,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // Credit Details
        val creditDetails = mutableListOf<CreditAccountDetailsEntity>()
        val creditDetailsArray = rootJson.optJSONArray("creditDetails") ?: JSONArray()
        for (i in 0 until creditDetailsArray.length()) {
            val obj = creditDetailsArray.getJSONObject(i)
            creditDetails.add(
                CreditAccountDetailsEntity(
                    accountId = obj.getLong("accountId"),
                    creditLimit = obj.getLong("creditLimit"),
                    statementDueDay = obj.getInt("statementDueDay"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
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
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    installmentPlanId = if (obj.isNull("installmentPlanId")) null else obj.getLong("installmentPlanId")
                )
            )
        }

        return BackupData(
            version = version,
            exportTimestamp = exportTimestamp,
            accounts = accounts,
            loanDetails = loanDetails,
            savingsDetails = savingsDetails,
            billDetails = billDetails,
            creditDetails = creditDetails,
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
            if (backupData.savingsDetails.isNotEmpty()) {
                repository.insertSavingsDetailsList(backupData.savingsDetails)
            }
            if (backupData.billDetails.isNotEmpty()) {
                repository.insertBillDetailsList(backupData.billDetails)
            }
            if (backupData.creditDetails.isNotEmpty()) {
                repository.insertCreditDetailsList(backupData.creditDetails)
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
                savingsDetailsCount = backupData.savingsDetails.size,
                billDetailsCount = backupData.billDetails.size,
                creditDetailsCount = backupData.creditDetails.size,
                installmentPlansCount = backupData.installmentPlans.size,
                transactionsCount = backupData.transactions.size
            )
        } catch (e: Exception) {
            RestoreResult.Error(e.message ?: "Failed to restore database backup.")
        }
    }
}
