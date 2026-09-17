package com.example.budgettracker.ui.account

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.LoanBillingCycleEntity
import com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.ui.components.DeleteTransactionConfirmDialog
import com.example.budgettracker.ui.theme.AmberGlow
import com.example.budgettracker.ui.theme.Green500
import com.example.budgettracker.ui.theme.MidnightNavy
import com.example.budgettracker.ui.theme.Orange500
import com.example.budgettracker.ui.theme.ZincCornerRadius
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius
import com.example.budgettracker.ui.util.BrandLogoMapper
import com.example.budgettracker.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AccountDetailBodyContent(
    account: AccountWithBalance,
    loanDetails: LoanAccountDetailsEntity? = null,
    savingsDetails: SavingsAccountDetailsEntity? = null,
    billDetails: BillAccountDetailsEntity? = null,
    creditDetails: CreditAccountDetailsEntity? = null,
    transactions: List<TransactionEntity>,
    allAccounts: List<AccountWithBalance>,
    installmentPlans: List<InstallmentPlanEntity> = emptyList(),
    paidCycles: List<LoanBillingCycleEntity> = emptyList(),
    pendingCycles: List<LoanBillingCycleEntity> = emptyList(),
    initialTab: Int = 0,
    onEditClick: () -> Unit,
    onNetWorthToggle: (Boolean) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit = {},
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onPayBill: (accountId: Long, amountCentavos: Long, cycleId: Long?) -> Unit = { _, _, _ -> },
    onLogTransactionForAccount: (accountId: Long) -> Unit = {},
    onUpdateCycleDueDate: (cycleId: Long, dueDateMillis: Long) -> Unit = { _, _ -> },
    onUpdateCycleAmountDue: (cycleId: Long, amountDueCentavos: Long) -> Unit = { _, _ -> },
    isBalanceVisible: Boolean = true
) {
    val isLoan = account.type == AccountType.LOAN || account.type == AccountType.BNPL
    val isCredit = account.type == AccountType.CREDIT

    if (isCredit) {
        CreditAccountDetailContent(
            account = account,
            creditDetails = creditDetails,
            transactions = transactions,
            allAccounts = allAccounts,
            paidCycles = paidCycles,
            pendingCycles = pendingCycles,
            initialTab = initialTab,
            onEditClick = onEditClick,
            onNetWorthToggle = onNetWorthToggle,
            onDeleteTransaction = onDeleteTransaction,
            onEditTransaction = onEditTransaction,
            onPayBill = onPayBill,
            onLogTransactionForAccount = onLogTransactionForAccount,
            onUpdateCycleDueDate = onUpdateCycleDueDate,
            onUpdateCycleAmountDue = onUpdateCycleAmountDue,
            isBalanceVisible = isBalanceVisible
        )
    } else if (isLoan) {
        LoanAccountDetailContent(
            account = account,
            loanDetails = loanDetails,
            transactions = transactions,
            allAccounts = allAccounts,
            installmentPlans = installmentPlans,
            paidCycles = paidCycles,
            pendingCycles = pendingCycles,
            initialTab = initialTab,
            onEditClick = onEditClick,
            onNetWorthToggle = onNetWorthToggle,
            onDeleteTransaction = onDeleteTransaction,
            onEditTransaction = onEditTransaction,
            onPayBill = onPayBill,
            onLogTransactionForAccount = onLogTransactionForAccount,
            onUpdateCycleDueDate = onUpdateCycleDueDate,
            onUpdateCycleAmountDue = onUpdateCycleAmountDue,
            isBalanceVisible = isBalanceVisible
        )
    } else {
        StandardAccountDetailContent(
            account = account,
            loanDetails = loanDetails,
            savingsDetails = savingsDetails,
            billDetails = billDetails,
            transactions = transactions,
            allAccounts = allAccounts,
            onEditClick = onEditClick,
            onNetWorthToggle = onNetWorthToggle,
            onDeleteTransaction = onDeleteTransaction,
            onEditTransaction = onEditTransaction,
            isBalanceVisible = isBalanceVisible
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailSheet(
    account: AccountWithBalance,
    loanDetails: LoanAccountDetailsEntity? = null,
    savingsDetails: SavingsAccountDetailsEntity? = null,
    billDetails: BillAccountDetailsEntity? = null,
    creditDetails: CreditAccountDetailsEntity? = null,
    transactions: List<TransactionEntity>,
    allAccounts: List<AccountWithBalance>,
    installmentPlans: List<InstallmentPlanEntity> = emptyList(),
    paidCycles: List<LoanBillingCycleEntity> = emptyList(),
    pendingCycles: List<LoanBillingCycleEntity> = emptyList(),
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onNetWorthToggle: (Boolean) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit = {},
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onPayBill: (accountId: Long, amountCentavos: Long, cycleId: Long?) -> Unit = { _, _, _ -> },
    onLogTransactionForAccount: (accountId: Long) -> Unit = {},
    onUpdateCycleDueDate: (cycleId: Long, dueDateMillis: Long) -> Unit = { _, _ -> },
    onUpdateCycleAmountDue: (cycleId: Long, amountDueCentavos: Long) -> Unit = { _, _ -> },
    isBalanceVisible: Boolean = true
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                BottomSheetDefaults.DragHandle()
            }
        }
    ) {
        AccountDetailBodyContent(
            account = account,
            loanDetails = loanDetails,
            savingsDetails = savingsDetails,
            billDetails = billDetails,
            creditDetails = creditDetails,
            transactions = transactions,
            allAccounts = allAccounts,
            installmentPlans = installmentPlans,
            paidCycles = paidCycles,
            pendingCycles = pendingCycles,
            initialTab = initialTab,
            onEditClick = onEditClick,
            onNetWorthToggle = onNetWorthToggle,
            onDeleteTransaction = { transactionToDelete = it },
            onEditTransaction = onEditTransaction,
            onPayBill = onPayBill,
            onLogTransactionForAccount = onLogTransactionForAccount,
            onUpdateCycleDueDate = onUpdateCycleDueDate,
            onUpdateCycleAmountDue = onUpdateCycleAmountDue,
            isBalanceVisible = isBalanceVisible
        )
    }

    transactionToDelete?.let { tx ->
        DeleteTransactionConfirmDialog(
            onConfirmDelete = {
                onDeleteTransaction(tx)
                transactionToDelete = null
            },
            onDismiss = { transactionToDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    account: AccountWithBalance,
    loanDetails: LoanAccountDetailsEntity? = null,
    savingsDetails: SavingsAccountDetailsEntity? = null,
    billDetails: BillAccountDetailsEntity? = null,
    creditDetails: CreditAccountDetailsEntity? = null,
    transactions: List<TransactionEntity>,
    allAccounts: List<AccountWithBalance>,
    installmentPlans: List<InstallmentPlanEntity> = emptyList(),
    paidCycles: List<LoanBillingCycleEntity> = emptyList(),
    pendingCycles: List<LoanBillingCycleEntity> = emptyList(),
    initialTab: Int = 0,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onNetWorthToggle: (Boolean) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit = {},
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onPayBill: (accountId: Long, amountCentavos: Long, cycleId: Long?) -> Unit = { _, _, _ -> },
    onLogTransactionForAccount: (accountId: Long) -> Unit = {},
    onUpdateCycleDueDate: (cycleId: Long, dueDateMillis: Long) -> Unit = { _, _ -> },
    onUpdateCycleAmountDue: (cycleId: Long, amountDueCentavos: Long) -> Unit = { _, _ -> },
    isBalanceVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = account.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Account",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AccountDetailBodyContent(
                account = account,
                loanDetails = loanDetails,
                savingsDetails = savingsDetails,
                billDetails = billDetails,
                creditDetails = creditDetails,
                transactions = transactions,
                allAccounts = allAccounts,
                installmentPlans = installmentPlans,
                paidCycles = paidCycles,
                pendingCycles = pendingCycles,
                initialTab = initialTab,
                onEditClick = onEditClick,
                onNetWorthToggle = onNetWorthToggle,
                onDeleteTransaction = { transactionToDelete = it },
                onEditTransaction = onEditTransaction,
                onPayBill = onPayBill,
                onLogTransactionForAccount = onLogTransactionForAccount,
                onUpdateCycleDueDate = onUpdateCycleDueDate,
                onUpdateCycleAmountDue = onUpdateCycleAmountDue,
                isBalanceVisible = isBalanceVisible
            )
        }
    }

    transactionToDelete?.let { tx ->
        DeleteTransactionConfirmDialog(
            onConfirmDelete = {
                onDeleteTransaction(tx)
                transactionToDelete = null
            },
            onDismiss = { transactionToDelete = null }
        )
    }
}

@Composable
private fun CreditAccountDetailContent(
    account: AccountWithBalance,
    creditDetails: CreditAccountDetailsEntity?,
    transactions: List<TransactionEntity>,
    allAccounts: List<AccountWithBalance>,
    paidCycles: List<LoanBillingCycleEntity>,
    pendingCycles: List<LoanBillingCycleEntity> = emptyList(),
    initialTab: Int = 0,
    onEditClick: () -> Unit,
    onNetWorthToggle: (Boolean) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onPayBill: (accountId: Long, amountCentavos: Long, cycleId: Long?) -> Unit,
    onLogTransactionForAccount: (accountId: Long) -> Unit,
    onUpdateCycleDueDate: (cycleId: Long, dueDateMillis: Long) -> Unit,
    onUpdateCycleAmountDue: (cycleId: Long, amountDueCentavos: Long) -> Unit,
    isBalanceVisible: Boolean
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var cycleToEditAmount by remember { mutableStateOf<LoanBillingCycleEntity?>(null) }

    val showDatePickerForCycle: (LoanBillingCycleEntity) -> Unit = { cycle ->
        val cycleCal = Calendar.getInstance().apply { timeInMillis = cycle.cycleDueDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onUpdateCycleDueDate(cycle.id, selectedCal.timeInMillis)
            },
            cycleCal.get(Calendar.YEAR),
            cycleCal.get(Calendar.MONTH),
            cycleCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {

            // Header Top Row: Logo, Name, Type Label ("Credit"), Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val brandLogoRes = BrandLogoMapper.getLogoResId(account.presetId, account.name)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (brandLogoRes != null) Color.Transparent
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (brandLogoRes != null) {
                            Icon(
                                painter = painterResource(id = brandLogoRes),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(44.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = account.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = account.type.toDisplayLabel(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(ZincSoftCornerRadius),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Account",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Header Card: "Available to spend" as primary, credit limit + owed as secondary stats
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ZincCornerRadius))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Available to spend",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val creditLimit = creditDetails?.creditLimit ?: 0L
                    val howMuchYouCanSpend = creditLimit + account.currentBalance

                    Text(
                        text = if (creditDetails != null) {
                            if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(howMuchYouCanSpend.coerceAtLeast(0L)) else "₱ ••••••"
                        } else "—",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Credit limit",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (creditDetails != null) {
                                    if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(creditLimit) else "₱ ••••••"
                                } else "Not set",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "What you owe",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val owedAmount = account.currentBalance
                            val owedFormatted = when {
                                !isBalanceVisible -> "₱ ••••••"
                                owedAmount < 0L -> CurrencyUtils.formatCentavosToPesos(owedAmount)
                                else -> "₱0.00"
                            }
                            Text(
                                text = owedFormatted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (owedAmount < 0L) Color(0xFFF87171) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Net Worth Toggle Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ZincCornerRadius))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Add to Total Net Worth",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Include this account's balance in net worth calculation",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = account.includeInNetWorth,
                        onCheckedChange = onNetWorthToggle
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Instructional Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ZincSoftCornerRadius))
                    .background(Color(0xFFF7D88F).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFF7D88F).copy(alpha = 0.35f), RoundedCornerShape(ZincSoftCornerRadius))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "💡 Use the 'Pay Bill' button to record your Credit Card payments.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Exactly 2 Tabs: Current | Paid
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AmberGlow,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AmberGlow
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Current",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) AmberGlow else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Paid",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) AmberGlow else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Contents
            when (selectedTab) {
                0 -> {
                    // TAB 1: CURRENT (Statement Card or Friendly empty state)
                    val pendingCycle = pendingCycles.firstOrNull()
                    if (pendingCycle == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ZincCornerRadius))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
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
                                    text = "You're all caught up!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "No payments due right now.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Statement Card
                        val today = LocalDate.now()
                        val cycleDueDate = Instant.ofEpochMilli(pendingCycle.cycleDueDate).atZone(ZoneId.systemDefault()).toLocalDate()
                        val daysDiff = ChronoUnit.DAYS.between(today, cycleDueDate)

                        val (badgeText, badgeColor) = when {
                            daysDiff < 0 -> Pair(
                                if (daysDiff == -1L) "1 day overdue" else "${-daysDiff} days overdue",
                                Color(0xFFF87171)
                            )
                            daysDiff == 0L -> Pair("Due today", AmberGlow)
                            daysDiff in 1L..3L -> Pair(
                                if (daysDiff == 1L) "Due in 1 day" else "Due in $daysDiff days",
                                Orange500
                            )
                            else -> Pair(
                                "Due " + SimpleDateFormat("MMM d", Locale.US).format(Date(pendingCycle.cycleDueDate)),
                                MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ZincCornerRadius))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Header: "This month's statement" + Status Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "This month's statement",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeColor.copy(alpha = 0.15f))
                                            .border(1.dp, badgeColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor
                                        )
                                    }
                                }

                                // Due Date Row
                                val formattedDueDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(pendingCycle.cycleDueDate))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showDatePickerForCycle(pendingCycle) },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Due Date",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formattedDueDate,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    TextButton(
                                        onClick = { showDatePickerForCycle(pendingCycle) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = "Change Due Date",
                                            tint = AmberGlow,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Change", fontSize = 12.sp, color = AmberGlow)
                                    }
                                }

                                // Amount Due Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Amount Due",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(pendingCycle.amountDue) else "₱ ••••••",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    TextButton(
                                        onClick = { cycleToEditAmount = pendingCycle },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Change Statement Amount",
                                            tint = AmberGlow,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit", fontSize = 12.sp, color = AmberGlow)
                                    }
                                }

                                // Amber Glow "Pay Bill" button
                                Button(
                                    onClick = { onPayBill(account.id, pendingCycle.amountDue, pendingCycle.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AmberGlow,
                                        contentColor = MidnightNavy
                                    ),
                                    shape = RoundedCornerShape(ZincSoftCornerRadius),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payment,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Pay Bill",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Recent transactions
                    if (transactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ZincCornerRadius))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "To record your card spending, please log the transactions you made.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Recent Transactions (${transactions.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            transactions.forEach { tx ->
                                AccountTransactionItemRow(
                                    transaction = tx,
                                    currentAccountId = account.id,
                                    accounts = allAccounts,
                                    onTransactionClick = { onEditTransaction(tx) },
                                    onDeleteClick = { onDeleteTransaction(tx) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 2: PAID (Settled Statements)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (paidCycles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(ZincCornerRadius))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No paid statements yet.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Settled Statements",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            paidCycles.forEach { cycle ->
                                val paidDateStr = SimpleDateFormat("MMM d, yyyy", Locale.US)
                                    .format(Date(cycle.paidDate ?: cycle.createdAt))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(ZincCornerRadius))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Paid on $paidDateStr",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = CurrencyUtils.formatCentavosToPesos(cycle.paidAmount ?: cycle.amountDue),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Green500.copy(alpha = 0.15f))
                                                .border(1.dp, Green500, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Green500,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Paid in full",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Green500
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }

        // Floating quick-add button (+)
        FloatingActionButton(
            onClick = { onLogTransactionForAccount(account.id) },
            containerColor = AmberGlow,
            contentColor = MidnightNavy,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log Transaction",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    cycleToEditAmount?.let { cycle ->
        EditAmountDueDialog(
            initialAmountCentavos = cycle.amountDue,
            title = "Change statement amount",
            subtitle = "Enter the statement amount due:",
            onConfirm = { newAmount ->
                onUpdateCycleAmountDue(cycle.id, newAmount)
                cycleToEditAmount = null
            },
            onDismiss = { cycleToEditAmount = null }
        )
    }
}

@Composable
private fun LoanAccountDetailContent(
    account: AccountWithBalance,
    loanDetails: LoanAccountDetailsEntity?,
    transactions: List<TransactionEntity>,
    allAccounts: List<AccountWithBalance>,
    installmentPlans: List<InstallmentPlanEntity>,
    paidCycles: List<LoanBillingCycleEntity>,
    pendingCycles: List<LoanBillingCycleEntity> = emptyList(),
    initialTab: Int = 0,
    onEditClick: () -> Unit,
    onNetWorthToggle: (Boolean) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onPayBill: (accountId: Long, amountCentavos: Long, cycleId: Long?) -> Unit,
    onLogTransactionForAccount: (accountId: Long) -> Unit,
    onUpdateCycleDueDate: (cycleId: Long, dueDateMillis: Long) -> Unit,
    onUpdateCycleAmountDue: (cycleId: Long, amountDueCentavos: Long) -> Unit,
    isBalanceVisible: Boolean
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var cycleToEditAmount by remember { mutableStateOf<LoanBillingCycleEntity?>(null) }

    val showDatePickerForCycle: (LoanBillingCycleEntity) -> Unit = { cycle ->
        val cycleCal = Calendar.getInstance().apply { timeInMillis = cycle.cycleDueDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onUpdateCycleDueDate(cycle.id, selectedCal.timeInMillis)
            },
            cycleCal.get(Calendar.YEAR),
            cycleCal.get(Calendar.MONTH),
            cycleCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {

            // Header Top Row: Logo, Name, Type Label, Edit Button

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val brandLogoRes = BrandLogoMapper.getLogoResId(account.presetId, account.name)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (brandLogoRes != null) Color.Transparent
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (brandLogoRes != null) {
                            Icon(
                                painter = painterResource(id = brandLogoRes),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(44.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = account.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = account.type.toDisplayLabel(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(ZincSoftCornerRadius),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Account",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Header Card: Credit Limit as primary (or Total Owed fallback), debt as secondary stat
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ZincCornerRadius))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val hasLimit = loanDetails?.creditLimit != null

                    Text(
                        text = if (hasLimit) "Credit limit" else "Total owed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (hasLimit) {
                        // Primary: credit limit — represents spending power, neutral color
                        Text(
                            text = if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(loanDetails!!.creditLimit) else "₱ ••••••",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        // No limit set — fall back to Total Owed as primary in Muted Coral
                        val totalOwedFormatted = if (account.currentBalance == 0L) {
                            "-₱0.00"
                        } else if (account.currentBalance < 0) {
                            CurrencyUtils.formatCentavosToPesos(account.currentBalance)
                        } else {
                            "-${CurrencyUtils.formatCentavosToPesos(account.currentBalance)}"
                        }
                        Text(
                            text = if (isBalanceVisible) totalOwedFormatted else "₱ ••••••",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171) // Muted Coral
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Two-Column Row: Available Credit & What you owe
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val availableCredit = BudgetRepository.calculateAvailableCredit(
                            loanDetails?.creditLimit,
                            account.currentBalance
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Available credit",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (loanDetails?.creditLimit != null) {
                                    if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(availableCredit ?: 0L) else "₱ ••••••"
                                } else "—",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "What you owe",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val owedAmount = account.currentBalance
                            val owedFormatted = when {
                                !isBalanceVisible -> "₱ ••••••"
                                owedAmount < 0L -> CurrencyUtils.formatCentavosToPesos(owedAmount)
                                else -> "₱0.00"
                            }
                            Text(
                                text = owedFormatted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (owedAmount < 0L) Color(0xFFF87171) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Net Worth Toggle Switch Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ZincCornerRadius))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Add to Total Net Worth",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Include this account's balance in net worth calculation",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = account.includeInNetWorth,
                        onCheckedChange = onNetWorthToggle
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Instructional Banner (Soft Beige #F7D88F background)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ZincSoftCornerRadius))
                    .background(Color(0xFFF7D88F).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFF7D88F).copy(alpha = 0.35f), RoundedCornerShape(ZincSoftCornerRadius))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "💡 Use the 'Pay Bill' button to record your Loan Bill payments.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Tabs: Current | Installments | Paid
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AmberGlow,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AmberGlow
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Current",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) AmberGlow else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Installments",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) AmberGlow else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "Paid",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) AmberGlow else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Contents
            when (selectedTab) {
                0 -> {
                    // TAB 1: CURRENT (Billing Cycle Cards or Caught up empty state)
                    if (pendingCycles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ZincCornerRadius))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
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
                                    text = "You're all caught up!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "No payments due right now.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            pendingCycles.forEach { cycle ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(ZincCornerRadius))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
                                        .padding(16.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Header of Card: Title + PAYMENT DUE Badge
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Billing Cycle",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFF87171).copy(alpha = 0.15f))
                                                    .border(1.dp, Color(0xFFF87171), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "PAYMENT DUE",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFF87171)
                                                )
                                            }
                                        }

                                        // Due Date Row
                                        val formattedDueDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(cycle.cycleDueDate))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Due Date",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = formattedDueDate,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            TextButton(
                                                onClick = { showDatePickerForCycle(cycle) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarToday,
                                                    contentDescription = "Change Due Date",
                                                    tint = AmberGlow,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Change", fontSize = 12.sp, color = AmberGlow)
                                            }
                                        }

                                        // Amount Due Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Amount Due",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(cycle.amountDue) else "₱ ••••••",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            TextButton(
                                                onClick = { cycleToEditAmount = cycle },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Amount Due",
                                                    tint = AmberGlow,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Edit", fontSize = 12.sp, color = AmberGlow)
                                            }
                                        }

                                        // Pay Bill Action Button
                                        Button(
                                            onClick = { onPayBill(account.id, cycle.amountDue, cycle.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AmberGlow,
                                                contentColor = MidnightNavy
                                            ),
                                            shape = RoundedCornerShape(ZincSoftCornerRadius),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Payment,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Pay Bill",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Empty transactions hint
                    if (transactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ZincCornerRadius))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "To record your Loan Bill, please log the transactions you made.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Recent Transactions (${transactions.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            transactions.forEach { tx ->
                                AccountTransactionItemRow(
                                    transaction = tx,
                                    currentAccountId = account.id,
                                    accounts = allAccounts,
                                    onTransactionClick = { onEditTransaction(tx) },
                                    onDeleteClick = { onDeleteTransaction(tx) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 2: INSTALLMENTS
                    val activePlans = installmentPlans.filter { it.remainingBalance > 0 }
                    val totalInstallmentOwed = BudgetRepository.calculateInstallmentPlansTotal(activePlans)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "From installment plans: ${CurrencyUtils.formatCentavosToPesos(totalInstallmentOwed)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (activePlans.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(ZincCornerRadius))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No active installment plans.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            activePlans.forEach { plan ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(ZincCornerRadius))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = plan.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${CurrencyUtils.formatCentavosToPesos(plan.monthlyPaymentAmount)} / mo",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Remaining: ${CurrencyUtils.formatCentavosToPesos(plan.remainingBalance)}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${plan.installmentsPaid} of ${plan.totalInstallments} months paid",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        val progress = (plan.installmentsPaid.toFloat() / maxOf(1, plan.totalInstallments).toFloat()).coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = AmberGlow,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 3: PAID
                    val completedPlans = installmentPlans.filter { it.remainingBalance <= 0 }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (paidCycles.isEmpty() && completedPlans.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(ZincCornerRadius))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No paid cycles or completed installments yet.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            if (paidCycles.isNotEmpty()) {
                                Text(
                                    text = "Settled Billing Cycles",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                paidCycles.forEach { cycle ->
                                    val paidDateStr = SimpleDateFormat("MMM d, yyyy", Locale.US)
                                        .format(Date(cycle.paidDate ?: cycle.createdAt))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(ZincCornerRadius))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Paid on $paidDateStr",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = CurrencyUtils.formatCentavosToPesos(cycle.paidAmount ?: cycle.amountDue),
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Green500.copy(alpha = 0.15f))
                                                    .border(1.dp, Green500, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Settled",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Green500
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (completedPlans.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Completed Installments",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                completedPlans.forEach { plan ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(ZincCornerRadius))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = plan.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Total: ${CurrencyUtils.formatCentavosToPesos(plan.totalPurchaseAmount)}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Green500.copy(alpha = 0.15f))
                                                    .border(1.dp, Green500, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Completed",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Green500
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }

        // Floating quick-add button (+)
        FloatingActionButton(
            onClick = { onLogTransactionForAccount(account.id) },
            containerColor = AmberGlow,
            contentColor = MidnightNavy,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log Transaction",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    cycleToEditAmount?.let { cycle ->
        EditAmountDueDialog(
            initialAmountCentavos = cycle.amountDue,
            onConfirm = { newAmount ->
                onUpdateCycleAmountDue(cycle.id, newAmount)
                cycleToEditAmount = null
            },
            onDismiss = { cycleToEditAmount = null }
        )
    }
}

@Composable
private fun EditAmountDueDialog(
    initialAmountCentavos: Long,
    title: String = "Update Amount Due",
    subtitle: String = "Enter the amount due for the current cycle:",
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var textInput by remember {
        val pesos = initialAmountCentavos / 100
        val cents = initialAmountCentavos % 100
        mutableStateOf(if (cents > 0) "$pesos.${String.format(Locale.US, "%02d", cents)}" else if (pesos > 0) pesos.toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Amount Due") },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(ZincCornerRadius),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = CurrencyUtils.parseInputToCentavos(textInput)
                    onConfirm(parsed)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberGlow, contentColor = MidnightNavy)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun StandardAccountDetailContent(
    account: AccountWithBalance,
    loanDetails: LoanAccountDetailsEntity?,
    savingsDetails: SavingsAccountDetailsEntity?,
    billDetails: BillAccountDetailsEntity?,
    transactions: List<TransactionEntity>,
    allAccounts: List<AccountWithBalance>,
    onEditClick: () -> Unit,
    onNetWorthToggle: (Boolean) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    isBalanceVisible: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Header: Account Info & Edit Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                val brandLogoRes = BrandLogoMapper.getLogoResId(account.presetId, account.name)
                val typeIcon = when (account.type) {
                    AccountType.CASH -> Icons.Outlined.AccountBalanceWallet
                    AccountType.BANK, AccountType.SAVINGS -> Icons.Outlined.AccountBalance
                    AccountType.E_WALLET -> Icons.Outlined.PhoneAndroid
                    AccountType.BNPL, AccountType.LOAN, AccountType.CREDIT -> Icons.Outlined.CreditCard
                    AccountType.BILL -> Icons.Outlined.Receipt
                    AccountType.ASSET -> Icons.Outlined.AccountBalance
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (brandLogoRes != null) Color.Transparent
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (brandLogoRes != null) {
                        Icon(
                            painter = painterResource(id = brandLogoRes),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(44.dp)
                        )
                    } else {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = account.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = account.type.toDisplayLabel(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onEditClick,
                shape = RoundedCornerShape(ZincSoftCornerRadius),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Account",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Balance Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ZincCornerRadius))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Current Balance",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(account.currentBalance) else "₱ ••••••",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (savingsDetails != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (savingsDetails.interestRate != null) {
                            Text(
                                text = "Interest: ${savingsDetails.interestRate}% p.a.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (savingsDetails.goalAmount != null) {
                            Text(
                                text = if (isBalanceVisible) "Goal: ${CurrencyUtils.formatCentavosToPesos(savingsDetails.goalAmount)}" else "Goal: ₱ ••••••",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (billDetails != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val dueDays = billDetails.parseDueDays()
                        val dueDaysText = dueDays.joinToString(", ") { "$it${getDayOfMonthSuffix(it)}" }
                        Text(
                            text = "Due Day(s): Every $dueDaysText",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (billDetails.amountDue != null) {
                            Text(
                                text = if (isBalanceVisible) "Amount Due: ${CurrencyUtils.formatCentavosToPesos(billDetails.amountDue)}" else "Amount Due: ₱ ••••••",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Net Worth Toggle Switch Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ZincCornerRadius))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Add to Total Net Worth",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Include this account's balance in net worth calculation",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = account.includeInNetWorth,
                    onCheckedChange = onNetWorthToggle
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transaction History Section Header
        Text(
            text = "Transactions (${transactions.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Transaction List
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ZincCornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transactions recorded for this account",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    AccountTransactionItemRow(
                        transaction = tx,
                        currentAccountId = account.id,
                        accounts = allAccounts,
                        onTransactionClick = { onEditTransaction(tx) },
                        onDeleteClick = { onDeleteTransaction(tx) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun AccountTransactionItemRow(
    transaction: TransactionEntity,
    currentAccountId: Long,
    accounts: List<AccountWithBalance>,
    onTransactionClick: () -> Unit = {},
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.US)
    val formattedDate = dateFormat.format(Date(transaction.timestamp))

    val isTransfer = transaction.type == TransactionType.TRANSFER
    val isIncomingTransfer = isTransfer && transaction.toAccountId == currentAccountId

    val isPositive = transaction.type == TransactionType.INCOME || isIncomingTransfer

    val amountPrefix = if (isPositive) "+" else "-"
    val amountColor = if (isPositive) Green500 else Orange500

    val subTitle = if (isTransfer) {
        if (isIncomingTransfer) {
            val fromName = accounts.find { it.id == transaction.accountId }?.name ?: "Account #${transaction.accountId}"
            "From $fromName • $formattedDate"
        } else {
            val toName = accounts.find { it.id == transaction.toAccountId }?.name ?: "Account #${transaction.toAccountId}"
            "To $toName • $formattedDate"
        }
    } else {
        formattedDate
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ZincCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
            .clickable(onClick = onTransactionClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = transaction.title.ifEmpty { transaction.category.ifEmpty { transaction.type.name } },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subTitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$amountPrefix${CurrencyUtils.formatCentavosToPesos(transaction.amount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Transaction",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun getDayOfMonthSuffix(n: Int): String {
    if (n in 11..13) return "th"
    return when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}
