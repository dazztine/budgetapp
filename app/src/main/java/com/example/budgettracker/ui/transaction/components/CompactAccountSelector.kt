package com.example.budgettracker.ui.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.AccountWithBalance
import com.example.budgettracker.data.local.entity.AccountWithLoanDetails
import com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.util.CurrencyUtils

@Composable
fun CompactAccountSelector(
    label: String,
    selectedAccount: AccountWithBalance?,
    accounts: List<AccountWithBalance>,
    onAccountSelected: (AccountWithBalance) -> Unit,
    modifier: Modifier = Modifier,
    loanAccounts: List<AccountWithLoanDetails> = emptyList(),
    creditDetailsList: List<CreditAccountDetailsEntity> = emptyList()
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (accounts.isEmpty()) {
            Text(
                text = "No accounts available",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val chunked = accounts.chunked(2)
            chunked.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { acc ->
                        val isSelected = acc.id == selectedAccount?.id
                        val isDebtAccount = acc.type == AccountType.LOAN || acc.type == AccountType.BNPL || acc.type == AccountType.CREDIT

                        val creditLimit: Long? = if (isDebtAccount) {
                            creditDetailsList.find { it.accountId == acc.id }?.creditLimit
                                ?: loanAccounts.find { it.account.id == acc.id }?.loanDetails?.creditLimit
                        } else null

                        val availableCredit = if (isDebtAccount) {
                            BudgetRepository.calculateAvailableCredit(creditLimit, acc.currentBalance)
                        } else null

                        val balanceText = when {
                            availableCredit != null -> CurrencyUtils.formatCentavosToPesos(availableCredit)
                            isDebtAccount && acc.currentBalance < 0L -> "Owed: ${CurrencyUtils.formatCentavosToPesos(-acc.currentBalance)}"
                            else -> CurrencyUtils.formatCentavosToPesos(acc.currentBalance)
                        }

                        val balanceTextColor = when {
                            availableCredit != null && availableCredit < 0L -> Color(0xFFF87171) // Muted Coral for negative available credit
                            isDebtAccount && acc.currentBalance < 0L && availableCredit == null -> Color(0xFFF87171)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        val typeIcon = when (acc.type) {
                            AccountType.CASH -> Icons.Outlined.AccountBalanceWallet
                            AccountType.BANK, AccountType.SAVINGS -> Icons.Outlined.AccountBalance
                            AccountType.E_WALLET -> Icons.Outlined.PhoneAndroid
                            AccountType.BNPL, AccountType.LOAN, AccountType.CREDIT -> Icons.Outlined.CreditCard
                            AccountType.BILL -> Icons.Outlined.Receipt
                            AccountType.ASSET -> Icons.Outlined.AccountBalance
                        }

                        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        val borderWidth = if (isSelected) 2.dp else 1.dp
                        val containerBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(ZincCornerRadius))
                                .background(containerBg)
                                .border(borderWidth, borderColor, RoundedCornerShape(ZincCornerRadius))
                                .clickable { onAccountSelected(acc) }
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = typeIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "${acc.name} • ${acc.type.toDisplayLabel()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = balanceText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = balanceTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
