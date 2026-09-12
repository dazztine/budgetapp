# Core Architecture & Data Layer Specification (R1, R3, R4, R5)

## 1. Observation

### 1.1 Existing Database Layer
- **`app/src/main/java/com/example/budgettracker/data/local/entity/Entities.kt`**:
  - `AccountEntity` (lines 19–28):
    ```kotlin
    @Entity(
        tableName = "accounts",
        indices = [Index(value = ["isActive", "displayOrder"])]
    )
    data class AccountEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val name: String,
        val type: AccountType,
        val presetId: String? = null,
        val initialBalance: Long = 0L,
        val isActive: Boolean = true,
        val displayOrder: Int = 0
    )
    ```
    *Observation*: `AccountEntity` stores monetary amounts as integer centavos (`Long`). Lacks timestamp fields (`createdAt`, `updatedAt`).
  - `LoanAccountDetailsEntity` (lines 41–50):
    ```kotlin
    @Entity(
        tableName = "loan_details",
        foreignKeys = [
            ForeignKey(
                entity = AccountEntity::class,
                parentColumns = ["id"],
                childColumns = ["accountId"],
                onDelete = ForeignKey.CASCADE
            )
        ]
    )
    data class LoanAccountDetailsEntity(
        @PrimaryKey val accountId: Long,
        val cycleDay1: Int,
        val cycleDay2: Int? = null,
        val minimumAmountDue: Long = 0L,
        val totalRemainingBalance: Long = 0L,
        val reminderEnabled: Boolean = true,
        val reminderDaysBefore: Int = 3
    )
    ```
    *Observation*: Correctly enforces `ForeignKey.CASCADE` on `accountId` referencing `AccountEntity(id)`. Primary key is `accountId` (1-to-1 account loan relation).
  - `TransactionEntity` (lines 76–88):
    ```kotlin
    @Entity(
        tableName = "transactions",
        foreignKeys = [
            ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
            ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["toAccountId"], onDelete = ForeignKey.RESTRICT)
        ],
        indices = [
            Index(value = ["accountId"]),
            Index(value = ["toAccountId"]),
            Index(value = ["timestamp"]),
            Index(value = ["type", "category"]),
            Index(value = ["type", "title"])
        ]
    )
    data class TransactionEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val type: TransactionType,
        val amount: Long,
        val adjustmentDirection: AdjustmentDirection? = null,
        val accountId: Long,
        val toAccountId: Long? = null,
        val category: String,
        val title: String,
        val timestamp: Long,
        val createdAt: Long = System.currentTimeMillis()
    )
    ```
    *Observation*: Correctly enforces `ForeignKey.RESTRICT` on both `accountId` and `toAccountId`. Lacks an explicit `note: String? = null` field requested in R1/R4.

- **`app/src/main/java/com/example/budgettracker/data/local/dao/AccountDao.kt`**:
  - Live Account Balance query (lines 41–60) uses a subquery calculating:
    `a.initialBalance + SUM(INCOME - EXPENSE - outgoing TRANSFER + incoming TRANSFER + ADJUSTMENT_INCREASE - ADJUSTMENT_DECREASE)`.
  - `getAllActiveWithBalances()` (lines 62–90) exposes `Flow<List<AccountWithBalance>>` with live balances.
  - `getTotalNetWorth()` (lines 92–113) aggregates balances across all active accounts (`isActive = 1`).
  - `getActiveAccountCount()` (lines 38–39) returns `Flow<Int>`.
  - Missing DAO methods: Dedicated soft-delete (`UPDATE accounts SET isActive = 0 WHERE id = :id`), restore account, and display order update.

- **`app/src/main/java/com/example/budgettracker/data/local/dao/TransactionDao.kt`**:
  - Has CRUD, `getAll()`, `getRecent(limit)`, `getByAccount(accountId)`, `getByDateRange(startTime, endTime)`.
  - Autocomplete queries (lines 42–67): `getDistinctCategories(type)`, `getDistinctTitles(type)`, and `getDistinctTitlesByCategory(type, category)` are properly scoped by `type`.
  - Missing DAO methods:
    1. Monthly Income vs Expense totals query excluding transfers and adjustments.
    2. Multi-parameter transaction history filter query (`accountId`, `type`, `category`, `startTime`, `endTime`).

### 1.2 Test Execution & Acceptance Criteria Alignment
- Command executed: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest`.
  - Result: `BUILD SUCCESSFUL in 58s`.
  - Only `LoanDateUtilsTest.kt` in `app/src/test/java/` was executed.
  - `DatabaseBalanceTest.kt` resides in `app/src/androidTest/java/com/example/budgettracker/DatabaseBalanceTest.kt`.
  - Acceptance Criteria in `ORIGINAL_REQUEST.md:39` specifies:
    `- [ ] .\gradlew.bat testDebugUnitTest passes all unit tests for live balance queries, transfer neutrality, loan due-date clamping (Feb 28/29, April 30), and autocomplete scoping.`
  - `gradle/libs.versions.toml` includes `robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }`.
  - Moving/enabling `DatabaseBalanceTest.kt` in `src/test/java/` using Robolectric will allow `testDebugUnitTest` to execute database tests on the JVM without requiring an emulator.

### 1.3 UI & Navigation Architecture
- **`app/src/main/java/com/example/budgettracker/MainActivity.kt`**: Currently contains default Compose boilerplate (`Greeting("Android")`).
- Jetpack Compose Material 3 is configured (`androidx.compose.material3`).

---

## 2. Logic Chain

### 2.1 Relational Integrity & Balance Queries (R1)
1. **Transfer Neutrality**:
   - For any transaction where `type == 'TRANSFER'`, funds move from `accountId` to `toAccountId`.
   - In individual account queries, `accountId` balance decreases by `amount`, while `toAccountId` balance increases by `amount`.
   - In `getTotalNetWorth()`, all active accounts are summed. When both accounts are active, `-amount + amount = 0`. Net worth remains strictly invariant across internal transfers.
2. **Monthly Income vs Expense Metric**:
   - Transfers shift money between accounts and adjustments correct bookkeeping errors; neither represents true earned income or operational consumption.
   - Therefore, the monthly aggregation query must strictly filter `WHERE type IN ('INCOME', 'EXPENSE')` and filter `timestamp` between the beginning of the target month and the end of the target month.
3. **Foreign Key Protection**:
   - `onDelete = ForeignKey.RESTRICT` on `TransactionEntity` guarantees that an `AccountEntity` cannot be hard-deleted if transactions reference it. Any attempt throws `SQLiteConstraintException`.
   - `onDelete = ForeignKey.CASCADE` on `LoanAccountDetailsEntity` ensures loan cycle details are wiped automatically if an account without transactions is hard-deleted.
   - For accounts with transactions, deletion MUST be performed via soft-delete (`isActive = false`), removing it from active queries while preserving relational history.

### 2.2 UI Hierarchy & Dynamic FAB (R3)
1. **Dynamic FAB Behavior**:
   - When `getActiveAccountCount() == 0`, user has no accounts to record expenses against. The FAB must operate in **Setup Mode** (displaying "Setup First Account" or linking directly to Account Creation / Batch Text Parser).
   - When `getActiveAccountCount() >= 1`, the FAB switches to **Transaction Mode** (displaying `+` and opening the Rapid Manual Entry Numpad).
2. **Strict 5-Section Dashboard**:
   - Jetpack Compose `LazyColumn` or scrollable `Column` with 5 distinct sections in fixed order:
     1. **Net Worth Banner**: Total balance with toggleable visibility (`VisualTransformation.None` vs `PasswordVisualTransformation` or bullet masking `₱ ••••••`).
     2. **Account Preview**: Up to 6 active accounts ordered by `displayOrder ASC, name ASC`. If total active accounts > 6, display an actionable "See All" card/chip.
     3. **"This Month: Income vs Expense" Summary**: Shows Total Income, Total Expense, Net Savings, and expense ratio.
     4. **Loan & Paylater Due-Date Carousel**: Horizontal pager/row for loan accounts (`AccountWithLoanDetails`). Dates clamped using `LoanDateUtils.calculateNextDueDate()`. Active warnings displayed if `LoanDateUtils.isDueSoon(dueDate, today, daysBefore = 7)` is true.
     5. **Recent Transactions List**: Top 5–10 recent transactions with category icons, account tags, and color-coded amounts (`+` green for income, `-` red for expense, `⇄` blue for transfer).

### 2.3 Rapid Manual Entry & Transaction Management (R4)
1. **Custom Numpad & Type Scoping**:
   - Soft keyboards on mobile cover 50% of the screen and slow down rapid entry. An integrated on-screen Numpad (digits 0–9, decimal, backspace, clear) enables rapid single-handed logging.
   - Autocomplete dropdowns/chips for Categories and Titles must query `transactionDao.getDistinctCategories(type)` passing the currently selected `TransactionType`. This prevents expense categories (e.g., "Dining", "Groceries") from polluting income entry (e.g., "Salary", "Dividends").
2. **Batch Workflow ("Save & Add Another")**:
   - Provides two primary buttons:
     - `Save & Add Another`: Inserts transaction into Room, keeps account/category selected, resets amount/title, and displays a transient confirmation toast/snackbar so the user can log multiple expenses in seconds.
     - `Save`: Inserts transaction and navigates back to Dashboard.
3. **Transaction History Multi-Filtering & Swipe-to-Delete**:
   - Single flexible Room query supporting optional parameters (`accountId`, `type`, `category`, `startTime`, `endTime`).
   - Material 3 `SwipeToDismissBox` with red background and delete icon. Deleting a transaction triggers Room deletion and displays a `Snackbar` with an `Undo` action (re-inserting the entity if clicked).
4. **Account Management & Soft-Cap of 10**:
   - Presets pre-populate common Philippine financial services:
     - E-Wallets: GCash, Maya, GrabPay.
     - Banks: BDO, BPI, UnionBank, Metrobank, Landbank, SeaBank, GoTyme.
     - BNPL / Credit: SPayLater, LazPayLater, GCredit, Maya Credit, Billease.
   - Soft-cap validation: When active accounts count reaches 10, adding an 11th account triggers a confirmation dialog: *"Soft-cap of 10 active accounts reached. Adding more accounts may clutter your dashboard. Continue?"* with Allow/Cancel options.

### 2.4 Data Ownership & Educational Definitions (R5)
1. **SAF Snapshot Roundtrip**:
   - Android Storage Access Framework ensures 100% offline file access without requiring dangerous storage permissions.
   - JSON format exports full relational database snapshot: accounts (including inactive), loan details, and transactions.
   - JSON import validates relational integrity and replaces database in an atomic Room transaction.
2. **CSV Export**:
   - Formats records into standard CSV columns (`ID, Date, Time, Type, Account, ToAccount, Category, Title, Amount, Note`).
3. **Financial Terminology Popup**:
   - Clear explanations of Net Worth, Transfer Neutrality, Adjustments, Dividends, and BNPL.

---

## 3. Detailed Specifications & Schemas

### 3.1 Database Schema Enhancements

#### AccountEntity Update:
Add timestamps and optional note support.
```kotlin
@Entity(
    tableName = "accounts",
    indices = [Index(value = ["isActive", "displayOrder"])]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val presetId: String? = null,
    val initialBalance: Long = 0L, // Centavos (e.g. 10000 = ₱100.00)
    val isActive: Boolean = true,  // Soft-delete flag
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

#### TransactionEntity Update:
Add explicit `note: String? = null` field.
```kotlin
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["toAccountId"]),
        Index(value = ["timestamp"]),
        Index(value = ["type", "category"]),
        Index(value = ["type", "title"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long,                                 // Centavos (positive)
    val adjustmentDirection: AdjustmentDirection? = null,
    val accountId: Long,                              // Source account
    val toAccountId: Long? = null,                    // Target account for TRANSFER
    val category: String,
    val title: String,
    val timestamp: Long,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
```

#### LoanAccountDetailsEntity:
```kotlin
@Entity(
    tableName = "loan_details",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LoanAccountDetailsEntity(
    @PrimaryKey
    val accountId: Long,
    val cycleDay1: Int,                               // 1..31
    val cycleDay2: Int? = null,                       // 1..31 (optional)
    val minimumAmountDue: Long = 0L,                  // Centavos
    val totalRemainingBalance: Long = 0L,             // Centavos
    val reminderEnabled: Boolean = true,
    val reminderDaysBefore: Int = 7
)
```

### 3.2 SQL Queries & DAO Methods

#### Missing AccountDao Methods:
```kotlin
@Query("UPDATE accounts SET isActive = 0, updatedAt = :timestamp WHERE id = :id")
suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis()): Int

@Query("UPDATE accounts SET isActive = 1, updatedAt = :timestamp WHERE id = :id")
suspend fun restoreAccount(id: Long, timestamp: Long = System.currentTimeMillis()): Int

@Query("UPDATE accounts SET displayOrder = :order WHERE id = :id")
suspend fun updateDisplayOrder(id: Long, order: Int): Int

@Query("SELECT COUNT(*) FROM accounts WHERE isActive = 1")
suspend fun getActiveCountDirect(): Int
```

#### Missing TransactionDao Methods:
```kotlin
data class MonthlyTotals(
    val totalIncome: Long,
    val totalExpense: Long
) {
    val netSavings: Long get() = totalIncome - totalExpense
    val savingsRate: Float get() = if (totalIncome > 0) (netSavings.toFloat() / totalIncome) * 100f else 0f
}

// 1. Monthly Income vs Expense (Excluding transfers and adjustments)
@Query("""
    SELECT 
        COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS totalIncome,
        COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS totalExpense
    FROM transactions
    WHERE timestamp >= :startTime AND timestamp <= :endTime
      AND type IN ('INCOME', 'EXPENSE')
""")
fun getMonthlyTotals(startTime: Long, endTime: Long): Flow<MonthlyTotals>

// 2. Multi-Filter Query for Transaction History
@Query("""
    SELECT * FROM transactions
    WHERE (:accountId IS NULL OR accountId = :accountId OR toAccountId = :accountId)
      AND (:type IS NULL OR type = :type)
      AND (:category IS NULL OR category = :category)
      AND (:startTime IS NULL OR timestamp >= :startTime)
      AND (:endTime IS NULL OR timestamp <= :endTime)
    ORDER BY timestamp DESC, id DESC
""")
fun getFilteredTransactions(
    accountId: Long? = null,
    type: TransactionType? = null,
    category: String? = null,
    startTime: Long? = null,
    endTime: Long? = null
): Flow<List<TransactionEntity>>
```

---

### 3.3 UI Architecture Specification

```
com.example.budgettracker
├── ui
│   ├── theme (Color, Theme, Type)
│   ├── navigation
│   │   ├── Screen.kt (Sealed class: Dashboard, AddTransaction, History, Accounts, Settings)
│   │   └── AppNavigation.kt
│   ├── dashboard
│   │   ├── DashboardScreen.kt (Scaffold + Dynamic FAB + Column of 5 sections)
│   │   ├── DashboardViewModel.kt
│   │   └── components
│   │       ├── NetWorthBanner.kt (Section 1: Net Worth + Eye Toggle)
│   │       ├── AccountPreviewSection.kt (Section 2: Up to 6 accounts + "See All")
│   │       ├── MonthlySummarySection.kt (Section 3: Income vs Expense summary)
│   │       ├── LoanDueCarouselSection.kt (Section 4: Clamped due dates + 7-day warnings)
│   │       └── RecentTransactionsSection.kt (Section 5: Recent transactions)
│   ├── transaction
│   │   ├── ManualTransactionScreen.kt (Numpad, Type pills, Autocomplete, "Save & Add Another")
│   │   ├── TransactionViewModel.kt
│   │   ├── TransactionHistoryScreen.kt (Multi-filter bar, Date groups, Swipe-to-delete)
│   │   └── components
│   │       ├── NumpadView.kt
│   │       └── FilterChipsRow.kt
│   ├── account
│   │   ├── AccountManagementScreen.kt (Preset grid, Custom account dialog, Soft-cap check)
│   │   └── AccountViewModel.kt
│   └── backup
│       ├── BackupRestoreDialog.kt (SAF JSON Roundtrip + CSV Export)
│       └── EducationalDefinitionsDialog.kt (Net Worth, Neutrality, Adjustments, Dividends, BNPL)
```

#### Dynamic FAB State Logic:
```kotlin
val activeAccountCount by viewModel.activeAccountCount.collectAsState(initial = 0)

FloatingActionButton(
    onClick = {
        if (activeAccountCount == 0) {
            navController.navigate(Screen.AccountManagement.route)
        } else {
            navController.navigate(Screen.ManualTransaction.route)
        }
    }
) {
    if (activeAccountCount == 0) {
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Set Up First Account")
        }
    } else {
        Icon(Icons.Default.Add, contentDescription = "Add Transaction")
    }
}
```

#### Autocomplete Scoping Logic:
In `TransactionViewModel.kt`:
```kotlin
val currentType = MutableStateFlow(TransactionType.EXPENSE)

val categories = currentType.flatMapLatest { type ->
    transactionDao.getDistinctCategories(type)
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

val titles = currentType.flatMapLatest { type ->
    transactionDao.getDistinctTitles(type)
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

---

### 3.4 Data Ownership (SAF JSON & CSV) Specification

#### JSON Roundtrip Structure:
```json
{
  "version": 1,
  "exportTimestamp": 1789123456789,
  "appName": "BudgetTracker",
  "accounts": [
    {
      "id": 1,
      "name": "GCash",
      "type": "E_WALLET",
      "presetId": "preset_gcash",
      "initialBalance": 10000,
      "isActive": true,
      "displayOrder": 0,
      "createdAt": 1789120000000,
      "updatedAt": 1789120000000
    }
  ],
  "loanDetails": [
    {
      "accountId": 2,
      "cycleDay1": 15,
      "cycleDay2": 30,
      "minimumAmountDue": 50000,
      "totalRemainingBalance": 200000,
      "reminderEnabled": true,
      "reminderDaysBefore": 7
    }
  ],
  "transactions": [
    {
      "id": 1,
      "type": "EXPENSE",
      "amount": 15000,
      "adjustmentDirection": null,
      "accountId": 1,
      "toAccountId": null,
      "category": "Food & Dining",
      "title": "Lunch",
      "timestamp": 1789121000000,
      "note": "Office lunch",
      "createdAt": 1789121000000
    }
  ]
}
```

#### CSV Export Format:
```csv
ID,Date,Time,Type,FromAccount,ToAccount,Category,Title,Amount,Note
1,2026-09-11,12:30:00,EXPENSE,GCash,,Food & Dining,Lunch,150.00,Office lunch
2,2026-09-11,14:00:00,TRANSFER,GCash,BPI,Transfer,Fund transfer,500.00,Savings
3,2026-09-11,15:00:00,INCOME,BPI,,Salary,Paycheck,25000.00,
```

#### SAF Intent Registration:
```kotlin
// Export JSON
val exportJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
) { uri -> uri?.let { viewModel.exportJsonSnapshot(it) } }

// Import JSON
val importJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri -> uri?.let { viewModel.importJsonSnapshot(it) } }

// Export CSV
val exportCsvLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/csv")
) { uri -> uri?.let { viewModel.exportCsv(it) } }
```

---

## 4. Caveats
1. `DatabaseBalanceTest.kt` is currently in `app/src/androidTest/java/` instead of `app/src/test/java/`. Moving it or referencing Robolectric in `app/build.gradle.kts` is necessary for `testDebugUnitTest` to execute Room database logic on the JVM.
2. Monetary amounts in `initialBalance`, `amount`, `minimumAmountDue`, and `totalRemainingBalance` are stored as `Long` centavos (e.g., 10000 = ₱100.00). Division by 100.0 must only occur at the UI/export display boundary to preserve exact integer math without floating-point drift.
3. Loan accounts (`AccountType.LOAN` and `AccountType.BNPL`) store outstanding debt in `LoanAccountDetailsEntity.totalRemainingBalance`. For net worth calculations, implementers should verify whether loan account initial balance is set to 0 or negative centavos to reflect liability in the aggregate sum.

---

## 5. Conclusion
The existing data layer provides a solid foundation with relational foreign keys (`RESTRICT` on transactions, `CASCADE` on loan details) and live balance subqueries that guarantee transfer neutrality. 

To achieve full compliance with requirements R1, R3, R4, and R5:
1. Complete the Room entities by adding `note` to `TransactionEntity` and timestamp fields to `AccountEntity`.
2. Implement missing DAO queries: `getMonthlyTotals` (excluding transfers and adjustments), `getFilteredTransactions` (dynamic multi-filter), and `softDelete`.
3. Build the Jetpack Compose UI with the strict 5-section Dashboard, Dynamic FAB, Numpad manual entry with strictly scoped autocomplete, transaction history with swipe-to-delete, Philippine presets with 10-account soft-cap, and SAF JSON/CSV backup roundtrip.
4. Relocate database unit tests to `src/test/java` with Robolectric so `.\gradlew.bat testDebugUnitTest` runs all database tests locally.

---

## 6. Verification Method

### 6.1 Build and Test Execution
Run the following commands using the Android Studio JDK:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

### 6.2 Key Assertions to Verify
1. **Transfer Neutrality**: Running `testTransferDoesNotAffectNetWorth()` verifies that transfers between accounts modify individual account balances while leaving global net worth unchanged.
2. **Foreign Key Restriction**: Hard-deleting an account with transactions throws `android.database.sqlite.SQLiteConstraintException`.
3. **Monthly Summaries**: Transactions of type `TRANSFER` and `ADJUSTMENT` are excluded from monthly income and expense totals.
4. **Autocomplete Scoping**: Calling `getDistinctCategories(TransactionType.EXPENSE)` does not return categories belonging to `TransactionType.INCOME`.
5. **Loan Date Clamping**: `LoanDateUtilsTest` confirms proper clamping on Feb 28/29 and April 30, and 7-day warnings for upcoming due dates.
