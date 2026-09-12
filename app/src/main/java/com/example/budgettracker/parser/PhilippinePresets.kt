package com.example.budgettracker.parser

import com.example.budgettracker.data.model.AccountType

data class AccountPreset(
    val id: String,
    val name: String,
    val defaultType: AccountType
)

object PhilippinePresets {
    val CATALOG = listOf(
        AccountPreset("preset_gcash", "GCash", AccountType.E_WALLET),
        AccountPreset("preset_maya", "Maya", AccountType.E_WALLET),
        AccountPreset("preset_bdo", "BDO", AccountType.BANK),
        AccountPreset("preset_bpi", "BPI", AccountType.BANK),
        AccountPreset("preset_cash", "Cash", AccountType.CASH),
        AccountPreset("preset_spaylater", "SPayLater", AccountType.BNPL),
        AccountPreset("preset_billease", "Billease", AccountType.BNPL),
        AccountPreset("preset_gloan", "GLoan", AccountType.LOAN),
        AccountPreset("preset_unionbank", "UnionBank", AccountType.BANK),
        AccountPreset("preset_metrobank", "Metrobank", AccountType.BANK),
        AccountPreset("preset_landbank", "Landbank", AccountType.BANK),
        AccountPreset("preset_rcbc", "RCBC", AccountType.BANK)
    )

    fun findMatchingPreset(inputName: String): AccountPreset? {
        val clean = inputName.trim()
        if (clean.isEmpty()) return null

        // Exact match or substring match first
        for (preset in CATALOG) {
            if (clean.contains(preset.name, ignoreCase = true) ||
                LevenshteinMatcher.isFuzzyMatch(clean, preset.name)
            ) {
                return preset
            }
        }
        return null
    }
}
