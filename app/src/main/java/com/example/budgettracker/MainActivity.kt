package com.example.budgettracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.ui.dashboard.DashboardViewModel
import com.example.budgettracker.ui.history.TransactionHistoryViewModel
import com.example.budgettracker.ui.navigation.AppNavigation
import com.example.budgettracker.ui.parse.QuickParseViewModel
import com.example.budgettracker.ui.theme.BudgetTrackerTheme
import com.example.budgettracker.ui.theme.ThemePreferences
import com.example.budgettracker.ui.transaction.TransactionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val themePreferences = ThemePreferences(applicationContext)
        val appPreferences = com.example.budgettracker.data.preference.AppPreferences(applicationContext)

        val db = AppDatabase.getInstance(applicationContext)
        val repository = BudgetRepository(
            accountDao = db.accountDao(),
            transactionDao = db.transactionDao(),
            loanDetailsDao = db.loanDetailsDao(),
            installmentPlanDao = db.installmentPlanDao(),
            savingsDetailsDao = db.savingsDetailsDao(),
            billDetailsDao = db.billDetailsDao(),
            loanBillingCycleDao = db.loanBillingCycleDao(),
            customCategoryDao = db.customCategoryDao(),
            recurringBillDao = db.recurringBillDao(),
            creditDetailsDao = db.creditDetailsDao()
        )

        val dashboardViewModel = DashboardViewModel(repository, appPreferences = appPreferences)
        val transactionViewModel = TransactionViewModel(repository)
        val quickParseViewModel = QuickParseViewModel(repository)
        val transactionHistoryViewModel = TransactionHistoryViewModel(repository)
        val reportsViewModel = com.example.budgettracker.ui.reports.ReportsViewModel(repository, appPreferences = appPreferences)

        setContent {
            val currentTheme by themePreferences.themeSetting.collectAsState()

            BudgetTrackerTheme(themeSetting = currentTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        repository = repository,
                        dashboardViewModel = dashboardViewModel,
                        transactionViewModel = transactionViewModel,
                        quickParseViewModel = quickParseViewModel,
                        transactionHistoryViewModel = transactionHistoryViewModel,
                        reportsViewModel = reportsViewModel,
                        themePreferences = themePreferences
                    )
                }
            }
        }
    }
}