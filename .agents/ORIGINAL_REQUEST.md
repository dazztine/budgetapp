# Original User Request

## 2026-09-11T07:44:54Z

Build a personal, offline-first budget and wallet tracking Android app in Kotlin + Jetpack Compose + Room with high-speed manual logging, a deterministic dual-path offline parser for quick paste/dictation, loan cycle due-date tracking, and local data export/import.

Working directory: d:/AndroidStudioProjects/BudgetTracker
Integrity mode: demo

## Requirements

### R1. Offline Core Data Layer & Relational Integrity
Implement the local Room database supporting Accounts (with soft-delete `isActive` and soft-cap of 10), Transactions (`EXPENSE`, `INCOME`, `TRANSFER`, `ADJUSTMENT` with direction), and Loan Details (`cycleDay1`, `cycleDay2`, minimum due, remaining balance). Enforce `onDelete = ForeignKey.RESTRICT` on transactions to prevent data loss on accounts with history, and `CASCADE` on loan details. Live balances and monthly Income vs Expense metrics must be computed dynamically via SQL aggregation without double-counting transfers.

### R2. Dual-Path Deterministic Offline Text Parser
Build a 100% offline, lightweight parser (Regex + Levenshtein fuzzy string matching) with two distinct pathways:
- **Batch Account Setup Parser:** Extracts multiple accounts, starting balances, and cycle dates from freeform paragraph text (e.g. `"GCash 3000, BDO savings 15000, SPayLater 2000 due on the 15th"`).
- **Single Transaction Parser:** Extracts transaction type, amount, matched account, category, and title from a sentence (e.g. `"150 gcash lunch"`).
Neither parser ever auto-saves without presenting an editable confirmation preview card for user verification.

### R3. Dynamic FAB & 5-Section Dashboard Hierarchy
Construct the main Dashboard following a strict top-to-bottom layout:
1. Net Worth banner (aggregated balance with visibility toggle)
2. Account preview (up to 6 accounts ordered by `displayOrder`, with a "See All" action when > 6)
3. "This Month: Income vs Expense" budgeting health summary
4. Loan & Paylater due-date carousel with short-month/leap-year date clamping (`LoanDateUtils`) and 7-day warnings
5. Recent transactions list
The floating action button (`+` FAB) must dynamically switch between **Setup Mode** (when 0 accounts exist) and **Transaction Mode** (when accounts exist).

### R4. Rapid Transaction Entry & Management
Provide a fast manual entry screen featuring a numpad, category/title autocomplete scoped strictly per transaction type, and a "Save & Add Another" batch workflow. Include full transaction history with multi-filtering (Date, Account, Category, Type) and swipe-to-delete. Include an Accounts management screen with Philippine presets (GCash, Maya, BDO, BPI, SPayLater, etc.) and custom account support.

### R5. Data Ownership & Educational Definitions
Implement local backup and restore via Android Storage Access Framework (SAF) to export/import complete JSON database snapshots and export CSV records for external spreadsheets. Include concise popup definitions for financial terminology (e.g. dividends, BNPL, adjustments).

## Acceptance Criteria

### Data & Logic Correctness
- [ ] `.\gradlew.bat testDebugUnitTest` passes all unit tests for live balance queries, transfer neutrality, loan due-date clamping (Feb 28/29, April 30), and autocomplete scoping.
- [ ] Hard deletion of an account with existing transactions is blocked by `ForeignKey.RESTRICT`.
- [ ] Transfers between accounts update individual account balances but net to zero in global net worth and are excluded from monthly income/expense totals.

### Offline Parser & Confirmation
- [ ] Batch setup parser successfully extracts all accounts and balances from `"GCash 3000, BDO savings 15000, SPayLater 2000 due on the 15th"`.
- [ ] Single transaction parser accurately identifies amount, account, and category from loose natural phrasing without network calls.
- [ ] All parsed results require user confirmation before committing to Room database.

### Build & User Experience
- [ ] `.\gradlew.bat assembleDebug` builds without errors.
- [ ] Dashboard displays all 5 vertical sections in exact specified order.
- [ ] FAB correctly triggers setup flow when zero accounts exist, and transaction entry when accounts exist.
- [ ] JSON backup and restore round-trips all accounts and transactions faithfully via SAF.
