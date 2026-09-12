package com.example.budgettracker.ui.backup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.backup.BackupData
import com.example.budgettracker.data.backup.BackupManager
import com.example.budgettracker.data.backup.RestoreResult
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.ui.theme.ZincCornerRadius
import kotlinx.coroutines.launch

@Composable
fun BackupRestoreDialog(
    repository: BudgetRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Export, 1 = Import

    // Export State
    var exportedJson by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(false) }

    // Import State
    var importInputJson by remember { mutableStateOf("") }
    var parsedBackupData by remember { mutableStateOf<BackupData?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var restoreStatus by remember { mutableStateOf<String?>(null) }
    var showConfirmRestoreAlert by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        title = {
            Text(
                text = "Offline Backup & Restore",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Export JSON", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Import JSON", fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTabIndex) {
                    0 -> { // Export Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Export your entire budget data (accounts, loan details, transactions) as a single offline JSON backup.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    scope.launch {
                                        isExporting = true
                                        val accounts = repository.getAllAccountsDirect()
                                        val loanDetails = repository.getAllLoanDetailsDirect()
                                        val installmentPlans = repository.getAllInstallmentPlansDirect()
                                        val transactions = repository.getAllTransactionsDirect()
                                        exportedJson = BackupManager.exportToJson(accounts, loanDetails, installmentPlans, transactions)
                                        isExporting = false
                                    }
                                },
                                shape = RoundedCornerShape(ZincCornerRadius),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isExporting
                            ) {
                                Text(if (isExporting) "Generating Backup..." else "Generate Backup JSON", fontSize = 13.sp)
                            }

                            if (exportedJson.isNotEmpty()) {
                                OutlinedTextField(
                                    value = exportedJson,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Budget Tracker Backup", exportedJson)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(ZincCornerRadius),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Copy to Clipboard", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    1 -> { // Import Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Paste your backup JSON below to restore database state. Caution: Resets existing local data.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = importInputJson,
                                onValueChange = {
                                    importInputJson = it
                                    parsedBackupData = null
                                    validationError = null
                                    restoreStatus = null
                                },
                                placeholder = { Text("Paste JSON backup string here...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            parsedBackupData = BackupManager.importFromJson(importInputJson)
                                            validationError = null
                                        } catch (e: Exception) {
                                            parsedBackupData = null
                                            validationError = e.message ?: "Invalid JSON backup structure."
                                        }
                                    },
                                    shape = RoundedCornerShape(ZincCornerRadius),
                                    modifier = Modifier.weight(1f),
                                    enabled = importInputJson.isNotBlank()
                                ) {
                                    Text("Validate JSON", fontSize = 13.sp)
                                }
                            }

                            validationError?.let { err ->
                                Text(
                                    text = "Error: $err",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            parsedBackupData?.let { backup ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(ZincCornerRadius))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(ZincCornerRadius))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Valid Backup Found (v${backup.version}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("• Accounts: ${backup.accounts.size}", fontSize = 12.sp)
                                        Text("• Loan Details: ${backup.loanDetails.size}", fontSize = 12.sp)
                                        Text("• Installments: ${backup.installmentPlans.size}", fontSize = 12.sp)
                                        Text("• Transactions: ${backup.transactions.size}", fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = { showConfirmRestoreAlert = true },
                                    shape = RoundedCornerShape(ZincCornerRadius),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isRestoring
                                ) {
                                    Text("Restore Database", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            restoreStatus?.let { status ->
                                Text(
                                    text = status,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(ZincCornerRadius),
        modifier = modifier
    )

    // Confirmation alert before wiping DB and restoring
    if (showConfirmRestoreAlert) {
        AlertDialog(
            onDismissRequest = { showConfirmRestoreAlert = false },
            title = { Text("Confirm Database Restore", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Warning: Restoring from backup will delete all existing local accounts and transactions and replace them with the imported backup contents. Proceed?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmRestoreAlert = false
                        val backup = parsedBackupData ?: return@Button
                        scope.launch {
                            isRestoring = true
                            val result = BackupManager.performRestore(repository, backup)
                            isRestoring = false
                            restoreStatus = when (result) {
                                is RestoreResult.Success -> "Success! Restored ${result.accountsCount} accounts, ${result.loanDetailsCount} loan details, and ${result.transactionsCount} transactions."
                                is RestoreResult.Error -> "Restore Error: ${result.message}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Overwrite & Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRestoreAlert = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
