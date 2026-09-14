package com.example.budgettracker.parser

import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.model.TransactionType

object SingleTransactionParser {

    fun parse(sentence: String, knownAccounts: List<AccountEntity>): ParsedTransaction {
        if (sentence.isBlank()) {
            return ParsedTransaction(
                type = TransactionType.EXPENSE,
                amountCentavos = 0L,
                accountName = null,
                category = "General",
                title = ""
            )
        }

        var working = sentence.trim()

        // -------------------------------------------------------------
        // Pillar 1: Morphological Affix De-prefixing (nag-Grab -> Grab, etc.)
        // -------------------------------------------------------------
        val deaffixedTokens = working.split("\\s+".toRegex()).map { token ->
            val stripped = TaglishLexicon.stripAffix(token)
            if (stripped.isNotBlank() && stripped != token) stripped else token
        }
        val normalizedSentence = deaffixedTokens.joinToString(" ")
        val lower = normalizedSentence.lowercase()

        // -------------------------------------------------------------
        // Pillar 2: Amount Slot Extraction (supports ₱, k, /mo, Tagalog words)
        // -------------------------------------------------------------
        var (amountCentavos, matchedAmtStr) = ParserUtils.parseAmountCentavos(working)

        // If no digit amount found, check Tagalog number phrases (e.g. "isang libo", "limang daan")
        if (amountCentavos == 0L) {
            for ((phrase, value) in TaglishLexicon.TAGALOG_NUMBER_MAP) {
                if (lower.contains(phrase)) {
                    amountCentavos = value * 100L
                    matchedAmtStr = phrase
                    break
                }
            }
        }

        // Check for Installment terms (e.g., "6 months", "6 mos", "12 buwan")
        var totalInstallments: Int? = null
        val installmentRegex = Regex("(?i)\\b(\\d{1,2})\\s*(?:months?|mos?|buwan)\\b")
        val instMatch = installmentRegex.find(working)
        if (instMatch != null) {
            totalInstallments = instMatch.groupValues[1].toIntOrNull()
        }

        val isPerMonth = lower.contains("/mo") || lower.contains("per month")
        if (isPerMonth && amountCentavos > 0L) {
            val months = totalInstallments ?: 6
            if (totalInstallments == null) {
                totalInstallments = 6
            }
            amountCentavos *= months
        }

        // -------------------------------------------------------------
        // Pillar 4: Intent Classification (TRANSFER, INCOME, INSTALLMENT, EXPENSE)
        // -------------------------------------------------------------
        val isTransfer = TaglishLexicon.TRANSFER_KEYWORDS.any { lower.contains(Regex("(?i)\\b$it\\b")) } ||
                (lower.contains(" to ") && (lower.contains("from ") || lower.contains("lipat") || lower.contains("send")))

        val isIncome = lower.startsWith("+") ||
                TaglishLexicon.INCOME_KEYWORDS.any { lower.contains(Regex("(?i)\\b$it\\b")) }

        val isInstallment = totalInstallments != null ||
                lower.contains("/mo") || lower.contains("per month") ||
                TaglishLexicon.INSTALLMENT_KEYWORDS.any { lower.contains(Regex("(?i)\\b$it\\b")) }

        val type = when {
            isTransfer -> TransactionType.TRANSFER
            isInstallment -> TransactionType.INSTALLMENT
            isIncome -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }

        // -------------------------------------------------------------
        // Pillars 3 & 5: Account Slot Matching (Marker Grammar + Strict Canonical Lock)
        // -------------------------------------------------------------
        data class AccountCandidate(val name: String, val index: Int, val length: Int)
        val matchedAccounts = mutableListOf<AccountCandidate>()

        // 1. Check tokens right after PAYMENT_METHOD_MARKERS ("gamit gcash", "via maya", etc.)
        for (marker in TaglishLexicon.PAYMENT_METHOD_MARKERS) {
            val markerRegex = Regex("(?i)\\b${Regex.escape(marker)}\\s+([A-Za-z0-9_]+)")
            val match = markerRegex.find(working)
            if (match != null) {
                val candidateToken = match.groupValues[1]
                val matchedAcc = findMatchingAccount(candidateToken, knownAccounts)
                if (matchedAcc != null && matchedAccounts.none { it.name.equals(matchedAcc, ignoreCase = true) }) {
                    matchedAccounts.add(AccountCandidate(matchedAcc, match.range.first, match.value.length))
                }
            }
        }

        // 2. Check tokens right after DESTINATION_MARKERS ("to maya", "papunta sa bpi", etc.)
        for (marker in TaglishLexicon.DESTINATION_MARKERS) {
            val markerRegex = Regex("(?i)\\b${Regex.escape(marker)}\\s+([A-Za-z0-9_]+)")
            val match = markerRegex.find(working)
            if (match != null) {
                val candidateToken = match.groupValues[1]
                val matchedAcc = findMatchingAccount(candidateToken, knownAccounts)
                if (matchedAcc != null && matchedAccounts.none { it.name.equals(matchedAcc, ignoreCase = true) }) {
                    matchedAccounts.add(AccountCandidate(matchedAcc, match.range.first, match.value.length))
                }
            }
        }

        // 3. Scan whole sentence for exact word boundary match on known accounts & presets
        if (matchedAccounts.isEmpty() || (isTransfer && matchedAccounts.size < 2)) {
            for (acc in knownAccounts) {
                val accRegex = Regex("(?i)\\b${Regex.escape(acc.name)}\\b")
                val m = accRegex.find(working)
                if (m != null && matchedAccounts.none { it.name.equals(acc.name, ignoreCase = true) }) {
                    matchedAccounts.add(AccountCandidate(acc.name, m.range.first, m.value.length))
                }
            }
        }

        // 4. Fallback to PhilippinePresets catalog exact match
        if (matchedAccounts.isEmpty() || (isTransfer && matchedAccounts.size < 2)) {
            for (preset in PhilippinePresets.CATALOG) {
                val pRegex = Regex("(?i)\\b${Regex.escape(preset.name)}\\b")
                val m = pRegex.find(working)
                if (m != null && matchedAccounts.none { it.name.equals(preset.name, ignoreCase = true) }) {
                    matchedAccounts.add(AccountCandidate(preset.name, m.range.first, m.value.length))
                }
            }
        }

        // 5. Fuzzy match on individual tokens (Pillar 5: voice typos like "gecash", "bpii")
        if (matchedAccounts.isEmpty()) {
            val tokens = working.split("\\s+".toRegex())
            for (token in tokens) {
                val cleanedToken = token.replace(Regex("[^A-Za-z0-9]"), "")
                if (cleanedToken.length < 3) continue

                val fuzzyAcc = findMatchingAccount(cleanedToken, knownAccounts)
                if (fuzzyAcc != null && matchedAccounts.none { it.name.equals(fuzzyAcc, ignoreCase = true) }) {
                    val idx = working.indexOf(token)
                    matchedAccounts.add(AccountCandidate(fuzzyAcc, idx, token.length))
                    break
                }
            }
        }

        // Sort matched accounts by position in sentence
        matchedAccounts.sortBy { it.index }

        val primaryAccountName: String? = matchedAccounts.getOrNull(0)?.name
        val toAccountName: String? = if (isTransfer && matchedAccounts.size > 1) matchedAccounts[1].name else null

        // -------------------------------------------------------------
        // Pillar 6: Category Inference & Residual Title Synthesis
        // -------------------------------------------------------------
        val category = CategoryDictionary.inferCategory(normalizedSentence, type)

        // Build clean residual title
        var residual = working

        // Remove matched amount
        if (matchedAmtStr != null) {
            residual = residual.replace(matchedAmtStr, " ")
        }
        // Remove installment string
        if (instMatch != null) {
            residual = residual.replace(instMatch.value, " ")
        }
        residual = residual.replace(Regex("(?i)\\b(?:/mo|per month|buwan-buwan)\\b"), " ")

        // Remove matched account names from residual (Canonical Lock ensures accounts are removed cleanly)
        for (candidate in matchedAccounts) {
            residual = residual.replace(Regex("(?i)\\b${Regex.escape(candidate.name)}\\b"), " ")
        }
        // Also remove catalog preset names if present
        for (preset in PhilippinePresets.CATALOG) {
            residual = residual.replace(Regex("(?i)\\b${Regex.escape(preset.name)}\\b"), " ")
        }

        // Remove payment/destination markers
        for (m in TaglishLexicon.PAYMENT_METHOD_MARKERS) {
            residual = residual.replace(Regex("(?i)\\b${Regex.escape(m)}\\b"), " ")
        }
        for (m in TaglishLexicon.DESTINATION_MARKERS) {
            residual = residual.replace(Regex("(?i)\\b${Regex.escape(m)}\\b"), " ")
        }

        // Remove common intent verbs from residual
        for (v in TaglishLexicon.TRANSFER_KEYWORDS + TaglishLexicon.INCOME_KEYWORDS + TaglishLexicon.EXPENSE_KEYWORDS) {
            residual = residual.replace(Regex("(?i)\\b${Regex.escape(v)}\\b"), " ")
        }

        // Split residual into tokens and filter out connective filler words
        val residualTokens = residual.split("\\s+".toRegex())
            .map { it.replace(Regex("[^A-Za-z0-9'-]"), "").trim() }
            .filter { it.isNotBlank() }
            .filter { !TaglishLexicon.CONNECTIVE_FILLER_WORDS.contains(it.lowercase()) }
            .map { TaglishLexicon.stripAffix(it) } // strip prefixes like nag-Grab -> Grab
            .filter { it.isNotBlank() && !TaglishLexicon.CONNECTIVE_FILLER_WORDS.contains(it.lowercase()) }

        val finalTitle = if (residualTokens.isNotEmpty()) {
            residualTokens.joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
        } else {
            category
        }

        return ParsedTransaction(
            type = type,
            amountCentavos = amountCentavos,
            accountName = primaryAccountName,
            toAccountName = toAccountName,
            category = category,
            title = finalTitle,
            totalInstallments = totalInstallments
        )
    }

    private fun findMatchingAccount(token: String, knownAccounts: List<AccountEntity>): String? {
        val clean = token.lowercase().trim()
        if (clean.isBlank()) return null

        // 1. Exact match known accounts
        for (acc in knownAccounts) {
            if (acc.name.equals(clean, ignoreCase = true)) return acc.name
        }
        // 2. Exact match presets
        for (preset in PhilippinePresets.CATALOG) {
            if (preset.name.equals(clean, ignoreCase = true) || preset.id.equals(clean, ignoreCase = true)) {
                return preset.name
            }
        }
        // 3. Fuzzy match known accounts
        for (acc in knownAccounts) {
            if (LevenshteinMatcher.isFuzzyMatch(clean, acc.name)) return acc.name
        }
        // 4. Fuzzy match presets
        for (preset in PhilippinePresets.CATALOG) {
            if (LevenshteinMatcher.isFuzzyMatch(clean, preset.name)) return preset.name
        }

        return null
    }
}

