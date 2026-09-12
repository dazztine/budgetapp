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
        val parts = input.split(".")
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
}
