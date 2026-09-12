package com.example.budgettracker.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.util.CurrencyUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SaveResult {
    object Idle : SaveResult()
    object Success : SaveResult()
    data class Error(val message: String) : SaveResult()
}

class TransactionViewModel(
    private val repository: BudgetRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = repository.activeAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _amountInput = MutableStateFlow("")
    val amountInput: StateFlow<String> = _amountInput.asStateFlow()

    private val _selectedType = MutableStateFlow(TransactionType.EXPENSE)
    val selectedType: StateFlow<TransactionType> = _selectedType.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId: StateFlow<Long?> = _selectedAccountId.asStateFlow()

    private val _selectedToAccountId = MutableStateFlow<Long?>(null)
    val selectedToAccountId: StateFlow<Long?> = _selectedToAccountId.asStateFlow()

    private val _categoryInput = MutableStateFlow("")
    val categoryInput: StateFlow<String> = _categoryInput.asStateFlow()

    private val _titleInput = MutableStateFlow("")
    val titleInput: StateFlow<String> = _titleInput.asStateFlow()

    private val _totalInstallmentsInput = MutableStateFlow("6")
    val totalInstallmentsInput: StateFlow<String> = _totalInstallmentsInput.asStateFlow()

    private val _noteInput = MutableStateFlow("")
    val noteInput: StateFlow<String> = _noteInput.asStateFlow()

    private val _timestamp = MutableStateFlow(System.currentTimeMillis())
    val timestamp: StateFlow<Long> = _timestamp.asStateFlow()

    private val _saveState = MutableStateFlow<SaveResult>(SaveResult.Idle)
    val saveState: StateFlow<SaveResult> = _saveState.asStateFlow()

    private var editingTransactionId: Long? = null

    // Calculator Expression Evaluation
    private var pendingOperator: String? = null
    private var storedOperand: Double? = null

    // Scoped Autocomplete Categories Stream
    @OptIn(ExperimentalCoroutinesApi::class)
    val categorySuggestions: StateFlow<List<String>> = _selectedType
        .flatMapLatest { type ->
            repository.getDistinctCategories(type)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Scoped Autocomplete Titles Stream
    @OptIn(ExperimentalCoroutinesApi::class)
    val titleSuggestions: StateFlow<List<String>> = _selectedType
        .flatMapLatest { type ->
            repository.getDistinctTitles(type)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val amountCentavos: Long
        get() {
            val eval = evaluateExpressionString(_amountInput.value)
            return if (eval.first != null) CurrencyUtils.parseInputToCentavos(eval.first.toString()) else 0L
        }

    fun onDigitInput(digit: String) {
        val current = _amountInput.value
        if (current.contains(".")) {
            val parts = current.split(".")
            if (parts.size > 1 && parts[1].length >= 2) {
                return
            }
        }
        val maxLen = if (current.contains(".")) 12 else 9
        if (current.length >= maxLen) return
        if (digit == "00" && current.isEmpty()) return

        _amountInput.value = current + digit
    }

    fun onDotInput() {
        val current = _amountInput.value
        if (!current.endsWith(".")) {
            _amountInput.value = "$current."
        }
    }

    fun onBackspace() {
        val current = _amountInput.value
        if (current.isNotEmpty()) {
            _amountInput.value = current.dropLast(1)
        }
    }

    fun onClear() {
        _amountInput.value = ""
    }

    fun onOperatorClick(op: String) {
        val current = _amountInput.value
        val normalizedOp = when (op) {
            "÷" -> "/"
            "×" -> "*"
            "−" -> "-"
            else -> op
        }
        if (current.isNotEmpty() && !current.endsWith("+") && !current.endsWith("-") && !current.endsWith("*") && !current.endsWith("/")) {
            _amountInput.value = "$current$normalizedOp"
        }
    }

    fun onEqualClick(): Boolean {
        val (result, error) = evaluateExpressionString(_amountInput.value)
        if (error != null) {
            _saveState.value = SaveResult.Error(error)
            return false
        } else if (result != null) {
            val formatted = if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                String.format(java.util.Locale.US, "%.2f", result)
            }
            _amountInput.value = formatted
            return true
        }
        return true
    }

    fun onToggleSign() {
        val current = _amountInput.value
        if (current.startsWith("-")) {
            _amountInput.value = current.removePrefix("-")
        } else if (current.isNotEmpty() && current != "0") {
            _amountInput.value = "-$current"
        }
    }

    fun onPasteInput(pastedText: String) {
        val numericOnly = pastedText.filter { it.isDigit() || it == '.' || it == '+' || it == '-' || it == '*' || it == '/' }
        if (numericOnly.isNotBlank()) {
            _amountInput.value = numericOnly
        }
    }

    fun evaluateExpressionString(input: String): Pair<Double?, String?> {
        val expr = input.trim()
            .replace("÷", "/")
            .replace("×", "*")
            .replace("x", "*")
            .replace("−", "-")
            .replace("—", "-")

        if (expr.isBlank()) return Pair(0.0, null)

        if (expr.endsWith("+") || expr.endsWith("-") || expr.endsWith("*") || expr.endsWith("/")) {
            return Pair(null, "Invalid mathematical expression: trailing operator")
        }

        return try {
            val tokens = mutableListOf<String>()
            var cur = StringBuilder()
            for (c in expr) {
                if (c in listOf('+', '-', '*', '/')) {
                    if (cur.isNotEmpty()) {
                        tokens.add(cur.toString().trim())
                        cur = StringBuilder()
                    }
                    tokens.add(c.toString())
                } else {
                    cur.append(c)
                }
            }
            if (cur.isNotEmpty()) tokens.add(cur.toString().trim())

            if (tokens.isEmpty()) return Pair(null, "Invalid mathematical expression")

            var result = tokens[0].toDoubleOrNull() ?: return Pair(null, "Invalid number: ${tokens[0]}")
            var i = 1
            while (i < tokens.size) {
                val op = tokens[i]
                if (i + 1 >= tokens.size) return Pair(null, "Incomplete expression")
                val nextNum = tokens[i + 1].toDoubleOrNull() ?: return Pair(null, "Invalid number: ${tokens[i + 1]}")
                result = when (op) {
                    "+" -> result + nextNum
                    "-" -> result - nextNum
                    "*" -> result * nextNum
                    "/" -> if (nextNum != 0.0) result / nextNum else return Pair(null, "Cannot divide by zero")
                    else -> result
                }
                i += 2
            }
            Pair(result, null)
        } catch (e: Exception) {
            Pair(null, "Invalid mathematical expression")
        }
    }

    fun setTransactionType(type: TransactionType) {
        _selectedType.value = type
    }

    fun setAccountId(accountId: Long?) {
        _selectedAccountId.value = accountId
    }

    fun setToAccountId(accountId: Long?) {
        _selectedToAccountId.value = accountId
    }

    fun setCategory(category: String) {
        _categoryInput.value = category
    }

    fun setTitle(title: String) {
        _titleInput.value = title
    }

    fun setTotalInstallments(installments: String) {
        _totalInstallmentsInput.value = installments.filter { it.isDigit() }
    }

    fun setNote(note: String) {
        _noteInput.value = note
    }

    fun setTimestamp(timeMillis: Long) {
        _timestamp.value = timeMillis
    }

    fun loadTransactionForEdit(transaction: TransactionEntity) {
        editingTransactionId = transaction.id
        _selectedType.value = transaction.type
        _selectedAccountId.value = transaction.accountId
        _selectedToAccountId.value = transaction.toAccountId
        _categoryInput.value = transaction.category
        _titleInput.value = transaction.title
        _noteInput.value = transaction.note ?: ""
        _timestamp.value = transaction.timestamp

        val pesos = transaction.amount / 100
        val cents = transaction.amount % 100
        _amountInput.value = if (cents > 0) String.format(java.util.Locale.US, "%d.%02d", pesos, cents) else pesos.toString()
    }

    fun saveTransaction(onSuccess: () -> Unit = {}) {
        val centavos = amountCentavos
        if (centavos <= 0L) {
            _saveState.value = SaveResult.Error("Amount must be greater than zero")
            return
        }

        val accountId = _selectedAccountId.value
        if (accountId == null) {
            _saveState.value = SaveResult.Error("Please select an account")
            return
        }

        val type = _selectedType.value
        val toAccountId = _selectedToAccountId.value
        if (type == TransactionType.TRANSFER) {
            if (toAccountId == null) {
                _saveState.value = SaveResult.Error("Please select a destination account")
                return
            }
            if (toAccountId == accountId) {
                _saveState.value = SaveResult.Error("Destination account must be different from source account")
                return
            }
        }

        val title = _titleInput.value.trim().ifEmpty { type.name }
        val category = _categoryInput.value.trim().ifEmpty { "General" }

        val transaction = TransactionEntity(
            id = editingTransactionId ?: 0L,
            type = type,
            accountId = accountId,
            toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
            amount = centavos,
            isAdjustment = false,
            category = category,
            title = title,
            note = _noteInput.value.trim().ifEmpty { null },
            timestamp = _timestamp.value
        )

        viewModelScope.launch(ioDispatcher) {
            try {
                if (editingTransactionId == null || editingTransactionId == 0L) {
                    repository.insertTransaction(transaction)

                    // If INSTALLMENT type, automatically create InstallmentPlanEntity
                    if (type == TransactionType.INSTALLMENT) {
                        val numInstallments = _totalInstallmentsInput.value.toIntOrNull() ?: 6
                        val monthly = centavos / maxOf(1, numInstallments)
                        val plan = InstallmentPlanEntity(
                            accountId = accountId,
                            title = title,
                            category = category,
                            totalPurchaseAmount = centavos,
                            totalInstallments = numInstallments,
                            installmentsPaid = 0,
                            remainingBalance = centavos,
                            monthlyPaymentAmount = monthly,
                            purchaseDate = _timestamp.value
                        )
                        repository.insertInstallmentPlan(plan)
                    }
                } else {
                    repository.updateTransaction(transaction)
                }
                _saveState.value = SaveResult.Success
                onSuccess()
            } catch (e: Exception) {
                _saveState.value = SaveResult.Error(e.message ?: "Failed to save transaction")
            }
        }
    }

    fun saveAndAddAnother(onSuccess: () -> Unit = {}) {
        saveTransaction {
            resetFormForNextEntry()
            onSuccess()
        }
    }

    fun resetFormForNextEntry() {
        _amountInput.value = ""
        _categoryInput.value = ""
        _titleInput.value = ""
        _noteInput.value = ""
        _totalInstallmentsInput.value = "6"
        _timestamp.value = System.currentTimeMillis()
        _saveState.value = SaveResult.Idle
        editingTransactionId = null
        pendingOperator = null
        storedOperand = null
    }

    fun resetSaveState() {
        _saveState.value = SaveResult.Idle
    }
}
