package com.example.budgettracker.e2e

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.dao.AccountDao
import com.example.budgettracker.data.local.dao.LoanDetailsDao
import com.example.budgettracker.data.local.dao.MonthlyTotals
import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Opaque-box test harness for BudgetTracker.
 * Executes tests through domain boundaries and public contracts without reliance on UI view trees.
 */
class OpaqueTestHarness : Closeable {

    val database: AppDatabase
    val accountDao: AccountDao
    val transactionDao: TransactionDao
    val loanDetailsDao: LoanDetailsDao

    init {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountDao = database.accountDao()
        transactionDao = database.transactionDao()
        loanDetailsDao = database.loanDetailsDao()
    }

    override fun close() {
        database.close()
    }

    // --- Core Account Operations ---

    fun createAccount(
        name: String,
        type: AccountType,
        initialBalanceCentavos: Long = 0L,
        presetId: String? = null,
        displayOrder: Int = 0
    ): Long = runBlocking {
        accountDao.insert(
            AccountEntity(
                name = name,
                type = type,
                initialBalance = initialBalanceCentavos,
                presetId = presetId,
                displayOrder = displayOrder
            )
        )
    }

    fun getAccount(id: Long): AccountEntity? = runBlocking {
        accountDao.getById(id)
    }

    fun getAllActiveAccounts(): List<AccountEntity> = runBlocking {
        accountDao.getAllActive().first()
    }

    fun getActiveAccountCount(): Int = runBlocking {
        accountDao.getActiveAccountCount().first()
    }

    fun softDeleteAccount(id: Long): Int = runBlocking {
        accountDao.softDelete(id)
    }

    fun restoreAccount(id: Long): Int = runBlocking {
        accountDao.restoreAccount(id)
    }

    fun updateDisplayOrder(id: Long, order: Int): Int = runBlocking {
        accountDao.updateDisplayOrder(id, order)
    }

    // --- Core Transaction Operations ---

    fun recordExpense(
        accountId: Long,
        amountCentavos: Long,
        category: String,
        title: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long = runBlocking {
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = amountCentavos,
                accountId = accountId,
                category = category,
                title = title,
                timestamp = timestamp
            )
        )
    }

    fun recordIncome(
        accountId: Long,
        amountCentavos: Long,
        category: String,
        title: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long = runBlocking {
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = amountCentavos,
                accountId = accountId,
                category = category,
                title = title,
                timestamp = timestamp
            )
        )
    }

    fun recordTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amountCentavos: Long,
        title: String = "Fund Transfer",
        timestamp: Long = System.currentTimeMillis()
    ): Long = runBlocking {
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = amountCentavos,
                accountId = fromAccountId,
                toAccountId = toAccountId,
                category = "Transfer",
                title = title,
                timestamp = timestamp
            )
        )
    }

    fun recordAdjustment(
        accountId: Long,
        amountCentavos: Long,
        isIncrease: Boolean,
        title: String = "Adjustment",
        timestamp: Long = System.currentTimeMillis()
    ): Long = runBlocking {
        transactionDao.insert(
            TransactionEntity(
                type = if (isIncrease) TransactionType.INCOME else TransactionType.EXPENSE,
                isAdjustment = true,
                amount = amountCentavos,
                accountId = accountId,
                category = "Adjustment",
                title = title,
                timestamp = timestamp
            )
        )
    }

    // --- Live Balance Queries ---

    fun getAccountBalance(accountId: Long): Long = runBlocking {
        accountDao.getAccountBalance(accountId).first() ?: 0L
    }

    fun getTotalNetWorth(): Long = runBlocking {
        accountDao.getTotalNetWorth().first()
    }

    fun getMonthlyTotals(startTime: Long, endTime: Long): MonthlyTotals = runBlocking {
        transactionDao.getMonthlyTotals(startTime, endTime).first()
    }

    // --- Loan Operations ---

    fun setupLoanDetails(
        accountId: Long,
        cycleDay1: Int,
        cycleDay2: Int? = null,
        minimumDueCentavos: Long = 0L,
        totalRemainingCentavos: Long = 0L,
        reminderDaysBefore: Int = 7
    ) = runBlocking {
        loanDetailsDao.insert(
            LoanAccountDetailsEntity(
                accountId = accountId,
                cycleDay1 = cycleDay1,
                cycleDay2 = cycleDay2,
                minimumAmountDue = minimumDueCentavos,
                totalRemainingBalance = totalRemainingCentavos,
                reminderDaysBefore = reminderDaysBefore
            )
        )
    }

    fun getLoanDetails(accountId: Long): LoanAccountDetailsEntity? = runBlocking {
        loanDetailsDao.getByAccountId(accountId)
    }

    // --- Reference Model & Oracles for Parsers (R2) & Export/Import (R5) ---

    object LevenshteinOracle {
        fun computeDistance(s1: String, s2: String): Int =
            com.example.budgettracker.parser.LevenshteinMatcher.computeDistance(s1, s2)

        fun computeSimilarity(s1: String, s2: String): Float =
            com.example.budgettracker.parser.LevenshteinMatcher.computeSimilarity(s1, s2)

        fun isFuzzyMatch(inputToken: String, targetToken: String): Boolean =
            com.example.budgettracker.parser.LevenshteinMatcher.isFuzzyMatch(inputToken, targetToken)
    }

    data class ParsedAccountSetupDto(
        var name: String,
        var type: AccountType,
        var presetId: String?,
        var initialBalanceCentavos: Long,
        var cycleDay1: Int? = null,
        var cycleDay2: Int? = null,
        val validationErrors: MutableList<String> = mutableListOf()
    )

    data class BatchParseResultDto(
        val accounts: List<ParsedAccountSetupDto>,
        val totalBalanceCentavos: Long,
        val hasErrors: Boolean
    )

    data class ParsedTransactionDto(
        val type: TransactionType,
        val amountCentavos: Long,
        val accountName: String?,
        val toAccountName: String? = null,
        val category: String,
        val title: String,
        val validationErrors: List<String> = emptyList()
    )

    object BatchSetupParserOracle {
        fun parse(text: String): BatchParseResultDto {
            val result = com.example.budgettracker.parser.BatchAccountSetupParser.parse(text)
            val dtos = result.accounts.map {
                ParsedAccountSetupDto(
                    name = it.name,
                    type = it.type,
                    presetId = it.presetId,
                    initialBalanceCentavos = it.initialBalanceCentavos,
                    cycleDay1 = it.cycleDay1,
                    cycleDay2 = it.cycleDay2,
                    validationErrors = it.validationErrors
                )
            }
            return BatchParseResultDto(dtos, result.totalBalanceCentavos, result.hasErrors)
        }
    }

    object SingleTransactionParserOracle {
        fun parse(sentence: String, knownAccounts: List<AccountEntity>): ParsedTransactionDto {
            val result = com.example.budgettracker.parser.SingleTransactionParser.parse(sentence, knownAccounts)
            return ParsedTransactionDto(
                type = result.type,
                amountCentavos = result.amountCentavos,
                accountName = result.accountName,
                toAccountName = result.toAccountName,
                category = result.category,
                title = result.title,
                validationErrors = result.validationErrors
            )
        }
    }

    // --- JSON Snapshot & CSV Helpers ---

    fun exportJsonSnapshot(): String = runBlocking {
        val accounts = accountDao.getAll().first()
        val transactions = transactionDao.getAll().first()
        val root = JSONObject()

        val accArray = JSONArray()
        for (a in accounts) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("name", a.name)
            obj.put("type", a.type.name)
            obj.put("presetId", a.presetId ?: JSONObject.NULL)
            obj.put("initialBalance", a.initialBalance)
            obj.put("isActive", a.isActive)
            obj.put("displayOrder", a.displayOrder)
            obj.put("createdAt", a.createdAt)
            obj.put("updatedAt", a.updatedAt)

            val loan = loanDetailsDao.getByAccountId(a.id)
            if (loan != null) {
                val lObj = JSONObject()
                lObj.put("cycleDay1", loan.cycleDay1)
                lObj.put("cycleDay2", loan.cycleDay2 ?: JSONObject.NULL)
                lObj.put("minimumAmountDue", loan.minimumAmountDue)
                lObj.put("totalRemainingBalance", loan.totalRemainingBalance)
                lObj.put("reminderDaysBefore", loan.reminderDaysBefore)
                obj.put("loanDetails", lObj)
            }
            accArray.put(obj)
        }
        root.put("accounts", accArray)

        val txArray = JSONArray()
        for (t in transactions) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("type", t.type.name)
            obj.put("isAdjustment", t.isAdjustment)
            obj.put("amount", t.amount)
            obj.put("accountId", t.accountId)
            obj.put("toAccountId", t.toAccountId ?: JSONObject.NULL)
            obj.put("category", t.category)
            obj.put("title", t.title)
            obj.put("note", t.note ?: JSONObject.NULL)
            obj.put("timestamp", t.timestamp)
            obj.put("createdAt", t.createdAt)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        root.toString(2)
    }

    fun importJsonSnapshot(jsonString: String) = runBlocking {
        val root = JSONObject(jsonString)
        val accArray = root.getJSONArray("accounts")
        val txArray = root.getJSONArray("transactions")

        // Atomic insertion
        for (i in 0 until accArray.length()) {
            val aObj = accArray.getJSONObject(i)
            val acc = AccountEntity(
                id = aObj.getLong("id"),
                name = aObj.getString("name"),
                type = AccountType.valueOf(aObj.getString("type")),
                presetId = if (aObj.isNull("presetId")) null else aObj.getString("presetId"),
                initialBalance = aObj.getLong("initialBalance"),
                isActive = aObj.getBoolean("isActive"),
                displayOrder = aObj.getInt("displayOrder"),
                createdAt = aObj.getLong("createdAt"),
                updatedAt = aObj.getLong("updatedAt")
            )
            accountDao.insert(acc)

            if (aObj.has("loanDetails") && !aObj.isNull("loanDetails")) {
                val lObj = aObj.getJSONObject("loanDetails")
                val loan = LoanAccountDetailsEntity(
                    accountId = acc.id,
                    cycleDay1 = lObj.getInt("cycleDay1"),
                    cycleDay2 = if (lObj.isNull("cycleDay2")) null else lObj.getInt("cycleDay2"),
                    minimumAmountDue = lObj.getLong("minimumAmountDue"),
                    totalRemainingBalance = lObj.getLong("totalRemainingBalance"),
                    reminderDaysBefore = lObj.getInt("reminderDaysBefore")
                )
                loanDetailsDao.insert(loan)
            }
        }

        for (i in 0 until txArray.length()) {
            val tObj = txArray.getJSONObject(i)
            val tx = TransactionEntity(
                id = tObj.getLong("id"),
                type = TransactionType.valueOf(tObj.getString("type")),
                isAdjustment = tObj.optBoolean("isAdjustment", false),
                amount = tObj.getLong("amount"),
                accountId = tObj.getLong("accountId"),
                toAccountId = if (tObj.isNull("toAccountId")) null else tObj.getLong("toAccountId"),
                category = tObj.getString("category"),
                title = tObj.getString("title"),
                note = if (tObj.isNull("note")) null else tObj.getString("note"),
                timestamp = tObj.getLong("timestamp"),
                createdAt = tObj.getLong("createdAt")
            )
            transactionDao.insert(tx)
        }
    }

    fun exportCsv(): String = runBlocking {
        val txs = transactionDao.getAll().first()
        val accounts = accountDao.getAll().first().associateBy { it.id }
        val sb = StringBuilder()
        sb.appendLine("Date,Type,Amount,Account,ToAccount,Category,Title")
        val dtf = DateTimeFormatter.ISO_LOCAL_DATE

        for (t in txs) {
            val dateStr = LocalDate.ofEpochDay(t.timestamp / (86400 * 1000)).format(dtf)
            val decimalAmt = String.format(java.util.Locale.US, "%.2f", t.amount / 100.0)
            val accName = accounts[t.accountId]?.name ?: "Unknown"
            val toAccName = t.toAccountId?.let { accounts[it]?.name } ?: ""
            sb.appendLine("$dateStr,${t.type.name},$decimalAmt,\"$accName\",\"$toAccName\",\"${t.category}\",\"${t.title}\"")
        }
        sb.toString()
    }
}
