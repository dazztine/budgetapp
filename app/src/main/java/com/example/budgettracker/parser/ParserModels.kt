package com.example.budgettracker.parser

import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType

data class ParsedAccountSetup(
    var name: String,
    var type: AccountType,
    var presetId: String? = null,
    var initialBalanceCentavos: Long = 0L,
    var cycleDay1: Int? = null,
    var cycleDay2: Int? = null,
    var dueDays: String = "15",
    val validationErrors: MutableList<String> = mutableListOf()
) {
    fun resolveDueDays(): String {
        return if (cycleDay1 != null) {
            if (cycleDay2 != null) "$cycleDay1,$cycleDay2" else "$cycleDay1"
        } else {
            dueDays
        }
    }
}

data class BatchParseResult(
    val accounts: List<ParsedAccountSetup>,
    val totalBalanceCentavos: Long,
    val hasErrors: Boolean
)

data class ParsedTransaction(
    val type: TransactionType,
    val amountCentavos: Long,
    val accountName: String?,
    val toAccountName: String? = null,
    val category: String,
    val title: String,
    val totalInstallments: Int? = null,
    val validationErrors: List<String> = emptyList()
)
