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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.ui.dashboard.UpcomingBillItem
import com.example.budgettracker.ui.theme.AmberGlow
import com.example.budgettracker.ui.theme.Green500
import com.example.budgettracker.ui.theme.MidnightNavy
import com.example.budgettracker.ui.theme.MutedCoral
import com.example.budgettracker.ui.theme.Orange500
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius
import com.example.budgettracker.ui.util.BrandLogoMapper
import com.example.budgettracker.util.CurrencyUtils
import com.example.budgettracker.util.DueDateStatus

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
                                UpcomingObligationsSubTabContent(
                                    upcomingObligations = state.upcomingObligations,
                                    onPayBill = { accountId, amount, cycleId ->
                                        onPayBill?.invoke(accountId, amount, cycleId)
                                    }
                                )
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

@Composable
fun UpcomingObligationsSubTabContent(
    upcomingObligations: List<UpcomingBillItem>,
    onPayBill: (accountId: Long, amountCentavos: Long, cycleId: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (upcomingObligations.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Green500.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Green500,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "No upcoming bills or payments",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "You're completely all caught up!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(upcomingObligations, key = { it.id }) { item ->
                UpcomingObligationCard(
                    item = item,
                    onPay = {
                        val accId = item.accountId
                        if (accId != null) {
                            onPayBill(accId, item.amountCentavos, item.cycleId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun UpcomingObligationCard(
    item: UpcomingBillItem,
    onPay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandLogoRes = BrandLogoMapper.getLogoResId(item.presetId, item.name)

    val typeIcon = when (item.accountType) {
        AccountType.CASH -> Icons.Outlined.AccountBalanceWallet
        AccountType.BANK, AccountType.SAVINGS -> Icons.Outlined.AccountBalance
        AccountType.E_WALLET -> Icons.Outlined.PhoneAndroid
        AccountType.BNPL, AccountType.LOAN, AccountType.CREDIT -> Icons.Outlined.CreditCard
        AccountType.BILL -> Icons.Outlined.Receipt
        AccountType.ASSET, null -> Icons.Outlined.Receipt
    }

    val (statusColor, statusText) = when (item.status) {
        DueDateStatus.OVERDUE -> Pair(MutedCoral, if (item.daysUntilDue < 0) "Overdue by ${-item.daysUntilDue}d" else "Overdue")
        DueDateStatus.DUE_SOON -> {
            if (item.daysUntilDue == 0L) Pair(Orange500, "Due Today")
            else Pair(AmberGlow, "${item.daysUntilDue}d left")
        }
        DueDateStatus.UPCOMING -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "${item.daysUntilDue}d left")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ZincCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(ZincCornerRadius)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon / Logo
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (brandLogoRes != null) {
                        Icon(
                            painter = painterResource(id = brandLogoRes),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Due: ${item.dueDate.month.name.take(3)} ${item.dueDate.dayOfMonth}, ${item.dueDate.year}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Amount and Status
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = CurrencyUtils.formatCentavosToPesos(item.amountCentavos),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            // Pay Action
            if (item.accountId != null) {
                Button(
                    onClick = onPay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGlow,
                        contentColor = MidnightNavy
                    ),
                    shape = RoundedCornerShape(ZincSoftCornerRadius),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pay",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
