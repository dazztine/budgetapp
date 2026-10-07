package com.example.budgettracker.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.TransactionEntity

import com.example.budgettracker.ui.plan.components.AddEditBudgetBottomSheet
import com.example.budgettracker.ui.plan.components.BudgetsSubTabContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    viewModel: PlanViewModel,
    onNavigateToSettings: () -> Unit = {},
    onPayBill: ((accountId: Long, amountCentavos: Long, cycleId: Long?) -> Unit)? = null,
    onNavigateToAccount: ((Long) -> Unit)? = null,
    onDepositToGoal: ((goalAccountId: Long, goalAccountName: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSubTab by rememberSaveable { mutableStateOf(PlanSubTab.BUDGETS) }

    var isBudgetSheetOpen by rememberSaveable { mutableStateOf(false) }
    var budgetSheetIsOverall by rememberSaveable { mutableStateOf(true) }
    var budgetSheetEditingCategory by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Plan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(top = 0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Secondary TabRow for Plan Sub-Tabs
            TabRow(
                selectedTabIndex = selectedSubTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedSubTab.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedSubTab.ordinal]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                PlanSubTab.entries.forEach { subTab ->
                    val isSelected = selectedSubTab == subTab
                    Tab(
                        selected = isSelected,
                        onClick = {
                            selectedSubTab = subTab
                            viewModel.selectSubTab(subTab)
                        },
                        text = {
                            Text(
                                text = subTab.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Sub-tab Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (val state = uiState) {
                    is PlanUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is PlanUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                        }
                    }
                    is PlanUiState.Success -> {
                        when (selectedSubTab) {
                            PlanSubTab.BUDGETS -> {
                                BudgetsSubTabContent(
                                    paceResult = state.paceResult,
                                    overallBudget = state.overallBudget,
                                    categoryBudgets = state.categoryBudgets,
                                    onSetOverallBudget = {
                                        budgetSheetIsOverall = true
                                        budgetSheetEditingCategory = null
                                        isBudgetSheetOpen = true
                                    },
                                    onAddCategoryBudget = {
                                        budgetSheetIsOverall = false
                                        budgetSheetEditingCategory = null
                                        isBudgetSheetOpen = true
                                    },
                                    onEditCategoryBudget = { categoryKey ->
                                        budgetSheetIsOverall = false
                                        budgetSheetEditingCategory = categoryKey
                                        isBudgetSheetOpen = true
                                    }
                                )
                            }
                            PlanSubTab.UPCOMING -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Upcoming sub-tab shell ready (${state.upcomingObligations.size} items)",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            PlanSubTab.GOALS -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Goals sub-tab shell ready (${state.goals.size} goals)",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            PlanSubTab.DEBTS -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Debts sub-tab shell ready (${state.debts.size} debt accounts)",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isBudgetSheetOpen && uiState is PlanUiState.Success) {
        val successState = uiState as PlanUiState.Success
        val editingBudget = if (budgetSheetIsOverall) {
            successState.overallBudget
        } else {
            successState.categoryBudgets.find { it.category == budgetSheetEditingCategory }
        }

        val daysInMonth = java.time.YearMonth.now().lengthOfMonth()

        AddEditBudgetBottomSheet(
            isOverall = budgetSheetIsOverall,
            existingBudget = editingBudget,
            availableCategories = successState.availableCategories,
            existingCategoryBudgets = successState.categoryBudgets,
            daysInMonth = daysInMonth,
            onSave = { category, centavos ->
                viewModel.upsertBudget(category, centavos, "MONTHLY")
                isBudgetSheetOpen = false
            },
            onDelete = { budget ->
                viewModel.deleteBudget(budget)
                isBudgetSheetOpen = false
            },
            onDismiss = {
                isBudgetSheetOpen = false
            }
        )
    }
}
