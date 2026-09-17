package com.example.budgettracker.ui.transaction

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import com.example.budgettracker.ui.transaction.components.CategorySelectionBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.ui.components.InvalidMathExpressionDialog
import com.example.budgettracker.ui.theme.AmberGlow
import com.example.budgettracker.ui.theme.Green500
import com.example.budgettracker.ui.theme.MidnightNavy
import com.example.budgettracker.ui.theme.Orange500
import com.example.budgettracker.ui.theme.Red500
import com.example.budgettracker.ui.theme.Zinc50
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius
import com.example.budgettracker.ui.transaction.components.CompactAccountSelector
import com.example.budgettracker.ui.transaction.components.NumpadView
import com.example.budgettracker.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAccounts: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val amountInput by viewModel.amountInput.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val selectedToAccountId by viewModel.selectedToAccountId.collectAsState()
    val categoryInput by viewModel.categoryInput.collectAsState()
    val titleInput by viewModel.titleInput.collectAsState()
    val totalInstallmentsInput by viewModel.totalInstallmentsInput.collectAsState()
    val noteInput by viewModel.noteInput.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val categorySuggestions by viewModel.categorySuggestions.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    val isToAccountLocked by viewModel.isToAccountLocked.collectAsState()

    var showCategoryBottomSheet by remember { mutableStateOf(false) }
    var isCalculatorVisible by remember { mutableStateOf(false) }
    var showInvalidMathDialog by remember { mutableStateOf(false) }

    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val selectedToAccount = accounts.find { it.id == selectedToAccountId }

    val amountColor = when (selectedType) {
        TransactionType.INCOME -> Green500
        TransactionType.TRANSFER -> Zinc50
        TransactionType.INSTALLMENT -> Red500
        TransactionType.EXPENSE -> Orange500
    }

    LaunchedEffect(saveState) {
        when (val state = saveState) {
            is SaveResult.Success -> {
                Toast.makeText(context, if (isEditing) "Transaction Updated!" else "Transaction Saved!", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
                onNavigateBack()
            }
            is SaveResult.Error -> {
                if (state.message.contains("expression", ignoreCase = true) || state.message.contains("operator", ignoreCase = true) || state.message.contains("number after", ignoreCase = true)) {
                    showInvalidMathDialog = true
                } else {
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                viewModel.resetSaveState()
            }
            SaveResult.Idle -> {}
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = isCalculatorVisible) {
        isCalculatorVisible = false
    }

    BackHandler(enabled = !isCalculatorVisible) {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Transaction" else "Log Transaction", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
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
            val formScrollState = rememberScrollState()

            LaunchedEffect(isCalculatorVisible) {
                if (isCalculatorVisible) {
                    formScrollState.animateScrollTo(formScrollState.maxValue)
                }
            }

            // Form input fields (Scrollable top area)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(formScrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Type Selector Tabs (Equal width row, single line labels)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TransactionType.entries.forEach { type ->
                        val labelStr = when (type) {
                            TransactionType.EXPENSE -> "Expense"
                            TransactionType.INCOME -> "Income"
                            TransactionType.TRANSFER -> "Transfer"
                            TransactionType.INSTALLMENT -> "Installment"
                        }
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { viewModel.setTransactionType(type) },
                            label = {
                                Text(
                                    text = labelStr,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGlow,
                                selectedLabelColor = MidnightNavy,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedType == type,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = AmberGlow
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Account Selection Card Grid (ref-account-selector-cards.png pattern)
                val isInstallment = selectedType == TransactionType.INSTALLMENT
                val availableAccounts = when (selectedType) {
                    TransactionType.INSTALLMENT -> accounts.filter { it.type == com.example.budgettracker.data.model.AccountType.LOAN || it.type == com.example.budgettracker.data.model.AccountType.BNPL }
                    TransactionType.TRANSFER -> accounts.filter { it.id != selectedToAccountId }
                    else -> accounts
                }

                if (isInstallment && availableAccounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(ZincSoftCornerRadius))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincSoftCornerRadius))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "No loan/BNPL accounts found. Create one first.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (onNavigateToAccounts != null) {
                                TextButton(
                                    onClick = onNavigateToAccounts,
                                    colors = ButtonDefaults.textButtonColors(contentColor = AmberGlow)
                                ) {
                                    Text("Go to Accounts", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    CompactAccountSelector(
                        label = when (selectedType) {
                            TransactionType.TRANSFER -> "From Account"
                            TransactionType.INSTALLMENT -> "Select Account to Deduct"
                            else -> "Account"
                        },
                        selectedAccount = selectedAccount,
                        accounts = availableAccounts,
                        onAccountSelected = { viewModel.setAccountId(it.id) }
                    )
                }

                if (selectedType == TransactionType.TRANSFER) {
                    if (isToAccountLocked && selectedToAccount != null) {
                        CompactAccountSelector(
                            label = "To Destination Account (Locked)",
                            selectedAccount = selectedToAccount,
                            accounts = listOf(selectedToAccount),
                            onAccountSelected = { /* locked when paying bill */ }
                        )
                    } else {
                        CompactAccountSelector(
                            label = "To Destination Account",
                            selectedAccount = selectedToAccount,
                            accounts = accounts.filter { it.id != selectedAccountId },
                            onAccountSelected = { viewModel.setToAccountId(it.id) }
                        )
                    }
                }

                // Category Picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryBottomSheet = true }
                    ) {
                        OutlinedTextField(
                            value = categoryInput,
                            onValueChange = {},
                            placeholder = { Text("Select category...", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            trailingIcon = {
                                TextButton(onClick = { showCategoryBottomSheet = true }) {
                                    Text("▼", fontSize = 10.sp)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Name Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (selectedType == TransactionType.INSTALLMENT) "Item Name" else "Name",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { viewModel.setTitle(it) },
                        placeholder = {
                            Text(
                                text = if (selectedType == TransactionType.INSTALLMENT) "e.g. iPhone 15, Refrigerator" else "Enter transaction name",
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // Specific fields for INSTALLMENT
                if (selectedType == TransactionType.INSTALLMENT) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Number of Installment Months", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = totalInstallmentsInput,
                            onValueChange = { viewModel.setTotalInstallments(it) },
                            placeholder = { Text("e.g. 3, 6, 12, 24", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }

                // Remarks Input
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { viewModel.setNote(it) },
                    placeholder = { Text("Remarks (optional)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Total Amount Input Display Banner (Tap to slide up Calculator)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ZincCornerRadius))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = if (isCalculatorVisible) 1.5.dp else 1.dp,
                            color = if (isCalculatorVisible) AmberGlow else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(ZincCornerRadius)
                        )
                        .clickable { isCalculatorVisible = true }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayAmount = CurrencyUtils.formatExpressionForDisplay(amountInput)

                        Column(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (isCalculatorVisible) "Amount (Editing...)" else "Amount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCalculatorVisible) AmberGlow else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isCalculatorVisible) "Live expression" else "Tap to enter/calculate",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = displayAmount,
                            fontSize = if (displayAmount.length > 14) 20.sp else 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = amountColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Primary Save Button when Calculator is hidden
                if (!isCalculatorVisible) {
                    Button(
                        onClick = { viewModel.saveTransaction() },
                        shape = RoundedCornerShape(ZincSoftCornerRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(if (isEditing) "Update Transaction" else "Save Transaction", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Numpad Calculator Panel (Hidden by default, slides up when user taps Amount field)
            AnimatedVisibility(
                visible = isCalculatorVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                val clipboardManager = LocalClipboardManager.current
                NumpadView(
                    amountExpression = amountInput,
                    onDigitClick = { viewModel.onDigitInput(it) },
                    onDotClick = { viewModel.onDotInput() },
                    onBackspaceClick = { viewModel.onBackspace() },
                    onClearClick = { viewModel.onClear() },
                    onOperatorClick = { viewModel.onOperatorClick(it) },
                    onEqualClick = {
                        val success = viewModel.onEqualClick()
                        if (!success) {
                            showInvalidMathDialog = true
                        }
                    },
                    onConfirmClick = {
                        viewModel.onConfirmAmount()
                        isCalculatorVisible = false
                    },
                    onToggleSignClick = { viewModel.onToggleSign() },
                    onPasteClick = {
                        val text = clipboardManager.getText()?.text
                        if (!text.isNullOrBlank()) {
                            viewModel.onPasteInput(text)
                        }
                    },
                    onDismiss = { isCalculatorVisible = false }
                )
            }
        }
    }

    if (showInvalidMathDialog) {
        InvalidMathExpressionDialog(
            onDismiss = { showInvalidMathDialog = false }
        )
    }

    if (showCategoryBottomSheet) {
        CategorySelectionBottomSheet(
            transactionType = selectedType,
            selectedCategory = categoryInput,
            customCategories = customCategories,
            onCategorySelected = { cat ->
                viewModel.setCategory(cat)
                showCategoryBottomSheet = false
            },
            onAddCustomCategory = { name, iconName ->
                viewModel.addCustomCategory(name, iconName)
            },
            onDismiss = { showCategoryBottomSheet = false }
        )
    }
}
