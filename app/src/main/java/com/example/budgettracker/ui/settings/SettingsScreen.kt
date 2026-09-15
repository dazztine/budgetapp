package com.example.budgettracker.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import com.example.budgettracker.ui.theme.ThemeSetting
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.export.CsvExporter
import com.example.budgettracker.data.repository.BudgetRepository
import com.example.budgettracker.ui.backup.BackupRestoreDialog
import com.example.budgettracker.ui.components.ThemePickerDialog
import androidx.compose.runtime.collectAsState
import com.example.budgettracker.ui.theme.ThemePreferences
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: BudgetRepository,
    themePreferences: ThemePreferences? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showBackupDialog by remember { mutableStateOf(false) }
    var showGlossarySheet by remember { mutableStateOf(false) }
    val glossarySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(top = 0.dp),
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsItemRow(
                icon = Icons.Outlined.UploadFile,
                title = "Local Backup & Export",
                subtitle = "Back up your data or export files",
                onClick = { showBackupDialog = true }
            )

            SettingsItemRow(
                icon = Icons.Outlined.Restore,
                title = "Restore",
                subtitle = "Restore from local backup",
                onClick = { showBackupDialog = true }
            )

            SettingsItemRow(
                icon = Icons.Outlined.Description,
                title = "CSV",
                subtitle = "Export as CSV file",
                onClick = {
                    scope.launch {
                        val transactions = repository.allTransactions.first()
                        val accounts = repository.activeAccounts.first()
                        val csvData = CsvExporter.generateCsv(transactions, accounts)

                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("kwago_Export.csv", csvData)
                        clipboard.setPrimaryClip(clip)

                        Toast.makeText(context, "CSV copied to clipboard (${transactions.size} records)", Toast.LENGTH_LONG).show()
                    }
                }
            )

            SettingsItemRow(
                icon = Icons.Outlined.Code,
                title = "JSON",
                subtitle = "Export as JSON file",
                onClick = { showBackupDialog = true }
            )

            SettingsItemRow(
                icon = Icons.Outlined.Book,
                title = "Financial Glossary",
                subtitle = "Learn financial terms",
                onClick = { showGlossarySheet = true }
            )

            var showThemeDialog by remember { mutableStateOf(false) }
            val themeSettingState = themePreferences?.themeSetting?.collectAsState()
            val currentThemeSetting = themeSettingState?.value ?: ThemeSetting.LIGHT

            SettingsItemRow(
                icon = Icons.Outlined.LightMode,
                title = "Theme",
                subtitle = when (currentThemeSetting) {
                    ThemeSetting.FOLLOW_DEVICE -> "Follow Device"
                    ThemeSetting.LIGHT -> "Light (Default)"
                    ThemeSetting.DARK -> "Dark"
                },
                onClick = { showThemeDialog = true }
            )

            if (showThemeDialog) {
                ThemePickerDialog(
                    currentSetting = currentThemeSetting,
                    onSettingSelected = { selected ->
                        themePreferences?.setThemeSetting(selected)
                    },
                    onDismiss = { showThemeDialog = false }
                )
            }

            val appVersion = remember {
                try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pInfo.versionName ?: "1.3.0"
                } catch (e: Exception) {
                    "1.3.0"
                }
            }

            SettingsItemRow(
                icon = Icons.Outlined.Info,
                title = "About",
                subtitle = "App version $appVersion",
                onClick = {
                    Toast.makeText(context, "kwago v$appVersion (Offline-First)", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            repository = repository,
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showGlossarySheet) {
        FinancialGlossaryDialog(
            sheetState = glossarySheetState,
            onDismiss = { showGlossarySheet = false }
        )
    }
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ZincSoftCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(ZincSoftCornerRadius))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
