package com.example.budgettracker.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.RecurringBillEntity
import com.example.budgettracker.ui.theme.AmberGlow
import com.example.budgettracker.ui.theme.MidnightNavy
import com.example.budgettracker.ui.theme.MutedCoral
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.util.CurrencyUtils

@Composable
fun EditRecurringBillDialog(
    bill: RecurringBillEntity,
    onDismiss: () -> Unit,
    onSave: (RecurringBillEntity) -> Unit,
    onDelete: (RecurringBillEntity) -> Unit
) {
    var name by remember { mutableStateOf(bill.name) }
    var amountInput by remember {
        val pesos = bill.amount / 100
        val cents = bill.amount % 100
        val str = if (cents > 0) String.format(java.util.Locale.US, "%d.%02d", pesos, cents) else pesos.toString()
        mutableStateOf(str)
    }
    var dueDayInput by remember { mutableStateOf(bill.dueDay.toString()) }
    var category by remember { mutableStateOf(bill.category) }
    var isAutoPay by remember { mutableStateOf(bill.isAutoPay) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Recurring Bill?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${bill.name}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(bill)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedCoral)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.surface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(ZincCornerRadius)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Recurring Bill", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete recurring bill",
                        tint = MutedCoral
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bill Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ZincCornerRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberGlow,
                        focusedLabelColor = AmberGlow
                    )
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount (₱)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ZincCornerRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberGlow,
                        focusedLabelColor = AmberGlow
                    )
                )

                OutlinedTextField(
                    value = dueDayInput,
                    onValueChange = { dueDayInput = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Due Day of Month (1-31)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ZincCornerRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberGlow,
                        focusedLabelColor = AmberGlow
                    )
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ZincCornerRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberGlow,
                        focusedLabelColor = AmberGlow
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto-Pay", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "Mark as automatically deducted",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAutoPay,
                        onCheckedChange = { isAutoPay = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MidnightNavy,
                            checkedTrackColor = AmberGlow
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountCentavos = CurrencyUtils.parseInputToCentavos(amountInput)
                    val dueDay = dueDayInput.toIntOrNull()?.coerceIn(1, 31) ?: bill.dueDay
                    if (name.isNotBlank() && amountCentavos > 0) {
                        onSave(
                            bill.copy(
                                name = name.trim(),
                                amount = amountCentavos,
                                dueDay = dueDay,
                                category = category.trim().ifBlank { "Bills" },
                                isAutoPay = isAutoPay,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberGlow,
                    contentColor = MidnightNavy
                ),
                shape = RoundedCornerShape(ZincCornerRadius)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(ZincCornerRadius)
    )
}
