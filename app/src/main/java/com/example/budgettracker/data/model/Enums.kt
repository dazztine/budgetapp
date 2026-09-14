package com.example.budgettracker.data.model

enum class AccountType {
    SAVINGS,
    CASH,
    E_WALLET,
    BANK,
    LOAN,
    BNPL,
    BILL,
    ASSET;

    fun toDisplayLabel(): String = when (this) {
        E_WALLET -> "EWALLET"
        CASH -> "Cash"
        BANK -> "Bank"
        SAVINGS -> "Savings"
        BNPL -> "BNPL"
        LOAN -> "Loan"
        BILL -> "Bill"
        ASSET -> "Asset"
    }
}

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    INSTALLMENT
}
