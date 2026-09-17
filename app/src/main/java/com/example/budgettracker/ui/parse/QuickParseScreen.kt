package com.example.budgettracker.ui.parse

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.ui.account.AddEditAccountDialog
import com.example.budgettracker.ui.components.AccountLimitReachedDialog
import com.example.budgettracker.ui.parse.components.BatchAccountsPreviewList
import com.example.budgettracker.ui.parse.components.SingleTransactionPreviewCard
import com.example.budgettracker.ui.theme.ZincSoftCornerRadius

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickParseScreen(
    viewModel: QuickParseViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inputText by viewModel.inputText.collectAsState()
    val parseResult by viewModel.parseResult.collectAsState()
    val activeAccounts by viewModel.activeAccounts.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var showAccountLimitDialog by remember { mutableStateOf(false) }
    var candidateAccountToCreate by remember { mutableStateOf<AccountEntity?>(null) }

    LaunchedEffect(parseResult) {
        when (val state = parseResult) {
            is ParseUiResult.Success -> {
                snackbarHostState.showSnackbar("Saved successfully to database")
                viewModel.resetParseResult()
            }
            is ParseUiResult.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetParseResult()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Parser", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                windowInsets = WindowInsets(top = 0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Quick Samples Chips ABOVE the input box
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sample Texts", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SampleChip(label = "Jollibee 250 GCash") {
                        viewModel.updateInputText("Jollibee 250 GCash")
                    }
                    SampleChip(label = "nag-Grab 320 via gcash") {
                        viewModel.updateInputText("nag-Grab 320 via gcash")
                    }
                    SampleChip(label = "lipat 500 gcash to maya") {
                        viewModel.updateInputText("lipat 500 gcash to maya")
                    }
                    SampleChip(label = "meralco 1500 gamit maya") {
                        viewModel.updateInputText("nagbayad ako 1500 sa meralco gamit maya")
                    }
                    SampleChip(label = "sahod 35k bpi") {
                        viewModel.updateInputText("sahod 35k bpi")
                    }
                    SampleChip(label = "s24 3500/mo spaylater") {
                        viewModel.updateInputText("bumili ako s24 3500/mo spaylater 6 months")
                    }
                }
            }

            // 2. Text Area Input Box (with Clear X icon)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paste transaction text",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                viewModel.updateInputText(pasted)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste Clipboard",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.updateInputText(it) },
                    placeholder = {
                        Text(
                            text = "Example:\nnag-Grab 320 via gcash\nor: Dinner 450 with Sarah GCash\nor: bumili ako s24 3500/mo spaylater 6 months",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateInputText("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Text",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    minLines = 4,
                    maxLines = 6,
                    shape = RoundedCornerShape(ZincSoftCornerRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Bottom-right tip label for voice dictate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Dictate",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp).height(14.dp)
                    )
                    Text(
                        text = "Use your keyboard mic to dictate",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3. Explicit "Parse Text" Button
            Button(
                onClick = { viewModel.parseText() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(ZincSoftCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(text = "Parse Text", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4. Dynamic Parse Result Container
            when (val state = parseResult) {
                is ParseUiResult.SingleTransaction -> {
                    SingleTransactionPreviewCard(
                        parsedTx = state.parsed,
                        accounts = activeAccounts,
                        onConfirm = { tx, accId, toAccId ->
                            viewModel.confirmAndSaveSingleTransaction(tx, accId, toAccId, onSuccess = onNavigateBack)
                        },
                        onAddNewAccount = { candidateName ->
                            if (activeAccounts.size >= 10) {
                                showAccountLimitDialog = true
                            } else {
                                candidateAccountToCreate = AccountEntity(name = candidateName, type = AccountType.BANK)
                                showCreateAccountDialog = true
                            }
                        }
                    )
                }
                is ParseUiResult.BatchAccounts -> {
                    BatchAccountsPreviewList(
                        parsedAccounts = state.accounts,
                        onConfirmSaveAll = { accountsList ->
                            if (activeAccounts.size + accountsList.size > 10) {
                                showAccountLimitDialog = true
                            } else {
                                viewModel.confirmAndSaveBatchAccounts(accountsList, onSuccess = onNavigateBack)
                            }
                        }
                    )
                }
                else -> {}
            }
        }
    }

    if (showCreateAccountDialog && candidateAccountToCreate != null) {
        AddEditAccountDialog(
            initialAccount = candidateAccountToCreate,
            onDismiss = {
                showCreateAccountDialog = false
                candidateAccountToCreate = null
            },
            onSaveFull = { acc, loan, sav, bill, credit ->
                viewModel.createAccount(acc, loan, sav, bill, credit)
                showCreateAccountDialog = false
                candidateAccountToCreate = null
            }
        )
    }

    if (showAccountLimitDialog) {
        AccountLimitReachedDialog(
            onDismiss = { showAccountLimitDialog = false }
        )
    }
}

@Composable
private fun SampleChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(ZincSoftCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(ZincSoftCornerRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
