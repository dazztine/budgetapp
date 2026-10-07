package com.example.budgettracker.domain

/**
 * Result of validating and parsing a user-entered peso input string.
 */
sealed class BudgetInputResult {
    data class Valid(val centavos: Long) : BudgetInputResult()
    data class Error(val message: String) : BudgetInputResult()
}

/**
 * Pure utility functions for validating, parsing, and formatting user-typed peso amounts.
 *
 * NON-NEGOTIABLE RULES:
 * - All monetary values are integer centavos (Long).
 * - No Double or Float arithmetic.
 * - Strict input validation with clear, inline user feedback.
 */
object PesoInput {

    const val MAX_BUDGET_CENTAVOS: Long = 100_000_000_00L // ₱100,000,000.00 max budget

    // Matches valid comma thousands grouping: e.g. "1,000", "50,000", "100,000,000"
    private val THOUSANDS_COMMA_REGEX = Regex("""^\d{1,3}(,\d{3})+$""")

    /**
     * Pure function to validate and parse user-typed peso amount text into integer centavos.
     *
     * Rules:
     * - Must not be blank.
     * - Disallows negative signs or negative amounts.
     * - Disallows invalid characters (only digits, thousands commas, and decimal point allowed).
     * - Validates comma placements (e.g. "1,5000" is rejected, "1,500" is accepted).
     * - At most one decimal point.
     * - Maximum 2 decimal places.
     * - Integer arithmetic only, no Double/Float.
     * - Must be > ₱0.00.
     * - Must not exceed [maxCentavos].
     */
    fun parseBudgetPesoInput(
        input: String,
        maxCentavos: Long = MAX_BUDGET_CENTAVOS
    ): BudgetInputResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return BudgetInputResult.Error("Amount cannot be empty")
        }
        if (trimmed.contains("-")) {
            return BudgetInputResult.Error("Amount cannot be negative")
        }
        if (!trimmed.all { it.isDigit() || it == ',' || it == '.' }) {
            return BudgetInputResult.Error("Amount contains invalid characters")
        }
        val dotCount = trimmed.count { it == '.' }
        if (dotCount > 1) {
            return BudgetInputResult.Error("Amount cannot have multiple decimal points")
        }

        val parts = trimmed.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else ""

        // Validate comma placement in integer part if commas are present
        if (integerPart.contains(",")) {
            if (!THOUSANDS_COMMA_REGEX.matches(integerPart)) {
                return BudgetInputResult.Error("Invalid comma placement")
            }
        }

        // Decimal part must not contain commas
        if (decimalPart.contains(",")) {
            return BudgetInputResult.Error("Invalid comma placement")
        }

        // Maximum 2 decimal places
        if (decimalPart.length > 2) {
            return BudgetInputResult.Error("Maximum 2 decimal places allowed")
        }

        val cleanInteger = integerPart.replace(",", "")
        val pesos = if (cleanInteger.isEmpty()) 0L else cleanInteger.toLongOrNull()
            ?: return BudgetInputResult.Error("Amount is too large")

        val cents = when (decimalPart.length) {
            0 -> 0L
            1 -> decimalPart.toLong() * 10L
            2 -> decimalPart.toLong()
            else -> return BudgetInputResult.Error("Maximum 2 decimal places allowed")
        }

        // Guard against overflow: pesos * 100 + cents <= Long.MAX_VALUE
        if (pesos > (Long.MAX_VALUE - cents) / 100L) {
            return BudgetInputResult.Error("Amount is too large")
        }

        val totalCentavos = pesos * 100L + cents
        if (totalCentavos <= 0L) {
            return BudgetInputResult.Error("Budget amount must be greater than ₱0")
        }
        if (totalCentavos > maxCentavos) {
            return BudgetInputResult.Error("Amount exceeds maximum allowed budget (₱100,000,000)")
        }

        return BudgetInputResult.Valid(totalCentavos)
    }

    /**
     * Pure function to format a user-typed amount string with thousands commas for live UI display,
     * while preserving cursor and fractional entry.
     */
    fun formatPesoInput(input: String): String {
        val clean = input.filter { it.isDigit() || it == '.' }
        val parts = clean.split(".")
        val integerStr = parts[0]
        val formattedInt = if (integerStr.isNotEmpty()) {
            val longVal = integerStr.toLongOrNull()
            if (longVal != null) String.format(java.util.Locale.US, "%,d", longVal) else integerStr
        } else ""
        return if (parts.size > 1) {
            val decimals = parts[1].take(2)
            "$formattedInt.$decimals"
        } else if (clean.endsWith(".")) {
            "$formattedInt."
        } else {
            formattedInt
        }
    }
}
