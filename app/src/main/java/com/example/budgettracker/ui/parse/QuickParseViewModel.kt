package com.example.budgettracker.ui.parse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
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

        // Multi-account setup is ONLY triggered if explicitly multiple accounts (>1) were found with balances
        val batchResult = BatchAccountSetupParser.parse(text)
        if (batchResult.accounts.size > 1) {
            _parseResult.value = ParseUiResult.BatchAccounts(batchResult.accounts)
            return
        }

        // Single transaction parse with 6-pillar engine
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
                val insertedId = repository.insertTransaction(transaction)

                if (parsedTx.type == TransactionType.INSTALLMENT) {
                    val numInstallments = parsedTx.totalInstallments ?: 6
                    val monthly = parsedTx.amountCentavos / maxOf(1, numInstallments)
                    val plan = InstallmentPlanEntity(
                        accountId = selectedAccountId,
                        title = parsedTx.title.ifBlank { parsedTx.category },
                        category = parsedTx.category,
                        totalPurchaseAmount = parsedTx.amountCentavos,
                        totalInstallments = numInstallments,
                        installmentsPaid = 0,
                        remainingBalance = parsedTx.amountCentavos,
                        monthlyPaymentAmount = monthly,
                        purchaseDate = System.currentTimeMillis()
                    )
                    val planId = repository.insertInstallmentPlan(plan)
                    repository.updateTransaction(transaction.copy(id = insertedId, installmentPlanId = planId))
                }

                _parseResult.value = ParseUiResult.Success
                _inputText.value = ""
                onSuccess()
            } catch (e: Exception) {
                _parseResult.value = ParseUiResult.Error(e.message ?: "Failed to save transaction")
            }
        }
    }

    fun createAccount(
        account: AccountEntity,
        loanDetails: LoanAccountDetailsEntity? = null,
        savingsDetails: com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity? = null,
        billDetails: com.example.budgettracker.data.local.entity.BillAccountDetailsEntity? = null,
        creditDetails: com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity? = null,
        onCreated: (Long) -> Unit = {}
    ) {
        if (activeAccounts.value.size >= 10) {
            _parseResult.value = ParseUiResult.Error("Account limit reached (maximum 10 accounts)")
            return
        }
        viewModelScope.launch(ioDispatcher) {
            val id = repository.insertAccount(account)
            if (loanDetails != null) {
                repository.insertLoanDetails(loanDetails.copy(accountId = id))
            }
            if (savingsDetails != null) {
                repository.insertSavingsDetails(savingsDetails.copy(accountId = id))
            }
            if (billDetails != null) {
                repository.insertBillDetails(billDetails.copy(accountId = id))
            }
            if (creditDetails != null) {
                repository.insertCreditDetails(creditDetails.copy(accountId = id))
            }
            if (account.type == AccountType.CREDIT || account.type == AccountType.LOAN || account.type == AccountType.BNPL || account.type == AccountType.BILL) {
                repository.ensurePendingCyclesForAccount(id)
            }
            onCreated(id)
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
        if (activeAccounts.value.size + accountsToSave.size > 10) {
            _parseResult.value = ParseUiResult.Error("Account limit reached (maximum 10 accounts)")
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
                    if (day1 != null || acc.dueDays.isNotBlank()) {
                        val loanDetails = LoanAccountDetailsEntity(
                            accountId = newAccountId,
                            dueDays = acc.resolveDueDays(),
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
