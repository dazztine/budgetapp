package com.example.budgettracker.ui.reports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.R
import com.example.budgettracker.ui.theme.AmberGlow
import com.example.budgettracker.ui.theme.MidnightNavy
import com.example.budgettracker.ui.theme.MutedCoral
import com.example.budgettracker.ui.theme.MutedSage
import com.example.budgettracker.ui.theme.Zinc400
import com.example.budgettracker.ui.theme.Zinc500
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.util.CurrencyUtils
import com.example.budgettracker.util.DueDateStatus
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AmberGlow)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header & Period Selector
        item {
            ReportsHeader(
                selectedPeriod = uiState.selectedPeriod,
                isBalanceVisible = uiState.isBalanceVisible,
                onPeriodSelected = { viewModel.selectPeriod(it) },
                onToggleBalance = { viewModel.toggleBalanceVisibility() }
            )
        }

        // 2. Tier 1: Spending by Category
        item {
            SpendingByCategoryCard(
                spending = uiState.spendingByCategory,
                totalExpense = uiState.totalExpenseInPeriod,
                isBalanceVisible = uiState.isBalanceVisible
            )
        }

        // 3. Tier 1: Income vs. Expense
        item {
            IncomeVsExpenseCard(
                comparison = uiState.incomeExpenseComparison,
                isBalanceVisible = uiState.isBalanceVisible
            )
        }

        // 4. Tier 1: Net Worth Over Time
        item {
            NetWorthOverTimeCard(
                netWorthHistory = uiState.netWorthHistory,
                currentNetWorth = uiState.currentNetWorth,
                isBalanceVisible = uiState.isBalanceVisible
            )
        }

        // 5. Tier 1: Upcoming Obligations
        item {
            UpcomingObligationsCard(
                obligations = uiState.upcomingObligations,
                totalDue = uiState.totalObligationsDue,
                isBalanceVisible = uiState.isBalanceVisible
            )
        }

        // 6. Tier 2: Top Merchants / Titles
        item {
            TopMerchantsCard(
                merchants = uiState.topMerchants,
                isBalanceVisible = uiState.isBalanceVisible
            )
        }

        // 7. Tier 2: Category Trend Over Time
        item {
            CategoryTrendCard(
                trends = uiState.categoryTrends,
                trendMonths = uiState.trendMonths,
                isBalanceVisible = uiState.isBalanceVisible
            )
        }

        // 8. Tier 2: Daily Burn Rate
        item {
            DailyBurnRateCard(
                burnRate = uiState.dailyBurnRate,
                selectedPeriod = uiState.selectedPeriod,
                isBalanceVisible = uiState.isBalanceVisible
            )
        }
    }
}

@Composable
private fun ReportsHeader(
    selectedPeriod: ReportPeriod,
    isBalanceVisible: Boolean,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onToggleBalance: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Reports",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Financial Analytics & Trends",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = false, radius = 22.dp),
                        role = Role.Button,
                        onClick = {
                            onToggleBalance()
                            focusManager.clearFocus()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isBalanceVisible,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.85f, animationSpec = tween(180, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                        scaleOut(targetScale = 0.85f, animationSpec = tween(180, easing = FastOutSlowInEasing))
                            )
                    },
                    label = "ReportsEyeToggle"
                ) { visible ->
                    Icon(
                        painter = painterResource(
                            id = if (visible) R.drawable.unhide_eye_kwago else R.drawable.hide_eye_kwago
                        ),
                        contentDescription = if (visible) "Hide Balance" else "Show Balance",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // Period Selector Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(ReportPeriod.entries) { period ->
                val isSelected = period == selectedPeriod
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) AmberGlow else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) AmberGlow else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onPeriodSelected(period) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MidnightNavy else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Tier 1: Spending by Category Card
 */
@Composable
private fun SpendingByCategoryCard(
    spending: List<CategorySpending>,
    totalExpense: Long,
    isBalanceVisible: Boolean
) {
    SectionCard(title = "Spending by Category") {
        if (spending.isEmpty()) {
            EmptyReportState(message = "No expenses recorded for this period.")
            return@SectionCard
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hand-rolled Compose Canvas Donut Chart
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 18.dp.toPx()
                    var startAngle = -90f
                    spending.forEach { item ->
                        val sweepAngle = (item.percentage / 100f) * 360f
                        drawArc(
                            color = item.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                        startAngle += sweepAngle
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Total",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(totalExpense) else "₱ ••••••",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Breakdown List
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                spending.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(item.color)
                            )
                            Text(
                                text = item.category,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f%%", item.percentage),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(item.amount) else "₱ •••",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tier 1: Income vs Expense Card
 */
@Composable
private fun IncomeVsExpenseCard(
    comparison: IncomeExpenseComparison,
    isBalanceVisible: Boolean
) {
    SectionCard(title = "Income vs. Expense") {
        // Top Net Badge
        val netColor = if (comparison.isSaved) MutedSage else MutedCoral
        val netPrefix = if (comparison.isSaved) "+" else "-"
        val netAbs = Math.abs(comparison.netAmount)
        val netLabel = if (comparison.isSaved) "saved" else "overspent"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(netColor.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBalanceVisible) {
                    "$netPrefix${CurrencyUtils.formatCentavosToPesos(netAbs)} $netLabel"
                } else {
                    "$netPrefix₱ •••••• $netLabel"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = netColor
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "In: " + (if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(comparison.totalIncome) else "₱ •••"),
                    fontSize = 11.sp,
                    color = MutedSage,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Out: " + (if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(comparison.totalExpense) else "₱ •••"),
                    fontSize = 11.sp,
                    color = MutedCoral,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (comparison.monthlyBars.isEmpty() || comparison.monthlyBars.all { it.income == 0L && it.expense == 0L }) {
            EmptyReportState(message = "No income or expense data in this period.")
            return@SectionCard
        }

        // Hand-rolled Compose Canvas Grouped Bar Chart
        val maxVal = comparison.monthlyBars.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1L)
        val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartHeight = size.height - 20.dp.toPx()
                val slotWidth = size.width / comparison.monthlyBars.size
                val barWidth = (slotWidth * 0.28f).coerceAtMost(16.dp.toPx()).coerceAtLeast(6.dp.toPx())
                val barGap = 3.dp.toPx()
                val baseline = chartHeight

                // Baseline line
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, baseline),
                    end = Offset(size.width, baseline),
                    strokeWidth = 1.dp.toPx()
                )

                comparison.monthlyBars.forEachIndexed { i, barData ->
                    val centerX = slotWidth * i + (slotWidth / 2f)

                    // Income bar (left)
                    val inHeight = (barData.income.toFloat() / maxVal) * chartHeight
                    val inLeft = centerX - barWidth - (barGap / 2f)
                    if (inHeight > 0) {
                        drawRoundRect(
                            color = MutedSage,
                            topLeft = Offset(inLeft, baseline - inHeight),
                            size = Size(barWidth, inHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }

                    // Expense bar (right)
                    val exHeight = (barData.expense.toFloat() / maxVal) * chartHeight
                    val exLeft = centerX + (barGap / 2f)
                    if (exHeight > 0) {
                        drawRoundRect(
                            color = MutedCoral,
                            topLeft = Offset(exLeft, baseline - exHeight),
                            size = Size(barWidth, exHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
            }
        }

        // Month Labels Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            comparison.monthlyBars.forEach { barData ->
                Text(
                    text = barData.monthLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Tier 1: Net Worth Over Time Card
 */
@Composable
private fun NetWorthOverTimeCard(
    netWorthHistory: List<NetWorthPoint>,
    currentNetWorth: Long,
    isBalanceVisible: Boolean
) {
    SectionCard(
        title = "Net Worth Over Time",
        subtitle = "Past 6 months",
        trailing = {
            Text(
                text = if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(currentNetWorth) else "₱ ••••••",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    ) {
        if (netWorthHistory.size < 2) {
            EmptyReportState(message = "Accumulating net worth history...")
            return@SectionCard
        }

        val minVal = netWorthHistory.minOf { it.netWorth }
        val maxVal = netWorthHistory.maxOf { it.netWorth }
        val range = (maxVal - minVal).coerceAtLeast(1L)
        val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(vertical = 4.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartHeight = size.height - 16.dp.toPx()
                val usableWidth = size.width - 24.dp.toPx()
                val leftPad = 12.dp.toPx()
                val topPad = 8.dp.toPx()

                // 2 dashed reference gridlines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, topPad),
                    end = Offset(size.width, topPad),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                val points = netWorthHistory.mapIndexed { index, point ->
                    val x = leftPad + (index.toFloat() / (netWorthHistory.size - 1)) * usableWidth
                    val y = chartHeight - ((point.netWorth - minVal).toFloat() / range) * (chartHeight - topPad)
                    Offset(x, y)
                }

                // Draw line path
                val path = Path().apply {
                    points.forEachIndexed { i, pt ->
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = path,
                    color = AmberGlow,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw points
                points.forEach { pt ->
                    drawCircle(color = AmberGlow, radius = 4.dp.toPx(), center = pt)
                    drawCircle(color = MidnightNavy, radius = 2.dp.toPx(), center = pt)
                }
            }
        }

        // Month Labels Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            netWorthHistory.forEach { point ->
                Text(
                    text = point.monthLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Tier 1: Upcoming Obligations Card
 */
@Composable
private fun UpcomingObligationsCard(
    obligations: List<UpcomingObligation>,
    totalDue: Long,
    isBalanceVisible: Boolean
) {
    SectionCard(
        title = "Upcoming Obligations",
        subtitle = "Next 30 days",
        trailing = {
            if (obligations.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isBalanceVisible) {
                            "${CurrencyUtils.formatCentavosToPesos(totalDue)} (${obligations.size})"
                        } else {
                            "₱ ••• (${obligations.size})"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) {
        if (obligations.isEmpty()) {
            EmptyReportState(message = "No bills or installments due in the next 30 days.")
            return@SectionCard
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            obligations.forEach { item ->
                val (statusColor, statusBg, statusText) = when (item.status) {
                    DueDateStatus.OVERDUE -> Triple(
                        MutedCoral,
                        MutedCoral.copy(alpha = 0.15f),
                        "Overdue"
                    )
                    DueDateStatus.DUE_SOON -> Triple(
                        MutedCoral,
                        MutedCoral.copy(alpha = 0.15f),
                        if (item.daysUntilDue == 0L) "Due today" else "Due in ${item.daysUntilDue}d"
                    )
                    DueDateStatus.UPCOMING -> Triple(
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant,
                        "In ${item.daysUntilDue}d"
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.typeLabel} • ${item.dueDate.format(DateTimeFormatter.ofPattern("MMM d"))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                        }

                        Text(
                            text = if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(item.amount) else "₱ •••",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tier 2: Top Merchants Card
 */
@Composable
private fun TopMerchantsCard(
    merchants: List<MerchantSpend>,
    isBalanceVisible: Boolean
) {
    SectionCard(title = "Top Merchants & Titles") {
        if (merchants.isEmpty()) {
            EmptyReportState(message = "No expense records found for this period.")
            return@SectionCard
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            merchants.forEach { merchant ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${merchant.rank}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = merchant.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${merchant.count} transaction${if (merchant.count > 1) "s" else ""}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = if (isBalanceVisible) CurrencyUtils.formatCentavosToPesos(merchant.totalAmount) else "₱ ••••••",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Tier 2: Category Trend Over Time Card
 */
@Composable
private fun CategoryTrendCard(
    trends: List<CategoryTrend>,
    trendMonths: List<String>,
    isBalanceVisible: Boolean
) {
    SectionCard(
        title = "Category Trends",
        subtitle = "Last 6 months"
    ) {
        if (trends.isEmpty() || trends.all { it.monthlyData.all { m -> m.amount == 0L } }) {
            EmptyReportState(message = "Not enough category data across the last 6 months.")
            return@SectionCard
        }

        val maxVal = trends.maxOf { t -> t.monthlyData.maxOf { it.amount } }.coerceAtLeast(1L)
        val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(vertical = 4.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartHeight = size.height - 16.dp.toPx()
                val usableWidth = size.width - 24.dp.toPx()
                val leftPad = 12.dp.toPx()
                val topPad = 8.dp.toPx()

                // Reference lines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                trends.forEach { trend ->
                    val points = trend.monthlyData.mapIndexed { index, data ->
                        val x = leftPad + (index.toFloat() / (trend.monthlyData.size - 1).coerceAtLeast(1)) * usableWidth
                        val y = chartHeight - (data.amount.toFloat() / maxVal) * (chartHeight - topPad)
                        Offset(x, y)
                    }

                    val path = Path().apply {
                        points.forEachIndexed { i, pt ->
                            if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = trend.color,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    points.forEach { pt ->
                        drawCircle(color = trend.color, radius = 3.dp.toPx(), center = pt)
                    }
                }
            }
        }

        // Legend Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            items(trends) { trend ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(trend.color)
                    )
                    Text(
                        text = trend.category,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Tier 2: Daily Burn Rate Card
 */
@Composable
private fun DailyBurnRateCard(
    burnRate: DailyBurnRate?,
    selectedPeriod: ReportPeriod,
    isBalanceVisible: Boolean
) {
    SectionCard(title = "Daily Burn Rate") {
        if (burnRate == null || burnRate.averageDailySpend == 0L) {
            EmptyReportState(message = "No spend recorded in this period to calculate burn rate.")
            return@SectionCard
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isBalanceVisible) {
                            "Averaging ${CurrencyUtils.formatCentavosToPesos(burnRate.averageDailySpend)}/day"
                        } else {
                            "Averaging ₱ ••••••/day"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (selectedPeriod == ReportPeriod.THIS_MONTH) {
                            "Day ${burnRate.daysElapsed} of ${burnRate.totalDaysInPeriod} days elapsed"
                        } else {
                            "Calculated across ${burnRate.daysElapsed} days in period"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (burnRate.projectedMonthTotal != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isBalanceVisible) {
                            "Estimated ${CurrencyUtils.formatCentavosToPesos(burnRate.projectedMonthTotal)} total this month at current daily pace"
                        } else {
                            "Estimated ₱ •••••• total this month at current daily pace"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Common Card Wrapper matching Kwago surface styles
 */
@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ZincCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(ZincCornerRadius)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                trailing?.invoke()
            }

            content()
        }
    }
}

@Composable
private fun EmptyReportState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
