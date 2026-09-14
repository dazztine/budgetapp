package com.example.budgettracker.ui.util

import com.example.budgettracker.R

object BrandLogoMapper {
    fun getLogoResId(presetId: String?, accountName: String?): Int? {
        val preset = presetId?.lowercase()?.trim() ?: ""
        val name = accountName?.lowercase()?.trim() ?: ""

        return when {
            preset.contains("gcash") || name.contains("gcash") -> R.drawable.ic_gcash
            preset.contains("maya") || name.contains("maya") || name.contains("paymaya") -> R.drawable.ic_maya
            preset.contains("grabpay") || name.contains("grabpay") || name.contains("grab") -> R.drawable.ic_grabpay
            preset.contains("shopeepay") || name.contains("shopeepay") || name.contains("shopee") -> R.drawable.ic_shopeepay
            preset.contains("bdo") || name.contains("bdo") -> R.drawable.ic_bdo
            preset.contains("bpi") || name.contains("bpi") -> R.drawable.ic_bpi
            preset.contains("unionbank") || name.contains("unionbank") || name == "ub" -> R.drawable.ic_unionbank
            preset.contains("metrobank") || name.contains("metrobank") -> R.drawable.ic_metrobank
            preset.contains("landbank") || name.contains("landbank") -> R.drawable.ic_landbank
            preset.contains("rcbc") || name.contains("rcbc") -> R.drawable.ic_rcbc
            preset.contains("cash") || name.contains("cash") -> R.drawable.ic_cash
            preset.contains("spaylater") || name.contains("spaylater") || name.contains("spay") -> R.drawable.ic_spaylater
            preset.contains("homecredit") || name.contains("home credit") || name.contains("homecredit") -> R.drawable.ic_homecredit
            preset.contains("atome") || name.contains("atome") -> R.drawable.ic_atome
            preset.contains("billease") || name.contains("billease") -> R.drawable.ic_billease
            preset.contains("gloan") || name.contains("gloan") -> R.drawable.ic_gloan
            preset.contains("meralco") || name.contains("meralco") -> R.drawable.ic_meralco
            preset.contains("maynilad") || name.contains("maynilad") -> R.drawable.ic_maynilad
            preset.contains("pldt") || name.contains("pldt") -> R.drawable.ic_pldt
            else -> null
        }
    }
}
