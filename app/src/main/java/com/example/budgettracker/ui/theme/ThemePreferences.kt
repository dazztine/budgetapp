package com.example.budgettracker.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _themeSetting = MutableStateFlow(loadThemeSetting())
    val themeSetting: StateFlow<ThemeSetting> = _themeSetting.asStateFlow()

    private fun loadThemeSetting(): ThemeSetting {
        val name = prefs.getString("theme_setting", ThemeSetting.LIGHT.name)
        return try {
            ThemeSetting.valueOf(name ?: ThemeSetting.LIGHT.name)
        } catch (e: Exception) {
            ThemeSetting.LIGHT
        }
    }

    fun setThemeSetting(setting: ThemeSetting) {
        prefs.edit().putString("theme_setting", setting.name).apply()
        _themeSetting.value = setting
    }
}
