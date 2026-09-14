package com.example.budgettracker.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.AccountWithLoanDetails
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.ui.util.BrandLogoMapper
import com.example.budgettracker.util.CurrencyUtils
import com.example.budgettracker.util.LoanDateUtils
import java.time.LocalDate

@Composable
fun AccountCard(
    accountWithBalance: AccountWithBalance,
    loanDetails: AccountWithLoanDetails? = null,
    isBalanceVisible: Boolean = true,
    onEditClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val typeIcon = when (accountWithBalance.type) {
        AccountType.CASH -> Icons.Outlined.AccountBalanceWallet
        AccountType.BANK, AccountType.SAVINGS -> Icons.Outlined.AccountBalance
        AccountType.E_WALLET -> Icons.Outlined.PhoneAndroid
        AccountType.BNPL, AccountType.LOAN -> Icons.Outlined.CreditCard
        AccountType.BILL -> Icons.Outlined.Receipt
        AccountType.ASSET -> Icons.Outlined.AccountBalance
    }

    val brandLogoRes = BrandLogoMapper.getLogoResId(
        accountWithBalance.presetId,
        accountWithBalance.name
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ZincCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
            .clickable(onClick = onEditClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Icon Badge Top Left
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
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
                        modifier = Modifier.size(38.dp)
                    )
                } else {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Amount & Name Vertically Stacked
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(accountWithBalance.currentBalance) else "₱ ••••••",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (accountWithBalance.type == AccountType.LOAN || accountWithBalance.type == AccountType.BNPL) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${accountWithBalance.name} • ${accountWithBalance.type.toDisplayLabel()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Loan/BNPL Details (if applicable)
            loanDetails?.loanDetails?.let { details ->
                val today = LocalDate.now()
                val dueDate = LoanDateUtils.calculateNextDueDate(today, details.cycleDay1, details.cycleDay2)
                val daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate)
                val isUrgent = daysUntilDue in 0..7

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isUrgent) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Due: $dueDate (${daysUntilDue}d)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
