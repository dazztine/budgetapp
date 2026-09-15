package com.example.budgettracker.ui.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.ui.theme.AmberGlow
import com.example.budgettracker.ui.theme.MidnightNavy
import com.example.budgettracker.ui.theme.Zinc950
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius
import com.example.budgettracker.util.CurrencyUtils

/**
 * Redesigned Kwago Amount Calculator Sheet.
 *
 * Structure:
 * 1. Drag handle bar at the top
 * 2. Two-tier Amount display row:
 *    - "Amount" small muted label
 *    - Large bold "₱<expression>"
 *    - Paste clipboard icon on the right
 * 3. 5-row button grid:
 *    - Row 1: AC, ÷, ×, ⌫ (backspace)
 *    - Row 2: 7, 8, 9, +
 *    - Row 3: 4, 5, 6, -
 *    - Row 4: 1, 2, 3, = (Midnight Navy)
 *    - Row 5: ., 0, +/-, ✓ (Amber Glow confirm)
 */
@Composable
fun NumpadView(
    amountExpression: String = "",
    onDigitClick: (String) -> Unit,
    onDotClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onOperatorClick: (String) -> Unit = {},
    onEqualClick: () -> Unit = {},
    onConfirmClick: () -> Unit = {},
    onToggleSignClick: () -> Unit = {},
    onPasteClick: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(topStart = ZincSoftCornerRadius, topEnd = ZincSoftCornerRadius)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(topStart = ZincSoftCornerRadius, topEnd = ZincSoftCornerRadius)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drag Indicator Bar at top (Tap/Drag to dismiss)
        Box(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 4.dp)
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline)
                .clickable(onClick = onDismiss)
        )

        // Tiered Amount Display Row:
        // Top: Small muted "Amount"
        // Bottom: Bold "₱<expression>" and clipboard icon aligned right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Amount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val formattedAmount = CurrencyUtils.formatExpressionForDisplay(amountExpression)
                val amountFontSize = if (formattedAmount.length > 14) 22.sp else 28.sp
                Text(
                    text = formattedAmount,
                    fontSize = amountFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onPasteClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentPaste,
                    contentDescription = "Paste numeric amount",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Row 1: AC, ÷, ×, ⌫
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalculatorButton(
                text = "AC",
                onClick = onClearClick,
                isOperator = true,
                modifier = Modifier.weight(1f)
            )
            CalculatorButton(
                text = "÷",
                onClick = { onOperatorClick("÷") },
                isOperator = true,
                modifier = Modifier.weight(1f)
            )
            CalculatorButton(
                text = "×",
                onClick = { onOperatorClick("×") },
                isOperator = true,
                modifier = Modifier.weight(1f)
            )
            CalculatorIconButton(
                icon = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                onClick = onBackspaceClick,
                isOperator = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: 7, 8, 9, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalculatorButton("7", { onDigitClick("7") }, modifier = Modifier.weight(1f))
            CalculatorButton("8", { onDigitClick("8") }, modifier = Modifier.weight(1f))
            CalculatorButton("9", { onDigitClick("9") }, modifier = Modifier.weight(1f))
            CalculatorButton("+", { onOperatorClick("+") }, isOperator = true, modifier = Modifier.weight(1f))
        }

        // Row 3: 4, 5, 6, −
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalculatorButton("4", { onDigitClick("4") }, modifier = Modifier.weight(1f))
            CalculatorButton("5", { onDigitClick("5") }, modifier = Modifier.weight(1f))
            CalculatorButton("6", { onDigitClick("6") }, modifier = Modifier.weight(1f))
            CalculatorButton("−", { onOperatorClick("−") }, isOperator = true, modifier = Modifier.weight(1f))
        }

        // Row 4: 1, 2, 3, = (Midnight Navy)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalculatorButton("1", { onDigitClick("1") }, modifier = Modifier.weight(1f))
            CalculatorButton("2", { onDigitClick("2") }, modifier = Modifier.weight(1f))
            CalculatorButton("3", { onDigitClick("3") }, modifier = Modifier.weight(1f))
            CalculatorButton("=", onEqualClick, isEqual = true, modifier = Modifier.weight(1f))
        }

        // Row 5: ., 0, +/-, ✓ (Amber Glow Confirm)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalculatorButton(".", onDotClick, modifier = Modifier.weight(1f))
            CalculatorButton("0", { onDigitClick("0") }, modifier = Modifier.weight(1f))
            CalculatorButton("+/-", onToggleSignClick, modifier = Modifier.weight(1f))
            CalculatorIconButton(
                icon = Icons.Default.Check,
                contentDescription = "Confirm amount",
                onClick = onConfirmClick,
                isConfirm = true,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun CalculatorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isEqual: Boolean = false,
    isConfirm: Boolean = false
) {
    CalculatorBaseButton(
        onClick = onClick,
        modifier = modifier,
        isOperator = isOperator,
        isEqual = isEqual,
        isConfirm = isConfirm
    ) { contentColor ->
        Text(
            text = text,
            fontSize = if (text.length > 2) 15.sp else 18.sp,
            fontWeight = if (isConfirm || isEqual || isOperator) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
private fun CalculatorIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isEqual: Boolean = false,
    isConfirm: Boolean = false
) {
    CalculatorBaseButton(
        onClick = onClick,
        modifier = modifier,
        isOperator = isOperator,
        isEqual = isEqual,
        isConfirm = isConfirm
    ) { contentColor ->
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CalculatorBaseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isEqual: Boolean = false,
    isConfirm: Boolean = false,
    content: @Composable (Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor = when {
        isConfirm -> if (isPressed) AmberGlow.copy(alpha = 0.8f) else AmberGlow
        isEqual -> if (isPressed) MidnightNavy.copy(alpha = 0.8f) else MidnightNavy
        isOperator -> if (isPressed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant
        else -> if (isPressed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isConfirm -> Zinc950
        isEqual -> Color.White
        isOperator -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when {
        isConfirm -> AmberGlow
        isEqual -> MidnightNavy
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(ZincCornerRadius))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(ZincCornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(contentColor)
    }
}
