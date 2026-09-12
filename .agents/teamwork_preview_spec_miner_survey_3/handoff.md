# Handoff Report: Algorithmic & Domain Logic Specification
**Author:** teamwork_preview_spec_miner_survey_3  
**Working Directory:** `d:/AndroidStudioProjects/BudgetTracker/.agents/teamwork_preview_spec_miner_survey_3`  
**Target Project:** `d:/AndroidStudioProjects/BudgetTracker`  
**Timestamp:** 2026-09-11T07:52:00Z  

---

## 1. Observation

### 1.1 Existing Files and Codebase Observations
1. **`ORIGINAL_REQUEST.md`** lines 15-20, 26, 30-32, 38-47:
   - **R2 Deterministic Offline Text Parser**: 100% offline, lightweight (Regex + Levenshtein fuzzy string matching). Dual-path:
     * Batch Account Setup: `"GCash 3000, BDO savings 15000, SPayLater 2000 due on the 15th"`.
     * Single Transaction: `"150 gcash lunch"`.
     * Neither parser ever auto-saves without presenting an editable confirmation preview card for user verification.
   - **R3 & LoanDateUtils**: Loan & Paylater due-date carousel with short-month/leap-year date clamping (`LoanDateUtils`) and 7-day warnings.
   - **R4 Autocomplete**: Numpad, category/title autocomplete scoped strictly per transaction type (`EXPENSE`, `INCOME`, `TRANSFER`, `ADJUSTMENT`).
   - **Acceptance Criteria**: All unit tests pass in `.\gradlew.bat testDebugUnitTest`, batch setup parses the sample string, single transaction parser parses loose natural phrasing without network calls.

2. **`app/src/main/java/com/example/budgettracker/util/LoanDateUtils.kt`** lines 7-61:
   - Currently implements `calculateNextDueDate(today: LocalDate, cycleDay1: Int, cycleDay2: Int?): LocalDate`.
   - Lines 25-28: `resolveDay(ym: YearMonth, targetDay: Int)` uses `targetDay.coerceAtMost(ym.lengthOfMonth())`.
   - Lines 52-60: `fun isDueSoon(dueDate: LocalDate, today: LocalDate = LocalDate.now(), daysBefore: Int = 3): Boolean`. Notice: The default is currently `3`, whereas R3 specifies a **7-day warning condition** (`daysBefore = 7`).

3. **`app/src/test/java/com/example/budgettracker/LoanDateUtilsTest.kt`** lines 1-73:
   - Contains tests for: `testSameMonthUpcomingDueDate`, `testDueDateIsToday`, `testSecondCycleDaySameMonth`, `testNextMonthRollover`, `testFebruaryClampingNonLeapYear` (2023 -> Feb 28), `testFebruaryClampingLeapYear` (2024 -> Feb 29), `test30DayMonthClampingFor31st` (April -> Apr 30), and `testIsDueSoon`.

4. **`app/src/main/java/com/example/budgettracker/data/local/entity/Entities.kt`** lines 13-88:
   - `AccountEntity`: `id`, `name`, `type: AccountType`, `presetId: String?`, `initialBalance: Long`, `isActive: Boolean`, `displayOrder: Int`.
   - `LoanAccountDetailsEntity`: `accountId: Long`, `cycleDay1: Int`, `cycleDay2: Int?`, `minimumAmountDue: Long`, `totalRemainingBalance: Long`, `reminderEnabled: Boolean`, `reminderDaysBefore: Int`.
   - `TransactionEntity`: `id: Long`, `type: TransactionType`, `amount: Long`, `adjustmentDirection: AdjustmentDirection?`, `accountId: Long`, `toAccountId: Long?`, `category: String`, `title: String`, `timestamp: Long`, `createdAt: Long`.
   - Indices on `TransactionEntity`: `Index(value = ["type", "category"])`, `Index(value = ["type", "title"])`.
   - Currency convention in tests (`DatabaseBalanceTest.kt` lines 53-62, 68-72): Amounts are represented in **minor units (centavos)** as `Long` (e.g. `10_000L` = ₱100.00, `20_000L` = ₱200.00).

5. **`app/src/main/java/com/example/budgettracker/data/local/dao/TransactionDao.kt`** lines 42-67:
   - DAO already defines scoped query methods:
     * `getDistinctCategories(type: TransactionType, limit: Int = 20): Flow<List<String>>`
     * `getDistinctTitles(type: TransactionType, limit: Int = 20): Flow<List<String>>`
     * `getDistinctTitlesByCategory(type: TransactionType, category: String, limit: Int = 20): Flow<List<String>>`

6. **Build & Test Execution**:
   - Running `.\gradlew.bat testDebugUnitTest` with `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` completes with `BUILD SUCCESSFUL in 2s` (26 actionable tasks up-to-date).

---

## 2. Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Parser | Batch Account Setup Parser | Segments freeform paragraph text into individual accounts, extracting name, preset, type, initial balance (in centavos), and loan/BNPL cycle days. | Freeform string (e.g. `"GCash 3000, BDO savings 15000, SPayLater 2000 due on the 15th"`) | `List<ParsedAccountSetup>` containing normalized fields, confidence score, validation flags | Returns partial parse with validation errors for malformed clauses; never throws uncaught exceptions | `ORIGINAL_REQUEST.md` R2 |
| 2 | Parser | Philippine Account Preset Catalog | Canonical registry of Philippine financial institutions (GCash, Maya, BDO, BPI, Landbank, SPayLater, Billease, etc.) with alias tokens, preset IDs, and default AccountType. | Preset alias or name token | Matched `PhilippineAccountPreset` | Returns null if no preset matches; marks as custom account | `ORIGINAL_REQUEST.md` R2, R4 |
| 3 | Parser | Levenshtein Fuzzy String Matcher | Zero-dependency, lightweight $O(M \times N)$ distance computation with normalization and thresholding for typo tolerance. | Two strings `(s1, s2)` | Distance integer and similarity float in `[0.0, 1.0]` | Returns distance = max length if one is empty | `ORIGINAL_REQUEST.md` R2 |
| 4 | Parser | Single Transaction Parser | Extracts transaction type (`EXPENSE`, `INCOME`, `TRANSFER`, `ADJUSTMENT`), numeric amount, source account, target account (if transfer), category, and title. | Single natural language phrase (e.g. `"150 gcash lunch"`) + active account list | `ParsedTransaction` object | Yields candidate with validation warnings if account/amount missing; never throws uncaught exceptions | `ORIGINAL_REQUEST.md` R2 |
| 5 | Parser | Tagalog/Taglish Financial Dictionary | Rule-based semantic dictionary recognizing Filipino colloquialisms (`sahod`, `gastos`, `bayad`, `palengke`, `lipat`, `merienda`). | Residual word tokens | Category identifier + title suggestion | Falls back to "General Expense" / "Other Income" if unmatched | `ORIGINAL_REQUEST.md` R2 |
| 6 | UX Contract | Editable Confirmation Preview Card | Non-blocking preview UI component displaying all extracted fields with interactive textfields/dropdowns before committing to Room. | `ParsedAccountSetup` list or `ParsedTransaction` | Confirmed entities ready for DAO insertion | Cancel/discard discards without database mutation; save commits atomically | `ORIGINAL_REQUEST.md` R2, AC |
| 7 | Date Utils | Loan Date Short-Month & Leap-Year Clamping | Clamps target cycle day to maximum days in target month (April 31 -> Apr 30; Feb 31 -> Feb 28 or 29). | `today: LocalDate`, `cycleDay1: Int`, `cycleDay2: Int?` | `LocalDate` of next upcoming due date | Throws `IllegalArgumentException` if cycleDay not in 1..31 | `ORIGINAL_REQUEST.md` R3, `LoanDateUtils.kt` |
| 8 | Date Utils | Upcoming Due-Date Calculation | Resolves the earliest unexpired cycle day in the current month; if all passed, calculates clamped date in following month. | `today: LocalDate`, `cycleDay1: Int`, `cycleDay2: Int?` | Next `LocalDate` | Supports single or dual cycle days (e.g. 15th and 30th) | `ORIGINAL_REQUEST.md` R3, `LoanDateUtils.kt` |
| 9 | Date Utils | 7-Day Due Date Warning Condition | Evaluates whether upcoming due date falls within $\le 7$ days from current date or is overdue. | `dueDate: LocalDate`, `today: LocalDate`, `daysBefore: Int = 7` | Boolean flag (`isDueSoon`) + warning status enum (`OVERDUE`, `DUE_SOON`, `UPCOMING`) | Handles overdue dates (`dueDate < today`) safely | `ORIGINAL_REQUEST.md` R3, AC |
| 10 | Autocomplete | Strict Scoped Category Autocomplete | Filters category suggestions strictly by active transaction type (`EXPENSE` vs `INCOME` vs `TRANSFER` vs `ADJUSTMENT`). | `TransactionType`, `query: String` | `List<String>` of matching category names | Returns top default presets if history is empty | `ORIGINAL_REQUEST.md` R4, `TransactionDao.kt` |
| 11 | Autocomplete | Strict Scoped Title Autocomplete | Suggests historical transaction titles filtered by `TransactionType` and optionally narrowed by selected `category`. | `TransactionType`, `category: String?`, `query: String` | `List<String>` of matching title suggestions | Returns empty list or popular titles if query empty | `ORIGINAL_REQUEST.md` R4, `TransactionDao.kt` |
| 12 | Architecture | Database Minor-Unit Currency Mapping | Maps user-facing decimal peso text (e.g. 150.50) to integer centavos (15050L) without floating-point inaccuracies. | Decimal string or Double | `Long` centavos | Rounds to nearest centavo, validates non-negative where required | `DatabaseBalanceTest.kt`, R1 |

---

## 3. Edge Cases

| # | Feature | Input | Observed / Expected Behavior |
|---|---------|-------|------------------------------|
| E1 | Batch Parser | `"GCash 3000, BDO savings 15000, SPayLater 2000 due on the 15th"` | Splits into 3 accounts: (1) GCash, ₱3,000 (300,000 centavos), E_WALLET; (2) BDO Savings, ₱15,000 (1,500,000 centavos), BANK; (3) SPayLater, ₱2,000 (200,000 centavos), BNPL, cycleDay1 = 15. |
| E2 | Batch Parser | `"GCash 3000 and BDO 5000"` | "and" serves as account delimiter. Yields 2 accounts (GCash 3000, BDO 5000). |
| E3 | Batch Parser | `"SPayLater 2000 due on the 15th and 30th"` | "and" inside "15th and 30th" is recognized as dual cycle date connector, NOT an account delimiter. Yields 1 account with `cycleDay1 = 15`, `cycleDay2 = 30`. |
| E4 | Batch Parser | `"₱1,500.50 Maya, PHP 20000 BPI, P500 Cash"` | Correctly parses currency prefixes (`₱`, `PHP`, `P`), thousand commas (`1,500.50`), and assigns centavos: 150,050; 2,000,000; 50,000. |
| E5 | Batch Parser | `"BDO 15k, Maya 2.5K"` | Handles 'k'/'K' multiplier (15k -> ₱15,000 = 1,500,000 centavos; 2.5K -> ₱2,500 = 250,000 centavos). |
| E6 | Batch Parser | `"GCash"` (missing amount) | Parser creates candidate with `initialBalance = 0L`, sets `validationErrors = ["Missing balance - please enter balance"]`. Card highlights field in yellow/orange. |
| E7 | Batch Parser | `"5000 due on the 20th"` (missing account name) | Parser creates candidate with `name = ""`, sets `validationErrors = ["Missing account name"]`. Card prompts user to type name. |
| E8 | Batch Parser | Soft-cap warning (> 10 accounts) | If parsed batch count + existing active accounts > 10, confirmation card displays warning badge: "Total accounts will exceed 10-account recommended limit." |
| E9 | Fuzzy Match | `"bdo"` vs `"bpi"` | Short acronyms ($\le 3$ chars) must have exact match. Do NOT fuzzy-match "bdo" to "bpi" despite Levenshtein distance = 1. |
| E10 | Fuzzy Match | `"gcas"`, `"spaylatr"`, `"mayya"` | Long enough ($\ge 4$ chars) with edit distance 1. Correctly matches GCash (sim 0.80), SPayLater (sim 0.89), Maya (sim 0.75). |
| E11 | Single Parser | `"150 gcash lunch"` | Amount = 15,000 centavos, Account = "GCash", Type = EXPENSE, Category = "Food & Dining", Title = "Lunch". |
| E12 | Single Parser | `"lunch 150 from gcash"` | Inverted word order. Same extraction result as E11. |
| E13 | Single Parser | `"salary 25000 bpi"` | Keyword "salary" sets Type = INCOME, Category = "Salary", Title = "Salary", Amount = 2,500,000 centavos, Account = "BPI". |
| E14 | Single Parser | `"+5000 freelance design maya"` | Leading "+" or "freelance" sets Type = INCOME, Category = "Freelance / Business", Title = "Freelance design", Account = "Maya". |
| E15 | Single Parser | `"transfer 1000 from gcash to bpi"` | Keywords "transfer", "from ... to" sets Type = TRANSFER, Source = "GCash", Target = "BPI", Amount = 100,000 centavos, Category = "Transfer". |
| E16 | Single Parser | `"gcash to bpi 500"` | Compact transfer notation sets Type = TRANSFER, Source = "GCash", Target = "BPI", Amount = 50,000 centavos. |
| E17 | Single Parser | `"jollibee 2pc chicken 250 gcash"` | Numeric token "2pc" has suffix "pc" and is ignored as amount; "250" is correctly identified as transaction amount. Title = "Jollibee 2pc chicken", Category = "Food & Dining". |
| E18 | Single Parser | `"lunch with 2 friends 300 cash"` | Number "2" precedes noun "friends"; number "300" is extracted as amount. Category = "Food & Dining", Title = "Lunch with 2 friends". |
| E19 | Single Parser | `"adjust bdo +100 interest"` | Type = ADJUSTMENT, direction = INCREASE, Account = BDO, Amount = 10,000 centavos, Category = "Adjustment", Title = "Interest". |
| E20 | Date Clamping | February 29 in non-leap year (2025) | `calculateNextDueDate(today = 2025-02-16, cycleDay1 = 30)` returns `2025-02-28`. |
| E21 | Date Clamping | February 29 in leap year (2024) | `calculateNextDueDate(today = 2024-02-16, cycleDay1 = 30)` returns `2024-02-29`. |
| E22 | Date Clamping | 31st day in 30-day month (April) | `calculateNextDueDate(today = 2026-04-01, cycleDay1 = 31)` returns `2026-04-30`. |
| E23 | Date Clamping | Today is the due date | `calculateNextDueDate(today = 2026-03-15, cycleDay1 = 15)` returns `2026-03-15` (due today). |
| E24 | Date Clamping | Year boundary rollover | `today = 2025-12-31, cycleDay1 = 15` returns `2026-01-15`. |
| E25 | 7-Day Warning | Due in 7 days | `isDueSoon(dueDate = 2026-03-22, today = 2026-03-15, daysBefore = 7)` returns `true`. |
| E26 | 7-Day Warning | Due in 8 days | `isDueSoon(dueDate = 2026-03-23, today = 2026-03-15, daysBefore = 7)` returns `false`. |
| E27 | 7-Day Warning | Due today (0 days) | `isDueSoon(dueDate = 2026-03-15, today = 2026-03-15, daysBefore = 7)` returns `true`. |
| E28 | 7-Day Warning | Overdue (due yesterday) | `dueDate = 2026-03-14, today = 2026-03-15`: `dueDate.isBefore(today)` returns `true` -> marked `OVERDUE` with urgent visual styling. |
| E29 | Autocomplete | Type = EXPENSE | Suggestions MUST NOT contain "Salary", "Freelance", "Transfer". Must suggest "Food & Dining", "Transport", "Utilities", etc. |
| E30 | Autocomplete | Type = INCOME | Suggestions MUST NOT contain "Food & Dining", "Groceries". Must suggest "Salary", "Freelance", "Investments", etc. |
| E31 | Autocomplete | Type switch mid-entry | Switching tab from EXPENSE to INCOME immediately invalidates category suggestions and resets dropdown to INCOME-scoped items. |
| E32 | Confirmation | Zero auto-save | Neither parser issues any `insert` call to Room. Confirmation card must be explicitly confirmed by user. |

---

## 4. Detailed Algorithmic Specifications

### 4.1 Batch Account Setup Parser Specification

#### 4.1.1 Data Contract
```kotlin
package com.example.budgettracker.parser

import com.example.budgettracker.data.model.AccountType

data class ParsedAccountSetup(
    val rawText: String,
    var name: String,
    var type: AccountType,
    var presetId: String?,
    var initialBalanceCentavos: Long, // 100 centavos = 1 PHP
    var cycleDay1: Int? = null,
    var cycleDay2: Int? = null,
    var confidenceScore: Float = 1.0f,
    val validationErrors: MutableList<String> = mutableListOf()
)

data class BatchParseResult(
    val accounts: List<ParsedAccountSetup>,
    val totalBalanceCentavos: Long,
    val hasErrors: Boolean
)
```

#### 4.1.2 Delimiter Segmentation Algorithm
1. **Clause Splitting Regex**:
   ```regex
   (?:\r?\n+|;+|(?:,(?!\s*\d{1,2}(?:st|nd|rd|th)))|(?:\s+and\s+(?!\d{1,2}(?:st|nd|rd|th))))
   ```
   * Splits on newlines `\n`, semicolons `;`.
   * Splits on commas `,`, EXCEPT when followed by an ordinal day (e.g. `", 15th"`).
   * Splits on `" and "`, EXCEPT when followed by an ordinal day (e.g. `"15th and 30th"`).
2. Clean each segment by trimming whitespace. Filter out empty segments.

#### 4.1.3 Segment Field Extraction Steps
For each individual segment string:
1. **Due Date Extraction**:
   Regex pattern:
   ```regex
   (?i)\b(?:due(?:\s+on|\s+every)?|\bcycle\b|\bevery\b)\s*(?:the\s*)?(\d{1,2})(?:st|nd|rd|th)?(?:\s*(?:and|&|,)\s*(?:the\s*)?(\d{1,2})(?:st|nd|rd|th)?)?
   ```
   * Extract `day1 = group(1).toInt()`.
   * Extract optional `day2 = group(2)?.toInt()`.
   * Validate `day1 in 1..31` and `day2 in 1..31`. If valid, assign `cycleDay1` and `cycleDay2` (sorted ascending).
   * Remove the matched due-date substring from the segment text.

2. **Amount Extraction**:
   Regex pattern:
   ```regex
   (?i)(?:(?:PHP|₱|P)\s*)?([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?|\d+(?:\.\d+)?\s*[kK])
   ```
   Conversion to centavos (`Long`):
   * Remove `PHP`, `₱`, `P`, commas `,` and spaces.
   * If ends with `k` or `K`, take numeric part, multiply by $1,000$, then multiply by $100$.
   * Else parse as `Double` / `BigDecimal`, multiply by $100$, round to `Long`.
   * Remove matched amount substring from the segment text.

3. **Account Name & Preset Fuzzy Matching**:
   * Clean residual segment text: remove noise words (`savings`, `account`, `balance`, `current`, `wallet`, `due`, `on`, `the`, `in`, `has`, `is`, `with`).
   * Compare candidate text with preset catalog using **Levenshtein Fuzzy Matcher**:
     - Check exact substring containment of preset alias tokens.
     - If matched: assign `presetId`, `AccountType` from preset. If preset name was "BDO" and user wrote "BDO savings", name becomes `"BDO Savings"`.
     - If no preset matched: Capitalize candidate text as custom account name. Inferred type:
       * If due date was found or segment contains "loan", "spay", "credit", "paylater" -> `AccountType.BNPL` or `AccountType.LOAN`.
       * If contains "bank", "savings" -> `AccountType.BANK`.
       * If contains "cash" -> `AccountType.CASH`.
       * Fallback -> `AccountType.BANK`.

4. **Validation Checks**:
   * If `name.isBlank()`: add error `"Missing account name"`.
   * If amount missing: add error `"Missing balance"`, default to `0L`.
   * If `cycleDay1 != null && cycleDay1 !in 1..31`: add error `"Cycle day must be 1 to 31"`.

---

### 4.2 Philippine Account Presets Catalog

```kotlin
data class PhilippineAccountPreset(
    val presetId: String,
    val displayName: String,
    val defaultType: AccountType,
    val aliases: List<String>,
    val defaultCycleDay: Int? = null
)

object PhilippinePresets {
    val PRESETS = listOf(
        // E-Wallets
        PhilippineAccountPreset("preset_gcash", "GCash", AccountType.E_WALLET, listOf("gcash", "g-cash", "g cash")),
        PhilippineAccountPreset("preset_maya", "Maya", AccountType.E_WALLET, listOf("maya", "paymaya", "pay-maya")),
        PhilippineAccountPreset("preset_grabpay", "GrabPay", AccountType.E_WALLET, listOf("grabpay", "grab pay")),
        PhilippineAccountPreset("preset_shopeepay", "ShopeePay", AccountType.E_WALLET, listOf("shopeepay", "shopee pay")),
        PhilippineAccountPreset("preset_coinsph", "Coins.ph", AccountType.E_WALLET, listOf("coins.ph", "coinsph", "coins")),

        // Banks
        PhilippineAccountPreset("preset_bdo", "BDO", AccountType.BANK, listOf("bdo", "bdo unibank")),
        PhilippineAccountPreset("preset_bpi", "BPI", AccountType.BANK, listOf("bpi", "bank of the philippine islands", "bpi family")),
        PhilippineAccountPreset("preset_metrobank", "Metrobank", AccountType.BANK, listOf("metrobank", "mbtc")),
        PhilippineAccountPreset("preset_unionbank", "UnionBank", AccountType.BANK, listOf("unionbank", "union bank", "ubp")),
        PhilippineAccountPreset("preset_landbank", "Landbank", AccountType.BANK, listOf("landbank", "land bank", "lbp")),
        PhilippineAccountPreset("preset_rcbc", "RCBC", AccountType.BANK, listOf("rcbc", "rizal commercial")),
        PhilippineAccountPreset("preset_securitybank", "Security Bank", AccountType.BANK, listOf("security bank", "securitybank", "sbc")),
        PhilippineAccountPreset("preset_pnb", "PNB", AccountType.BANK, listOf("pnb", "philippine national bank")),
        PhilippineAccountPreset("preset_chinabank", "Chinabank", AccountType.BANK, listOf("chinabank", "china bank", "cbc")),
        PhilippineAccountPreset("preset_eastwest", "EastWest", AccountType.BANK, listOf("eastwest", "eastwest bank", "ewb")),
        PhilippineAccountPreset("preset_cimb", "CIMB Bank", AccountType.BANK, listOf("cimb", "cimb bank")),
        PhilippineAccountPreset("preset_seabank", "Seabank", AccountType.BANK, listOf("seabank", "sea bank")),
        PhilippineAccountPreset("preset_gotyme", "GoTyme Bank", AccountType.BANK, listOf("gotyme", "go tyme")),
        PhilippineAccountPreset("preset_tonik", "Tonik", AccountType.BANK, listOf("tonik", "tonik bank")),

        // Cash
        PhilippineAccountPreset("preset_cash", "Cash", AccountType.CASH, listOf("cash", "wallet", "petty cash")),

        // BNPL & Loans
        PhilippineAccountPreset("preset_spaylater", "SPayLater", AccountType.BNPL, listOf("spaylater", "spay later", "shopee paylater"), defaultCycleDay = 15),
        PhilippineAccountPreset("preset_lazpaylater", "LazPayLater", AccountType.BNPL, listOf("lazpaylater", "lazada paylater"), defaultCycleDay = 16),
        PhilippineAccountPreset("preset_gcredit", "GCredit", AccountType.LOAN, listOf("gcredit", "g-credit", "g credit")),
        PhilippineAccountPreset("preset_gloan", "GLoan", AccountType.LOAN, listOf("gloan", "g-loan", "g loan")),
        PhilippineAccountPreset("preset_mayacredit", "Maya Credit", AccountType.LOAN, listOf("maya credit", "paymaya credit")),
        PhilippineAccountPreset("preset_billease", "Billease", AccountType.BNPL, listOf("billease", "bill ease")),
        PhilippineAccountPreset("preset_homecredit", "Home Credit", AccountType.LOAN, listOf("home credit", "homecredit", "hc"))
    )
}
```

---

### 4.3 Levenshtein Distance & Fuzzy Matcher

```kotlin
object LevenshteinMatcher {

    /**
     * Standard Wagner-Fischer algorithm with O(min(M, N)) space.
     */
    fun computeDistance(s1: String, s2: String): Int {
        val a = s1.trim().lowercase()
        val b = s2.trim().lowercase()
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val lenA = a.length
        val lenB = b.length
        var prev = IntArray(lenB + 1) { it }
        var curr = IntArray(lenB + 1)

        for (i in 1..lenA) {
            curr[0] = i
            for (j in 1..lenB) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,       // insertion
                    prev[j] + 1,           // deletion
                    prev[j - 1] + cost     // substitution
                )
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[lenB]
    }

    fun computeSimilarity(s1: String, s2: String): Float {
        val maxLen = maxOf(s1.trim().length, s2.trim().length)
        if (maxLen == 0) return 1.0f
        val dist = computeDistance(s1, s2)
        return (1.0f - (dist.toFloat() / maxLen.toFloat())).coerceIn(0.0f, 1.0f)
    }

    /**
     * Threshold Rules:
     * 1. Short acronyms (<= 3 chars): STRICT EXACT MATCH ONLY (distance == 0).
     *    Prevents false positive collision between "BDO" and "BPI" (distance 1, but completely different banks).
     * 2. Medium words (4..6 chars): Distance <= 1 (e.g. "gcas" -> "gcash", "mayaa" -> "maya").
     * 3. Long words (>= 7 chars): Distance <= 2 or similarity >= 0.70 (e.g. "spaylatr" -> "spaylater").
     */
    fun isFuzzyMatch(inputToken: String, targetToken: String): Boolean {
        val a = inputToken.trim().lowercase()
        val b = targetToken.trim().lowercase()
        if (a == b) return true
        val minLen = minOf(a.length, b.length)
        val maxLen = maxOf(a.length, b.length)

        if (maxLen <= 3) return false // Strict acronym rule!

        val dist = computeDistance(a, b)
        return when {
            maxLen in 4..6 -> dist <= 1
            else -> dist <= 2 || (dist.toFloat() / maxLen.toFloat() <= 0.30f)
        }
    }
}
```

---

### 4.4 Single Transaction Parser Specification

#### 4.4.1 Data Contract
```kotlin
data class ParsedTransaction(
    val rawInput: String,
    var type: TransactionType,
    var amountCentavos: Long,
    var accountId: Long?,
    var accountName: String?,
    var toAccountId: Long? = null,     // populated when type == TRANSFER
    var toAccountName: String? = null,
    var category: String,
    var title: String,
    var confidenceScore: Float = 1.0f,
    val validationErrors: MutableList<String> = mutableListOf()
)
```

#### 4.4.2 Transaction Pipeline Steps
```
Raw Input Sentence (e.g. "150 gcash lunch")
           │
           ▼
[Step 1: Extract & Clean Amount]
    - Match currency prefixes (₱, PHP, P) and numbers (e.g. 150 -> 15_000 centavos)
    - Ignore numbers with unit suffixes (e.g. "2pc", "3x", "100g")
           │
           ▼
[Step 2: Detect Transaction Type]
    - Transfer keywords: "transfer", "lipat", "send", "from ... to", "->"
    - Income markers: "+", "salary", "sahod", "freelance", "dividend", "received"
    - Adjustment markers: "adjust", "adjustment", "correction"
    - Default: EXPENSE
           │
           ▼
[Step 3: Account Fuzzy Matching against User's Active Accounts]
    - If TRANSFER: identify source account and target account ("from X to Y" or "X to Y")
    - Else: match 1 account against active accounts in Room using LevenshteinMatcher
    - If no account matched: select user's default/first account with validation warning
           │
           ▼
[Step 4: Category & Title Semantic Inference]
    - Remaining residual words passed through Philippine Domain Dictionary
    - Matches category (e.g. "lunch" -> "Food & Dining")
    - Sets Title = Capitalized residual phrase or detected keyword ("Lunch")
           │
           ▼
[Step 5: Emit ParsedTransaction to UI Preview Card]
    - Present editable fields in Compose Preview Card
    - Await user verification & explicit "Save" click (Zero auto-save)
```

#### 4.4.3 Tagalog/Taglish Semantic Category Dictionary

```kotlin
object CategoryDictionary {
    private val EXPENSE_RULES = mapOf(
        "Food & Dining" to listOf(
            "lunch", "dinner", "breakfast", "merienda", "snack", "eat", "food",
            "jollibee", "mcdo", "mcdonalds", "kfc", "chowking", "mang inasal",
            "starbucks", "coffee", "cafe", "tea", "milk tea", "boba", "grabfood",
            "foodpanda", "restaurant", "burger", "pizza", "ramen", "tapsilog",
            "carinderia", "canteen", "milktea", "samgyup", "buffet"
        ),
        "Transportation" to listOf(
            "grab", "angkas", "joyride", "moveit", "taxi", "cab", "mrt", "lrt",
            "train", "jeep", "jeepney", "bus", "trike", "tricycle", "commute",
            "fare", "pamasahe", "gas", "gasoline", "fuel", "shell", "petron",
            "caltex", "toll", "rfid", "easytrip", "autosweep", "parking"
        ),
        "Groceries" to listOf(
            "grocery", "groceries", "supermarket", "puregold", "sm supermarket",
            "robinsons supermarket", "dali", "osave", "market", "palengke",
            "meat", "gulay", "vegetables", "fruits", "rice", "bigas", "soap",
            "shampoo", "toothpaste"
        ),
        "Bills & Utilities" to listOf(
            "meralco", "electric", "kuryente", "maynilad", "manila water",
            "water bill", "tubig", "pldt", "converge", "globe", "smart", "wifi",
            "internet", "phone bill", "postpaid", "load", "rent", "condo dues",
            "association dues", "bill", "bills"
        ),
        "Shopping" to listOf(
            "shopee", "lazada", "tiktok shop", "zalora", "clothes", "shoes",
            "uniqlo", "zara", "h&m", "mall", "cosmetics", "skincare", "gadget",
            "hardware"
        ),
        "Health & Medical" to listOf(
            "mercury drug", "watsons", "pharmacy", "medicine", "gamot",
            "doctor", "clinic", "dentist", "dental", "hospital", "checkup",
            "vitamins", "prescription"
        ),
        "Entertainment" to listOf(
            "netflix", "spotify", "youtube", "cinema", "movie", "steam",
            "games", "game", "playstation", "concert", "ktv", "bar", "drinks"
        ),
        "Education" to listOf(
            "tuition", "school", "books", "supplies", "uniform", "course", "udemy"
        ),
        "Personal Care" to listOf(
            "salon", "haircut", "gupit", "barber", "spa", "massage", "gym",
            "fitness", "anytime fitness"
        )
    )

    private val INCOME_RULES = mapOf(
        "Salary" to listOf("salary", "sahod", "sweldo", "payroll", "paycheck", "bonus", "13th month"),
        "Freelance" to listOf("freelance", "client", "raket", "upwork", "fiverr", "side hustle", "project"),
        "Business" to listOf("business", "sales", "kita", "profit", "customer", "order"),
        "Investments" to listOf("dividend", "dividends", "interest", "yield", "stocks", "crypto", "payout"),
        "Gifts & Allowance" to listOf("gift", "regalo", "allowance", "baon", "ayuda", "pamasko")
    )

    fun inferCategory(type: TransactionType, tokens: List<String>): String {
        val rules = when (type) {
            TransactionType.INCOME -> INCOME_RULES
            TransactionType.EXPENSE -> EXPENSE_RULES
            TransactionType.TRANSFER -> return "Transfer"
            TransactionType.ADJUSTMENT -> return "Adjustment"
        }

        for ((category, keywords) in rules) {
            for (token in tokens) {
                if (keywords.contains(token.lowercase())) {
                    return category
                }
            }
        }
        return if (type == TransactionType.INCOME) "Other Income" else "Other Expense"
    }
}
```

---

### 4.5 Editable Confirmation Preview Card UX Contract

```
┌─────────────────────────────────────────────────────────────┐
│  EDITABLE CONFIRMATION PREVIEW CARD                         │
├─────────────────────────────────────────────────────────────┤
│  Type: [ EXPENSE ]  [ INCOME ]  [ TRANSFER ]  [ ADJUST ]     │
│                                                             │
│  Amount:   [ ₱ 150.00                                   ]   │
│  Account:  [ GCash                                    ▼ ]   │
│  Target:   [ (Only visible if TRANSFER)               ▼ ]   │
│  Category: [ Food & Dining                            ▼ ]   │
│  Title:    [ Lunch                                      ]   │
│  Date:     [ Today, Sep 11, 2026                      📅 ]   │
│                                                             │
│  Confidence:  ● 95% Match (GCash via exact preset token)    │
├─────────────────────────────────────────────────────────────┤
│  [ Cancel / Discard ]       [ Save & Add Another ] [ Save ] │
└─────────────────────────────────────────────────────────────┘
```

**Key Constraints & Rules**:
1. **Zero Auto-Save Invariant**: Neither `BatchAccountParser` nor `SingleTransactionParser` calls Room DAOs (`insert`, `update`). The output is purely a memory representation (`ParsedTransaction` or `List<ParsedAccountSetup>`).
2. **Interactive Editing**: Every prefilled field is an active Material 3 input (TextField, OutlinedTextField, DropdownMenu). The user can alter the amount, reassign the account, or change the category before committing.
3. **Commit Trigger**: The Room database is mutated ONLY upon clicking "Save" or "Save & Add Another".
4. **Validation Guard**: If required fields are invalid (e.g. missing account or amount $\le 0$), the "Save" button is disabled and an inline error message is shown.

---

### 4.6 LoanDateUtils & Cycle Due-Date Tracking Specification

#### 4.6.1 Formulas and Clamping Rules
1. **Target Clamping**:
   $$ \text{clampedDay}(\text{YearMonth}, d) = \min(d, \text{YearMonth.lengthOfMonth}()) $$
   - Non-leap February: max 28. (e.g. Day 31 -> Feb 28).
   - Leap February: max 29. (e.g. Day 31 -> Feb 29).
   - 30-day months (April, June, Sept, Nov): max 30. (e.g. Day 31 -> 30).
   - 31-day months: max 31.

2. **Upcoming Date Selection**:
   Given current date $T$ and cycle days $(d_1, d_2)$ where $d_1, d_2 \in [1, 31]$:
   - Candidates in current month $M_0$:
     $$ C_0 = \{ \text{clampedDay}(M_0, d) \mid d \in \{d_1, d_2\} \text{ and } \text{clampedDay}(M_0, d) \ge T \} $$
   - If $C_0 \ne \emptyset$, next due date is $\min(C_0)$.
   - If $C_0 = \emptyset$, candidates in next month $M_1 = M_0 + 1 \text{ month}$:
     $$ C_1 = \{ \text{clampedDay}(M_1, d) \mid d \in \{d_1, d_2\} \} $$
   - Next due date is $\min(C_1)$.

3. **7-Day Warning Condition**:
   Given due date $D$ and current date $T$:
   $$ \Delta = \text{ChronoUnit.DAYS.between}(T, D) $$
   Status resolution:
   - If $\Delta < 0$: Status is `OVERDUE` (due date has elapsed; urgent warning).
   - If $\Delta \in [0, 7]$: Status is `DUE_SOON` (7-day warning active; amber badge).
   - If $\Delta > 7$: Status is `UPCOMING` (normal status; neutral badge).

```kotlin
enum class DueDateStatus {
    OVERDUE,
    DUE_SOON,
    UPCOMING
}

fun getDueDateStatus(dueDate: LocalDate, today: LocalDate = LocalDate.now()): DueDateStatus {
    val days = ChronoUnit.DAYS.between(today, dueDate)
    return when {
        days < 0 -> DueDateStatus.OVERDUE
        days in 0..7 -> DueDateStatus.DUE_SOON
        else -> DueDateStatus.UPCOMING
    }
}
```

---

### 4.7 Autocomplete Scoping Specification

#### 4.7.1 Scope Partitioning
Suggestions must be strictly isolated per `TransactionType`:
- **EXPENSE Scoping**:
  * Allowed Categories: Food & Dining, Transportation, Groceries, Bills & Utilities, Shopping, Health & Medical, Entertainment, Education, Personal Care, Miscellaneous, etc.
  * Historical Queries: `TransactionDao.getDistinctCategories(TransactionType.EXPENSE)` and `TransactionDao.getDistinctTitles(TransactionType.EXPENSE)`.
  * **Strict Negative Constraint**: MUST NEVER return Income categories (Salary, Freelance, Business) or Transfer categories.
- **INCOME Scoping**:
  * Allowed Categories: Salary, Freelance, Business, Investments, Gifts & Allowance, Refunds, Other Income.
  * Historical Queries: `TransactionDao.getDistinctCategories(TransactionType.INCOME)` and `TransactionDao.getDistinctTitles(TransactionType.INCOME)`.
  * **Strict Negative Constraint**: MUST NEVER return Expense categories (Groceries, Food, Utilities) or Transfer categories.
- **TRANSFER Scoping**:
  * Category is locked to "Transfer" / "Fund Transfer".
  * Autocomplete applies to Transfer Notes/Titles (e.g. "Allowance transfer", "Savings deposit", "Bill payment").
- **ADJUSTMENT Scoping**:
  * Categories: Balance Reconciliation, Bank Interest, Fee Correction, Opening Balance.

---

## 5. Logic Chain

1. **Observation 1 & 4**: Room stores amounts as `Long` centavos (e.g. 10000L = ₱100.00). Humans type natural decimals or integers in text ("3000", "15000.50").
   **Inference 1**: The parser must parse decimal strings into minor units (`Long`) by multiplying by 100 and rounding, preventing double-precision rounding drift and ensuring 1:1 compatibility with Room schemas.

2. **Observation 1**: Freeform account batch text may contain "and" both as account separators ("GCash 3000 and BDO 5000") and as cycle day conjunctions ("due on the 15th and 30th").
   **Inference 2**: A simple `.split("and")` would corrupt dual-cycle date parsing. The delimiter regex must use negative lookahead `(?!\d{1,2}(?:st|nd|rd|th))` to distinguish account boundaries from ordinal date conjunctions.

3. **Observation 1 & 3**: Philippine bank acronyms such as "BDO" and "BPI" have edit distance = 1.
   **Inference 3**: An unconstrained Levenshtein distance $\le 1$ would erroneously match "BDO" to "BPI". The fuzzy matcher must implement a strict acronym threshold (length $\le 3$ requires exact match), while allowing edit distance $\le 1$ for 4-6 chars ("gcas" -> "gcash") and edit distance $\le 2$ for $\ge 7$ chars ("spaylatr" -> "spaylater").

4. **Observation 2 & Acceptance Criteria**: `LoanDateUtils.isDueSoon` currently defaults to `daysBefore = 3`, whereas the user requirements explicitly demand a 7-day warning condition.
   **Inference 4**: `LoanDateUtils` must be updated or supplemented with `daysBefore = 7` (and a dedicated `isWithin7Days(dueDate, today)` / `DueDateStatus` check) to satisfy the acceptance criteria.

5. **Observation 5 & Acceptance Criteria**: `TransactionDao` already provides `getDistinctCategories(type)` and `getDistinctTitles(type)`.
   **Inference 5**: The UI autocomplete repository can consume these existing Flow queries directly, layered on top of static category defaults, ensuring 100% strict scoping per transaction type with zero cross-contamination.

6. **Observation 1 & Acceptance Criteria**: Acceptance criteria mandates that neither parser ever auto-saves without presenting an editable confirmation card.
   **Inference 6**: Parser modules must be pure functions with no side effects on Room DAOs. Committing to Room must only be executed via user UI actions on the preview card.

---

## 6. Caveats

1. **NLP Boundaries**: This parser is 100% deterministic, offline, and lightweight (Regex + Levenshtein). It does not use LLMs or heavy ML models, ensuring zero latency and zero privacy/network footprint on device. As a result, highly fragmented or ungrammatical text with overlapping numbers may require user touch-up in the confirmation card (which is why the editable preview card is an architectural requirement).
2. **Currency Multi-Unit**: The specification standardizes on Philippine Pesos (PHP) with 100 centavos per peso. If foreign currencies are entered, they are parsed as numerical values into the active account.
3. **No Code Implementations in Survey Stage**: In accordance with Specification Miner rules, this report provides complete algorithmic specifications, regexes, data models, and test matrices without modifying the app source code directly.

---

## 7. Conclusion

The algorithmic and domain logic requirements for BudgetTracker R2, R3, and R4 have been fully mapped, verified against existing codebase files, and specified in granular detail:
1. **Deterministic Dual-Path Offline Parser**: Fully specified with regex tokenizers, negative-lookahead delimiter splitting, Levenshtein distance matrix with short-acronym guard, 30 Philippine account presets, and a rich Tagalog/Taglish financial dictionary.
2. **Editable Confirmation Preview Card**: Formally constrained as a non-blocking, zero-auto-save UI barrier between parser memory objects and Room DAOs.
3. **LoanDateUtils & Cycle Tracking**: Mathematically defined with short-month/leap-year clamping, next-occurrence cycle logic, and a 7-day warning condition.
4. **Autocomplete Scoping**: Categorized into 4 isolated domain scopes ensuring strict segregation between expense, income, transfer, and adjustment suggestions.

---

## 8. Verification Method

To independently verify these specifications and tests once implemented:

1. **Execute Project Unit Tests**:
   Run in PowerShell:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat testDebugUnitTest
   ```
2. **Verify Required Unit Test Coverage**:
   - `BatchAccountParserTest`:
     * Test `"GCash 3000, BDO savings 15000, SPayLater 2000 due on the 15th"` produces 3 accounts with correct balances and cycle dates.
     * Test `"SPayLater 2000 due on the 15th and 30th"` correctly parses dual cycle days (15, 30).
     * Test `"BDO 15k, Maya 2.5k"` produces 1,500,000 centavos and 250,000 centavos.
     * Test typo tolerance: `"gcas 500"` matches GCash; `"bdo"` does not match BPI.
   - `SingleTransactionParserTest`:
     * Test `"150 gcash lunch"` -> EXPENSE, 15000 centavos, GCash, Food & Dining, Lunch.
     * Test `"salary 25000 bpi"` -> INCOME, 2500000 centavos, BPI, Salary.
     * Test `"transfer 1000 from gcash to bpi"` -> TRANSFER, source GCash, target BPI.
     * Test `"lunch with 2 friends 300 cash"` -> amount = 30000 centavos (not 2).
   - `LoanDateUtilsTest`:
     * Verify leap year Feb 29 clamping (2024) vs non-leap year Feb 28 clamping (2023, 2025).
     * Verify April 31 clamping to April 30.
     * Verify 7-day warning condition (`isDueSoon(dueDate, today, daysBefore = 7)`): exactly true for days 0..7, false for day 8, overdue for day -1.
   - `AutocompleteScopingTest`:
     * Verify `TransactionType.EXPENSE` excludes "Salary", "Freelance".
     * Verify `TransactionType.INCOME` excludes "Food", "Groceries".
