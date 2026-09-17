package com.example.budgettracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius


data class GlossaryItem(
    val title: String,
    val description: String,
    val note: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialGlossaryDialog(
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val items = listOf(
        GlossaryItem(
            title = "Installment",
            description = "A payment made in parts over a period of time.",
            note = "It usually has a fixed monthly amount and due date.",
            icon = Icons.Outlined.CreditCard
        ),
        GlossaryItem(
            title = "BNPL (Buy Now Pay Later)",
            description = "Short-term financing that lets you purchase today and pay on a future date.",
            note = "Includes e-wallet pay later services like SPayLater or LazPayLater.",
            icon = Icons.Outlined.AccountBalanceWallet
        ),
        GlossaryItem(
            title = "Net Worth",
            description = "The sum of all your cash & asset balances minus unpaid loans/debt.",
            note = "Represents your overall financial health baseline.",
            icon = Icons.Outlined.Savings
        ),
        GlossaryItem(
            title = "Cash Flow",
            description = "The net amount of money flowing into and out of your accounts over time.",
            note = "Positive cash flow means you earned more than you spent.",
            icon = Icons.Outlined.SwapHoriz
        ),
        GlossaryItem(
            title = "Reconciliation",
            description = "Comparing recorded app transactions against actual bank or e-wallet balances.",
            note = "Helps catch missing fees, forgotten expenses, or math errors.",
            icon = Icons.Outlined.AccountBalance
        ),
        GlossaryItem(
            title = "Statement Cut-off Date",
            description = "The last day of a billing cycle where transactions are tallied for your monthly bill.",
            note = "Transactions after this date roll over to the next billing cycle.",
            icon = Icons.Outlined.ReceiptLong
        ),
        GlossaryItem(
            title = "Payment Due Date",
            description = "The deadline by which minimum or full payment must be made to avoid penalty fees.",
            note = "Always pay on or before this date.",
            icon = Icons.Outlined.CalendarToday
        ),
        GlossaryItem(
            title = "Minimum Amount Due",
            description = "The lowest payment required by a card issuer to keep your account in good standing.",
            note = "Paying only the minimum incurs finance charges on remaining balance.",
            icon = Icons.Outlined.AttachMoney
        ),
        GlossaryItem(
            title = "Interest Rate",
            description = "The fee charged by lenders or earned on savings, expressed as a percentage.",
            note = "High interest rates increase borrowing cost significantly over time.",
            icon = Icons.Outlined.Percent
        ),
        GlossaryItem(
            title = "Amortization",
            description = "Spreading out a loan into regular installment payments over a set duration.",
            note = "Each payment covers both principal reduction and interest charges.",
            icon = Icons.Outlined.Timeline
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Financial Glossary",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.forEach { item ->
                    GlossaryCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun GlossaryCard(item: GlossaryItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(ZincSoftCornerRadius)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(ZincSoftCornerRadius)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = item.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.note,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
