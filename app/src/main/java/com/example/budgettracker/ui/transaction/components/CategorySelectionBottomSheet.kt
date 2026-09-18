package com.example.budgettracker.ui.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.ui.components.StandardBottomSheetDragHandle
import com.example.budgettracker.ui.theme.AmberGlow
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionBottomSheet(
    transactionType: TransactionType,
    selectedCategory: String,
    customCategories: List<CategoryItem>,
    onCategorySelected: (String) -> Unit,
    onAddCustomCategory: (name: String, iconName: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isAddingCustomCategory by remember { mutableStateOf(false) }
    var customNameInput by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf(CategoryIconMapper.AVAILABLE_CUSTOM_ICONS.first()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetCategories = remember(transactionType) {
        CategoryIconMapper.getPresetCategories(transactionType)
    }

    val allCategories = remember(presetCategories, customCategories) {
        val customMap = customCategories.map { it.name.lowercase() to it }.toMap()
        val combined = presetCategories.filter { !customMap.containsKey(it.name.lowercase()) } + customCategories
        combined
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { StandardBottomSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            if (isAddingCustomCategory) {
                // Add Custom Category Form
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New ${transactionType.name.lowercase().replaceFirstChar { it.uppercase() }} Category",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = {
                        isAddingCustomCategory = false
                        errorMessage = null
                        customNameInput = ""
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customNameInput,
                    onValueChange = {
                        customNameInput = it
                        errorMessage = null
                    },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Coffee & Snacks") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    shape = RoundedCornerShape(ZincCornerRadius),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Icon",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Icon Picker Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 44.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    items(CategoryIconMapper.AVAILABLE_CUSTOM_ICONS) { iconKey ->
                        val isSelected = selectedIconName == iconKey
                        val iconVector = CategoryIconMapper.getIcon(iconKey)

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) AmberGlow.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) AmberGlow else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIconName = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = iconKey,
                                tint = if (isSelected) AmberGlow else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val trimmed = customNameInput.trim()
                        if (trimmed.isBlank()) {
                            errorMessage = "Category name cannot be empty"
                            return@Button
                        }
                        // Check duplicates against presets and existing custom categories
                        val isDuplicate = allCategories.any {
                            it.name.trim().equals(trimmed, ignoreCase = true)
                        }
                        if (isDuplicate) {
                            errorMessage = "Category '$trimmed' already exists for this type"
                            return@Button
                        }

                        onAddCustomCategory(trimmed, selectedIconName)
                        onCategorySelected(trimmed)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGlow,
                        contentColor = Color(0xFF18181B)
                    ),
                    shape = RoundedCornerShape(ZincSoftCornerRadius),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Save Category", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Category",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    TextButton(
                        onClick = { isAddingCustomCategory = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = AmberGlow)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Custom", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Categories Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    items(allCategories) { item ->
                        val isSelected = item.name.equals(selectedCategory, ignoreCase = true)
                        val iconVector = CategoryIconMapper.getIcon(item.iconName)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(ZincCornerRadius))
                                .clickable {
                                    onCategorySelected(item.name)
                                    onDismiss()
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) AmberGlow.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) AmberGlow else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = item.name,
                                    tint = if (isSelected) AmberGlow else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Text(
                                text = item.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AmberGlow else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
