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

    private val _upcomingBillsViewMode = MutableStateFlow(loadUpcomingBillsViewMode())
    val upcomingBillsViewMode: StateFlow<String> = _upcomingBillsViewMode.asStateFlow()

    private fun loadUpcomingBillsViewMode(): String {
        return prefs.getString(KEY_UPCOMING_BILLS_VIEW_MODE, "VERTICAL") ?: "VERTICAL"
    }

    fun toggleUpcomingBillsViewMode() {
        val newMode = if (_upcomingBillsViewMode.value == "VERTICAL") "HORIZONTAL" else "VERTICAL"
        setUpcomingBillsViewMode(newMode)
    }

    fun setUpcomingBillsViewMode(mode: String) {
        prefs.edit().putString(KEY_UPCOMING_BILLS_VIEW_MODE, mode).apply()
        _upcomingBillsViewMode.value = mode
    }

    companion object {
        private const val KEY_IS_BALANCE_VISIBLE = "is_balance_visible"
        private const val KEY_UPCOMING_BILLS_VIEW_MODE = "upcoming_bills_view_mode"
    }
}
