# Forensic Audit Report: Milestone 1 (M1) Core Data Layer & Test Runner

**Auditor Agent**: `teamwork_preview_auditor_m1_1`  
**Working Directory**: `d:/AndroidStudioProjects/BudgetTracker/.agents/teamwork_preview_auditor_m1_1`  
**Target Work Product**: Milestone 1 (Data Layer, DAOs, Entities, LoanDateUtils, Tests, Build Configuration)  
**Profile**: General Project  
**Integrity Mode**: Demo (from `ORIGINAL_REQUEST.md`)  
**Verdict**: **CLEAN**  

---

## 1. Observation

### 1.1 Source Files Inspected
1. **`app/src/main/java/com/example/budgettracker/data/local/entity/Entities.kt` (lines 1–113)**:
   - `AccountEntity`: Real SQLite entity mapped to table `accounts`. Includes `@PrimaryKey(autoGenerate = true) val id: Long = 0`, `isActive: Boolean = true`, `displayOrder: Int = 0`, `createdAt: Long`, `updatedAt: Long`. Index on `("isActive", "displayOrder")`.
   - `LoanAccountDetailsEntity`: Real SQLite entity mapped to table `loan_details` with foreign key `ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE)`.
   - `TransactionEntity`: Real SQLite entity mapped to table `transactions` with foreign keys:
     - `ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT)`
     - `ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["toAccountId"], onDelete = ForeignKey.RESTRICT)`
     Indices on `accountId`, `toAccountId`, `timestamp`, `("type", "category")`, `("type", "title")`. Includes `note: String? = null`.
   - POJOs `AccountWithBalance` and `AccountWithLoanDetails` for Room query projections.

2. **`app/src/main/java/com/example/budgettracker/data/local/dao/AccountDao.kt` (lines 1–135)**:
   - `getAccountBalance`: Authentic dynamic SQL aggregation query:
     ```sql
     SELECT a.initialBalance + COALESCE((
         SELECT SUM(
             CASE 
                 WHEN t.type = 'INCOME' AND t.accountId = a.id THEN t.amount
                 WHEN t.type = 'EXPENSE' AND t.accountId = a.id THEN -t.amount
                 WHEN t.type = 'TRANSFER' AND t.accountId = a.id THEN -t.amount
                 WHEN t.type = 'TRANSFER' AND t.toAccountId = a.id THEN t.amount
                 WHEN t.type = 'ADJUSTMENT' AND t.accountId = a.id AND t.adjustmentDirection = 'INCREASE' THEN t.amount
                 WHEN t.type = 'ADJUSTMENT' AND t.accountId = a.id AND t.adjustmentDirection = 'DECREASE' THEN -t.amount
                 ELSE 0
             END
         )
         FROM transactions t
         WHERE t.accountId = a.id OR t.toAccountId = a.id
     ), 0)
     FROM accounts a
     WHERE a.id = :accountId
     ```
   - `getTotalNetWorth`: Aggregates dynamic balances across all `a.isActive = 1` accounts.
   - `softDelete`: `UPDATE accounts SET isActive = 0, updatedAt = :timestamp WHERE id = :id`.
   - `restoreAccount`: `UPDATE accounts SET isActive = 1, updatedAt = :timestamp WHERE id = :id`.
   - `updateDisplayOrder`: `UPDATE accounts SET displayOrder = :order WHERE id = :id`.
   - `getActiveCountDirect`: `SELECT COUNT(*) FROM accounts WHERE isActive = 1`.

3. **`app/src/main/java/com/example/budgettracker/data/local/dao/TransactionDao.kt` (lines 1–104)**:
   - `getMonthlyTotals`: Computes income and expense totals strictly filtering out transfers and adjustments:
     ```sql
     SELECT 
         COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS totalIncome,
         COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS totalExpense
     FROM transactions
     WHERE timestamp >= :startTime AND timestamp <= :endTime
       AND type IN ('INCOME', 'EXPENSE')
     ```
   - `MonthlyTotals` data class computes `netSavings` (`totalIncome - totalExpense`) and `savingsRate` (`(netSavings / totalIncome) * 100f`).
   - `getFilteredTransactions`: Dynamic multi-parameter filter handling nullable `accountId`, `type`, `category`, `startTime`, and `endTime`.
   - `getDistinctCategories` and `getDistinctTitles`: Autocomplete queries scoped strictly by `type = :type`.

4. **`app/src/main/java/com/example/budgettracker/util/LoanDateUtils.kt` (lines 1–86)**:
   - `calculateNextDueDate`: Handles date clamping via `targetDay.coerceAtMost(ym.lengthOfMonth())` with `java.time.YearMonth`, candidate evaluation in current month, and rollover to next month.
   - `isDueSoon`: Default threshold `daysBefore = 7`, detects overdue (`dueDate.isBefore(today)`) and upcoming due dates within window.
   - `getDueDateStatus`: Returns `DueDateStatus.OVERDUE` for `< 0` days, `DueDateStatus.DUE_SOON` for `0..7` days, and `DueDateStatus.UPCOMING` for `> 7` days.

5. **`app/src/test/java/com/example/budgettracker/DatabaseBalanceUnitTest.kt` (lines 1–580)**:
   - 9 Robolectric unit tests executing against real in-memory SQLite database (`Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()`).
   - Zero mocking libraries (`mockito`, `mockk`) used. Genuine SQLite tables and foreign key enforcements tested.
   - Covers: live balance calculations with transfers and adjustments, net worth neutrality of transfers, monthly income/expense totals excluding transfers/adjustments, autocomplete scoping per type, `ForeignKey.RESTRICT` SQLite constraint failure, `ForeignKey.CASCADE` on loan details, soft delete and restore, display order updates, and multi-filter transaction queries.

6. **`app/src/test/java/com/example/budgettracker/LoanDateUtilsTest.kt` (lines 1–234)**:
   - 20 unit tests covering leap year February clamping (2024, 2028 -> 29), non-leap year February clamping (2023, 2025, 2026, 2100 -> 28), 30-day month clamping (Apr, Jun, Sep, Nov -> 30), year boundary rollovers (Dec to Jan), dual cycle days, overdue/due soon/upcoming window thresholds, and boundary validation exceptions (0 and 32 throw `IllegalArgumentException`).

7. **`app/build.gradle.kts` (lines 1–75)**:
   - Robolectric 4.14.1, `androidx.test.core`, and `androidx.junit` configured.
   - Java 21 JBR configured for Test task execution to support ASM bytecode manipulation under AGP compileSdk 37.

### 1.2 Prohibited Patterns & Integrity Scans
- **Hardcoded test returns**: Grep searches for `TODO`, `FIXME`, `NotImplemented`, `stub`, `mock` in `app/src/main` returned 0 results.
- **Facade implementations**: No constant returns or dummy methods found. All DAO methods define real SQL queries executed by Room.
- **Pre-populated test logs**: Searched workspace for pre-existing `*.log` files predating the runs — none found.
- **Self-certifying tests**: Unit tests insert dynamic entities with specific IDs, dates, and amounts, and assert calculated values against real SQLite query results.
- **Layout compliance**: Verified `.agents/` folder contains only metadata markdown files. No source code or tests exist inside `.agents/`.

### 1.3 Empirical Verification Results
1. **Unit Test Execution**:
   - Command:
     ```powershell
     $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest
     ```
   - Verbatim Output:
     ```
     > Task :app:testDebugUnitTest

     BUILD SUCCESSFUL in 1m 53s
     31 actionable tasks: 8 executed, 23 up-to-date
     Configuration cache entry reused.
     ```
   - Test XML Result Summary:
     - `TEST-com.example.budgettracker.DatabaseBalanceUnitTest.xml`: 9 tests, 0 skipped, 0 failures, 0 errors (time: 14.752s).
     - `TEST-com.example.budgettracker.LoanDateUtilsTest.xml`: 20 tests, 0 skipped, 0 failures, 0 errors (time: 0.151s).
     - `TEST-com.example.budgettracker.ExampleUnitTest.xml`: 1 test, 0 skipped, 0 failures, 0 errors.
     - **Total: 30 tests executed, 30 passed, 0 failed.**

2. **Debug Build Execution**:
   - Command:
     ```powershell
     $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug
     ```
   - Verbatim Output:
     ```
     > Task :app:assembleDebug
     > Task :app:createDebugApkListingFileRedirect

     BUILD SUCCESSFUL in 57s
     37 actionable tasks: 17 executed, 20 up-to-date
     Configuration cache entry stored.
     ```
   - Produced APK artifact: `app/build/outputs/apk/debug/app-debug.apk` (Size: 12,023,913 bytes, LastWriteTime: 09/11/2026 4:28:16 pm).

---

## 2. Logic Chain

1. **Entities & Relational Constraints (Observation 1.1 #1)**:
   - R1 explicitly requires `onDelete = ForeignKey.RESTRICT` on transactions to prevent accidental hard deletion of accounts with transaction history, and `CASCADE` on loan details.
   - Inspection of `Entities.kt` lines 34-41 and 56-69 proves that `ForeignKey.CASCADE` is declared on `LoanAccountDetailsEntity` referencing `AccountEntity`, and `ForeignKey.RESTRICT` is declared on `TransactionEntity` referencing `AccountEntity` for both `accountId` and `toAccountId`.
   - `DatabaseBalanceUnitTest.testForeignKeyRestrictPreventsHardDeleteOfAccountWithTransactions` verifies that attempting to delete an account with transactions raises `android.database.sqlite.SQLiteConstraintException`.
   - `DatabaseBalanceUnitTest.testForeignKeyCascadeOnLoanDetails` verifies that deleting an account without transactions cascades and deletes its corresponding loan details row.

2. **Authentic SQL Computation & Invariant Integrity (Observation 1.1 #2 & #3)**:
   - R1 mandates that live balances and monthly Income vs Expense metrics are computed dynamically via SQL aggregation without double-counting transfers.
   - In `AccountDao.kt` (`getAccountBalance` and `getTotalNetWorth`), a transfer subtracts from `accountId` and adds to `toAccountId`. In `getTotalNetWorth()`, the sum across all active accounts yields a net zero delta (`-amount + amount = 0`), which preserves net-worth neutrality.
   - `DatabaseBalanceUnitTest.testTransferDoesNotAffectNetWorth` explicitly proves this invariant.
   - In `TransactionDao.kt`, `getMonthlyTotals` groups and sums amounts strictly `WHERE type IN ('INCOME', 'EXPENSE')`, inherently excluding `TRANSFER` and `ADJUSTMENT` transactions from monthly income/expense metrics.
   - `DatabaseBalanceUnitTest.testMonthlyTotalsCorrectlyIncludesIncomeExpenseAndExcludesTransfersAdjustments` confirms that transfers and adjustments do not distort monthly budget metrics.

3. **No Facades, Stubs, or Mocks (Observation 1.2)**:
   - In accordance with Demo Integrity Mode rules, the implementation must not rely on facade stubs or pre-calculated hardcoded answers.
   - Source code analysis confirmed 0 instances of `mock`, `stub`, `NotImplemented`, or dummy constants.
   - Robolectric boots an authentic Android environment with a real SQLite engine, executing the exact Room-generated DAO SQL implementations.

4. **Build & Test Correctness (Observation 1.3)**:
   - Both `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest` and `assembleDebug` run successfully with exit code 0.
   - All 30 unit tests pass with zero failures.
   - The compiled debug APK was verified on disk.

---

## 3. Caveats

1. **Robolectric & Java Bytecode Instrumentation**:
   - The project's AGP setup uses `compileSdk = 37` and Gradle runs via Android Studio's bundled JBR (Java 25). Robolectric 4.14.1 requires Java 21 bytecode compatibility for ASM bytecode generation during test execution. This is addressed in `app/build.gradle.kts` by routing `Test` task executables to the local IntelliJ Java 21 JBR runtime (`C:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2024.3.1.1/jbr/bin/java.exe`).
   - If executed on another machine without that exact IntelliJ JBR path, Gradle defaults to the active JVM, which must support ASM 9.7 (Java 21 recommended).

---

## 4. Conclusion

The Milestone 1 (M1) work product passes all forensic integrity checks under Demo Mode:
- **No hardcoded test values, mock returns, or bypasses detected.**
- **Room DAOs compute account balances, net worth, monthly totals, and autocomplete options through authentic SQL queries.**
- **SQLite foreign key constraints (`RESTRICT` on transactions, `CASCADE` on loan details) are genuinely enforced.**
- **Robolectric unit test suite executes real in-memory SQLite transactions with a 100% pass rate (30/30 passed).**
- **Debug APK builds cleanly (`assembleDebug` exit code 0).**

**Verdict: CLEAN**

---

## 5. Verification Method

### 5.1 Commands
Execute in PowerShell:
```powershell
# 1. Run unit test suite
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest

# 2. Build debug APK
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug
```

### 5.2 Files to Inspect
- `app/src/main/java/com/example/budgettracker/data/local/entity/Entities.kt`
- `app/src/main/java/com/example/budgettracker/data/local/dao/AccountDao.kt`
- `app/src/main/java/com/example/budgettracker/data/local/dao/TransactionDao.kt`
- `app/src/main/java/com/example/budgettracker/util/LoanDateUtils.kt`
- `app/src/test/java/com/example/budgettracker/DatabaseBalanceUnitTest.kt`
- `app/src/test/java/com/example/budgettracker/LoanDateUtilsTest.kt`
- `app/build.gradle.kts`
- `app/build/outputs/apk/debug/app-debug.apk`

### 5.3 Invalidation Conditions
- Any failure or non-zero exit code during `testDebugUnitTest` or `assembleDebug`.
- Presence of any dummy/mock classes or hardcoded return constants in Room DAOs or entities.
- Removal of `ForeignKey.RESTRICT` on `TransactionEntity` or `ForeignKey.CASCADE` on `LoanAccountDetailsEntity`.
- Inclusion of `TRANSFER` or `ADJUSTMENT` in `getMonthlyTotals` income/expense calculations.
