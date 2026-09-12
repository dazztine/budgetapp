package com.example.budgettracker.ui.parse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.parser.ParsedTransaction
import com.example.budgettracker.ui.theme.Green500
import com.example.budgettracker.ui.theme.Orange500
import com.example.budgettracker.ui.theme.Red500
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius
import com.example.budgettracker.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SingleTransactionPreviewCard(
    parsedTx: ParsedTransaction,
    accounts: List<AccountEntity>,
    onConfirm: (ParsedTransaction, Long, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedAccountId by remember(parsedTx) {
        val matchedId = parsedTx.accountName?.let { name ->
            accounts.find { it.name.equals(name, ignoreCase = true) }?.id
        }
        mutableStateOf(matchedId ?: accounts.firstOrNull()?.id)
    }

    var selectedToAccountId by remember(parsedTx) {
        val matchedId = parsedTx.toAccountName?.let { name ->
            accounts.find { it.name.equals(name, ignoreCase = true) }?.id
        }
        mutableStateOf(matchedId)
    }

    var editableAmountCentavos by remember(parsedTx) { mutableStateOf(parsedTx.amountCentavos) }
    var amountInputText by remember(parsedTx) { mutableStateOf((parsedTx.amountCentavos / 100.0).toString()) }
    var selectedType by remember(parsedTx) { mutableStateOf(parsedTx.type) }
    var category by remember(parsedTx) { mutableStateOf(parsedTx.category) }
    var title by remember(parsedTx) { mutableStateOf(parsedTx.title) }

    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var isEditingAmount by remember { mutableStateOf(false) }
    var isEditingCategory by remember { mutableStateOf(false) }

    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val todayDateStr = remember { dateFormat.format(Date()) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // "Parsed Successfully" Status Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ZincSoftCornerRadius))
                .background(Green500.copy(alpha = 0.12f))
                .border(1.dp, Green500.copy(alpha = 0.3f), RoundedCornerShape(ZincSoftCornerRadius))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Green500,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Parsed Successfully",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green500
                )
            }
        }

        // Interactive Editable Confirmation Card (Mockup Screen 4)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ZincSoftCornerRadius))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincSoftCornerRadius))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Field 1: Account (Tap to Edit)
                Box(modifier = Modifier.fillMaxWidth()) {
                    EditableFieldRow(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        label = "Account",
                        value = selectedAccount?.name ?: "Select Account",
                        onClick = { accountDropdownExpanded = true }
                    )

                    DropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.type.name})") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Field 2: Amount (Tap to Edit)
                if (isEditingAmount) {
                    OutlinedTextField(
                        value = amountInputText,
                        onValueChange = {
                            amountInputText = it
                            val parsedDouble = it.toDoubleOrNull()
                            if (parsedDouble != null) {
                                editableAmountCentavos = (parsedDouble * 100).toLong()
                            }
                        },
                        label = { Text("Amount (₱)") },
                        singleLine = true,
                        trailingIcon = {
                            TextButton(onClick = { isEditingAmount = false }) {
                                Text("Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    EditableFieldRow(
                        icon = Icons.Outlined.Payment,
                        label = "Amount",
                        value = CurrencyUtils.formatCentavosToPesos(editableAmountCentavos),
                        onClick = { isEditingAmount = true }
                    )
                }

                // Field 3: Category (Tap to Edit)
                if (isEditingCategory) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        trailingIcon = {
                            TextButton(onClick = { isEditingCategory = false }) {
                                Text("Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    EditableFieldRow(
                        icon = Icons.Outlined.Category,
                        label = "Category",
                        value = category,
                        onClick = { isEditingCategory = true }
                    )
                }

                // Field 4: Type (Tap to Toggle / Select)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { typeDropdownExpanded = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Sell,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = "Type",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Type Badge
                        val badgeColor = when (selectedType) {
                            TransactionType.EXPENSE -> Orange500
                            TransactionType.INCOME -> Green500
                            TransactionType.INSTALLMENT -> Red500
                            TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = selectedType.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        TransactionType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Field 5: Date
                EditableFieldRow(
                    icon = Icons.Outlined.CalendarToday,
                    label = "Date",
                    value = todayDateStr,
                    onClick = {}
                )
            }
        }

        // Review Info Callout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ZincSoftCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = "Review before saving",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Please confirm the details are correct before saving this transaction.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Confirm & Save Full-Width Primary Button
        Button(
            onClick = {
                val updatedParsedTx = parsedTx.copy(
                    type = selectedType,
                    amountCentavos = editableAmountCentavos,
                    category = category.trim(),
                    title = title.ifBlank { category.trim() }
                )
                selectedAccountId?.let { accId ->
                    onConfirm(updatedParsedTx, accId, selectedToAccountId)
                }
            },
            shape = RoundedCornerShape(ZincSoftCornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = selectedAccountId != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Confirm & Save",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EditableFieldRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
