package com.example.budgettracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.data.repository.BudgetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionHistoryViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    val allAccounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId: StateFlow<Long?> = _selectedAccountId.asStateFlow()

    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    val selectedType: StateFlow<TransactionType?> = _selectedType.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        _selectedAccountId,
        _selectedType,
        _selectedCategory
    ) { accountId, type, category ->
        Triple(accountId, type, category)
    }.flatMapLatest { (accountId, type, category) ->
        repository.getFilteredTransactions(
            accountId = accountId,
            type = type,
            category = category
        )
    }.combine(_searchQuery) { transactions, query ->
        if (query.isBlank()) {
            transactions
        } else {
            transactions.filter { tx ->
                tx.title.contains(query, ignoreCase = true) ||
                        tx.category.contains(query, ignoreCase = true) ||
                        (tx.note?.contains(query, ignoreCase = true) == true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedAccount(accountId: Long?) {
        _selectedAccountId.value = accountId
    }

    fun setSelectedType(type: TransactionType?) {
        _selectedType.value = type
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearFilters() {
        _selectedAccountId.value = null
        _selectedType.value = null
        _selectedCategory.value = null
        _searchQuery.value = ""
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
