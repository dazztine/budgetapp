package com.example.budgettracker.parser

import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.model.TransactionType

object SingleTransactionParser {

    fun parse(sentence: String, knownAccounts: List<AccountEntity>): ParsedTransaction {
        val lower = sentence.lowercase().trim()

        // 1. Transaction Type Detection
        val isTransfer = lower.contains("transfer") || lower.contains("lipat") || lower.contains(" to ")
        val isIncome = lower.startsWith("+") || lower.contains("salary") || lower.contains("sahod") ||
                lower.contains("sweldo") || lower.contains("freelance") || lower.contains("income") ||
                lower.contains("interest")

        val type = when {
            isTransfer -> TransactionType.TRANSFER
            isIncome -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }

        // 2. Amount Extraction
        val (amountCentavos, matchedAmtStr) = ParserUtils.parseAmountCentavos(sentence)
        var residual = sentence
        if (matchedAmtStr != null) {
            residual = residual.replace(matchedAmtStr, " ")
        }

        // 3. Account Matching by position (Exact Match First, Fuzzy Second)
        data class AccountMatch(val name: String, val index: Int, val matchedText: String)
        val matches = mutableListOf<AccountMatch>()

        // Step 3a: Exact word boundary matching on known active accounts
        for (acc in knownAccounts) {
            val accRegex = Regex("(?i)\\b${Regex.escape(acc.name)}\\b")
            val m = accRegex.find(residual)
            if (m != null) {
                matches.add(AccountMatch(acc.name, m.range.first, m.value))
            }
        }

        // Step 3b: If no exact match, try token-level fuzzy matching
        if (matches.isEmpty()) {
            val tokens = residual.split(" ")
            for (token in tokens) {
                if (token.isBlank()) continue
                for (acc in knownAccounts) {
                    if (LevenshteinMatcher.isFuzzyMatch(token, acc.name)) {
                        val tokenIdx = residual.indexOf(token)
                        if (tokenIdx >= 0) {
                            matches.add(AccountMatch(acc.name, tokenIdx, token))
                            break
                        }
                    }
                }
                if (matches.isNotEmpty()) break
            }
        }

        // Step 3c: Fallback to presets catalog exact match
        if (matches.isEmpty()) {
            for (preset in PhilippinePresets.CATALOG) {
                val pRegex = Regex("(?i)\\b${Regex.escape(preset.name)}\\b")
                val m = pRegex.find(residual)
                if (m != null) {
                    matches.add(AccountMatch(preset.name, m.range.first, m.value))
                }
            }
        }

        // Sort matches by position in sentence
        matches.sortBy { it.index }

        var matchedAccount: String? = null
        var matchedToAccount: String? = null

        if (matches.isNotEmpty()) {
            matchedAccount = matches[0].name
            residual = residual.replace(Regex("(?i)\\b${Regex.escape(matches[0].matchedText)}\\b"), " ")
            if (isTransfer && matches.size > 1) {
                matchedToAccount = matches[1].name
                residual = residual.replace(Regex("(?i)\\b${Regex.escape(matches[1].matchedText)}\\b"), " ")
            }
        }

        val primaryAccountName = matchedAccount

        // 4. Category Inference
        val category = CategoryDictionary.inferCategory(sentence, type)

        // 5. Title Synthesis from residual tokens
        var cleanedTitle = residual.replace(Regex("(?i)\\b(?:from|to|transfer|with|friends|php|₱|sahod|sweldo|salary|pamasahe)\\b"), " ")
            .replace(Regex("[₱,;!?]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val finalTitle = if (cleanedTitle.isBlank()) {
            category
        } else {
            cleanedTitle.split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
        }

        return ParsedTransaction(
            type = type,
            amountCentavos = amountCentavos,
            accountName = primaryAccountName,
            toAccountName = matchedToAccount,
            category = category,
            title = finalTitle
        )
    }
}
