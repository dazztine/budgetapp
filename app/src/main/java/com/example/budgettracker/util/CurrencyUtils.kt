package com.example.budgettracker.util

import java.util.Locale

object CurrencyUtils {
    /**
     * Formats positive centavos Long (e.g. 15050L) into display string "₱150.50".
     * Zero or negative amounts are formatted similarly with optional negative sign handling.
     */
    fun formatCentavosToPesos(centavos: Long): String {
        val isNegative = centavos < 0
        val absCentavos = Math.abs(centavos)
        val pesos = absCentavos / 100
        val cents = absCentavos % 100
        val formatted = String.format(Locale.US, "₱%,d.%02d", pesos, cents)
        return if (isNegative) "-$formatted" else formatted
    }

    /**
     * Converts raw string input from numpad (e.g. "150.5" or "150") into positive centavos Long.
     */
    fun parseInputToCentavos(input: String): Long {
        if (input.isBlank()) return 0L
        val cleanInput = input.replace(",", "")
        val parts = cleanInput.split(".")
        val pesosStr = parts[0].filter { it.isDigit() }
        val pesos = pesosStr.toLongOrNull() ?: 0L
        val cents = if (parts.size > 1) {
            val centsStr = parts[1].filter { it.isDigit() }.take(2).padEnd(2, '0')
            centsStr.toLongOrNull() ?: 0L
        } else {
            0L
        }
        return pesos * 100L + cents
    }

    /**
     * Formats raw expression or numeric string for live display in the UI (calculator display,
     * pinned Amount banner).
     *
     * Rules:
     * - Blank or "0" -> "₱0"
     * - When displaying a plain number (not mid-expression with pending operators),
     *   inserts thousand comma separators into the integer part (e.g. "555555555" -> "₱555,555,555").
     * - Preserves explicit decimal point and fractional digits entered by user (e.g. "500." -> "₱500.", "500.5" -> "₱500.5", "500.50" -> "₱500.50").
     * - Negative numbers are prefixed with "-₱" (e.g. "-500" -> "-₱500").
     * - Mid-expression with pending operators (e.g. "1000+250", "1000+"): preserves raw expression
     *   character for character, prefixed with "₱" (e.g. "₱1000+250", "₱1000+").
     */
    fun formatExpressionForDisplay(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "₱0"

        val isNegative = trimmed.startsWith("-")
        val contentWithoutSign = if (isNegative) trimmed.drop(1) else trimmed

        if (contentWithoutSign.isEmpty()) {
            return "₱-"
        }

        // Check if mid-expression with pending operators (+, -, *, /, ÷, ×, −)
        val hasOperators = contentWithoutSign.any { it in listOf('+', '-', '*', '/', '÷', '×', '−') }
        if (hasOperators) {
            return "₱$trimmed"
        }

        // It is a single number. Format the integer part with commas.
        val hasDecimal = contentWithoutSign.contains(".")
        val parts = contentWithoutSign.split(".", limit = 2)
        val integerDigits = parts[0]
        val decimalDigits = if (parts.size > 1) parts[1] else ""

        val formattedInteger = if (integerDigits.isEmpty()) {
            "0"
        } else {
            integerDigits.reversed().chunked(3).joinToString(",").reversed()
        }

        val numberString = if (hasDecimal) {
            "$formattedInteger.$decimalDigits"
        } else {
            formattedInteger
        }

        return if (isNegative) "-₱$numberString" else "₱$numberString"
    }
}
