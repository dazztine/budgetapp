# Project: BudgetTracker

## Architecture
BudgetTracker is a personal, offline-first budget and wallet tracking Android app built with Kotlin, Jetpack Compose, and Room. It operates 100% offline without remote network dependencies.

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Jetpack Compose UI                            │
│  - 5-Section Dashboard (Net Worth, Accounts <=6, Income/Expense,      │
│    Loan Carousel with 7-Day Warning, Recent Transactions)             │
│  - Dynamic FAB (Setup Mode if 0 accounts; Transaction Mode if >= 1)   │
│  - Rapid Manual Entry (Numpad, Scoped Autocomplete, Save & Add Another)│
│  - Editable Confirmation Preview Card (Zero Auto-Save Invariant)      │
│  - Transaction History (Multi-Filter Bar, Swipe-to-Delete + Undo)     │
│  - Account Management (Philippine Presets, Custom, Soft-Cap 10 Check) │
│  - Data Ownership (SAF JSON Roundtrip Backup/Restore, CSV Export)      │
│  - Financial Education Popup (Net Worth, Neutrality, BNPL, etc.)      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           ViewModel Layer                              │
│  - DashboardViewModel, TransactionViewModel, AccountViewModel,        │
│    BackupViewModel                                                     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                   ┌────────────────┴────────────────┐
                   ▼                                 ▼
┌──────────────────────────────────────┐ ┌───────────────────────────────┐
│ Deterministic Offline Parser Engine  │ │      BudgetRepository         │
│  - LevenshteinMatcher (Acronym Guard)│ │  - Live Balance Aggregation   │
│  - BatchAccountSetupParser           │ │  - Monthly Income vs Expense  │
│  - SingleTransactionParser           │ │  - Multi-Filter Queries       │
│  - PhilippinePresets Catalog         │ │  - Atomic JSON Import/Export  │
│  - Tagalog/Taglish Dictionary        │ └───────────────┬───────────────┘
└──────────────────────────────────────┘                 │
                                                         ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Room Database Layer                             │
│  - AppDatabase (version = 1, exportSchema = false)                    │
│  - AccountEntity (soft-delete isActive, displayOrder, centavos Long)   │
│  - LoanAccountDetailsEntity (ForeignKey.CASCADE to AccountEntity.id)   │
│  - TransactionEntity (ForeignKey.RESTRICT to accountId & toAccountId)  │
│  - AccountDao, TransactionDao, LoanDetailsDao                          │
│  - LoanDateUtils (short-month/leap-year clamping, 7-day warnings)      │
└────────────────────────────────────────────────────────────────────────┘
```

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Room Relational Integrity | AccountEntity, LoanAccountDetailsEntity (CASCADE), TransactionEntity (RESTRICT) | M1 | R1, survey |
| 2 | Live Balance & Net Worth SQL Queries | Dynamically computed account balance subquery and transfer-neutral global net worth | M1 | R1, survey |
| 3 | Monthly Income vs Expense Aggregation | SQL aggregation query excluding transfers and adjustments for budgeting health | M1 | R1, survey |
| 4 | Soft-Delete & Display Ordering | Room DAO methods for account soft-delete, restore, and display order resequencing | M1 | R1, survey |
| 5 | LoanDateUtils Clamping & 7-Day Warning | Clamping for short-month (Apr 30) & leap-year (Feb 28/29) plus 7-day warning status | M1 | R1, R3, survey |
| 6 | JVM Unit Test Environment | Robolectric / SQLite bundled configuration running all database tests in testDebugUnitTest | M1 | AC, survey |
| 7 | Levenshtein Fuzzy Match Engine | Pure Kotlin string distance with strict acronym threshold (length <= 3 exact match) | M2 | R2, survey |
| 8 | Philippine Account Presets Catalog | 30+ canonical Philippine presets (GCash, Maya, BDO, BPI, SPayLater, Billease, etc.) | M2 | R2, R4, survey |
| 9 | Batch Account Setup Parser | Segments freeform paragraph text into accounts, balances, and cycle days with centavos mapping | M2 | R2, survey |
| 10 | Single Transaction Parser | Loose natural language parser extracting type, amount, account, category, and title | M2 | R2, survey |
| 11 | Tagalog/Taglish Financial Dictionary | Semantic mapping for Filipino colloquialisms (sahod, pamasahe, jollibee, etc.) | M2 | R2, survey |
| 12 | Zero Auto-Save Contract | Parsers return memory objects; never write to Room without user confirmation | M2 | R2, AC, survey |
| 13 | Budget Repository | Central domain repository bridging Room DAOs and ViewModels | M3 | R1, R3, R4, survey |
| 14 | Dashboard State Management | Exposes Net Worth toggle, Account Preview (<=6 + See All), Monthly Totals, Loan Carousel, Recent Items | M3 | R3, survey |
| 15 | Dynamic FAB State Machine | Dynamic state switching: Setup Mode when 0 accounts vs Transaction Mode when >= 1 accounts | M3 | R3, AC, survey |
| 16 | Strictly Scoped Autocomplete State | Distinct category and title suggestions strictly partitioned per TransactionType | M3 | R4, AC, survey |
| 17 | Numpad & Batch Entry Workflow State | State machine for fast manual numpad entry and "Save & Add Another" | M3 | R4, survey |
| 18 | Account Management & Soft-Cap 10 State | State handling for presets, custom accounts, soft-delete, and soft-cap 10 dialog | M3 | R4, survey |
| 19 | SAF Backup/Restore & CSV State | Atomic JSON database snapshot roundtrip and CSV export generation | M3 | R5, AC, survey |
| 20 | Compose Navigation & Base Scaffold | App screen routing (Dashboard, Entry, History, Accounts, Settings) | M4 | R3, R4, R5, survey |
| 21 | Section 1: Net Worth Banner | Banner with masked/unmasked toggle and active currency display | M4 | R3, survey |
| 22 | Section 2: Account Preview | Up to 6 active accounts ordered by displayOrder + actionable "See All" button | M4 | R3, survey |
| 23 | Section 3: Monthly Summary | Income, Expense, Net Savings, and savings rate budgeting card | M4 | R3, survey |
| 24 | Section 4: Loan Due Carousel | Horizontal card carousel with clamped dates and 7-day warning badges | M4 | R3, survey |
| 25 | Section 5: Recent Transactions List | Recent transaction cards with icons, account badges, and color-coded amounts | M4 | R3, survey |
| 26 | Dynamic FAB UI Component | UI FAB switching between setup button and '+' transaction entry | M4 | R3, AC, survey |
| 27 | Rapid Manual Entry Screen | On-screen Numpad, transaction type toggle, scoped autocomplete, "Save & Add Another" | M4 | R4, survey |
| 28 | Quick Paste / Dictation Parser Bar | Text input for batch/single parser and Editable Confirmation Preview Card | M4 | R2, AC, survey |
| 29 | Editable Confirmation Preview Card UI | Interactive review card with zero auto-save before saving to Room | M4 | R2, AC, survey |
| 30 | Transaction History Screen | Multi-filter chips (Date, Account, Category, Type) and swipe-to-delete with Undo | M4 | R4, survey |
| 31 | Account Management Screen | Grid of Philippine presets, custom account creator, soft-delete, soft-cap warning | M4 | R4, survey |
| 32 | SAF File Export/Import Handlers | Android Storage Access Framework file creation/opening for JSON snapshot and CSV | M4 | R5, AC, survey |
| 33 | Financial Terminology Popup | Definitions for Dividends, BNPL, Adjustments, Transfer Neutrality, Net Worth | M4 | R5, survey |
| 34 | E2E Test Suite Validation | 100% pass of Tiers 1-4 opaque-box test cases from TEST_READY.md | M5 | AC, survey |
| 35 | Adversarial Coverage Hardening | White-box gap analysis, edge case verification, stress testing (Tier 5) | M5 | AC, survey |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Core Data Layer & Test Runner | Features 1, 2, 3, 4, 5, 6: Room entity tweaks, DAO methods, LoanDateUtils 7-day warning, JVM testDebugUnitTest setup | none | IN_PROGRESS |
| M2 | Deterministic Offline Parser Engine | Features 7, 8, 9, 10, 11, 12: LevenshteinMatcher, Presets, Batch Parser, Single Parser, Tagalog Dictionary, unit tests | M1 | PLANNED |
| M3 | Repository & ViewModel State Layer | Features 13, 14, 15, 16, 17, 18, 19: BudgetRepository, DashboardViewModel, TransactionViewModel, AccountViewModel, BackupViewModel | M1, M2 | PLANNED |
| M4 | Jetpack Compose UI & SAF Integration | Features 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33: Dashboard 5 sections, dynamic FAB, Numpad, History, Accounts, SAF | M3 | PLANNED |
| M5 | E2E Validation & Hardening | Features 34, 35: Verify 100% pass on E2E test suite from TEST_READY.md and adversarial stress testing | M4, TEST_READY | PLANNED |
| TEST | E2E Testing Track | Requirement-driven opaque-box test suite (Tiers 1-4), TEST_INFRA.md, TEST_READY.md | none | IN_PROGRESS |

## Interface Contracts

### 1. Database & DAOs ↔ Repository / Domain
- `AccountDao`:
  - `getAllActiveWithBalances(): Flow<List<AccountWithBalance>>`
  - `getTotalNetWorth(): Flow<Long?>`
  - `getActiveAccountCount(): Flow<Int>`
  - `getMonthlyTotals(startTime: Long, endTime: Long): Flow<MonthlyTotals>`
  - `softDelete(id: Long, timestamp: Long): Int`
  - `restoreAccount(id: Long, timestamp: Long): Int`
  - `updateDisplayOrder(id: Long, order: Int): Int`
- `TransactionDao`:
  - `getFilteredTransactions(accountId: Long?, type: TransactionType?, category: String?, startTime: Long?, endTime: Long?): Flow<List<TransactionEntity>>`
  - `getDistinctCategories(type: TransactionType, limit: Int): Flow<List<String>>`
  - `getDistinctTitles(type: TransactionType, limit: Int): Flow<List<String>>`
  - `getDistinctTitlesByCategory(type: TransactionType, category: String, limit: Int): Flow<List<String>>`
  - `insert(transaction: TransactionEntity): Long`
  - `delete(transaction: TransactionEntity): Int`
- `LoanDateUtils`:
  - `calculateNextDueDate(today: LocalDate, cycleDay1: Int, cycleDay2: Int?): LocalDate`
  - `isDueSoon(dueDate: LocalDate, today: LocalDate, daysBefore: Int = 7): Boolean`
  - `getDueDateStatus(dueDate: LocalDate, today: LocalDate): DueDateStatus`

### 2. Offline Parser Engine ↔ ViewModel / UI
- `BatchAccountSetupParser`:
  - `parse(text: String): BatchParseResult` where `BatchParseResult(accounts: List<ParsedAccountSetup>, totalBalanceCentavos: Long, hasErrors: Boolean)`
- `SingleTransactionParser`:
  - `parse(text: String, activeAccounts: List<AccountEntity>): ParsedTransaction`
- `LevenshteinMatcher`:
  - `computeDistance(s1: String, s2: String): Int`
  - `computeSimilarity(s1: String, s2: String): Float`
  - `isFuzzyMatch(inputToken: String, targetToken: String): Boolean`

### 3. Repository ↔ ViewModels
- `BudgetRepository`:
  - `val activeAccountsWithBalances: Flow<List<AccountWithBalance>>`
  - `val totalNetWorth: Flow<Long>`
  - `val activeAccountCount: Flow<Int>`
  - `fun getMonthlyTotals(year: Int, month: Int): Flow<MonthlyTotals>`
  - `fun getFilteredTransactions(filter: TransactionFilter): Flow<List<TransactionEntity>>`
  - `suspend fun createAccount(account: AccountEntity, loanDetails: LoanAccountDetailsEntity?): Long`
  - `suspend fun recordTransaction(transaction: TransactionEntity): Long`
  - `suspend fun softDeleteAccount(accountId: Long)`
  - `suspend fun exportJsonSnapshot(outputStream: OutputStream)`
  - `suspend fun importJsonSnapshot(inputStream: InputStream): Result<Unit>`
  - `suspend fun exportCsv(outputStream: OutputStream)`

## Code Layout
- `app/src/main/java/com/example/budgettracker/`:
  - `data/model/`: `Enums.kt`, `PresetModels.kt`, `FilterModels.kt`
  - `data/local/`: `AppDatabase.kt`, `Converters.kt`
  - `data/local/entity/`: `Entities.kt` (`AccountEntity`, `LoanAccountDetailsEntity`, `TransactionEntity`)
  - `data/local/dao/`: `AccountDao.kt`, `TransactionDao.kt`, `LoanDetailsDao.kt`
  - `data/repository/`: `BudgetRepository.kt`
  - `parser/`: `LevenshteinMatcher.kt`, `PhilippinePresets.kt`, `CategoryDictionary.kt`, `BatchAccountSetupParser.kt`, `SingleTransactionParser.kt`, `ParserModels.kt`
  - `util/`: `LoanDateUtils.kt`, `CurrencyUtils.kt`, `BackupUtils.kt`
  - `ui/theme/`: `Color.kt`, `Theme.kt`, `Type.kt`
  - `ui/navigation/`: `Screen.kt`, `AppNavigation.kt`
  - `ui/dashboard/`: `DashboardScreen.kt`, `DashboardViewModel.kt`, `components/*` (NetWorthBanner, AccountPreviewSection, MonthlySummarySection, LoanDueCarouselSection, RecentTransactionsSection)
  - `ui/transaction/`: `ManualTransactionScreen.kt`, `TransactionViewModel.kt`, `TransactionHistoryScreen.kt`, `components/*` (NumpadView, EditablePreviewCard, FilterChipsRow)
  - `ui/account/`: `AccountManagementScreen.kt`, `AccountViewModel.kt`, `components/*` (PresetGrid, CustomAccountDialog, SoftCapDialog)
  - `ui/backup/`: `BackupRestoreDialog.kt`, `BackupViewModel.kt`, `EducationalDefinitionsDialog.kt`
  - `MainActivity.kt`: Entry activity hosting Compose AppNavigation
- `app/src/test/java/com/example/budgettracker/`:
  - `LoanDateUtilsTest.kt`
  - `DatabaseBalanceUnitTest.kt` (Room JVM tests via Robolectric)
  - `BatchAccountParserTest.kt`
  - `SingleTransactionParserTest.kt`
  - `LevenshteinMatcherTest.kt`
  - `ScopedAutocompleteTest.kt`
  - `JsonRoundtripTest.kt`
- `app/src/androidTest/java/com/example/budgettracker/`:
  - `DatabaseBalanceTest.kt`
