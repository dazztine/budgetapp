package com.example.budgettracker.ui.dashboard.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import com.example.budgettracker.data.local.entity.CreditAccountDetailsEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.ui.util.BrandLogoMapper
import com.example.budgettracker.util.CurrencyUtils

@Composable
fun AccountCard(
    accountWithBalance: AccountWithBalance,
    loanDetails: AccountWithLoanDetails? = null,
    creditDetails: CreditAccountDetailsEntity? = null,
    isBalanceVisible: Boolean = true,
    onEditClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val typeIcon = when (accountWithBalance.type) {
        AccountType.CASH -> Icons.Outlined.AccountBalanceWallet
        AccountType.BANK, AccountType.SAVINGS -> Icons.Outlined.AccountBalance
        AccountType.E_WALLET -> Icons.Outlined.PhoneAndroid
        AccountType.CREDIT -> Icons.Outlined.CreditCard
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
            .height(112.dp)
            .clip(RoundedCornerShape(ZincCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincCornerRadius))
            .clickable(onClick = onEditClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Circular logo/icon on the left
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
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

            // Middle Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = accountWithBalance.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val isDebtAccount = accountWithBalance.type == AccountType.LOAN
                    || accountWithBalance.type == AccountType.BNPL
                    || accountWithBalance.type == AccountType.CREDIT

                if (isDebtAccount) {
                    // Primary: credit limit (or "—" if not set) — neutral color, represents spending power
                    val limitAmount: Long? = creditDetails?.creditLimit ?: loanDetails?.loanDetails?.creditLimit
                    val primaryText = when {
                        !isBalanceVisible -> "₱ ••••••"
                        limitAmount != null -> CurrencyUtils.formatCentavosToPesos(limitAmount)
                        else -> null // no limit: fall back to showing owed amount as primary
                    }

                    if (primaryText != null) {
                        Text(
                            text = primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Secondary: owed amount — only shown when there is actual debt
                        val owedAmount = accountWithBalance.currentBalance
                        if (owedAmount < 0L) {
                            val owedFormatted = if (isBalanceVisible) {
                                "Owed: ${CurrencyUtils.formatCentavosToPesos(owedAmount)}"
                            } else {
                                "Owed: ₱ ••••••"
                            }
                            Text(
                                text = owedFormatted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFF87171), // Muted Coral
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        // No limit set: fall back — show total owed as primary in Muted Coral
                        Text(
                            text = if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(accountWithBalance.currentBalance) else "₱ ••••••",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(accountWithBalance.currentBalance) else "₱ ••••••",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "• ${accountWithBalance.type.toDisplayLabel()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right-aligned chevron (>) vertically centered on the card
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View account details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

