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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius

@Composable
fun NumpadView(
    onDigitClick: (String) -> Unit,
    onDotClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onOperatorClick: (String) -> Unit = {},
    onEqualClick: () -> Unit = {},
    onConfirmClick: () -> Unit = {},
    onToggleSignClick: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = ZincSoftCornerRadius, topEnd = ZincSoftCornerRadius))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(topStart = ZincSoftCornerRadius, topEnd = ZincSoftCornerRadius))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drag Indicator Bar at top (Tap/Drag to dismiss)
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .width(42.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline)
                .clickable(onClick = onDismiss)
        )

        // Row 1: AC, +/-, ÷, ×
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalculatorButton("AC", onClearClick, isAction = true, modifier = Modifier.weight(1f))
            CalculatorButton("+/-", onToggleSignClick, isAction = true, modifier = Modifier.weight(1f))
            CalculatorButton("÷", { onOperatorClick("÷") }, isOperator = true, modifier = Modifier.weight(1f))
            CalculatorButton("×", { onOperatorClick("×") }, isOperator = true, modifier = Modifier.weight(1f))
        }

        // Row 2: 7, 8, 9, −
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalculatorButton("7", { onDigitClick("7") }, modifier = Modifier.weight(1f))
            CalculatorButton("8", { onDigitClick("8") }, modifier = Modifier.weight(1f))
            CalculatorButton("9", { onDigitClick("9") }, modifier = Modifier.weight(1f))
            CalculatorButton("−", { onOperatorClick("−") }, isOperator = true, modifier = Modifier.weight(1f))
        }

        // Row 3: 4, 5, 6, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalculatorButton("4", { onDigitClick("4") }, modifier = Modifier.weight(1f))
            CalculatorButton("5", { onDigitClick("5") }, modifier = Modifier.weight(1f))
            CalculatorButton("6", { onDigitClick("6") }, modifier = Modifier.weight(1f))
            CalculatorButton("+", { onOperatorClick("+") }, isOperator = true, modifier = Modifier.weight(1f))
        }

        // Row 4: 1, 2, 3, =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalculatorButton("1", { onDigitClick("1") }, modifier = Modifier.weight(1f))
            CalculatorButton("2", { onDigitClick("2") }, modifier = Modifier.weight(1f))
            CalculatorButton("3", { onDigitClick("3") }, modifier = Modifier.weight(1f))
            CalculatorButton("=", onEqualClick, isOperator = true, modifier = Modifier.weight(1f))
        }

        // Row 5: ., 0, ⌫, ✓
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalculatorButton(".", onDotClick, modifier = Modifier.weight(1f))
            CalculatorButton("0", { onDigitClick("0") }, modifier = Modifier.weight(1f))
            CalculatorButton("⌫", onBackspaceClick, isAction = true, modifier = Modifier.weight(1f))
            CalculatorButton("✓", onConfirmClick, isConfirm = true, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CalculatorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAction: Boolean = false,
    isOperator: Boolean = false,
    isConfirm: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor = when {
        isPressed -> MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.3f)
        isConfirm -> MaterialTheme.colorScheme.primary
        isOperator -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isConfirm -> MaterialTheme.colorScheme.onPrimary
        isAction -> MaterialTheme.colorScheme.error
        isOperator -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(ZincCornerRadius))
            .background(containerColor)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(ZincCornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = if (isConfirm || isOperator || isAction) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}
