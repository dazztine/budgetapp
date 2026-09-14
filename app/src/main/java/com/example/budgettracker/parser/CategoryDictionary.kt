package com.example.budgettracker.parser

import com.example.budgettracker.data.model.TransactionType

object CategoryDictionary {

    fun inferCategory(sentence: String, type: TransactionType): String {
        val lower = sentence.lowercase().trim()

        return when (type) {
            TransactionType.INCOME -> when {
                lower.contains("salary") || lower.contains("sahod") || lower.contains("sweldo") || lower.contains("paycheck") || lower.contains("sweldohan") -> "Salary"
                lower.contains("freelance") || lower.contains("raket") || lower.contains("sideline") || lower.contains("project") -> "Freelance"
                lower.contains("ayuda") || lower.contains("bonus") || lower.contains("tinubo") || lower.contains("interest") || lower.contains("tubo") -> "Bonus / Investment"
                else -> "Other Income"
            }
            TransactionType.TRANSFER -> "Transfer"
            TransactionType.INSTALLMENT -> "Installment"
            TransactionType.EXPENSE -> when {
                lower.contains("lunch") || lower.contains("dinner") || lower.contains("breakfast") ||
                        lower.contains("food") || lower.contains("jollibee") || lower.contains("jolibee") || lower.contains("mcdo") ||
                        lower.contains("mcdonalds") || lower.contains("chowking") || lower.contains("kfc") ||
                        lower.contains("mang inasal") || lower.contains("starbucks") || lower.contains("coffee") ||
                        lower.contains("milktea") || lower.contains("merienda") || lower.contains("snack") ||
                        lower.contains("kain") || lower.contains("pagkain") || lower.contains("fastfood") ||
                        lower.contains("burger") || lower.contains("pizza") -> "Food & Dining"
                lower.contains("grab") || lower.contains("angkas") || lower.contains("joyride") ||
                        lower.contains("moveit") || lower.contains("jeep") || lower.contains("pamasahe") ||
                        lower.contains("bus") || lower.contains("mrt") || lower.contains("lrt") ||
                        lower.contains("taxi") || lower.contains("gas") || lower.contains("gasolina") ||
                        lower.contains("petron") || lower.contains("shell") || lower.contains("caltex") ||
                        lower.contains("toll") || lower.contains("easytrip") || lower.contains("autosweep") -> "Transportation"
                lower.contains("groceries") || lower.contains("grocery") || lower.contains("milk") ||
                        lower.contains("supermarket") || lower.contains("puregold") || lower.contains("savemore") ||
                        lower.contains("waltermart") || lower.contains("palengke") -> "Groceries"
                lower.contains("bill") || lower.contains("meralco") || lower.contains("maynilad") ||
                        lower.contains("manila water") || lower.contains("pldt") || lower.contains("globe") ||
                        lower.contains("smart") || lower.contains("converge") || lower.contains("dito") ||
                        lower.contains("electricity") || lower.contains("water") || lower.contains("internet") ||
                        lower.contains("wifi") || lower.contains("kuryente") || lower.contains("tubig") ||
                        lower.contains("load") -> "Bills & Utilities"
                lower.contains("shopee") || lower.contains("lazada") || lower.contains("tiktok shop") ||
                        lower.contains("uniqlo") || lower.contains("zara") || lower.contains("shein") ||
                        lower.contains("sm store") || lower.contains("mall") || lower.contains("shopping") -> "Shopping"
                lower.contains("mercury drug") || lower.contains("watsons") || lower.contains("gamot") ||
                        lower.contains("hospital") || lower.contains("clinic") || lower.contains("doctor") ||
                        lower.contains("dentist") || lower.contains("med") -> "Health & Medical"
                lower.contains("netflix") || lower.contains("spotify") || lower.contains("cinema") ||
                        lower.contains("sine") || lower.contains("movie") || lower.contains("steam") -> "Entertainment"
                else -> "General"
            }
        }
    }
}
