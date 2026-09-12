## 2026-09-11T07:52:34Z

You are teamwork_preview_worker_m1.
Your working directory: d:/AndroidStudioProjects/BudgetTracker/.agents/teamwork_preview_worker_m1
Project root: d:/AndroidStudioProjects/BudgetTracker
Original user request file: d:/AndroidStudioProjects/BudgetTracker/.agents/ORIGINAL_REQUEST.md
Project plan: d:/AndroidStudioProjects/BudgetTracker/PROJECT.md

You are assigned Milestone 1 (M1): Core Data Layer & Test Runner.

You MUST read d:/AndroidStudioProjects/BudgetTracker/.agents/ORIGINAL_REQUEST.md first.
Also read the survey handoff reports for exact specifications and queries:
- d:/AndroidStudioProjects/BudgetTracker/.agents/teamwork_preview_explorer_survey_1/handoff.md
- d:/AndroidStudioProjects/BudgetTracker/.agents/teamwork_preview_explorer_survey_2/handoff.md
- d:/AndroidStudioProjects/BudgetTracker/.agents/teamwork_preview_spec_miner_survey_3/handoff.md

Tasks:
1. Update Room entities in `app/src/main/java/com/example/budgettracker/data/local/entity/Entities.kt`:
   - Add `note: String? = null` to `TransactionEntity`.
   - Add `createdAt: Long = System.currentTimeMillis()`, `updatedAt: Long = System.currentTimeMillis()` to `AccountEntity`.
2. Update `AccountDao.kt`:
   - Add `softDelete(id: Long, timestamp: Long = System.currentTimeMillis()): Int`
   - Add `restoreAccount(id: Long, timestamp: Long = System.currentTimeMillis()): Int`
   - Add `updateDisplayOrder(id: Long, order: Int): Int`
   - Add `getActiveCountDirect(): Int`
3. Update `TransactionDao.kt`:
   - Add `data class MonthlyTotals(val totalIncome: Long, val totalExpense: Long)` with helper properties `netSavings` and `savingsRate`.
   - Add `getMonthlyTotals(startTime: Long, endTime: Long): Flow<MonthlyTotals>` query filtering `WHERE type IN ('INCOME', 'EXPENSE')` and `timestamp >= :startTime AND timestamp <= :endTime`.
   - Add `getFilteredTransactions(accountId: Long? = null, type: TransactionType? = null, category: String? = null, startTime: Long? = null, endTime: Long? = null): Flow<List<TransactionEntity>>`.
4. Update `app/src/main/java/com/example/budgettracker/util/LoanDateUtils.kt`:
   - Set default `daysBefore = 7` on `isDueSoon(dueDate: LocalDate, today: LocalDate = LocalDate.now(), daysBefore: Int = 7): Boolean`.
   - Add `enum class DueDateStatus { OVERDUE, DUE_SOON, UPCOMING }` and `fun getDueDateStatus(dueDate: LocalDate, today: LocalDate = LocalDate.now()): DueDateStatus`.
5. Configure JVM Test Runner in `app/build.gradle.kts`:
   - Add `testImplementation(libs.robolectric)` and `testImplementation(libs.androidx.test.core)`.
   - Ensure Robolectric tests run smoothly.
6. Create `app/src/test/java/com/example/budgettracker/DatabaseBalanceUnitTest.kt`:
   - Implement tests running on JVM via Robolectric testing:
     * Live balance queries with transfers and adjustments
     * Transfer net-worth neutrality (transfers net to zero in global net worth)
     * Monthly totals correctly include income/expense and exclude transfers/adjustments
     * Autocomplete scoping per transaction type
     * Hard deletion blocked by `ForeignKey.RESTRICT` when transactions exist
     * `ForeignKey.CASCADE` on loan details
     * Soft delete functionality (`isActive = false`)
7. Run and verify build and tests:
   `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest`
   `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
   Both must succeed with exit code 0!

Write ownership:
- `app/build.gradle.kts`
- `app/src/main/java/com/example/budgettracker/data/local/entity/Entities.kt`
- `app/src/main/java/com/example/budgettracker/data/local/dao/AccountDao.kt`
- `app/src/main/java/com/example/budgettracker/data/local/dao/TransactionDao.kt`
- `app/src/main/java/com/example/budgettracker/util/LoanDateUtils.kt`
- `app/src/test/java/com/example/budgettracker/DatabaseBalanceUnitTest.kt`
- `app/src/test/java/com/example/budgettracker/LoanDateUtilsTest.kt`
