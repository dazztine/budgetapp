package com.example.budgettracker.ui.dashboard.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.ui.dashboard.UpcomingBillItem
import com.example.budgettracker.ui.theme.AmberGlow
import com.example.budgettracker.ui.theme.Green500
import com.example.budgettracker.ui.theme.MutedCoral
import com.example.budgettracker.ui.theme.MutedSage
import com.example.budgettracker.ui.theme.Orange500
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius
import com.example.budgettracker.ui.util.BrandLogoMapper
import com.example.budgettracker.util.CurrencyUtils
import com.example.budgettracker.util.DueDateStatus

@Composable
fun UpcomingBillsWidget(
    upcomingBills: List<UpcomingBillItem>,
    viewMode: String,
    onToggleViewMode: () -> Unit,
    onBillClick: (UpcomingBillItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Upcoming Bills",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (upcomingBills.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${upcomingBills.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleViewMode,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (viewMode == "VERTICAL") Icons.Default.ViewCarousel else Icons.Default.TableRows,
                    contentDescription = if (viewMode == "VERTICAL") "Switch to carousel view" else "Switch to list view",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (upcomingBills.isEmpty()) {
            // Clean empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(ZincCornerRadius))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(ZincCornerRadius)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Green500,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "No upcoming bills",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "You're all caught up on pending obligations.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            if (viewMode == "VERTICAL") {
                // Stacked Vertical List (Compact Row Style)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    upcomingBills.forEach { item ->
                        UpcomingBillVerticalCard(
                            item = item,
                            onClick = { onBillClick(item) }
                        )
                    }
                }
            } else {
                // Horizontal Carousel / Paged Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(upcomingBills, key = { "${it.id}_${it.cycleId}_${it.recurringBillId}" }) { item ->
                        UpcomingBillCard(
                            item = item,
                            onClick = { onBillClick(item) },
                            modifier = Modifier
                                .width(200.dp)
                                .height(148.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact, sleek row-style card for Upcoming Bills in Vertical List mode.
 * Significantly reduced height compared to the carousel card, with dedicated row layout.
 */
@Composable
fun UpcomingBillVerticalCard(
    item: UpcomingBillItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandLogoRes = BrandLogoMapper.getLogoResId(item.presetId, item.name)

    val typeIcon = when (item.accountType) {
        AccountType.CASH -> Icons.Outlined.AccountBalanceWallet
        AccountType.BANK, AccountType.SAVINGS -> Icons.Outlined.AccountBalance
        AccountType.E_WALLET -> Icons.Outlined.PhoneAndroid
        AccountType.BNPL, AccountType.LOAN, AccountType.CREDIT -> Icons.Outlined.CreditCard
        AccountType.BILL -> Icons.Outlined.Receipt
        AccountType.ASSET, null -> Icons.Outlined.Receipt
    }

    val (statusColor, statusText) = when (item.status) {
        DueDateStatus.OVERDUE -> Pair(MutedCoral, "Overdue")
        DueDateStatus.DUE_SOON -> {
            if (item.daysUntilDue == 0L) Pair(Orange500, "Due Today")
            else Pair(AmberGlow, "${item.daysUntilDue}d left")
        }
        DueDateStatus.UPCOMING -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "${item.daysUntilDue}d left")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ZincSoftCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(ZincSoftCornerRadius)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon / Brand Logo
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (brandLogoRes != null) {
                    Icon(
                        painter = painterResource(id = brandLogoRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Title & Due info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.dueDate.month.name.take(3)} ${item.dueDate.dayOfMonth} • ${item.subtitle}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Amount Due & Status Pill
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = CurrencyUtils.formatCentavosToPesos(item.amountCentavos),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun UpcomingBillCard(
    item: UpcomingBillItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandLogoRes = BrandLogoMapper.getLogoResId(item.presetId, item.name)

    val typeIcon = when (item.accountType) {
        AccountType.CASH -> Icons.Outlined.AccountBalanceWallet
        AccountType.BANK, AccountType.SAVINGS -> Icons.Outlined.AccountBalance
        AccountType.E_WALLET -> Icons.Outlined.PhoneAndroid
        AccountType.BNPL, AccountType.LOAN, AccountType.CREDIT -> Icons.Outlined.CreditCard
        AccountType.BILL -> Icons.Outlined.Receipt
        AccountType.ASSET, null -> Icons.Outlined.Receipt
    }

    val (statusColor, statusText) = when (item.status) {
        DueDateStatus.OVERDUE -> Pair(MutedCoral, "Overdue")
        DueDateStatus.DUE_SOON -> {
            if (item.daysUntilDue == 0L) Pair(Orange500, "Due Today")
            else Pair(AmberGlow, "${item.daysUntilDue}d left")
        }
        DueDateStatus.UPCOMING -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "${item.daysUntilDue}d left")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(ZincCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(ZincCornerRadius)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Logo / Icon + Due Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon / Logo Badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (brandLogoRes != null) {
                        Icon(
                            painter = painterResource(id = brandLogoRes),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Due Status Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Middle: Amount Due
            Column {
                Text(
                    text = "Amount Due",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyUtils.formatCentavosToPesos(item.amountCentavos),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom: Name + Subtitle + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${item.dueDate.month.name.take(3)} ${item.dueDate.dayOfMonth}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
