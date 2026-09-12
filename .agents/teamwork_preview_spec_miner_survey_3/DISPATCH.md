## 2026-09-11T07:46:37Z

You are teamwork_preview_spec_miner_survey_3.
Your working directory: d:/AndroidStudioProjects/BudgetTracker/.agents/teamwork_preview_spec_miner_survey_3
Project root: d:/AndroidStudioProjects/BudgetTracker
Original user request file: d:/AndroidStudioProjects/BudgetTracker/.agents/ORIGINAL_REQUEST.md

Your task is to thoroughly analyze and specify the algorithmic and domain logic requirements:
You MUST read d:/AndroidStudioProjects/BudgetTracker/.agents/ORIGINAL_REQUEST.md first.

Investigate & specify:
1. Deterministic Dual-Path Offline Parser (R2):
   - 100% offline, lightweight (Regex + Levenshtein fuzzy string matching).
   - Batch Account Setup Parser:
     * Extracts multiple accounts, starting balances, and cycle dates from freeform text (e.g. "GCash 3000, BDO savings 15000, SPayLater 2000 due on the 15th").
     * Parsing edge cases, delimiters (comma, newlines, "and"), currency symbols (PHP, ₱, P), due date patterns ("due on the 15th", "due 15th", "15th").
     * Account name normalization and fuzzy matching against Philippine presets or custom names.
   - Single Transaction Parser:
     * Extracts transaction type (EXPENSE, INCOME, TRANSFER, etc.), amount, matched account, category, title from a sentence (e.g. "150 gcash lunch").
     * Fuzzy matching accounts with Levenshtein distance.
     * Category inference / dictionary / rules.
   - Editable Confirmation Preview Card:
     * Neither parser ever auto-saves without presenting an editable confirmation card for user verification.
2. LoanDateUtils & Cycle Due-Date Tracking:
   - Due date calculation with short-month and leap-year clamping (e.g. day 31 clamped to Feb 28/29, April 30).
   - Upcoming due-date calculation (next occurrence relative to current date).
   - 7-day warning condition (when due date is within 7 days from today).
3. Autocomplete scoping per transaction type:
   - Strict category and title suggestion scoping (e.g. EXPENSE categories like Food, Transport, Utilities vs INCOME categories like Salary, Freelance vs TRANSFER).
4. Edge cases, unit test requirements, and acceptance criteria verification.

Deliverables:
- Maintain progress.md in your working directory with periodic updates and "Last visited:" header.
- Write your complete specification and algorithmic test designs to d:/AndroidStudioProjects/BudgetTracker/.agents/teamwork_preview_spec_miner_survey_3/handoff.md
- When finished, send a message to parent with the summary and path to handoff.md.
