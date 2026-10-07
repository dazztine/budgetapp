package com.example.budgettracker.ui.plan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.BudgetEntity
import com.example.budgettracker.domain.BudgetInputResult
import com.example.budgettracker.domain.PesoInput
import com.example.budgettracker.ui.transaction.components.CategoryIconMapper
import com.example.budgettracker.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditBudgetBottomSheet(
    isOverall: Boolean,
    existingBudget: BudgetEntity?,
    availableCategories: List<String>,
    existingCategoryBudgets: List<BudgetEntity>,
    daysInMonth: Int,
    onSave: (category: String?, amountCentavos: Long) -> Unit,
    onDelete: (budget: BudgetEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialAmount = remember(existingBudget) {
        if (existingBudget != null && existingBudget.amount > 0L) {
            val pesos = existingBudget.amount / 100L
            val cents = existingBudget.amount % 100L
            if (cents > 0L) {
                String.format(java.util.Locale.US, "%,d.%02d", pesos, cents)
            } else {
                String.format(java.util.Locale.US, "%,d", pesos)
            }
        } else {
            ""
        }
    }

    val initialCategory = remember(existingBudget) {
        existingBudget?.category
    }

    var typedAmountText by rememberSaveable { mutableStateOf(initialAmount) }
    var selectedCategory by rememberSaveable { mutableStateOf(initialCategory) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    val parseResult = remember(typedAmountText) {
        if (typedAmountText.isBlank()) {
            null
        } else {
            PesoInput.parseBudgetPesoInput(typedAmountText)
        }
    }

    // Filter available categories: exclude categories with an existing budget, except the current one being edited
    val selectableCategories = remember(availableCategories, existingCategoryBudgets, existingBudget) {
        val takenCategories = existingCategoryBudgets
            .filter { it.id != existingBudget?.id }
            .mapNotNull { it.category?.lowercase() }
            .toSet()
        availableCategories.filter { !takenCategories.contains(it.lowercase()) }
    }

    val title = when {
        isOverall && existingBudget != null -> "Edit Monthly Budget"
        isOverall -> "Set Monthly Budget"
        existingBudget != null -> "Edit Category Budget"
        else -> "Set Category Budget"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Picker (for per-category budget)
            if (!isOverall) {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (existingBudget != null) {
                    // Locked category display when editing existing category budget
                    val catName = existingBudget.category ?: "Category"
                    val icon = CategoryIconMapper.getIcon(catName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = catName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    // Category Selection Chips / FlowRow
                    if (selectableCategories.isEmpty()) {
                        Text(
                            text = "All active categories already have a budget configured.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectableCategories.forEach { categoryName ->
                                val isSelected = selectedCategory.equals(categoryName, ignoreCase = true)
                                val icon = CategoryIconMapper.getIcon(categoryName)

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable { selectedCategory = categoryName }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = categoryName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Amount Input Field
            Text(
                text = "Budget Amount",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            val isError = parseResult is BudgetInputResult.Error
            val errorMessage = (parseResult as? BudgetInputResult.Error)?.message

            OutlinedTextField(
                value = typedAmountText,
                onValueChange = { input ->
                    // Keep typed string, allowing digits, commas, and dots
                    if (input.all { it.isDigit() || it == ',' || it == '.' }) {
                        typedAmountText = PesoInput.formatPesoInput(input)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("0.00") },
                prefix = {
                    Text(
                        text = "₱",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                singleLine = true,
                isError = isError,
                supportingText = {
                    when {
                        isError && errorMessage != null -> {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                        parseResult is BudgetInputResult.Valid -> {
                            val centavos = parseResult.centavos
                            val dailyCentavos = if (daysInMonth > 0) centavos / daysInMonth.toLong() else 0L
                            val dailyFormatted = CurrencyUtils.formatCentavosToPesos(dailyCentavos)
                            Text(
                                text = "Equivalent to $dailyFormatted/day for $daysInMonth days",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            val canSave = parseResult is BudgetInputResult.Valid && (isOverall || !selectedCategory.isNullOrBlank())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (existingBudget != null) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }

                Button(
                    onClick = {
                        val valid = parseResult as? BudgetInputResult.Valid ?: return@Button
                        val category = if (isOverall) null else selectedCategory
                        onSave(category, valid.centavos)
                        onDismiss()
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(if (existingBudget != null) 1.5f else 1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Budget")
                }
            }
        }
    }

    if (showDeleteConfirmation && existingBudget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete this budget?") },
            text = { Text("Your transaction history won't be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete(existingBudget)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
