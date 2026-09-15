package com.example.budgettracker.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.ui.account.AccountDetailSheet
import com.example.budgettracker.ui.backup.BackupRestoreDialog
import com.example.budgettracker.ui.components.DeleteTransactionConfirmDialog
import com.example.budgettracker.ui.dashboard.components.AccountCard
import com.example.budgettracker.ui.dashboard.components.NetWorthCard
import com.example.budgettracker.ui.dashboard.components.RecentTransactionsList
import com.example.budgettracker.ui.theme.Green500
import com.example.budgettracker.ui.theme.Orange500
import com.example.budgettracker.ui.theme.Red500
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius
import com.example.budgettracker.util.CurrencyUtils
import com.example.budgettracker.util.LoanDateUtils
import kotlinx.coroutines.flow.flowOf
import androidx.compose.runtime.LaunchedEffect
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    repository: BudgetRepository,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onEditTransaction: (TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val netWorth by viewModel.netWorth.collectAsState()
    val accountsWithBalances by viewModel.activeAccountsWithBalances.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val loanAccounts by viewModel.loanAccounts.collectAsState()
    val billAccounts by viewModel.billAccounts.collectAsState()
    val activeInstallments by viewModel.activeInstallmentPlans.collectAsState()
    val monthlyTotals by viewModel.monthlyTotals.collectAsState()

    var showBackupDialog by remember { mutableStateOf(false) }
    val isBalanceVisible by viewModel.isBalanceVisible.collectAsState()
    var transactionToDelete by remember { mutableStateOf<com.example.budgettracker.data.local.entity.TransactionEntity?>(null) }
    var selectedAccountForDetailSheet by remember { mutableStateOf<com.example.budgettracker.data.local.entity.AccountWithBalance?>(null) }

    val detailSheetTransactions by remember(selectedAccountForDetailSheet?.id) {
        selectedAccountForDetailSheet?.id?.let { id ->
            viewModel.getTransactionsForAccount(id)
        } ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    BackHandler(enabled = selectedAccountForDetailSheet != null) {
        selectedAccountForDetailSheet = null
    }

    BackHandler(enabled = transactionToDelete != null) {
        transactionToDelete = null
    }

    BackHandler(enabled = showBackupDialog) {
        showBackupDialog = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MyWallet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(top = 0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Net Worth Hero Card
            NetWorthCard(
                netWorth = netWorth,
                isBalanceVisible = isBalanceVisible,
                onToggleVisibility = { viewModel.toggleBalanceVisibility() },
                useBrandOwlToggle = true
            )

            // Accounts Section (2x2 Grid + See More)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Accounts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(onClick = onNavigateToAccounts) {
                        Text("See all", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (accountsWithBalances.isEmpty()) {
                    Text(
                        text = "No active accounts added yet.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    val dashboardAccounts = accountsWithBalances.take(4)
                    val chunked = dashboardAccounts.chunked(2)

                    chunked.forEach { rowAccounts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowAccounts.forEach { accountItem ->
                                val loanDetail = loanAccounts.find { it.account.id == accountItem.id }
                                AccountCard(
                                    accountWithBalance = accountItem,
                                    loanDetails = loanDetail,
                                    isBalanceVisible = isBalanceVisible,
                                    onEditClick = { selectedAccountForDetailSheet = accountItem },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowAccounts.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (accountsWithBalances.size > 4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ZincSoftCornerRadius))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincSoftCornerRadius))
                                .clickable(onClick = onNavigateToAccounts)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "See more",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // "This Month" Income / Expense Summary Cards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "This Month",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income Summary Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(ZincSoftCornerRadius))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincSoftCornerRadius))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NorthEast,
                                    contentDescription = "Income",
                                    tint = Green500,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    text = CurrencyUtils.formatCentavosToPesos(monthlyTotals.totalIncome),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Green500
                                )
                            }
                            Text(
                                text = "Income",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Expense Summary Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(ZincSoftCornerRadius))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincSoftCornerRadius))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SouthWest,
                                    contentDescription = "Expense",
                                    tint = Orange500,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    text = CurrencyUtils.formatCentavosToPesos(monthlyTotals.totalExpense),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Orange500
                                )
                            }
                            Text(
                                text = "Expense",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // "Upcoming Due Dates" Carousel
            val dueItems = remember(loanAccounts, billAccounts, activeInstallments) {
                val list = mutableListOf<UpcomingDueItem>()
                val today = LocalDate.now()

                for (loan in loanAccounts) {
                    val details = loan.loanDetails ?: continue
                    val dueDate = LoanDateUtils.calculateNextDueDate(today, details.cycleDay1, details.cycleDay2)
                    val monthStr = dueDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                    val dayStr = dueDate.dayOfMonth
                    list.add(
                        UpcomingDueItem(
                            title = loan.account.name,
                            dateDisplay = "$monthStr $dayStr",
                            minPayment = details.minimumAmountDue
                        )
                    )
                }

                for (bill in billAccounts) {
                    val details = bill.billDetails ?: continue
                    val dueDate = LoanDateUtils.calculateNextDueDate(today, details.dueDay, null)
                    val monthStr = dueDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                    val dayStr = dueDate.dayOfMonth
                    list.add(
                        UpcomingDueItem(
                            title = bill.account.name,
                            dateDisplay = "$monthStr $dayStr",
                            minPayment = details.amountDue ?: 0L
                        )
                    )
                }

                for (plan in activeInstallments) {
                    val acc = accountsWithBalances.find { it.id == plan.accountId }
                    val date = Date(plan.purchaseDate)
                    val format = SimpleDateFormat("MMM d", Locale.getDefault())
                    list.add(
                        UpcomingDueItem(
                            title = plan.title,
                            dateDisplay = format.format(date),
                            minPayment = plan.monthlyPaymentAmount
                        )
                    )
                }

                list
            }

            if (dueItems.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upcoming Due Dates",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        TextButton(onClick = onNavigateToAccounts) {
                            Text("See all", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(dueItems) { due ->
                            DueCard(item = due)
                        }
                    }
                }
            }

            // Recent Activity Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(onClick = onNavigateToHistory) {
                        Text("View All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                val accountsList = accountsWithBalances.map {
                    AccountEntity(
                        id = it.id,
                        name = it.name,
                        type = it.type,
                        presetId = it.presetId,
                        initialBalance = it.initialBalance,
                        isActive = it.isActive,
                        displayOrder = it.displayOrder
                    )
                }

                RecentTransactionsList(
                    transactions = recentTransactions,
                    accounts = accountsList,
                    onTransactionClick = onEditTransaction,
                    onDeleteClick = { transactionToDelete = it }
                )
            }
        }
    }

    transactionToDelete?.let { tx ->
        DeleteTransactionConfirmDialog(
            onConfirmDelete = {
                viewModel.deleteTransaction(tx)
                transactionToDelete = null
            },
            onDismiss = { transactionToDelete = null }
        )
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            repository = repository,
            onDismiss = { showBackupDialog = false }
        )
    }

    selectedAccountForDetailSheet?.let { selectedAcc ->
        val currentAccWithBalance = accountsWithBalances.find { it.id == selectedAcc.id } ?: selectedAcc
        val loanDetail = loanAccounts.find { it.account.id == selectedAcc.id }?.loanDetails

        var currentSavingsDetail by remember(selectedAcc.id) { mutableStateOf<com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity?>(null) }
        var currentBillDetail by remember(selectedAcc.id) { mutableStateOf<com.example.budgettracker.data.local.entity.BillAccountDetailsEntity?>(null) }

        LaunchedEffect(selectedAcc.id) {
            currentSavingsDetail = viewModel.getSavingsDetailsForAccount(selectedAcc.id)
            currentBillDetail = viewModel.getBillDetailsForAccount(selectedAcc.id)
        }

        AccountDetailSheet(
            account = currentAccWithBalance,
            loanDetails = loanDetail,
            savingsDetails = currentSavingsDetail,
            billDetails = currentBillDetail,
            transactions = detailSheetTransactions,
            allAccounts = accountsWithBalances.map {
                AccountEntity(
                    id = it.id,
                    name = it.name,
                    type = it.type,
                    presetId = it.presetId,
                    initialBalance = it.initialBalance,
                    isActive = it.isActive,
                    includeInNetWorth = it.includeInNetWorth,
                    displayOrder = it.displayOrder
                )
            },
            onDismiss = { selectedAccountForDetailSheet = null },
            onEditClick = {
                selectedAccountForDetailSheet = null
                onNavigateToAccounts()
            },
            onNetWorthToggle = { include ->
                viewModel.updateNetWorthInclusion(selectedAcc.id, include)
            },
            onDeleteTransaction = { tx ->
                viewModel.deleteTransaction(tx)
            },
            onEditTransaction = { tx ->
                selectedAccountForDetailSheet = null
                onEditTransaction(tx)
            },
            isBalanceVisible = isBalanceVisible
        )
    }
}

private data class UpcomingDueItem(
    val title: String,
    val dateDisplay: String,
    val minPayment: Long
)

@Composable
private fun DueCard(item: UpcomingDueItem) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(ZincSoftCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincSoftCornerRadius))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = item.dateDisplay,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.minPayment > 0L) {
                Text(
                    text = "Min ${CurrencyUtils.formatCentavosToPesos(item.minPayment)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Red500
                )
            }
        }
    }
}
