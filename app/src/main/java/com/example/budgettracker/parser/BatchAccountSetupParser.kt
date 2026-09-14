package com.example.budgettracker.parser

import com.example.budgettracker.data.model.AccountType

object BatchAccountSetupParser {

    private val FILLER_WORDS = setOf(
        "i", "have", "had", "got", "get", "right", "now", "my", "current", "account", "wallet",
        "balance", "is", "at", "pesos", "peso", "p", "php", "in", "has", "with", "the", "value",
        "there", "here", "on", "me", "just", "only", "already", "saved", "total", "for", "a", "an",
        "that", "this", "these", "those",
        "meron", "akong", "may", "mayroon", "pondo", "pera", "sa", "kay", "ko", "mga", "nito", "natin"
    )

    private val SETUP_KEYWORDS = setOf(
        "meron akong", "meron", "may", "mayroon", "pondo sa", "pera sa", "my balance",
        "i have", "saved", "current balance", "balance is", "initial balance", "starting balance"
    )

    fun isAccountSetupExpression(text: String): Boolean {
        val lower = text.lowercase().trim()
        return SETUP_KEYWORDS.any { lower.contains(it) } || lower.contains(":") || lower.contains(";")
    }

    fun parse(text: String): BatchParseResult {
        if (text.isBlank()) {
            return BatchParseResult(emptyList(), 0L, false)
        }

        val segments = text.split(ParserUtils.CLAUSE_SPLITTER)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val accounts = mutableListOf<ParsedAccountSetup>()

        for (seg in segments) {
            var working = seg
            var day1: Int? = null
            var day2: Int? = null

            // 1. Extract due date / cycle days
            val dateMatch = ParserUtils.DUE_DATE_REGEX.find(working)
            if (dateMatch != null) {
                day1 = dateMatch.groupValues[1].toIntOrNull()
                if (dateMatch.groupValues.size > 2 && dateMatch.groupValues[2].isNotBlank()) {
                    day2 = dateMatch.groupValues[2].toIntOrNull()
                }
                working = working.replace(dateMatch.value, " ")
            }

            // 2. Extract initial balance amount
            val (amountCentavos, matchedAmtStr) = ParserUtils.parseAmountCentavos(working)
            if (matchedAmtStr != null) {
                working = working.replace(matchedAmtStr, " ")
            }

            // 3. Clean residual string
            val cleanStr = working.replace(Regex("[₱,;!?]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            if (cleanStr.isBlank() && amountCentavos == 0L && day1 == null) {
                continue
            }

            // 4. Preset and Type Matching (Exact Match First, Fuzzy Second)
            var matchedPresetId: String? = null
            var matchedType = AccountType.BANK
            var matchedPresetName: String? = null

            // Step 4a: Exact word boundary match
            for (preset in PhilippinePresets.CATALOG) {
                val pRegex = Regex("(?i)\\b${Regex.escape(preset.name)}\\b")
                if (pRegex.containsMatchIn(cleanStr)) {
                    matchedPresetId = preset.id
                    matchedType = preset.defaultType
                    matchedPresetName = preset.name
                    break
                }
            }

            // Step 4b: Fuzzy match if no exact preset match
            if (matchedPresetId == null) {
                val tokens = cleanStr.split(" ")
                for (preset in PhilippinePresets.CATALOG) {
                    if (tokens.any { LevenshteinMatcher.isFuzzyMatch(it, preset.name) }) {
                        matchedPresetId = preset.id
                        matchedType = preset.defaultType
                        matchedPresetName = preset.name
                        break
                    }
                }
            }

            // Construct final display name
            val finalName = if (matchedPresetName != null) {
                // Preset matched: only accept valid financial modifiers (e.g. "BDO Savings"), never conversational words
                val validModifiers = setOf("savings", "checking", "payroll", "credit", "card", "debit", "wallet")
                val nonPresetTokens = cleanStr.split(" ")
                    .filter { !it.equals(matchedPresetName, ignoreCase = true) }
                    .map { it.lowercase() }
                    .filter { validModifiers.contains(it) }

                if (nonPresetTokens.isEmpty()) {
                    matchedPresetName
                } else {
                    matchedPresetName + " " + nonPresetTokens.joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
                }
            } else {
                // No preset matched: strip filler words from cleanStr
                cleanStr.split(" ")
                    .filter { !FILLER_WORDS.contains(it.lowercase()) }
                    .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
                    .ifBlank {
                        cleanStr.split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
                    }.ifBlank { "Custom Account" }
            }

            // Infer BNPL / Loan if cycle days or keywords present
            if (day1 != null || finalName.contains("paylater", ignoreCase = true) || finalName.contains("billease", ignoreCase = true)) {
                if (matchedType == AccountType.BANK || matchedType == AccountType.E_WALLET) {
                    matchedType = AccountType.BNPL
                }
            } else if (finalName.contains("loan", ignoreCase = true)) {
                matchedType = AccountType.LOAN
            } else if (finalName.equals("cash", ignoreCase = true)) {
                matchedType = AccountType.CASH
            }

            accounts.add(
                ParsedAccountSetup(
                    name = finalName,
                    type = matchedType,
                    presetId = matchedPresetId,
                    initialBalanceCentavos = amountCentavos,
                    cycleDay1 = day1,
                    cycleDay2 = day2
                )
            )
        }

        val total = accounts.sumOf { it.initialBalanceCentavos }
        val hasErrors = accounts.any { it.validationErrors.isNotEmpty() }
        return BatchParseResult(accounts, total, hasErrors)
    }
}
