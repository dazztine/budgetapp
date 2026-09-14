package com.example.budgettracker.data.export

import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun generateCsv(
        transactions: List<TransactionEntity>,
        accounts: List<AccountEntity>
    ): String {
        val accountMap = accounts.associateBy { it.id }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val sb = StringBuilder()
        sb.append("ID,Date,Type,IsAdjustment,Account,ToAccount,Category,Title,Amount,InstallmentPlanId,Note\n")

        for (tx in transactions) {
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val typeStr = tx.type.name
            val isAdjStr = if (tx.isAdjustment) "TRUE" else "FALSE"
            val accountName = accountMap[tx.accountId]?.name ?: "Account #${tx.accountId}"
            val toAccountName = tx.toAccountId?.let { id -> accountMap[id]?.name ?: "Account #$id" } ?: ""
            val category = escapeCsv(tx.category)
            val title = escapeCsv(tx.title)
            val amountFormatted = String.format(Locale.US, "%.2f", tx.amount / 100.0)
            val installmentPlanIdStr = tx.installmentPlanId?.toString() ?: ""
            val note = escapeCsv(tx.note ?: "")

            sb.append("${tx.id},$dateStr,$typeStr,$isAdjStr,\"$accountName\",\"$toAccountName\",\"$category\",\"$title\",$amountFormatted,$installmentPlanIdStr,\"$note\"\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
