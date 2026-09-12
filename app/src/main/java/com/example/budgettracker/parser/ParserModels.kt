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
    val validationErrors: MutableList<String> = mutableListOf()
)

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
    val validationErrors: List<String> = emptyList()
)
