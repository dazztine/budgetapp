package com.example.budgettracker.ui.util

import com.example.budgettracker.R

object BrandLogoMapper {
    fun getLogoResId(presetId: String?, accountName: String?): Int? {
        val preset = presetId?.lowercase()?.trim()
        val name = accountName?.lowercase()?.trim() ?: ""

        return when {
            preset == "gcash" || name.contains("gcash") -> R.drawable.ic_gcash
            preset == "maya" || name.contains("maya") || name.contains("paymaya") -> R.drawable.ic_maya
            preset == "bdo" || name.contains("bdo") -> R.drawable.ic_bdo
            preset == "bpi" || name.contains("bpi") -> R.drawable.ic_bpi
            preset == "cash" || name.contains("cash") -> R.drawable.ic_cash
            preset == "spaylater" || name.contains("spaylater") || name.contains("spay") -> R.drawable.ic_spaylater
            preset == "billease" || name.contains("billease") -> R.drawable.ic_billease
            preset == "gloan" || name.contains("gloan") -> R.drawable.ic_gloan
            preset == "unionbank" || name.contains("unionbank") || name == "ub" -> R.drawable.ic_unionbank
            preset == "metrobank" || name.contains("metrobank") -> R.drawable.ic_metrobank
            preset == "landbank" || name.contains("landbank") -> R.drawable.ic_landbank
            preset == "rcbc" || name.contains("rcbc") -> R.drawable.ic_rcbc
            else -> null
        }
    }
}
