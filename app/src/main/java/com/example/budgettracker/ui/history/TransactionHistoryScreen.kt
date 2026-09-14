package com.example.budgettracker.ui.history

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.budgettracker.ui.components.DeleteTransactionConfirmDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.ui.theme.Green400
import com.example.budgettracker.ui.theme.Green500
import com.example.budgettracker.ui.theme.Orange500
import com.example.budgettracker.ui.theme.Red400
import com.example.budgettracker.ui.theme.Red500
import com.example.budgettracker.ui.theme.ZincCornerRadius
import com.example.budgettracker.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: TransactionHistoryViewModel,
    onNavigateBack: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.allAccounts.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Back", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    if (selectedAccountId != null || selectedType != null || searchQuery.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearFilters() }) {
                            Text("Clear Filters", fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search transactions by title or category...", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Type Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { viewModel.setSelectedType(null) },
                        label = { Text("All Types", fontSize = 12.sp) }
                    )
                }
                items(TransactionType.entries.toTypedArray()) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { viewModel.setSelectedType(if (selectedType == type) null else type) },
                        label = { Text(type.name, fontSize = 12.sp) }
                    )
                }
            }

            // Account Filter Chips
            if (accounts.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedAccountId == null,
                            onClick = { viewModel.setSelectedAccount(null) },
                            label = { Text("All Accounts", fontSize = 12.sp) }
                        )
                    }
                    items(accounts) { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { viewModel.setSelectedAccount(if (selectedAccountId == acc.id) null else acc.id) },
                            label = { Text(acc.name, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Results count banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${transactions.size} transaction${if (transactions.size == 1) "" else "s"} found",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Transactions List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching transactions found.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        val sourceAcc = accounts.find { it.id == tx.accountId }
                        val destAcc = tx.toAccountId?.let { id -> accounts.find { it.id == id } }

                        val amountColor = when (tx.type) {
                            TransactionType.INCOME -> Green500
                            TransactionType.EXPENSE -> Orange500
                            TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
                            TransactionType.INSTALLMENT -> Red500
                        }

                        val prefix = when (tx.type) {
                            TransactionType.INCOME -> "+"
                            TransactionType.EXPENSE -> "-"
                            TransactionType.INSTALLMENT -> "-"
                            else -> ""
                        }

                        val accountSubtext = when (tx.type) {
                            TransactionType.TRANSFER -> "${sourceAcc?.name ?: "Account #${tx.accountId}"} → ${destAcc?.name ?: "Account #${tx.toAccountId}"}"
                            else -> sourceAcc?.name ?: "Account #${tx.accountId}"
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ZincCornerRadius))
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(ZincCornerRadius))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(ZincCornerRadius))
                                .clickable { onEditTransaction(tx) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tx.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = tx.category,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = accountSubtext,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = dateFormat.format(Date(tx.timestamp)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "$prefix${CurrencyUtils.formatCentavosToPesos(tx.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = amountColor
                                    )

                                    TextButton(
                                        onClick = { transactionToDelete = tx }
                                    ) {
                                        Text("Delete", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before deleting transaction
    transactionToDelete?.let { tx ->
        DeleteTransactionConfirmDialog(
            onConfirmDelete = {
                viewModel.deleteTransaction(tx)
                transactionToDelete = null
            },
            onDismiss = { transactionToDelete = null }
        )
    }
}
