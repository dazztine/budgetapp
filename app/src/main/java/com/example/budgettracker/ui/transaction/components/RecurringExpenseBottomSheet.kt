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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.ui.components.StandardBottomSheetDragHandle
import com.example.budgettracker.ui.theme.AmberGlow
import com.example.budgettracker.ui.theme.MidnightNavy
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpenseBottomSheet(
    initialBillName: String,
    initialFrequency: String,
    initialDueDay: Int,
    initialDueMonth: Int,
    onConfirm: (billName: String, frequency: String, dueDay: Int, dueMonth: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var billName by remember { mutableStateOf(initialBillName) }
    var selectedFrequency by remember { mutableStateOf(initialFrequency) } // "DAILY", "MONTHLY", "YEARLY"
    var selectedDueDay by remember { mutableIntStateOf(initialDueDay.coerceIn(1, 31)) }
    var selectedDueMonth by remember { mutableIntStateOf(initialDueMonth.coerceIn(1, 12)) }

    var isMonthDropdownOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { StandardBottomSheetDragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Make Recurring Bill",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Automatically create a Bill Account and track upcoming billing cycles.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bill Account Name
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Bill Account Name",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = billName,
                    onValueChange = { billName = it },
                    placeholder = { Text("e.g. PLDT, Netflix, Rent", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberGlow,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // Recurrence Frequency
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Frequency",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val frequencies = listOf("Daily", "Monthly", "Yearly")
                    frequencies.forEach { freq ->
                        val freqKey = freq.uppercase()
                        val isSelected = selectedFrequency == freqKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFrequency = freqKey },
                            label = { Text(freq, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGlow,
                                selectedLabelColor = MidnightNavy,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = AmberGlow
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Frequency Details & Due Day Picker
            when (selectedFrequency) {
                "DAILY" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(ZincSoftCornerRadius))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "💡 Repeats every day. First upcoming bill will be scheduled for tomorrow.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                "MONTHLY" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Due Day of Month",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items((1..31).toList()) { day ->
                                val isSelected = day == selectedDueDay
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AmberGlow else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            1.dp,
                                            if (isSelected) AmberGlow else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedDueDay = day }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "$day",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MidnightNavy else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Repeats monthly on day $selectedDueDay. First upcoming bill scheduled for next month.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                "YEARLY" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Annual Due Date",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Month Selector Box
                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(ZincSoftCornerRadius))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(ZincSoftCornerRadius))
                                    .clickable { isMonthDropdownOpen = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                val monthName = Month.of(selectedDueMonth).getDisplayName(TextStyle.FULL, Locale.US)
                                Text(
                                    text = monthName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                DropdownMenu(
                                    expanded = isMonthDropdownOpen,
                                    onDismissRequest = { isMonthDropdownOpen = false }
                                ) {
                                    (1..12).forEach { m ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    Month.of(m).getDisplayName(TextStyle.FULL, Locale.US),
                                                    fontSize = 13.sp
                                                )
                                            },
                                            onClick = {
                                                selectedDueMonth = m
                                                isMonthDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Day Selector (horizontal mini-row or display)
                            Text(
                                text = "Day:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1.8f)
                            ) {
                                items((1..31).toList()) { day ->
                                    val isSelected = day == selectedDueDay
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) AmberGlow else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { selectedDueDay = day }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "$day",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MidnightNavy else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        val selectedMonthName = Month.of(selectedDueMonth).getDisplayName(TextStyle.FULL, Locale.US)
                        Text(
                            text = "Repeats annually on $selectedMonthName $selectedDueDay.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(ZincSoftCornerRadius),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        val finalName = billName.trim().ifEmpty { initialBillName }
                        onConfirm(finalName, selectedFrequency, selectedDueDay, selectedDueMonth)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGlow,
                        contentColor = MidnightNavy
                    ),
                    shape = RoundedCornerShape(ZincSoftCornerRadius),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("Apply Recurring Bill", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
