package com.example.budgettracker.ui.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.ui.account.AccountsScreen
import com.example.budgettracker.ui.dashboard.DashboardScreen
import com.example.budgettracker.ui.dashboard.DashboardViewModel
import com.example.budgettracker.ui.history.TransactionHistoryScreen
import com.example.budgettracker.ui.history.TransactionHistoryViewModel
import com.example.budgettracker.ui.parse.QuickParseScreen
import com.example.budgettracker.ui.parse.QuickParseViewModel
import com.example.budgettracker.ui.reports.ReportsScreen
import com.example.budgettracker.ui.reports.ReportsViewModel
import com.example.budgettracker.ui.settings.SettingsScreen
import com.example.budgettracker.ui.transaction.ManualTransactionScreen
import com.example.budgettracker.ui.transaction.TransactionViewModel

@Composable
fun AppNavigation(
    repository: BudgetRepository,
    dashboardViewModel: DashboardViewModel,
    transactionViewModel: TransactionViewModel,
    quickParseViewModel: QuickParseViewModel,
    transactionHistoryViewModel: TransactionHistoryViewModel,
    reportsViewModel: ReportsViewModel,
    themePreferences: com.example.budgettracker.ui.theme.ThemePreferences? = null,
    modifier: Modifier = Modifier,
    initialTab: BottomTab = BottomTab.DASHBOARD
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(initialTab) }
    var showTransactionModal by remember { mutableStateOf(false) }
    var isHistoryVisible by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = !showTransactionModal && !isHistoryVisible) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    val onEditTransaction: (TransactionEntity) -> Unit = { tx ->
        transactionViewModel.loadTransactionForEdit(tx)
        showTransactionModal = true
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isHistoryVisible) {
            TransactionHistoryScreen(
                viewModel = transactionHistoryViewModel,
                onNavigateBack = { isHistoryVisible = false },
                onEditTransaction = onEditTransaction
            )
        } else {
            Scaffold(
                bottomBar = {
                    MainBottomNavigation(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (currentTab) {
                        BottomTab.DASHBOARD -> {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                repository = repository,
                                onNavigateToAddTransaction = {
                                    transactionViewModel.resetFormForNextEntry()
                                    showTransactionModal = true
                                },
                                onNavigateToAccounts = { currentTab = BottomTab.ACCOUNTS },
                                onNavigateToHistory = { isHistoryVisible = true },
                                onEditTransaction = onEditTransaction
                            )
                        }

                        BottomTab.SMART_PARSER -> {
                            QuickParseScreen(
                                viewModel = quickParseViewModel,
                                onNavigateBack = { currentTab = BottomTab.DASHBOARD }
                            )
                        }

                        BottomTab.ACCOUNTS -> {
                            AccountsScreen(
                                viewModel = dashboardViewModel,
                                onEditTransaction = onEditTransaction
                            )
                        }

                        BottomTab.REPORTS -> {
                            ReportsScreen(
                                viewModel = reportsViewModel
                            )
                        }

                        BottomTab.SETTINGS -> {
                            SettingsScreen(
                                repository = repository,
                                themePreferences = themePreferences
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Animated Overlay Modal for Manual Transaction Logging / Editing
        AnimatedVisibility(
            visible = showTransactionModal,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            ManualTransactionScreen(
                viewModel = transactionViewModel,
                onNavigateBack = {
                    showTransactionModal = false
                    transactionViewModel.resetFormForNextEntry()
                }
            )
        }
    }
}
