package com.example.budgettracker.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.sp
import com.example.budgettracker.R
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import com.example.budgettracker.data.local.entity.BillAmountType
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
    LOAN_BNPL("Loan/BNPL", Icons.Outlined.CreditCard),
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
    val billDetails: BillAccountDetailsEntity?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountDialog(
    initialAccount: AccountEntity? = null,
    initialLoanDetails: LoanAccountDetailsEntity? = null,
    initialSavingsDetails: SavingsAccountDetailsEntity? = null,
    initialBillDetails: BillAccountDetailsEntity? = null,
    currentBalance: Long? = null,
    onDismiss: () -> Unit,
    onSave: (AccountEntity, LoanAccountDetailsEntity?) -> Unit = { _, _ -> },
    onSaveFull: (AccountEntity, LoanAccountDetailsEntity?, SavingsAccountDetailsEntity?, BillAccountDetailsEntity?) -> Unit = { acc, loan, _, _ -> onSave(acc, loan) },
    onSaveWithAdjustment: (AccountEntity, LoanAccountDetailsEntity?, Long, Boolean) -> Unit = { acc, loan, _, _ -> onSave(acc, loan) },
    onSaveWithAdjustmentFull: (AccountEntity, LoanAccountDetailsEntity?, SavingsAccountDetailsEntity?, BillAccountDetailsEntity?, Long, Boolean) -> Unit = { acc, loan, _, _, bal, adj -> onSaveWithAdjustment(acc, loan, bal, adj) },
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

    var dueDayInput by remember {
        mutableStateOf(
            initialBillDetails?.dueDay?.toString()
                ?: initialLoanDetails?.cycleDay1?.toString()
                ?: "15"
        )
    }
    var dueDay2Input by remember { mutableStateOf(initialLoanDetails?.cycleDay2?.toString() ?: "") }
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

    var expandedDueDayDropdown by remember { mutableStateOf(false) }

    // Adjustment confirmation prompt state
    var pendingAccountSave by remember { mutableStateOf<PendingSaveData?>(null) }
    var pendingTargetBalCentavos by remember { mutableStateOf(0L) }
    var showAdjustmentPrompt by remember { mutableStateOf(false) }

    if (showAdjustmentPrompt && pendingAccountSave != null) {
        val pending = pendingAccountSave!!
        BalanceAdjustmentConfirmDialog(
            onConfirmYes = {
                onSaveWithAdjustmentFull(pending.account, pending.loanDetails, pending.savingsDetails, pending.billDetails, pendingTargetBalCentavos, true)
                showAdjustmentPrompt = false
                onDismiss()
            },
            onConfirmNo = {
                onSaveWithAdjustmentFull(pending.account, pending.loanDetails, pending.savingsDetails, pending.billDetails, pendingTargetBalCentavos, false)
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

                        // 2. Starting Balance Field
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

                        // Accordion 2: Bill Details (Fix #1: EXPANDED by default)
                        if (selectedCategory == ModalCategory.BILL) {
                            AccordionCard(
                                title = "Bill Details",
                                icon = Icons.Outlined.Receipt,
                                isExpanded = isBillDetailsExpanded,
                                onToggle = { isBillDetailsExpanded = !isBillDetailsExpanded }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Due Day Dropdown
                                        Box(modifier = Modifier.weight(1f)) {
                                            OutlinedTextField(
                                                value = dueDayInput,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Due Day *") },
                                                trailingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.ExpandMore,
                                                        contentDescription = null,
                                                        modifier = Modifier.clickable { expandedDueDayDropdown = true }
                                                    )
                                                },
                                                shape = RoundedCornerShape(ZincCornerRadius),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { expandedDueDayDropdown = true }
                                            )

                                            DropdownMenu(
                                                expanded = expandedDueDayDropdown,
                                                onDismissRequest = { expandedDueDayDropdown = false }
                                            ) {
                                                (1..31).forEach { day ->
                                                    DropdownMenuItem(
                                                        text = { Text(day.toString()) },
                                                        onClick = {
                                                            dueDayInput = day.toString()
                                                            expandedDueDayDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Amount Due Field
                                        OutlinedTextField(
                                            value = amountDueInput,
                                            onValueChange = { amountDueInput = it },
                                            label = { Text("Amount Due (optional)") },
                                            placeholder = { Text("2,500.00") },
                                            leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(ZincCornerRadius),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = dueDayInput,
                                            onValueChange = { dueDayInput = it },
                                            label = { Text("Due Day 1 *") },
                                            placeholder = { Text("15") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(ZincCornerRadius),
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = dueDay2Input,
                                            onValueChange = { dueDay2Input = it },
                                            label = { Text("Due Day 2 (Opt)") },
                                            placeholder = { Text("30") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(ZincCornerRadius),
                                            modifier = Modifier.weight(1f)
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
                                val finalName = name.trim().ifBlank { selectedPreset?.name ?: "Custom Account" }
                                val targetBalCentavos = CurrencyUtils.parseInputToCentavos(balanceInput)

                                val account = AccountEntity(
                                    id = initialAccount?.id ?: 0L,
                                    name = finalName,
                                    type = selectedType,
                                    presetId = selectedPresetId.ifEmpty { selectedPreset?.id },
                                    initialBalance = if (initialAccount == null) targetBalCentavos else initialAccount.initialBalance,
                                    displayOrder = initialAccount?.displayOrder ?: 0
                                )

                                val d1 = dueDayInput.toIntOrNull() ?: 15
                                val d2 = dueDay2Input.toIntOrNull()
                                val minDue = CurrencyUtils.parseInputToCentavos(amountDueInput)

                                val loanDetails = if (selectedCategory == ModalCategory.LOAN_BNPL) {
                                    LoanAccountDetailsEntity(
                                        accountId = initialAccount?.id ?: 0L,
                                        cycleDay1 = d1,
                                        cycleDay2 = d2,
                                        minimumAmountDue = minDue,
                                        totalRemainingBalance = targetBalCentavos,
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
                                        dueDay = d1,
                                        amountDue = if (amountDueInput.isNotBlank()) CurrencyUtils.parseInputToCentavos(amountDueInput) else null,
                                        amountType = if (billAmountType == "Estimated") BillAmountType.ESTIMATED else BillAmountType.FIXED
                                    )
                                } else null

                                if (initialAccount != null && currentBalance != null && targetBalCentavos != currentBalance) {
                                    pendingAccountSave = PendingSaveData(account, loanDetails, savingsDetails, billDetails)
                                    pendingTargetBalCentavos = targetBalCentavos
                                    showAdjustmentPrompt = true
                                } else {
                                    onSaveWithAdjustmentFull(account, loanDetails, savingsDetails, billDetails, targetBalCentavos, false)
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
