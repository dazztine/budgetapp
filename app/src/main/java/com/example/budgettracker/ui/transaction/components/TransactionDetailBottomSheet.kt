package com.example.budgettracker.ui.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.budgettracker.ui.components.StandardBottomSheetDragHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.ui.components.DeleteTransactionConfirmDialog
import com.example.budgettracker.ui.theme.AmberGlow
import com.example.budgettracker.ui.theme.Green500
import com.example.budgettracker.ui.theme.MidnightNavy
import com.example.budgettracker.ui.theme.MutedCoral
import com.example.budgettracker.ui.theme.Orange500
import com.example.budgettracker.ui.theme.Red500
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailBottomSheet(
    transaction: TransactionEntity,
    accounts: List<AccountWithBalance> = emptyList(),
    accountEntities: List<AccountEntity> = emptyList(),
    onDismiss: () -> Unit,
    onEditClick: (TransactionEntity) -> Unit,
    onDeleteClick: (TransactionEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val sourceAccountName = accounts.find { it.id == transaction.accountId }?.name
        ?: accountEntities.find { it.id == transaction.accountId }?.name
        ?: "Account #${transaction.accountId}"

    val destinationAccountName = transaction.toAccountId?.let { id ->
        accounts.find { it.id == id }?.name
            ?: accountEntities.find { it.id == id }?.name
            ?: "Account #$id"
    }

    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> Green500
        TransactionType.EXPENSE -> Orange500
        TransactionType.INSTALLMENT -> Red500
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
    }

    val amountPrefix = when (transaction.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE, TransactionType.INSTALLMENT -> "-"
        TransactionType.TRANSFER -> ""
    }

    val typeLabel = when (transaction.type) {
        TransactionType.INCOME -> "Income"
        TransactionType.EXPENSE -> "Expense"
        TransactionType.INSTALLMENT -> "Installment"
        TransactionType.TRANSFER -> "Transfer"
    }

    val typeBgColor = when (transaction.type) {
        TransactionType.INCOME -> Green500.copy(alpha = 0.12f)
        TransactionType.EXPENSE -> Orange500.copy(alpha = 0.12f)
        TransactionType.INSTALLMENT -> Red500.copy(alpha = 0.12f)
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }

    val typeTextColor = when (transaction.type) {
        TransactionType.INCOME -> Green500
        TransactionType.EXPENSE -> Orange500
        TransactionType.INSTALLMENT -> Red500
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
    }

    val formattedDateTime = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(transaction.timestamp))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { StandardBottomSheetDragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Row: Type Badge + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(typeBgColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = typeLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeTextColor
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

            // Amount and Title Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$amountPrefix${CurrencyUtils.formatCentavosToPesos(transaction.amount)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                val displayTitle = transaction.title.ifEmpty {
                    transaction.category.ifEmpty { typeLabel }
                }
                Text(
                    text = displayTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Details Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ZincCornerRadius))
                    .background(MaterialTheme.colorScheme.background)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(ZincCornerRadius)
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Category
                    if (transaction.category.isNotEmpty()) {
                        DetailInfoRow(
                            icon = CategoryIconMapper.getIcon(transaction.category),
                            label = "Category",
                            value = transaction.category
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }

                    // Account
                    if (transaction.type == TransactionType.TRANSFER) {
                        DetailInfoRow(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            label = "From Account",
                            value = sourceAccountName
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        DetailInfoRow(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            label = "To Account",
                            value = destinationAccountName ?: "Account #${transaction.toAccountId}"
                        )
                    } else {
                        DetailInfoRow(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            label = "Account",
                            value = sourceAccountName
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Date & Time
                    DetailInfoRow(
                        icon = Icons.Default.Schedule,
                        label = "Date & Time",
                        value = formattedDateTime
                    )

                    // Note if present
                    if (!transaction.note.isNullOrBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        DetailInfoRow(
                            icon = Icons.Default.Notes,
                            label = "Notes",
                            value = transaction.note
                        )
                    }

                    // Linked Installment Plan indicator
                    if (transaction.installmentPlanId != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        DetailInfoRow(
                            icon = Icons.Default.CreditCard,
                            label = "Plan",
                            value = "Linked to Installment Plan"
                        )
                    }
                }
            }

            // Action Buttons (Delete & Edit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MutedCoral
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MutedCoral.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        onDismiss()
                        onEditClick(transaction)
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGlow,
                        contentColor = MidnightNavy
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Transaction", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        DeleteTransactionConfirmDialog(
            onConfirmDelete = {
                showDeleteConfirmDialog = false
                onDismiss()
                onDeleteClick(transaction)
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
