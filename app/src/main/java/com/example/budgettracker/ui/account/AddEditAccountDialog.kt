package com.example.budgettracker.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Payment
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.sp
import com.example.budgettracker.R
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import com.example.budgettracker.data.local.entity.BillAmountType
import com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.ui.components.BalanceAdjustmentConfirmDialog
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius
import com.example.budgettracker.ui.util.BrandLogoMapper
import com.example.budgettracker.util.CurrencyUtils
import kotlin.math.abs

enum class ModalCategory(val label: String, val icon: ImageVector) {
    SAVINGS("Savings", Icons.Outlined.AccountBalance),
    E_WALLET("E-Wallet", Icons.Outlined.PhoneAndroid),
    CREDIT("Credit", Icons.Outlined.CreditCard),
    LOAN_BNPL("Loan/BNPL", Icons.Default.Payment),
    BILL("Bill", Icons.Outlined.Receipt),
    OTHER("Other", Icons.Default.MoreHoriz)
}

data class PresetItem(
    val id: String,
    val name: String,
    val defaultType: AccountType,
    val category: ModalCategory
)

private val PRESET_ITEMS = listOf(
    // Savings
    PresetItem("bdo", "BDO", AccountType.SAVINGS, ModalCategory.SAVINGS),
    PresetItem("bpi", "BPI", AccountType.SAVINGS, ModalCategory.SAVINGS),
    PresetItem("unionbank", "UnionBank", AccountType.SAVINGS, ModalCategory.SAVINGS),

    // E-Wallet
    PresetItem("gcash", "GCash", AccountType.E_WALLET, ModalCategory.E_WALLET),
    PresetItem("maya", "Maya", AccountType.E_WALLET, ModalCategory.E_WALLET),
    PresetItem("grabpay", "GrabPay", AccountType.E_WALLET, ModalCategory.E_WALLET),
    PresetItem("shopeepay", "ShopeePay", AccountType.E_WALLET, ModalCategory.E_WALLET),

    // Credit
    PresetItem("bdo_credit", "BDO Credit Card", AccountType.CREDIT, ModalCategory.CREDIT),
    PresetItem("bpi_credit", "BPI Credit Card", AccountType.CREDIT, ModalCategory.CREDIT),
    PresetItem("unionbank_credit", "UnionBank Credit Card", AccountType.CREDIT, ModalCategory.CREDIT),
    PresetItem("metrobank_credit", "Metrobank Credit Card", AccountType.CREDIT, ModalCategory.CREDIT),
    PresetItem("rcbc_credit", "RCBC Credit Card", AccountType.CREDIT, ModalCategory.CREDIT),
    PresetItem("securitybank_credit", "Security Bank Credit Card", AccountType.CREDIT, ModalCategory.CREDIT),

    // Loan/BNPL
    PresetItem("spaylater", "SPayLater", AccountType.BNPL, ModalCategory.LOAN_BNPL),
    PresetItem("homecredit", "Home Credit", AccountType.BNPL, ModalCategory.LOAN_BNPL),
    PresetItem("atome", "Atome", AccountType.BNPL, ModalCategory.LOAN_BNPL),
    PresetItem("billease", "BillEase", AccountType.BNPL, ModalCategory.LOAN_BNPL),

    // Bill
    PresetItem("meralco", "Meralco", AccountType.BILL, ModalCategory.BILL),
    PresetItem("maynilad", "Maynilad", AccountType.BILL, ModalCategory.BILL),
    PresetItem("pldt", "PLDT", AccountType.BILL, ModalCategory.BILL),

    // Other
    PresetItem("cash", "Cash", AccountType.CASH, ModalCategory.OTHER)
)

private enum class ModalStep {
    PRESET_GRID,
    DETAILS_FORM
}

data class PendingSaveData(
    val account: AccountEntity,
    val loanDetails: LoanAccountDetailsEntity?,
    val savingsDetails: SavingsAccountDetailsEntity?,
    val billDetails: BillAccountDetailsEntity?,
    val creditDetails: CreditAccountDetailsEntity?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountDialog(
    initialAccount: AccountEntity? = null,
    initialLoanDetails: LoanAccountDetailsEntity? = null,
    initialSavingsDetails: SavingsAccountDetailsEntity? = null,
    initialBillDetails: BillAccountDetailsEntity? = null,
    initialCreditDetails: CreditAccountDetailsEntity? = null,
    currentBalance: Long? = null,
    onDismiss: () -> Unit,
    onSave: (AccountEntity, LoanAccountDetailsEntity?) -> Unit = { _, _ -> },
    onSaveFull: (AccountEntity, LoanAccountDetailsEntity?, SavingsAccountDetailsEntity?, BillAccountDetailsEntity?, CreditAccountDetailsEntity?) -> Unit = { acc, loan, _, _, _ -> onSave(acc, loan) },
    onSaveWithAdjustment: (AccountEntity, LoanAccountDetailsEntity?, Long, Boolean) -> Unit = { acc, loan, _, _ -> onSave(acc, loan) },
    onSaveWithAdjustmentFull: (AccountEntity, LoanAccountDetailsEntity?, SavingsAccountDetailsEntity?, BillAccountDetailsEntity?, CreditAccountDetailsEntity?, Long, Boolean) -> Unit = { acc, loan, _, _, _, bal, adj -> onSaveWithAdjustment(acc, loan, bal, adj) },
    onSoftDelete: ((Long) -> Unit)? = null
) {
    var currentStep by remember {
        mutableStateOf(if (initialAccount == null) ModalStep.PRESET_GRID else ModalStep.DETAILS_FORM)
    }

    var selectedCategory by remember {
        mutableStateOf(
            when (initialAccount?.type) {
                AccountType.SAVINGS, AccountType.BANK -> ModalCategory.SAVINGS
                AccountType.E_WALLET -> ModalCategory.E_WALLET
                AccountType.CREDIT -> ModalCategory.CREDIT
                AccountType.BNPL, AccountType.LOAN -> ModalCategory.LOAN_BNPL
                AccountType.BILL -> ModalCategory.BILL
                AccountType.CASH -> ModalCategory.OTHER
                else -> ModalCategory.SAVINGS
            }
        )
    }

    var selectedPreset by remember {
        mutableStateOf<PresetItem?>(
            initialAccount?.presetId?.let { pid -> PRESET_ITEMS.find { it.id == pid } }
        )
    }

    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var selectedType by remember { mutableStateOf(initialAccount?.type ?: AccountType.SAVINGS) }
    var selectedPresetId by remember { mutableStateOf(initialAccount?.presetId ?: "") }

    // Starting Balance defaults to empty with placeholder 0.00
    var balanceInput by remember {
        mutableStateOf(
            if (currentBalance != null) {
                val pesos = currentBalance / 100
                val cents = currentBalance % 100
                if (cents > 0) String.format(java.util.Locale.US, "%,d.%02d", pesos, cents) else String.format(java.util.Locale.US, "%,d.00", pesos)
            } else if (initialAccount != null) {
                val pesos = initialAccount.initialBalance / 100
                val cents = initialAccount.initialBalance % 100
                if (cents > 0) String.format(java.util.Locale.US, "%,d.%02d", pesos, cents) else String.format(java.util.Locale.US, "%,d.00", pesos)
            } else ""
        )
    }

    // Bill Details expanded by default (true), Savings Details collapsed by default (false)
    var isSavingsDetailsExpanded by remember { mutableStateOf(initialSavingsDetails != null) }
    var isBillDetailsExpanded by remember { mutableStateOf(true) }
    var isLoanDetailsExpanded by remember { mutableStateOf(true) }

    // Additional fields
    var interestRateInput by remember { mutableStateOf(initialSavingsDetails?.interestRate?.toString() ?: "") }
    var goalAmountInput by remember {
        mutableStateOf(
            initialSavingsDetails?.goalAmount?.let { (it / 100).toString() } ?: ""
        )
    }

    val initialDueDaysList = remember(initialLoanDetails, initialBillDetails) {
        initialLoanDetails?.parseDueDays()
            ?: initialBillDetails?.parseDueDays()
            ?: listOf(15)
    }
    var selectedDueDays by remember { mutableStateOf<List<Int>>(initialDueDaysList) }
    var amountDueInput by remember {
        mutableStateOf(
            initialBillDetails?.amountDue?.let { (it / 100).toString() }
                ?: initialLoanDetails?.minimumAmountDue?.let { (it / 100).toString() }
                ?: ""
        )
    }
    var billAmountType by remember {
        mutableStateOf(
            if (initialBillDetails?.amountType == BillAmountType.ESTIMATED) "Estimated" else "Fixed"
        )
    }
    var creditLimitInput by remember {
        mutableStateOf(
            initialCreditDetails?.creditLimit?.let { (it / 100).toString() }
                ?: initialLoanDetails?.creditLimit?.let { (it / 100).toString() }
                ?: ""
        )
    }

    var creditStatementDueDay by remember {
        mutableIntStateOf(initialCreditDetails?.statementDueDay ?: 15)
    }

    var expandedDueDayDropdown by remember { mutableStateOf(false) }

    // Adjustment confirmation prompt state
    var pendingAccountSave by remember { mutableStateOf<PendingSaveData?>(null) }
    var pendingTargetBalCentavos by remember { mutableStateOf(0L) }
    var showAdjustmentPrompt by remember { mutableStateOf(false) }

    if (showAdjustmentPrompt && pendingAccountSave != null) {
        val pending = pendingAccountSave!!
        BalanceAdjustmentConfirmDialog(
            onConfirmYes = {
                onSaveWithAdjustmentFull(pending.account, pending.loanDetails, pending.savingsDetails, pending.billDetails, pending.creditDetails, pendingTargetBalCentavos, true)
                showAdjustmentPrompt = false
                onDismiss()
            },
            onConfirmNo = {
                onSaveWithAdjustmentFull(pending.account, pending.loanDetails, pending.savingsDetails, pending.billDetails, pending.creditDetails, pendingTargetBalCentavos, false)
                showAdjustmentPrompt = false
                onDismiss()
            },
            onDismiss = { showAdjustmentPrompt = false }
        )
    }

    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        BackHandler(enabled = currentStep == ModalStep.DETAILS_FORM && initialAccount == null) {
            currentStep = ModalStep.PRESET_GRID
        }

        BackHandler(enabled = currentStep == ModalStep.PRESET_GRID || initialAccount != null) {
            onDismiss()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.68f)
                .padding(horizontal = 18.dp)
                .padding(bottom = 14.dp)
        ) {

                // Modal Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentStep == ModalStep.DETAILS_FORM && initialAccount == null) {
                            IconButton(
                                onClick = { currentStep = ModalStep.PRESET_GRID },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Presets",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Text(
                            text = if (initialAccount == null) "Add Account" else "Edit Account",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // STEP 1: PRESET GRID VIEW
                if (currentStep == ModalStep.PRESET_GRID) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Category Pills Row (Horizontal Scroll)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(ModalCategory.entries.toTypedArray()) { cat ->
                                val isSelected = cat == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(ZincSoftCornerRadius))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = cat.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                        )
                                        Text(
                                            text = cat.label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Presets",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        // Presets 2-Column Grid
                        val categoryPresets = PRESET_ITEMS.filter { it.category == selectedCategory }
                        val allItemsForGrid = categoryPresets + listOf<PresetItem?>(null) // null represents "Custom Account"
                        val rowCount = (allItemsForGrid.size + 1) / 2

                        for (rowIndex in 0 until rowCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val item1 = allItemsForGrid.getOrNull(rowIndex * 2)
                                val item2 = allItemsForGrid.getOrNull(rowIndex * 2 + 1)

                                if (item1 != null) {
                                    PresetGridCard(
                                        preset = item1,
                                        onClick = {
                                            selectedPreset = item1
                                            name = item1.name
                                            selectedType = item1.defaultType
                                            selectedPresetId = item1.id
                                            currentStep = ModalStep.DETAILS_FORM
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else if (rowIndex * 2 < allItemsForGrid.size) {
                                    CustomAccountGridCard(
                                        onClick = {
                                            selectedPreset = null
                                            name = ""
                                            selectedType = when (selectedCategory) {
                                                ModalCategory.SAVINGS -> AccountType.SAVINGS
                                                ModalCategory.E_WALLET -> AccountType.E_WALLET
                                                ModalCategory.CREDIT -> AccountType.CREDIT
                                                ModalCategory.LOAN_BNPL -> AccountType.BNPL
                                                ModalCategory.BILL -> AccountType.BILL
                                                ModalCategory.OTHER -> AccountType.CASH
                                            }
                                            selectedPresetId = ""
                                            currentStep = ModalStep.DETAILS_FORM
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                if (item2 != null) {
                                    PresetGridCard(
                                        preset = item2,
                                        onClick = {
                                            selectedPreset = item2
                                            name = item2.name
                                            selectedType = item2.defaultType
                                            selectedPresetId = item2.id
                                            currentStep = ModalStep.DETAILS_FORM
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else if (rowIndex * 2 + 1 < allItemsForGrid.size) {
                                    CustomAccountGridCard(
                                        onClick = {
                                            selectedPreset = null
                                            name = ""
                                            selectedType = when (selectedCategory) {
                                                ModalCategory.SAVINGS -> AccountType.SAVINGS
                                                ModalCategory.E_WALLET -> AccountType.E_WALLET
                                                ModalCategory.CREDIT -> AccountType.CREDIT
                                                ModalCategory.LOAN_BNPL -> AccountType.BNPL
                                                ModalCategory.BILL -> AccountType.BILL
                                                ModalCategory.OTHER -> AccountType.CASH
                                            }
                                            selectedPresetId = ""
                                            currentStep = ModalStep.DETAILS_FORM
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Fix #4: Disabled Add Account button on preset-grid screens
                    Button(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(ZincSoftCornerRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Add Account", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // STEP 2: DETAILS FORM VIEW
                if (currentStep == ModalStep.DETAILS_FORM) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Fix #3: Selected Preset Hero Card at top (Tappable with chevron to return to Preset Grid)
                        val displayName = selectedPreset?.name ?: name.ifBlank { "Custom Account" }
                        val displayCategory = selectedCategory.label
                        val logoRes = BrandLogoMapper.getLogoResId(selectedPresetId, displayName)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ZincCornerRadius))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(ZincCornerRadius))
                                .clickable {
                                    if (initialAccount == null) {
                                        currentStep = ModalStep.PRESET_GRID
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (logoRes != null) Color.Transparent else MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (logoRes != null) {
                                            Icon(
                                                painter = painterResource(id = logoRes),
                                                contentDescription = null,
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = selectedCategory.icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = displayName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = displayCategory,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Change Preset",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Account Details Header
                        Text(
                            text = "Account Details",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        // 1. Account Name Field (optional / editable) with clear X icon
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Account Name (optional)") },
                            singleLine = true,
                            trailingIcon = {
                                if (name.isNotEmpty()) {
                                    IconButton(onClick = { name = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(ZincCornerRadius),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 2. Starting Balance Field (Hidden for Loan, BNPL, and Credit)
                        val isLoanBnplOrCredit = selectedCategory == ModalCategory.LOAN_BNPL || selectedCategory == ModalCategory.CREDIT
                        if (!isLoanBnplOrCredit) {
                            OutlinedTextField(
                                value = balanceInput,
                                onValueChange = { balanceInput = CurrencyUtils.formatAmountInput(it) },
                                label = { Text("Starting Balance *") },
                                placeholder = { Text("0.00") },
                                leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    if (balanceInput.isNotEmpty()) {
                                        IconButton(onClick = { balanceInput = "" }) {
                                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(ZincCornerRadius),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Credit Card Details
                        if (selectedCategory == ModalCategory.CREDIT) {
                            OutlinedTextField(
                                value = creditLimitInput,
                                onValueChange = { creditLimitInput = CurrencyUtils.formatAmountInput(it) },
                                label = { Text("Credit Limit *") },
                                placeholder = { Text("e.g. 50,000.00") },
                                leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    if (creditLimitInput.isNotEmpty()) {
                                        IconButton(onClick = { creditLimitInput = "" }) {
                                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(ZincCornerRadius),
                                modifier = Modifier.fillMaxWidth()
                            )

                            SingleDayPicker(
                                selectedDay = creditStatementDueDay,
                                onDaySelected = { creditStatementDueDay = it },
                                label = "Statement Due Day *"
                            )
                        }

                        // Accordion 1: Savings Details (Fix #1: Collapsed by default)
                        if (selectedCategory == ModalCategory.SAVINGS) {
                            AccordionCard(
                                title = "Savings Details",
                                icon = Icons.Outlined.AccountBalance,
                                isExpanded = isSavingsDetailsExpanded,
                                onToggle = { isSavingsDetailsExpanded = !isSavingsDetailsExpanded }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = interestRateInput,
                                        onValueChange = { interestRateInput = it },
                                        label = { Text("Interest Rate (optional)") },
                                        placeholder = { Text("e.g. 3.5") },
                                        leadingIcon = { Text("%", fontWeight = FontWeight.Bold) },
                                        trailingIcon = { Text("%", fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(ZincCornerRadius),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = goalAmountInput,
                                        onValueChange = { goalAmountInput = it },
                                        label = { Text("Goal Amount (optional)") },
                                        placeholder = { Text("e.g. 100,000") },
                                        leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) },
                                        trailingIcon = { Text("₱", fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(ZincCornerRadius),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Accordion 2: Bill Details (EXPANDED by default)
                        if (selectedCategory == ModalCategory.BILL) {
                            AccordionCard(
                                title = "Bill Details",
                                icon = Icons.Outlined.Receipt,
                                isExpanded = isBillDetailsExpanded,
                                onToggle = { isBillDetailsExpanded = !isBillDetailsExpanded }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DueDaysChipPicker(
                                        selectedDays = selectedDueDays,
                                        onDaysChanged = { selectedDueDays = it }
                                    )

                                    OutlinedTextField(
                                        value = amountDueInput,
                                        onValueChange = { amountDueInput = it },
                                        label = { Text("Typical / Expected Amount Due (₱)") },
                                        placeholder = { Text("0.00") },
                                        leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(ZincCornerRadius),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Amount Type Segmented Control
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Amount Type", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(ZincCornerRadius))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(2.dp)
                                        ) {
                                            val isFixed = billAmountType == "Fixed"
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isFixed) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                    .clickable { billAmountType = "Fixed" }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Fixed",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isFixed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (!isFixed) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                    .clickable { billAmountType = "Estimated" }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Estimated",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (!isFixed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Accordion 3: Loan/BNPL Cycle Details
                        if (selectedCategory == ModalCategory.LOAN_BNPL) {
                            AccordionCard(
                                title = "Loan / BNPL Billing Cycle",
                                icon = Icons.Outlined.CreditCard,
                                isExpanded = isLoanDetailsExpanded,
                                onToggle = { isLoanDetailsExpanded = !isLoanDetailsExpanded }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DueDaysChipPicker(
                                        selectedDays = selectedDueDays,
                                        onDaysChanged = { selectedDueDays = it }
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        OutlinedTextField(
                                            value = creditLimitInput,
                                            onValueChange = { creditLimitInput = it },
                                            label = { Text("Credit Limit (optional)") },
                                            placeholder = { Text("0.00") },
                                            leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(ZincCornerRadius),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "Setting a credit limit helps track your available credit",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = amountDueInput,
                                        onValueChange = { amountDueInput = it },
                                        label = { Text("Min Amount Due (₱)") },
                                        placeholder = { Text("0.00") },
                                        leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(ZincCornerRadius),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons Row (Add Account / Save Changes)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (initialAccount != null && onSoftDelete != null) {
                            OutlinedButton(
                                onClick = {
                                    onSoftDelete(initialAccount.id)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(ZincSoftCornerRadius),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .weight(0.4f)
                                    .height(48.dp)
                            ) {
                                Text("Delete", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (selectedCategory == ModalCategory.CREDIT) {
                                    if (creditLimitInput.isBlank() || CurrencyUtils.parseInputToCentavos(creditLimitInput) <= 0L) {
                                        Toast.makeText(context, "Please enter your credit limit", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }

                                val finalName = name.trim().ifBlank { selectedPreset?.name ?: "Custom Account" }
                                val finalType = if (selectedCategory == ModalCategory.CREDIT) AccountType.CREDIT else selectedType
                                val isDebtAccount = finalType == AccountType.LOAN || finalType == AccountType.BNPL || finalType == AccountType.CREDIT || selectedCategory == ModalCategory.LOAN_BNPL || selectedCategory == ModalCategory.CREDIT

                                val rawBalCentavos = if (isDebtAccount) 0L else CurrencyUtils.parseInputToCentavos(balanceInput)
                                val targetBalCentavos = if (isDebtAccount) 0L else rawBalCentavos

                                val account = AccountEntity(
                                    id = initialAccount?.id ?: 0L,
                                    name = finalName,
                                    type = finalType,
                                    presetId = selectedPresetId.ifEmpty { selectedPreset?.id },
                                    initialBalance = if (isDebtAccount) 0L else if (initialAccount == null) targetBalCentavos else initialAccount.initialBalance,
                                    displayOrder = initialAccount?.displayOrder ?: 0
                                )

                                val dueDaysStr = selectedDueDays.sorted().joinToString(",").ifBlank { "15" }
                                val minDue = CurrencyUtils.parseInputToCentavos(amountDueInput)

                                val loanDetails = if (selectedCategory == ModalCategory.LOAN_BNPL) {
                                    LoanAccountDetailsEntity(
                                        accountId = initialAccount?.id ?: 0L,
                                        creditLimit = if (creditLimitInput.isNotBlank()) CurrencyUtils.parseInputToCentavos(creditLimitInput) else null,
                                        dueDays = dueDaysStr,
                                        minimumAmountDue = minDue,
                                        totalRemainingBalance = if (initialAccount == null) 0L else (initialLoanDetails?.totalRemainingBalance ?: 0L),
                                        reminderEnabled = true,
                                        reminderDaysBefore = 7
                                    )
                                } else null

                                val savingsDetails = if (selectedCategory == ModalCategory.SAVINGS && (interestRateInput.isNotBlank() || goalAmountInput.isNotBlank())) {
                                    SavingsAccountDetailsEntity(
                                        accountId = initialAccount?.id ?: 0L,
                                        interestRate = interestRateInput.toDoubleOrNull(),
                                        goalAmount = if (goalAmountInput.isNotBlank()) CurrencyUtils.parseInputToCentavos(goalAmountInput) else null
                                    )
                                } else null

                                val billDetails = if (selectedCategory == ModalCategory.BILL) {
                                    BillAccountDetailsEntity(
                                        accountId = initialAccount?.id ?: 0L,
                                        dueDays = dueDaysStr,
                                        amountDue = if (amountDueInput.isNotBlank()) CurrencyUtils.parseInputToCentavos(amountDueInput) else null,
                                        amountType = if (billAmountType == "Estimated") BillAmountType.ESTIMATED else BillAmountType.FIXED
                                    )
                                } else null

                                val creditDetails = if (selectedCategory == ModalCategory.CREDIT) {
                                    CreditAccountDetailsEntity(
                                        accountId = initialAccount?.id ?: 0L,
                                        creditLimit = CurrencyUtils.parseInputToCentavos(creditLimitInput),
                                        statementDueDay = creditStatementDueDay.coerceIn(1, 31)
                                    )
                                } else null

                                if (!isDebtAccount && initialAccount != null && currentBalance != null && targetBalCentavos != currentBalance) {
                                    pendingAccountSave = PendingSaveData(account, loanDetails, savingsDetails, billDetails, creditDetails)
                                    pendingTargetBalCentavos = targetBalCentavos
                                    showAdjustmentPrompt = true
                                } else {
                                    onSaveWithAdjustmentFull(account, loanDetails, savingsDetails, billDetails, creditDetails, targetBalCentavos, false)
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(ZincSoftCornerRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = if (initialAccount == null) "Add Account" else "Save Changes",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

        }
    }
}

@Composable
private fun PresetGridCard(
    preset: PresetItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoRes = BrandLogoMapper.getLogoResId(preset.id, preset.name)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(ZincCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(ZincCornerRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (logoRes != null) Color.Transparent else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoRes != null) {
                        Icon(
                            painter = painterResource(id = logoRes),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            imageVector = preset.category.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = preset.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CustomAccountGridCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(ZincCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(ZincCornerRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Custom Account",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AccordionCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ZincCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(ZincCornerRadius))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp).padding(end = 6.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isExpanded) {
                content()
            }
        }
    }
}

@Composable
fun DueDaysChipPicker(
    selectedDays: List<Int>,
    onDaysChanged: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDropdown by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Due Day(s) of Month *",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                OutlinedButton(
                    onClick = { showAddDropdown = true },
                    shape = RoundedCornerShape(ZincSoftCornerRadius),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Day", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                DropdownMenu(
                    expanded = showAddDropdown,
                    onDismissRequest = { showAddDropdown = false }
                ) {
                    val availableDays = (1..31).filter { it !in selectedDays }
                    if (availableDays.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("All days selected") },
                            onClick = { showAddDropdown = false }
                        )
                    } else {
                        availableDays.forEach { day ->
                            DropdownMenuItem(
                                text = { Text("Day $day") },
                                onClick = {
                                    onDaysChanged((selectedDays + day).sorted())
                                    showAddDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Selected Days Row
        if (selectedDays.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                selectedDays.forEach { day ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Day $day",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (selectedDays.size > 1) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove day",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            onDaysChanged(selectedDays.filter { it != day })
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Presets Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf(
                "15th" to listOf(15),
                "30th" to listOf(30),
                "15th & 30th" to listOf(15, 30),
                "1st & 15th" to listOf(1, 15)
            )
            presets.forEach { (label, days) ->
                val isSelected = selectedDays == days
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onDaysChanged(days) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SingleDayPicker(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    label: String = "Statement Due Day *",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(ZincCornerRadius))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(ZincCornerRadius))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Day $selectedDay of each month",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Pick Due Day",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.height(240.dp)
            ) {
                (1..31).forEach { day ->
                    DropdownMenuItem(
                        text = { Text("Day $day") },
                        onClick = {
                            onDaySelected(day)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

