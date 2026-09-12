package com.example.budgettracker.parser

import com.example.budgettracker.data.model.TransactionType

object CategoryDictionary {

    fun inferCategory(sentence: String, type: TransactionType): String {
        val lower = sentence.lowercase().trim()

        return when (type) {
            TransactionType.INCOME -> when {
                lower.contains("salary") || lower.contains("sahod") || lower.contains("sweldo") || lower.contains("paycheck") -> "Salary"
                lower.contains("freelance") || lower.contains("raket") -> "Freelance"
                else -> "Other Income"
            }
            TransactionType.TRANSFER -> "Transfer"
            TransactionType.INSTALLMENT -> "Installment"
            TransactionType.EXPENSE -> when {
                lower.contains("lunch") || lower.contains("dinner") || lower.contains("breakfast") ||
                        lower.contains("food") || lower.contains("jollibee") || lower.contains("mcdo") ||
                        lower.contains("chowking") || lower.contains("kfc") -> "Food & Dining"
                lower.contains("grab") || lower.contains("jeep") || lower.contains("pamasahe") ||
                        lower.contains("bus") || lower.contains("mrt") || lower.contains("lrt") ||
                        lower.contains("taxi") || lower.contains("gas") -> "Transportation"
                lower.contains("groceries") || lower.contains("grocery") || lower.contains("milk") ||
                        lower.contains("supermarket") || lower.contains("puregold") -> "Groceries"
                lower.contains("bill") || lower.contains("meralco") || lower.contains("maynilad") ||
                        lower.contains("electricity") || lower.contains("water") || lower.contains("internet") -> "Utilities"
                else -> "Other Expense"
            }
        }
    }
}
