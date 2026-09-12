package com.example.budgettracker.parser

object ParserUtils {

    val CLAUSE_SPLITTER = Regex(
        "(?:\\r?\\n+|;+|(?:,(?!\\d{3}\\b|\\d{3}\\.|\\d{3},|\\s*\\d{1,2}(?:st|nd|rd|th)))|(?:\\s+and\\s+(?!\\d{1,2}(?:st|nd|rd|th))))",
        RegexOption.IGNORE_CASE
    )

    val DUE_DATE_REGEX = Regex(
        "(?i)\\b(?:due(?:\\s+on|\\s+every)?|cycle|every)\\s*(?:the\\s*)?(\\d{1,2})(?:st|nd|rd|th)?(?:\\s*(?:and|&|,)\\s*(?:the\\s*)?(\\d{1,2})(?:st|nd|rd|th)?)?"
    )

    val AMOUNT_REGEX = Regex(
        "(?:₱|(?i:\\bPHP\\b|\\bP(?=\\s*\\d)|\\+))\\s*([0-9]{1,3}(?:,[0-9]{3})+\\.[0-9]{1,2}|[0-9]{1,3}(?:,[0-9]{3})+|\\d+(?:\\.\\d+)?\\s*[kK]|[0-9]+\\.[0-9]{1,2}|[0-9]+)|([0-9]{1,3}(?:,[0-9]{3})+\\.[0-9]{1,2}|[0-9]{1,3}(?:,[0-9]{3})+|\\d+(?:\\.\\d+)?\\s*[kK]|[0-9]+\\.[0-9]{1,2}|[0-9]+)"
    )

    fun parseAmountCentavos(text: String): Pair<Long, String?> {
        val amtMatch = AMOUNT_REGEX.find(text) ?: return Pair(0L, null)
        val matchedStr = amtMatch.value
        val numStr = amtMatch.groupValues[1].ifEmpty { amtMatch.groupValues[2] }
        val rawNum = numStr.replace(",", "").trim()

        val centavos = if (rawNum.endsWith("k", ignoreCase = true)) {
            val numPart = rawNum.dropLast(1).toDoubleOrNull() ?: 0.0
            (numPart * 1000.0 * 100.0).toLong()
        } else {
            val numPart = rawNum.toDoubleOrNull() ?: 0.0
            (numPart * 100.0).toLong()
        }

        return Pair(centavos, matchedStr)
    }
}
