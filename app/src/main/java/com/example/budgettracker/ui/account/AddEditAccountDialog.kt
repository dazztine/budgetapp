package com.example.budgettracker.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.util.CurrencyUtils
import kotlin.math.abs

@Composable
fun AddEditAccountDialog(
    initialAccount: AccountEntity? = null,
    initialLoanDetails: LoanAccountDetailsEntity? = null,
    currentBalance: Long? = null,
    onDismiss: () -> Unit,
    onSave: (AccountEntity, LoanAccountDetailsEntity?) -> Unit = { _, _ -> },
    onSaveWithAdjustment: (AccountEntity, LoanAccountDetailsEntity?, Long, Boolean) -> Unit = { acc, loan, _, _ -> onSave(acc, loan) },
    onSoftDelete: ((Long) -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var selectedType by remember { mutableStateOf(initialAccount?.type ?: AccountType.E_WALLET) }
    var selectedPresetId by remember { mutableStateOf(initialAccount?.presetId ?: "") }
    var balanceInput by remember {
        mutableStateOf(
            if (currentBalance != null) {
                val pesos = currentBalance / 100
                val cents = currentBalance % 100
                if (cents > 0) String.format(java.util.Locale.US, "%d.%02d", pesos, cents) else pesos.toString()
            } else if (initialAccount != null) {
                val pesos = initialAccount.initialBalance / 100
                val cents = initialAccount.initialBalance % 100
                if (cents > 0) String.format(java.util.Locale.US, "%d.%02d", pesos, cents) else pesos.toString()
            } else ""
        )
    }

    // Loan details state
    var cycleDay1 by remember { mutableStateOf(initialLoanDetails?.cycleDay1?.toString() ?: "") }
    var cycleDay2 by remember { mutableStateOf(initialLoanDetails?.cycleDay2?.toString() ?: "") }
    var minDueInput by remember { mutableStateOf(initialLoanDetails?.minimumAmountDue?.let { (it / 100).toString() } ?: "") }

    var expandedType by remember { mutableStateOf(false) }

    // Adjustment confirmation prompt state
    var pendingAccountSave by remember { mutableStateOf<Pair<AccountEntity, LoanAccountDetailsEntity?>?>(null) }
    var pendingTargetBalCentavos by remember { mutableStateOf(0L) }
    var showAdjustmentPrompt by remember { mutableStateOf(false) }

    if (showAdjustmentPrompt && pendingAccountSave != null) {
        val (acc, loan) = pendingAccountSave!!
        val diff = pendingTargetBalCentavos - (currentBalance ?: 0L)

        AlertDialog(
            onDismissRequest = { showAdjustmentPrompt = false },
            title = { Text("Log Balance Adjustment?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Balance changed by ${if (diff > 0) "+" else ""}${CurrencyUtils.formatCentavosToPesos(diff)}.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Do you want to log this adjustment in your transaction history as a transaction record?",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Yes: Creates an ${if (diff > 0) "Income" else "Expense"} (Adjustment) transaction.\n• No: Silently updates stored account baseline.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveWithAdjustment(acc, loan, pendingTargetBalCentavos, true)
                        showAdjustmentPrompt = false
                        onDismiss()
                    }
                ) {
                    Text("Yes (Log Record)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onSaveWithAdjustment(acc, loan, pendingTargetBalCentavos, false)
                        showAdjustmentPrompt = false
                        onDismiss()
                    }
                ) {
                    Text("No (Silent Update)")
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(ZincCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (initialAccount == null) "Add New Account" else "Edit Account",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 1. Account Name Field (First)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(ZincCornerRadius),
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Account Type Selector (Second)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = { Text("▼", fontSize = 12.sp) },
                        shape = RoundedCornerShape(ZincCornerRadius),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedType = true }
                    )

                    DropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        AccountType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                // 3. Balance Input Field (Third)
                OutlinedTextField(
                    value = balanceInput,
                    onValueChange = { balanceInput = it },
                    label = { Text(if (initialAccount == null) "Starting Balance (₱)" else "Account Balance (₱)") },
                    singleLine = true,
                    shape = RoundedCornerShape(ZincCornerRadius),
                    modifier = Modifier.fillMaxWidth()
                )

                // 4. Loan / BNPL Cycle Details (if LOAN or BNPL type selected)
                if (selectedType == AccountType.LOAN || selectedType == AccountType.BNPL) {
                    Text("Loan / BNPL Billing Cycle", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = cycleDay1,
                            onValueChange = { cycleDay1 = it },
                            label = { Text("Due Day 1") },
                            singleLine = true,
                            shape = RoundedCornerShape(ZincCornerRadius),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = cycleDay2,
                            onValueChange = { cycleDay2 = it },
                            label = { Text("Due Day 2 (Opt)") },
                            singleLine = true,
                            shape = RoundedCornerShape(ZincCornerRadius),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = minDueInput,
                        onValueChange = { minDueInput = it },
                        label = { Text("Min Amount Due (₱)") },
                        singleLine = true,
                        shape = RoundedCornerShape(ZincCornerRadius),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Dialog Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (initialAccount != null && onSoftDelete != null) {
                        OutlinedButton(
                            onClick = {
                                onSoftDelete(initialAccount.id)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(ZincCornerRadius),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(ZincCornerRadius),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) return@Button
                            val targetBalCentavos = CurrencyUtils.parseInputToCentavos(balanceInput)
                            val account = AccountEntity(
                                id = initialAccount?.id ?: 0L,
                                name = name.trim(),
                                type = selectedType,
                                presetId = selectedPresetId.ifEmpty { null },
                                initialBalance = if (initialAccount == null) targetBalCentavos else initialAccount.initialBalance,
                                displayOrder = initialAccount?.displayOrder ?: 0
                            )

                            val loanDetails = if (selectedType == AccountType.LOAN || selectedType == AccountType.BNPL) {
                                val d1 = cycleDay1.toIntOrNull() ?: 15
                                val d2 = cycleDay2.toIntOrNull()
                                val minDue = CurrencyUtils.parseInputToCentavos(minDueInput)
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

                            if (initialAccount != null && currentBalance != null && targetBalCentavos != currentBalance) {
                                pendingAccountSave = Pair(account, loanDetails)
                                pendingTargetBalCentavos = targetBalCentavos
                                showAdjustmentPrompt = true
                            } else {
                                onSaveWithAdjustment(account, loanDetails, targetBalCentavos, false)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(ZincCornerRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
