# BudgetTracker Opaque-Box Test Infrastructure & Strategy

## 1. Executive Summary & Testing Philosophy
BudgetTracker is an offline-first, privacy-respecting personal finance manager designed for the Philippine financial ecosystem. To ensure absolute data integrity, deterministic computation, and resilience against real-world user interactions, this testing infrastructure establishes an **opaque-box (black-box) testing harness**.

### Core Guarantees & Testing Invariants
1. **100% Deterministic & Offline:** All tests run strictly within the local JVM without any network requests, external telemetry, or unseeded non-deterministic random inputs.
2. **Decoupled from UI Framework Internals:** Tests interact via domain contracts, DAOs, utility methods, and business state machines rather than fragile Compose UI node trees. If UI components are refactored or themes updated, tests remain rock-solid.
3. **Integer Centavo Precision:** Financial amounts are verified using 64-bit integers (`Long`) representing centavos (1 PHP = 100 centavos) to avoid floating-point drift.
4. **Relational Invariants:**
   - Deletion of accounts with active transactions is blocked by `ForeignKey.RESTRICT`.
   - Deletion of loan accounts automatically cascades to `LoanAccountDetailsEntity` via `ForeignKey.CASCADE`.
   - Intra-wallet transfers are net-worth neutral and excluded from monthly budgeting health summaries.
5. **Zero Auto-Save Parser Contract:** Parsers operate strictly as pure functional extractors producing memory objects; no database mutations occur without explicit confirmation.

---

## 2. 4-Tier Test Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│              Tier 4: Real-World Application Scenarios                   │
│   (End-to-End User Journeys: Paycheck Setup, Multi-Expense Day,         │
│    Loan Due Date & Repayment, Full Disaster Recovery & SAF Snapshot)    │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│              Tier 3: Cross-Feature Combinations                         │
│   (Pairwise Interactions: Batch Parse -> Confirm -> Room Live Balances, │
│    Transfers -> Monthly Summary Exclusion, Loan -> 7-Day Warning,       │
│    Scoped Autocomplete Isolation, Soft-Delete -> Balance Exclusion)     │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│              Tier 2: Boundary & Corner Cases                            │
│   (Feb 28/29 Leap Years, Apr 30, Soft-Cap 10, Negative Balances,       │
│    Centavo Precision Limits, Acronym Collision Guard, Empty/Malformed)  │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│              Tier 1: Feature Coverage                                   │
│   (Core Account Lifecycle, Live Balances, Transfer Neutrality,          │
│    Loan Date Clamping & 7-Day Warning, Offline Parsers, Export/Import)  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Tier 1: Feature Coverage (>= 5 per Core Capability)

### 3.1 Account Lifecycle & Integrity
- **T1.1.1 Account Creation & Field Integrity:** Verifies creation of standard accounts (Cash, Bank, E-Wallet) with custom name, initial balance, and display order.
- **T1.1.2 Soft-Delete Flag (`isActive`):** Verifies soft-deleting an account sets `isActive = false` and updates timestamp without deleting the row.
- **T1.1.3 Account Restoration:** Verifies restoring a soft-deleted account restores `isActive = true` and makes it visible to active queries.
- **T1.1.4 Display Ordering Resequencing:** Verifies modifying `displayOrder` correctly sorts preview order in `getAllActive()`.
- **T1.1.5 Philippine Preset Association:** Verifies accounts mapped to presets retain `presetId` (e.g., `preset_gcash`, `preset_bdo`, `preset_maya`).

### 3.2 Live Balances & Net Worth
- **T1.2.1 Income Credit Aggregation:** Verifies income transactions increase target account balance by exact centavo amount.
- **T1.2.2 Expense Debit Aggregation:** Verifies expense transactions decrease target account balance.
- **T1.2.3 Adjustment Increase:** Verifies `ADJUSTMENT` with `INCREASE` direction adds to account balance and net worth.
- **T1.2.4 Adjustment Decrease:** Verifies `ADJUSTMENT` with `DECREASE` direction subtracts from account balance and net worth.
- **T1.2.5 Multi-Account Net Worth Summation:** Verifies `getTotalNetWorth()` dynamically aggregates balances across multiple active accounts accurately.

### 3.3 Transfer Mechanism & Net Worth Neutrality
- **T1.3.1 Debit-Credit Symmetry:** Verifies transferring ₱50.00 from Account A to Account B reduces A by 5,000 centavos and increases B by 5,000 centavos.
- **T1.3.2 Global Net Worth Invariance:** Verifies that global net worth before and after any valid transfer is strictly identical.
- **T1.3.3 Monthly Summary Neutrality:** Verifies transfers are excluded from monthly income and monthly expense totals.
- **T1.3.4 Bidirectional Transfers:** Verifies multiple back-and-forth transfers between accounts maintain balance consistency.
- **T1.3.5 Chained Transfers:** Verifies multi-hop transfers (A -> B -> C) preserve conservation of funds across all accounts.

### 3.4 Loans, Paylaters & Due-Date Tracking
- **T1.4.1 Single Cycle Day Upcoming Resolution:** Verifies resolving upcoming cycle day 15 when today is the 10th resolves to 15th of current month.
- **T1.4.2 Single Cycle Day Passed Rollover:** Verifies resolving upcoming cycle day 15 when today is the 16th rolls over to 15th of next month.
- **T1.4.3 Dual Cycle Day Resolution:** Verifies bi-monthly cycle days (e.g., 15th and 30th) selects the earliest unexpired cycle day.
- **T1.4.4 Loan Account Details Insertion:** Verifies inserting `LoanAccountDetailsEntity` linked to an account persists cycle days, minimum due, and remaining balance.
- **T1.4.5 Cascade Deletion on Hard Delete:** Verifies deleting an account entity cascades deletion to its associated `LoanAccountDetailsEntity`.

### 3.5 Offline Text Parsers & Zero Auto-Save Invariant
- **T1.5.1 Batch Parser Account Segmentation:** Verifies freeform text `"GCash 3000, BDO savings 15000, SPayLater 2000 due on the 15th"` produces 3 distinct parsed account items.
- **T1.5.2 Centavo Conversion in Parsers:** Verifies numeric expressions ("3000", "15000.50", "2.5k") map accurately to minor centavo integers.
- **T1.5.3 Single Transaction Parsing:** Verifies `"150 gcash lunch"` parses into amount 15,000, account "GCash", type `EXPENSE`, category "Food & Dining".
- **T1.5.4 Tagalog Financial Colloquialisms:** Verifies Filipino phrases (`"sahod 25000 bpi"`, `"pamasahe 50 cash"`) map to appropriate categories (`Salary`, `Transportation`).
- **T1.5.5 Zero Auto-Save Memory Invariant:** Verifies running batch or single parser produces memory data transfer objects and performs zero database insertions.

### 3.6 Data Ownership: JSON Snapshot & CSV Export
- **T1.6.1 Full JSON Snapshot Generation:** Verifies serialization of active accounts, transactions, and loan details into a complete JSON snapshot.
- **T1.6.2 Relational Integrity Snapshot Restore:** Verifies importing a JSON snapshot reconstitutes all entities with identical IDs, balances, and foreign keys.
- **T1.6.3 Atomic Import Transaction:** Verifies snapshot restore executes atomically, rolling back completely if malformed payload is encountered.
- **T1.6.4 CSV History Export Formatting:** Verifies CSV export outputs standard comma-separated lines with headers `Date, Type, Amount, Account, Category, Title`.
- **T1.6.5 Safe Decimal Formatting in CSV:** Verifies centavo amounts (e.g. 15050L) format to standard decimal currency string (`150.50`) in CSV.

---

## 4. Tier 2: Boundary & Corner Cases (>= 5 per Area)

### 4.1 Date Clamping Edge Cases
- **T2.1.1 Leap Year February (Feb 29):** Verifies cycle day 30 or 31 on leap year (e.g., 2024) clamps to `2024-02-29`.
- **T2.1.2 Non-Leap Year February (Feb 28):** Verifies cycle day 30 or 31 on non-leap year (e.g., 2023 or 2025) clamps to `2025-02-28`.
- **T2.1.3 30-Day Month Clamping (April 30):** Verifies cycle day 31 in April clamps to `2026-04-30`.
- **T2.1.4 Same-Day Due Date Evaluation:** Verifies when today is exactly the due date (e.g., today is 15th, cycle day 15), the calculated due date is today.
- **T2.1.5 Year Boundary Rollover:** Verifies cycle date rollover from December 31st correctly computes target in January of the subsequent year.

### 4.2 Account Soft-Cap (10 Accounts) & Lifecycle Invariants
- **T2.2.1 Boundary of 10 Active Accounts:** Verifies system allows up to 10 active accounts without warning flags.
- **T2.2.2 11th Account Soft-Cap Warning:** Verifies attempting to create an 11th active account produces a soft-cap alert/warning state.
- **T2.2.3 Soft-Deleted Accounts Exclusion from Cap:** Verifies accounts with `isActive = false` do NOT count toward the 10 active account soft-cap.
- **T2.2.4 Restoration at Soft-Cap Limit:** Verifies restoring a soft-deleted account when 10 active accounts exist is flagged appropriately.
- **T2.2.5 Soft-Delete and Re-Create Cycle:** Verifies soft-deleting an account allows creating a replacement account without exceeding the 10-account active count.

### 4.3 Transfer Neutrality & Boundary Invariants
- **T2.3.1 Zero-Balance Source Transfer:** Verifies transfer from an account with zero balance succeeds, producing a negative balance on source while preserving global net worth.
- **T2.3.2 Transfer of Total Balance:** Verifies transferring the exact account balance reduces source to exactly zero centavos.
- **T2.3.3 High-Value Transfer:** Verifies transferring multi-million centavos between accounts maintains net worth equality without integer overflow.
- **T2.3.4 Inter-Type Transfers (Bank to E-Wallet to Cash):** Verifies transfers across disparate account types preserve total liquidity across categories.
- **T2.3.5 Symmetric Multi-Account Transfer Loop:** Verifies A -> B -> C -> A circular transfer of identical amounts restores individual account balances to starting figures.

### 4.4 Extreme, Zero & Negative Balances
- **T2.4.1 Negative Account Balance Support:** Verifies accounts can carry negative balances (e.g., overdraft or loan balance), correctly reducing global net worth.
- **T2.4.2 Zero Centavo Transactions:** Verifies handling of 0 centavo transactions without division-by-zero or calculation faults.
- **T2.4.3 Centavo Precision Boundary (1 Centavo):** Verifies transactions of 1 centavo (`1L` = ₱0.01) update balances and summaries with exact granularity.
- **T2.4.4 Large Values Boundary:** Verifies amounts exceeding ₱100,000,000 (10,000,000,000 centavos) operate safely within 64-bit signed `Long` limits.
- **T2.4.5 Empty Account State:** Verifies fresh database with 0 accounts returns 0L net worth and empty lists without throwing `NullPointerException` or `NoSuchElementException`.

### 4.5 Text Parser Typo Tolerance & Acronym Collision Guard
- **T2.5.1 Acronym Collision Guard (BDO vs BPI):** Verifies short acronyms ($\le 3$ characters) require exact match; "bdo" is NEVER fuzzy-matched to "bpi" despite Levenshtein distance = 1.
- **T2.5.2 Typo Tolerance for 4-Letter Tokens:** Verifies `"gcas"` correctly matches `"GCash"` (Levenshtein distance 1).
- **T2.5.3 Typo Tolerance for Long Tokens:** Verifies `"spaylatr"` correctly matches `"SPayLater"` (Levenshtein distance $\le 2$).
- **T2.5.4 Empty & Whitespace-Only Text Input:** Verifies parsing `""` or `"   "` returns clean empty result with zero errors and zero auto-saves.
- **T2.5.5 Special Characters & Noise Punctuation:** Verifies parsing input with noisy symbols (`₱1,500.50! Maya???`) extracts correct amount 150,050 and account "Maya".

### 4.6 7-Day Warning Boundary Conditions
- **T2.6.1 Overdue Due Date (< 0 Days):** Verifies due date in the past resolves to `DueDateStatus.OVERDUE`.
- **T2.6.2 Due Today (0 Days):** Verifies due date equal to today resolves to `DueDateStatus.DUE_SOON`.
- **T2.6.3 Exactly 7 Days Away:** Verifies due date in exactly 7 days resolves to `DueDateStatus.DUE_SOON`.
- **T2.6.4 Exactly 8 Days Away:** Verifies due date in exactly 8 days resolves to `DueDateStatus.UPCOMING`.
- **T2.6.5 Distant Future (20+ Days):** Verifies due date in 20+ days resolves to `DueDateStatus.UPCOMING`.

---

## 5. Tier 3: Cross-Feature Combinations (Pairwise Interactions)

### 3.1 Batch Parse -> Editable Preview -> Room Live Balances
- **Workflow:** Freeform text `"GCash 3000, BDO 15000"` is parsed into 2 candidate accounts. User adjusts BDO initial balance to 16,000 in preview card and clicks "Confirm All".
- **Assertion:** Database contains both accounts; BDO has initial balance 1,600,000 centavos; `getAllActiveWithBalances()` returns exact matching balances; Net worth equals 1,900,000 centavos.

### 3.2 Transfers & Live Balances -> Monthly Health Summary Exclusion
- **Workflow:** Account has ₱10,000 initial balance. User receives ₱5,000 salary (income), spends ₱2,000 on groceries (expense), and transfers ₱3,000 to savings (transfer).
- **Assertion:** Account balance reflects all 3 actions (10,000 + 5,000 - 2,000 - 3,000 = 10,000). But `getMonthlyTotals()` strictly records Income = 5,000 and Expense = 2,000. Transfer of 3,000 is completely absent from income/expense totals.

### 3.3 Loan Account Setup -> Cycle Date Clamping -> 7-Day Warning -> Carousel Status
- **Workflow:** User creates SPayLater account with cycle day 31 on April 24th.
- **Assertion:** Next due date clamps to `April 30th`. Difference from April 24th to April 30th is 6 days. System marks loan as `DUE_SOON` (within 7-day warning threshold) for dashboard carousel display.

### 3.4 Scoped Category Autocomplete -> Transaction Type Switching
- **Workflow:** Transaction history contains "Food & Dining" (`EXPENSE`) and "Salary" (`INCOME`). User opens entry screen with type `EXPENSE`, observes "Food & Dining", then switches type toggle to `INCOME`.
- **Assertion:** Autocomplete query for `EXPENSE` returns ONLY "Food & Dining". Autocomplete query for `INCOME` returns ONLY "Salary". Zero cross-contamination across types.

### 3.5 Account Soft-Deletion -> Balance Exclusion & Restore Inclusion
- **Workflow:** Accounts A (₱1,000) and B (₱2,000) active. Net worth = ₱3,000. User soft-deletes Account A.
- **Assertion:** `getAllActiveWithBalances()` returns only Account B. Net worth updates to ₱2,000. After calling `restoreAccount(A)`, Account A reappears with ₱1,000 and Net worth returns to ₱3,000.

### 3.6 ForeignKey.RESTRICT Guard -> Hard Delete Prevention
- **Workflow:** User creates account and records 1 transaction against it. User or system attempts hard deletion via `accountDao.delete(account)`.
- **Assertion:** SQLite throws `SQLiteConstraintException` due to `ForeignKey.RESTRICT`. Account and transactions remain intact.

### 3.7 Database Snapshot Export -> Wipe -> Import Roundtrip
- **Workflow:** Populate database with 3 accounts, 5 transactions, and 1 loan detail. Export snapshot to JSON. Wipe database completely. Import snapshot.
- **Assertion:** Reconstituted database matches pre-wipe state 100% across account IDs, names, balances, loan cycle dates, and transaction records.

---

## 6. Tier 4: Real-World Application Scenarios (End-to-End User Journeys)

### 4.1 Scenario 4.1: Paycheck & Initial Wallet Setup Journey
- **User Story:** A new user in Manila installs BudgetTracker on payday.
- **Steps:**
  1. User starts app with 0 accounts. Floating Action Button is in **Setup Mode**.
  2. User pastes freeform text: `"GCash 5000, BDO Savings 45000, Cash 2500, SPayLater 1500 due on 15th"`.
  3. Batch parser extracts all 4 accounts with types and cycle days.
  4. User verifies preview card and confirms creation.
  5. Dynamic FAB transitions to **Transaction Mode** (accounts count = 4).
  6. Global Net Worth calculates to ₱54,000.00 (5,400,000 centavos).
  7. SPayLater loan carousel card is created with next due date on the 15th.

### 4.2 Scenario 4.2: High-Velocity Multi-Expense Day Journey
- **User Story:** A commuter navigates daily expenses across Manila using quick paste and manual logging.
- **Steps:**
  1. Morning: User quick-pastes `"120 grab car gcash"`. Confirms expense. GCash balance decreases by ₱120.00.
  2. Lunch: User logs manual expense: ₱180.00 cash for "Jollibee lunch". Scoped category autocompletes "Food & Dining". Cash balance decreases by ₱180.00.
  3. Afternoon: User transfers ₱1,000.00 from BDO to GCash to top up wallet. GCash increases by ₱1,000.00, BDO decreases by ₱1,000.00. Net worth unchanged.
  4. Evening: User quick-pastes `"pamasahe 45 cash jeep"`. Confirms expense. Cash decreases by ₱45.00.
  5. Verification: Net worth accurately reflects initial minus all 3 expenses. Transfer had zero impact on net worth. Monthly summary reflects total expenses of ₱345.00 and income of ₱0.00.

### 4.3 Scenario 4.3: Mid-Month Loan Repayment Journey
- **User Story:** User tracks an upcoming BNPL due date and executes payment.
- **Steps:**
  1. SPayLater due date is in 3 days. Status badge shows `DUE_SOON`.
  2. User logs a payment transaction of ₱1,500.00 from BDO to SPayLater (or repayment expense).
  3. Loan remaining balance is updated.
  4. Next due date advances to next cycle.
  5. Warning status transitions from `DUE_SOON` to `UPCOMING`.

### 4.4 Scenario 4.4: Complete Disaster Recovery Journey
- **User Story:** User switches phones or creates offline backup before OS reset.
- **Steps:**
  1. Complete multi-account wallet with active transaction history and loan schedules.
  2. Export JSON snapshot string.
  3. Perform database wipe / simulated fresh install.
  4. Verify database is completely empty.
  5. Import JSON snapshot.
  6. Verify all accounts, loan details, transactions, live balances, and net worth are restored with 100% fidelity.

---

## 7. Determinism & Offline Test Execution Command
To run all opaque-box unit and E2E tests deterministically on Windows PowerShell:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
```
