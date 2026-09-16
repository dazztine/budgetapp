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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

    companion object {
        const val MAX_EXPRESSION_LENGTH = 20
    }

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val accounts: StateFlow<List<AccountEntity>> = repository.activeAccounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val accountBalances: StateFlow<Map<Long, Long>> = repository.activeAccountsWithBalances
        .map { list -> list.associate { it.id to it.currentBalance } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

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

    private val _editingTransactionId = MutableStateFlow<Long?>(null)
    val editingTransactionId: StateFlow<Long?> = _editingTransactionId.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

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
            val result = eval.first ?: return 0L
            val formatted = String.format(java.util.Locale.US, "%.2f", result)
            return CurrencyUtils.parseInputToCentavos(formatted)
        }

    fun onDigitInput(digit: String) {
        val current = _amountInput.value
        if (current.length + digit.length > MAX_EXPRESSION_LENGTH) {
            _toastMessage.tryEmit("Amount limit reached")
            return
        }
        val activeToken = current.takeLastWhile { it !in listOf('+', '-', '*', '/', '÷', '×') }
        if (activeToken.contains(".")) {
            val decimals = activeToken.substringAfter(".")
            if (decimals.length >= 2) {
                return
            }
        }
        val maxLen = if (activeToken.contains(".")) 12 else 9
        if (activeToken.length >= maxLen) return
        if (digit == "00" && activeToken.isEmpty()) return

        _amountInput.value = current + digit
    }

    fun onDotInput() {
        val current = _amountInput.value
        val activeToken = current.takeLastWhile { it !in listOf('+', '-', '*', '/', '÷', '×') }
        if (!activeToken.contains(".")) {
            val needed = if (activeToken.isEmpty()) 2 else 1
            if (current.length + needed > MAX_EXPRESSION_LENGTH) {
                _toastMessage.tryEmit("Amount limit reached")
                return
            }
            if (activeToken.isEmpty()) {
                _amountInput.value = "${current}0."
            } else {
                _amountInput.value = "$current."
            }
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
        val current = _amountInput.value.trim()
        val displayOp = when (op) {
            "/" -> "÷"
            "*" -> "×"
            "-" -> "−"
            else -> op
        }
        if (current.isEmpty()) {
            if (displayOp == "−" || displayOp == "-") {
                if (current.length + 1 > MAX_EXPRESSION_LENGTH) {
                    _toastMessage.tryEmit("Amount limit reached")
                    return
                }
                _amountInput.value = "-"
            }
            return
        }
        val lastChar = current.last()
        if (lastChar in listOf('+', '-', '*', '/', '÷', '×', '−')) {
            // Replace trailing operator (length remains identical)
            _amountInput.value = current.dropLast(1) + displayOp
        } else {
            if (current.length + 1 > MAX_EXPRESSION_LENGTH) {
                _toastMessage.tryEmit("Amount limit reached")
                return
            }
            _amountInput.value = "$current$displayOp"
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

    fun onConfirmAmount(): Boolean {
        if (_amountInput.value.isBlank()) {
            _amountInput.value = "0"
            return true
        }
        return onEqualClick()
    }

    fun onToggleSign() {
        val current = _amountInput.value
        if (current.startsWith("-")) {
            _amountInput.value = current.removePrefix("-")
        } else if (current.isNotEmpty() && current != "0") {
            if (current.length + 1 > MAX_EXPRESSION_LENGTH) {
                _toastMessage.tryEmit("Amount limit reached")
                return
            }
            _amountInput.value = "-$current"
        }
    }

    fun onPasteInput(pastedText: String) {
        val sb = StringBuilder()
        var hasDecimal = false
        for (c in pastedText) {
            if (c.isDigit()) {
                sb.append(c)
            } else if (c == '.' && !hasDecimal) {
                sb.append(c)
                hasDecimal = true
            }
        }
        var numericOnly = sb.toString()
        if (numericOnly.isNotBlank()) {
            if (numericOnly.length > MAX_EXPRESSION_LENGTH) {
                numericOnly = numericOnly.take(MAX_EXPRESSION_LENGTH)
                _toastMessage.tryEmit("Amount limit reached")
            }
            _amountInput.value = numericOnly
        }
    }

    fun evaluateExpressionString(input: String): Pair<Double?, String?> {
        var expr = input.trim()
            .replace(",", "")
            .replace("÷", "/")
            .replace("×", "*")
            .replace("x", "*")
            .replace("−", "-")
            .replace("—", "-")

        if (expr.isBlank()) return Pair(0.0, null)

        // Drop any trailing operator for graceful evaluation
        while (expr.endsWith("+") || expr.endsWith("-") || expr.endsWith("*") || expr.endsWith("/")) {
            expr = expr.dropLast(1).trim()
        }
        if (expr.isBlank()) return Pair(0.0, null)

        return try {
            val tokens = mutableListOf<String>()
            var i = 0
            while (i < expr.length) {
                val c = expr[i]
                if (c.isWhitespace()) {
                    i++
                    continue
                }
                if (c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/"))) {
                    val sb = StringBuilder("-")
                    i++
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    if (sb.length == 1) return Pair(null, "Invalid mathematical expression")
                    tokens.add(sb.toString())
                } else if (c in listOf('+', '-', '*', '/')) {
                    tokens.add(c.toString())
                    i++
                } else if (c.isDigit() || c == '.') {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    tokens.add(sb.toString())
                } else {
                    return Pair(null, "Invalid character: $c")
                }
            }

            if (tokens.isEmpty()) return Pair(null, "Invalid mathematical expression")

            // Pass 1: PEMDAS Multiplication and Division
            val pass1Tokens = mutableListOf<String>()
            var idx = 0
            while (idx < tokens.size) {
                val token = tokens[idx]
                if (token == "*" || token == "/") {
                    if (pass1Tokens.isEmpty() || idx + 1 >= tokens.size) {
                        return Pair(null, "Invalid mathematical expression")
                    }
                    val left = pass1Tokens.removeAt(pass1Tokens.lastIndex).toDoubleOrNull()
                        ?: return Pair(null, "Invalid number")
                    val right = tokens[idx + 1].toDoubleOrNull()
                        ?: return Pair(null, "Invalid number: ${tokens[idx + 1]}")
                    if (token == "/" && right == 0.0) {
                        return Pair(null, "Cannot divide by zero")
                    }
                    val res = if (token == "*") left * right else left / right
                    pass1Tokens.add(res.toString())
                    idx += 2
                } else {
                    pass1Tokens.add(token)
                    idx++
                }
            }

            // Pass 2: PEMDAS Addition and Subtraction
            if (pass1Tokens.isEmpty()) return Pair(0.0, null)
            var result = pass1Tokens[0].toDoubleOrNull()
                ?: return Pair(null, "Invalid number: ${pass1Tokens[0]}")
            var j = 1
            while (j < pass1Tokens.size) {
                val op = pass1Tokens[j]
                if (j + 1 >= pass1Tokens.size) return Pair(null, "Incomplete expression")
                val right = pass1Tokens[j + 1].toDoubleOrNull()
                    ?: return Pair(null, "Invalid number: ${pass1Tokens[j + 1]}")
                result = when (op) {
                    "+" -> result + right
                    "-" -> result - right
                    else -> return Pair(null, "Unexpected operator: $op")
                }
                j += 2
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
        _editingTransactionId.value = transaction.id
        _isEditing.value = transaction.id != 0L
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
        val currentEditId = _editingTransactionId.value

        val transaction = TransactionEntity(
            id = currentEditId ?: 0L,
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
                if (currentEditId == null || currentEditId == 0L) {
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
        _editingTransactionId.value = null
        _isEditing.value = false
        pendingOperator = null
        storedOperand = null
    }

    fun resetSaveState() {
        _saveState.value = SaveResult.Idle
    }
}
