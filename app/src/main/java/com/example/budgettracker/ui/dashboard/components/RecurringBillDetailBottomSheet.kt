package com.example.budgettracker.ui.dashboard.components

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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.RecurringBillEntity
import com.example.budgettracker.ui.theme.AmberGlow
import com.example.budgettracker.ui.theme.Green500
import com.example.budgettracker.ui.theme.MidnightNavy
import com.example.budgettracker.ui.theme.MutedCoral
import com.example.budgettracker.ui.theme.Orange500
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.ui.transaction.components.CategoryIconMapper
import com.example.budgettracker.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillDetailBottomSheet(
    bill: RecurringBillEntity,
    accounts: List<AccountWithBalance>,
    onDismiss: () -> Unit,
    onEditClick: (RecurringBillEntity) -> Unit,
    onDeleteClick: (RecurringBillEntity) -> Unit,
    onPayClick: (RecurringBillEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val linkedAccount = bill.accountId?.let { id -> accounts.find { it.id == id } }

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
            // Header Row: Badge + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Orange500.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Recurring Bill",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Orange500
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

            // Amount and Name Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = CurrencyUtils.formatCentavosToPesos(bill.amount),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = bill.name,
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
                    DetailInfoRow(
                        icon = CategoryIconMapper.getIcon(bill.category),
                        label = "Category",
                        value = bill.category.ifEmpty { "Bills & Utilities" }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Repeat Frequency
                    DetailInfoRow(
                        icon = Icons.Default.Repeat,
                        label = "Schedule",
                        value = "Every month on day ${bill.dueDay}"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Linked Account
                    DetailInfoRow(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        label = "Payment Account",
                        value = linkedAccount?.name ?: "None selected"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Auto-Pay status
                    DetailInfoRow(
                        icon = Icons.Default.Payment,
                        label = "Auto-Pay",
                        value = if (bill.isAutoPay) "Enabled" else "Manual Payment"
                    )
                }
            }

            // Action Buttons (Delete, Edit, Pay Now)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onEditClick(bill)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        onDismiss()
                        onPayClick(bill)
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGlow,
                        contentColor = MidnightNavy
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pay Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Recurring Bill?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${bill.name}'? This will remove all future upcoming reminders for this bill.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDismiss()
                        onDeleteClick(bill)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedCoral)
                ) {
                    Text("Delete", color = androidx.compose.ui.graphics.Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
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
