package com.example.budgettracker.data.preference

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _isBalanceVisible = MutableStateFlow(loadBalanceVisibility())
    val isBalanceVisible: StateFlow<Boolean> = _isBalanceVisible.asStateFlow()

    private fun loadBalanceVisibility(): Boolean {
        return prefs.getBoolean(KEY_IS_BALANCE_VISIBLE, true)
    }

    fun toggleBalanceVisibility() {
        setBalanceVisibility(!_isBalanceVisible.value)
    }

    fun setBalanceVisibility(visible: Boolean) {
        prefs.edit().putBoolean(KEY_IS_BALANCE_VISIBLE, visible).apply()
        _isBalanceVisible.value = visible
    }

    companion object {
        private const val KEY_IS_BALANCE_VISIBLE = "is_balance_visible"
    }
}
