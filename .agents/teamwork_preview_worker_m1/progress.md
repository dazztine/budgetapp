# Progress Log - Milestone 1: Core Data Layer & Test Runner
Last visited: 2026-09-11T08:02:00Z

## Status
Milestone 1 Complete. All implementation tasks, tests, and build verifications successful.

## Completed Tasks
- [x] Initialized DISPATCH.md, BRIEFING.md, and progress.md.
- [x] Reviewed ORIGINAL_REQUEST.md, PROJECT.md, and survey handoffs 1, 2, and 3.
- [x] Updated Room entities in `Entities.kt`:
  - Added `note: String? = null` to `TransactionEntity`.
  - Added `createdAt: Long = System.currentTimeMillis()`, `updatedAt: Long = System.currentTimeMillis()` to `AccountEntity`.
- [x] Updated `AccountDao.kt`:
  - Added `softDelete(id: Long, timestamp: Long): Int`
  - Added `restoreAccount(id: Long, timestamp: Long): Int`
  - Added `updateDisplayOrder(id: Long, order: Int): Int`
  - Added `getActiveCountDirect(): Int`
- [x] Updated `TransactionDao.kt`:
  - Added `data class MonthlyTotals(val totalIncome: Long, val totalExpense: Long)` with `netSavings` and `savingsRate`.
  - Added `getMonthlyTotals(startTime: Long, endTime: Long): Flow<MonthlyTotals>` filtering `WHERE type IN ('INCOME', 'EXPENSE')`.
  - Added `getFilteredTransactions(accountId, type, category, startTime, endTime): Flow<List<TransactionEntity>>`.
- [x] Updated `LoanDateUtils.kt`:
  - Updated `isDueSoon` default `daysBefore = 7`.
  - Added `DueDateStatus` enum (`OVERDUE`, `DUE_SOON`, `UPCOMING`).
  - Added `getDueDateStatus(dueDate: LocalDate, today: LocalDate = LocalDate.now()): DueDateStatus`.
  - Updated `LoanDateUtilsTest.kt` with tests for default 7-day warning and DueDateStatus state transitions.
- [x] Configured JVM test runner in `app/build.gradle.kts`:
  - Added `testImplementation(libs.robolectric)`, `testImplementation(libs.androidx.test.core)`, and `testImplementation(libs.androidx.junit)`.
  - Added `testOptions { unitTests.isIncludeAndroidResources = true }`.
  - Configured Test tasks to use Java 21 JBR runtime for ASM bytecode compatibility.
  - Added `app/src/test/resources/robolectric.properties` with `sdk=34`.
- [x] Created `DatabaseBalanceUnitTest.kt` in `app/src/test/java/com/example/budgettracker/`:
  - Live balance queries with transfers and adjustments.
  - Transfer net-worth neutrality (transfers net to zero in global net worth).
  - Monthly totals correctly include income/expense and exclude transfers/adjustments.
  - Autocomplete scoping per transaction type.
  - Hard deletion blocked by `ForeignKey.RESTRICT` when transactions exist.
  - `ForeignKey.CASCADE` on loan details.
  - Soft delete functionality (`isActive = false`) and restoration.
  - Display order updating and sequencing.
  - Multi-parameter transaction history filtering.
- [x] Verified build and tests:
  - `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest` passed (20 tests, 0 failures, exit code 0).
  - `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` passed (exit code 0).
- [x] Generated comprehensive handoff report in `handoff.md`.
