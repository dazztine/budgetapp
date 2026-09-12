package com.example.budgettracker.ui.account

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.ui.dashboard.DashboardViewModel
import com.example.budgettracker.ui.dashboard.components.AccountCard
import com.example.budgettracker.ui.dashboard.components.NetWorthCard
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accountsWithBalances by viewModel.activeAccountsWithBalances.collectAsState()
    val loanAccounts by viewModel.loanAccounts.collectAsState()
    val netWorth by viewModel.netWorth.collectAsState()

    var selectedFilterCategory by remember { mutableStateOf("All") }

    var showAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var loanDetailsToEdit by remember { mutableStateOf<LoanAccountDetailsEntity?>(null) }

    val categories = listOf("All", "Cash", "Bank", "E-Wallet", "BNPL", "Loan", "Savings")

    val filteredAccounts = accountsWithBalances.filter { acc ->
        when (selectedFilterCategory) {
            "All" -> true
            "Cash" -> acc.type == AccountType.CASH
            "Bank" -> acc.type == AccountType.BANK
            "E-Wallet" -> acc.type == AccountType.E_WALLET
            "BNPL" -> acc.type == AccountType.BNPL
            "Loan" -> acc.type == AccountType.LOAN
            "Savings" -> acc.type == AccountType.SAVINGS
            else -> true
        }
    }

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
                onClick = {}
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
                        AccountCard(
                            accountWithBalance = acc1,
                            loanDetails = loan1,
                            onEditClick = {
                                accountToEdit = AccountEntity(
                                    id = acc1.id,
                                    name = acc1.name,
                                    type = acc1.type,
                                    presetId = acc1.presetId,
                                    initialBalance = acc1.initialBalance,
                                    isActive = acc1.isActive,
                                    displayOrder = acc1.displayOrder
                                )
                                loanDetailsToEdit = loan1?.loanDetails
                                showAccountDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else if (idx1 == itemsToDisplay.size) {
                        InlineAddAccountCard(
                            onClick = {
                                if (accountsWithBalances.size >= 10) {
                                    Toast.makeText(context, "Account limit reached (max 10 accounts)", Toast.LENGTH_SHORT).show()
                                } else {
                                    accountToEdit = null
                                    loanDetailsToEdit = null
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
                        AccountCard(
                            accountWithBalance = acc2,
                            loanDetails = loan2,
                            onEditClick = {
                                accountToEdit = AccountEntity(
                                    id = acc2.id,
                                    name = acc2.name,
                                    type = acc2.type,
                                    presetId = acc2.presetId,
                                    initialBalance = acc2.initialBalance,
                                    isActive = acc2.isActive,
                                    displayOrder = acc2.displayOrder
                                )
                                loanDetailsToEdit = loan2?.loanDetails
                                showAccountDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else if (idx2 == itemsToDisplay.size) {
                        InlineAddAccountCard(
                            onClick = {
                                if (accountsWithBalances.size >= 10) {
                                    Toast.makeText(context, "Account limit reached (max 10 accounts)", Toast.LENGTH_SHORT).show()
                                } else {
                                    accountToEdit = null
                                    loanDetailsToEdit = null
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
            currentBalance = currentBalForEdit,
            onDismiss = { showAccountDialog = false },
            onSaveWithAdjustment = { account, loanDetails, targetBal, logAdjustment ->
                if (accountToEdit != null && currentBalForEdit != null && targetBal != currentBalForEdit) {
                    val diff = targetBal - currentBalForEdit
                    if (logAdjustment) {
                        val type = if (diff > 0) TransactionType.INCOME else TransactionType.EXPENSE
                        viewModel.saveAccount(account, loanDetails)
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
                        viewModel.saveAccount(updatedAccount, loanDetails)
                        Toast.makeText(context, "Balance baseline updated!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.saveAccount(account, loanDetails)
                    Toast.makeText(context, "Account saved!", Toast.LENGTH_SHORT).show()
                }
            },
            onSoftDelete = { accountId ->
                viewModel.softDeleteAccount(accountId)
            }
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
            .height(96.dp)
            .clip(RoundedCornerShape(ZincCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .dashedBorder(
                width = 1.5.dp,
                color = outlineColor,
                cornerRadius = ZincCornerRadius
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Account",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Add Account",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
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
