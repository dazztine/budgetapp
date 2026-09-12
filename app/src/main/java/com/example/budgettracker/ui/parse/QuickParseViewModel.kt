package com.example.budgettracker.ui.parse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.parser.BatchAccountSetupParser
import com.example.budgettracker.parser.ParsedAccountSetup
import com.example.budgettracker.parser.ParsedTransaction
import com.example.budgettracker.parser.SingleTransactionParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ParseUiResult {
    object Idle : ParseUiResult()
    data class SingleTransaction(val parsed: ParsedTransaction) : ParseUiResult()
    data class BatchAccounts(val accounts: List<ParsedAccountSetup>) : ParseUiResult()
    data class Error(val message: String) : ParseUiResult()
    object Success : ParseUiResult()
}

class QuickParseViewModel(
    private val repository: BudgetRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    val activeAccounts: StateFlow<List<AccountEntity>> = repository.activeAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _parseResult = MutableStateFlow<ParseUiResult>(ParseUiResult.Idle)
    val parseResult: StateFlow<ParseUiResult> = _parseResult.asStateFlow()

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun parseText() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) {
            _parseResult.value = ParseUiResult.Error("Please enter or paste text to parse")
            return
        }

        val accountsList = activeAccounts.value

        // Check if multi-account setup text or setup expression (e.g. "meron akong 200 sa maribank")
        val batchResult = BatchAccountSetupParser.parse(text)
        val isSetup = BatchAccountSetupParser.isAccountSetupExpression(text)
        if (batchResult.accounts.isNotEmpty() && (accountsList.isEmpty() || batchResult.accounts.size > 1 || isSetup)) {
            _parseResult.value = ParseUiResult.BatchAccounts(batchResult.accounts)
            return
        }

        // Single transaction parse
        val singleResult = SingleTransactionParser.parse(text, accountsList)
        if (singleResult.amountCentavos > 0L) {
            _parseResult.value = ParseUiResult.SingleTransaction(singleResult)
        } else {
            _parseResult.value = ParseUiResult.Error("Could not extract a valid amount from text")
        }
    }

    fun confirmAndSaveSingleTransaction(
        parsedTx: ParsedTransaction,
        selectedAccountId: Long?,
        selectedToAccountId: Long? = null,
        onSuccess: () -> Unit = {}
    ) {
        if (selectedAccountId == null) {
            _parseResult.value = ParseUiResult.Error("Please select an account for this transaction")
            return
        }

        val transaction = TransactionEntity(
            type = parsedTx.type,
            amount = parsedTx.amountCentavos,
            accountId = selectedAccountId,
            toAccountId = selectedToAccountId,
            category = parsedTx.category,
            title = parsedTx.title,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch(ioDispatcher) {
            try {
                repository.insertTransaction(transaction)
                _parseResult.value = ParseUiResult.Success
                _inputText.value = ""
                onSuccess()
            } catch (e: Exception) {
                _parseResult.value = ParseUiResult.Error(e.message ?: "Failed to save transaction")
            }
        }
    }

    fun confirmAndSaveBatchAccounts(
        accountsToSave: List<ParsedAccountSetup>,
        onSuccess: () -> Unit = {}
    ) {
        if (accountsToSave.isEmpty()) {
            _parseResult.value = ParseUiResult.Error("No accounts to save")
            return
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                accountsToSave.forEach { acc ->
                    val accountEntity = AccountEntity(
                        name = acc.name,
                        type = acc.type,
                        presetId = acc.presetId,
                        initialBalance = acc.initialBalanceCentavos
                    )
                    val newAccountId = repository.insertAccount(accountEntity)

                    val day1 = acc.cycleDay1
                    if (day1 != null) {
                        val loanDetails = LoanAccountDetailsEntity(
                            accountId = newAccountId,
                            cycleDay1 = day1,
                            cycleDay2 = acc.cycleDay2,
                            minimumAmountDue = 0L,
                            totalRemainingBalance = acc.initialBalanceCentavos
                        )
                        repository.insertLoanDetails(loanDetails)
                    }
                }
                _parseResult.value = ParseUiResult.Success
                _inputText.value = ""
                onSuccess()
            } catch (e: Exception) {
                _parseResult.value = ParseUiResult.Error(e.message ?: "Failed to save accounts")
            }
        }
    }

    fun resetParseResult() {
        _parseResult.value = ParseUiResult.Idle
    }
}
