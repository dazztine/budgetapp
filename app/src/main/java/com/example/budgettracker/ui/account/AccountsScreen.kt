package com.example.budgettracker.ui.account

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.ui.components.AccountLimitReachedDialog
import com.example.budgettracker.ui.components.DeleteAccountBlockedDialog
import com.example.budgettracker.ui.dashboard.DashboardViewModel
import com.example.budgettracker.ui.dashboard.components.AccountCard
import com.example.budgettracker.ui.dashboard.components.NetWorthCard
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: DashboardViewModel,
    targetAccountId: Long? = null,
    scrollToPaySection: Boolean = false,
    onClearTargetAccount: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onPayBill: ((accountId: Long, amountCentavos: Long, cycleId: Long?) -> Unit)? = null,
    onLogTransactionForAccount: ((accountId: Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountsWithBalances by viewModel.activeAccountsWithBalances.collectAsState()
    val loanAccounts by viewModel.loanAccounts.collectAsState()
    val creditDetailsList by viewModel.creditDetailsList.collectAsState()
    val hiddenAccounts by viewModel.hiddenAccounts.collectAsState()
    val netWorth by viewModel.netWorth.collectAsState()

    val isBalanceVisible by viewModel.isBalanceVisible.collectAsState()
    var selectedFilterCategory by remember { mutableStateOf("All") }
    var isHiddenSectionExpanded by remember { mutableStateOf(false) }

    var selectedAccountForDetailSheet by remember { mutableStateOf<AccountWithBalance?>(null) }
    var detailScrollToPaySection by remember { mutableStateOf(false) }

    LaunchedEffect(targetAccountId, accountsWithBalances) {
        if (targetAccountId != null && accountsWithBalances.isNotEmpty()) {
            val acc = accountsWithBalances.find { it.id == targetAccountId }
            if (acc != null) {
                // Short slight pause so user sees that it landed on Accounts page before expanding the detail
                kotlinx.coroutines.delay(180)
                selectedAccountForDetailSheet = acc
                detailScrollToPaySection = scrollToPaySection
                onClearTargetAccount()
            }
        }
    }
    val detailSheetTransactions by remember(selectedAccountForDetailSheet?.id) {
        selectedAccountForDetailSheet?.id?.let { id ->
            viewModel.getTransactionsForAccount(id)
        } ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val detailSheetInstallmentPlans by remember(selectedAccountForDetailSheet?.id) {
        selectedAccountForDetailSheet?.id?.let { id ->
            viewModel.getInstallmentPlansForAccount(id)
        } ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val detailSheetPaidCycles by remember(selectedAccountForDetailSheet?.id) {
        selectedAccountForDetailSheet?.id?.let { id ->
            viewModel.getPaidBillingCyclesForAccount(id)
        } ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val detailSheetPendingCycles by remember(selectedAccountForDetailSheet?.id) {
        selectedAccountForDetailSheet?.id?.let { id ->
            viewModel.getPendingBillingCyclesForAccount(id)
        } ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    var showAccountDialog by remember { mutableStateOf(false) }
    var showAccountLimitDialog by remember { mutableStateOf(false) }
    var showDeleteBlockedDialog by remember { mutableStateOf<Long?>(null) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var loanDetailsToEdit by remember { mutableStateOf<LoanAccountDetailsEntity?>(null) }
    var savingsDetailsToEdit by remember { mutableStateOf<SavingsAccountDetailsEntity?>(null) }
    var billDetailsToEdit by remember { mutableStateOf<BillAccountDetailsEntity?>(null) }
    var creditDetailsToEdit by remember { mutableStateOf<com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity?>(null) }

    val categories = listOf("All", "Cash", "Bank", "E-Wallet", "Credit", "Loan", "Savings", "Bill")

    val filteredAccounts = accountsWithBalances.filter { acc ->
        when (selectedFilterCategory) {
            "All" -> true
            "Cash" -> acc.type == AccountType.CASH
            "Bank" -> acc.type == AccountType.BANK
            "E-Wallet" -> acc.type == AccountType.E_WALLET
            "Credit" -> acc.type == AccountType.CREDIT
            "Loan" -> acc.type == AccountType.LOAN || acc.type == AccountType.BNPL
            "Savings" -> acc.type == AccountType.SAVINGS
            "Bill" -> acc.type == AccountType.BILL
            else -> true
        }
    }

    BackHandler(enabled = selectedAccountForDetailSheet != null) {
        selectedAccountForDetailSheet = null
    }

    BackHandler(enabled = showAccountLimitDialog) {
        showAccountLimitDialog = false
    }

    BackHandler(enabled = showDeleteBlockedDialog != null) {
        showDeleteBlockedDialog = null
    }

    if (selectedAccountForDetailSheet != null) {
        val selectedAcc = selectedAccountForDetailSheet!!
        val currentAccWithBalance = accountsWithBalances.find { it.id == selectedAcc.id } ?: selectedAcc
        val loanDetail = loanAccounts.find { it.account.id == selectedAcc.id }?.loanDetails

        var currentSavingsDetail by remember(selectedAcc.id) { mutableStateOf<SavingsAccountDetailsEntity?>(null) }
        var currentBillDetail by remember(selectedAcc.id) { mutableStateOf<BillAccountDetailsEntity?>(null) }
        var currentCreditDetail by remember(selectedAcc.id) { mutableStateOf<com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity?>(null) }

        LaunchedEffect(selectedAcc.id) {
            currentSavingsDetail = viewModel.getSavingsDetailsForAccount(selectedAcc.id)
            currentBillDetail = viewModel.getBillDetailsForAccount(selectedAcc.id)
            currentCreditDetail = viewModel.getCreditDetailsForAccount(selectedAcc.id)
        }

        AccountDetailScreen(
            account = currentAccWithBalance,
            loanDetails = loanDetail,
            savingsDetails = currentSavingsDetail,
            billDetails = currentBillDetail,
            creditDetails = currentCreditDetail,
            transactions = detailSheetTransactions,
            allAccounts = accountsWithBalances,
            installmentPlans = detailSheetInstallmentPlans,
            paidCycles = detailSheetPaidCycles,
            pendingCycles = detailSheetPendingCycles,
            initialTab = 0,
            scrollToPaySection = detailScrollToPaySection,
            onNavigateBack = {
                selectedAccountForDetailSheet = null
                detailScrollToPaySection = false
            },
            onEditClick = {
                val accToEdit = selectedAcc
                selectedAccountForDetailSheet = null
                scope.launch {
                    accountToEdit = AccountEntity(
                        id = accToEdit.id,
                        name = accToEdit.name,
                        type = accToEdit.type,
                        presetId = accToEdit.presetId,
                        initialBalance = accToEdit.initialBalance,
                        isActive = accToEdit.isActive,
                        includeInNetWorth = accToEdit.includeInNetWorth,
                        displayOrder = accToEdit.displayOrder
                    )
                    loanDetailsToEdit = loanDetail
                    savingsDetailsToEdit = viewModel.getSavingsDetailsForAccount(accToEdit.id)
                    billDetailsToEdit = viewModel.getBillDetailsForAccount(accToEdit.id)
                    creditDetailsToEdit = currentCreditDetail
                    showAccountDialog = true
                }
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
            onPayBill = { accId, amt, cycleId ->
                selectedAccountForDetailSheet = null
                onPayBill?.invoke(accId, amt, cycleId)
            },
            onLogTransactionForAccount = { accId ->
                selectedAccountForDetailSheet = null
                onLogTransactionForAccount?.invoke(accId)
            },
            onUpdateCycleDueDate = { cycleId, dueDate ->
                viewModel.updateCycleDueDate(cycleId, dueDate)
            },
            onUpdateCycleAmountDue = { cycleId, amt ->
                viewModel.updateCycleAmountDue(cycleId, amt)
            },
            isBalanceVisible = isBalanceVisible,
            modifier = modifier
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Accounts", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    windowInsets = WindowInsets(top = 0.dp),
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Total Net Worth Card at Top
            NetWorthCard(
                netWorth = netWorth,
                isBalanceVisible = isBalanceVisible,
                onToggleVisibility = { viewModel.toggleBalanceVisibility() }
            )

            // 2. Category Filter Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedFilterCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(ZincSoftCornerRadius))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedFilterCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Grid of Account Cards (2-per-row) + Inline Add Card
            val itemsToDisplay = filteredAccounts
            val totalItems = itemsToDisplay.size + 1 // +1 for the inline Add Account card
            val rowCount = (totalItems + 1) / 2

            for (rowIndex in 0 until rowCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val idx1 = rowIndex * 2
                    val idx2 = rowIndex * 2 + 1

                    // First Item in Row
                    if (idx1 < itemsToDisplay.size) {
                        val acc1 = itemsToDisplay[idx1]
                        val loan1 = loanAccounts.find { it.account.id == acc1.id }
                        val credit1 = creditDetailsList.find { it.accountId == acc1.id }
                        AccountCard(
                            accountWithBalance = acc1,
                            loanDetails = loan1,
                            creditDetails = credit1,
                            isBalanceVisible = isBalanceVisible,
                            onEditClick = {
                                selectedAccountForDetailSheet = acc1
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else if (idx1 == itemsToDisplay.size) {
                        InlineAddAccountCard(
                            onClick = {
                                if (accountsWithBalances.size >= 10) {
                                    showAccountLimitDialog = true
                                } else {
                                    accountToEdit = null
                                    loanDetailsToEdit = null
                                    savingsDetailsToEdit = null
                                    billDetailsToEdit = null
                                    creditDetailsToEdit = null
                                    showAccountDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Second Item in Row
                    if (idx2 < itemsToDisplay.size) {
                        val acc2 = itemsToDisplay[idx2]
                        val loan2 = loanAccounts.find { it.account.id == acc2.id }
                        val credit2 = creditDetailsList.find { it.accountId == acc2.id }
                        AccountCard(
                            accountWithBalance = acc2,
                            loanDetails = loan2,
                            creditDetails = credit2,
                            isBalanceVisible = isBalanceVisible,
                            onEditClick = {
                                selectedAccountForDetailSheet = acc2
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else if (idx2 == itemsToDisplay.size) {
                        InlineAddAccountCard(
                            onClick = {
                                if (accountsWithBalances.size >= 10) {
                                    showAccountLimitDialog = true
                                } else {
                                    accountToEdit = null
                                    loanDetailsToEdit = null
                                    savingsDetailsToEdit = null
                                    billDetailsToEdit = null
                                    creditDetailsToEdit = null
                                    showAccountDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 4. Hidden Accounts Collapsible Section
            if (hiddenAccounts.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ZincCornerRadius))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isHiddenSectionExpanded = !isHiddenSectionExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Hidden Accounts (${hiddenAccounts.size})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = if (isHiddenSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isHiddenSectionExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isHiddenSectionExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                hiddenAccounts.forEach { hiddenAcc ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(ZincSoftCornerRadius))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = hiddenAcc.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = hiddenAcc.type.toDisplayLabel(),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                if (accountsWithBalances.size >= 10) {
                                                    showAccountLimitDialog = true
                                                } else {
                                                    viewModel.restoreAccount(hiddenAcc.id)
                                                    Toast.makeText(context, "${hiddenAcc.name} restored", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(ZincSoftCornerRadius),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Un-hide", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }

    // Add/Edit Account Modal Dialog
    if (showAccountDialog) {
        val currentBalForEdit = accountToEdit?.id?.let { editId ->
            accountsWithBalances.find { it.id == editId }?.currentBalance
        }

        AddEditAccountDialog(
            initialAccount = accountToEdit,
            initialLoanDetails = loanDetailsToEdit,
            initialSavingsDetails = savingsDetailsToEdit,
            initialBillDetails = billDetailsToEdit,
            initialCreditDetails = creditDetailsToEdit,
            currentBalance = currentBalForEdit,
            onDismiss = { showAccountDialog = false },
            onSaveWithAdjustmentFull = { account, loanDetails, savingsDetails, billDetails, creditDetails, targetBal, logAdjustment ->
                if (accountToEdit != null && currentBalForEdit != null && targetBal != currentBalForEdit) {
                    val diff = targetBal - currentBalForEdit
                    if (logAdjustment) {
                        val type = if (diff > 0) TransactionType.INCOME else TransactionType.EXPENSE
                        viewModel.saveAccount(account, loanDetails, savingsDetails, billDetails, creditDetails)
                        viewModel.logBalanceAdjustment(
                            accountId = account.id,
                            type = type,
                            amountCentavos = abs(diff),
                            title = "Balance Adjustment: ${account.name}"
                        )
                        Toast.makeText(context, "Balance adjusted & logged to history!", Toast.LENGTH_SHORT).show()
                    } else {
                        val updatedAccount = account.copy(
                            initialBalance = account.initialBalance + diff
                        )
                        viewModel.saveAccount(updatedAccount, loanDetails, savingsDetails, billDetails, creditDetails)
                        Toast.makeText(context, "Balance baseline updated!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.saveAccount(account, loanDetails, savingsDetails, billDetails, creditDetails)
                    Toast.makeText(context, "Account saved!", Toast.LENGTH_SHORT).show()
                }
            },
            onSoftDelete = { accountId ->
                scope.launch {
                    val count = viewModel.getTransactionCountForAccount(accountId)
                    if (count > 0) {
                        showDeleteBlockedDialog = accountId
                    } else {
                        viewModel.softDeleteAccount(accountId)
                        Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Account Limit Reached Dialog
    if (showAccountLimitDialog) {
        AccountLimitReachedDialog(
            onDismiss = { showAccountLimitDialog = false }
        )
    }

    // Delete Account Blocked Dialog
    showDeleteBlockedDialog?.let { accountId ->
        DeleteAccountBlockedDialog(
            onHideAccount = {
                viewModel.softDeleteAccount(accountId)
                Toast.makeText(context, "Account hidden", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showDeleteBlockedDialog = null }
        )
    }
}

@Composable
private fun InlineAddAccountCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(ZincCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .dashedBorder(
                width = 1.dp,
                color = outlineColor.copy(alpha = 0.8f),
                cornerRadius = ZincCornerRadius
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Account",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Add Account",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Text(
                    text = "New account",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp,
    on: Dp = 6.dp,
    off: Dp = 4.dp
) = drawWithContent {
    drawContent()
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(on.toPx(), off.toPx()), 0f)
    )
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
        style = stroke
    )
}
