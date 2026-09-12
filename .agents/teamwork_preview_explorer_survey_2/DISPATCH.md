## 2026-09-11T07:46:37Z
Task assignment from parent:
Design the core architecture and data layer based on requirements R1, R3, R4, R5 and acceptance criteria.
You MUST read d:/AndroidStudioProjects/BudgetTracker/.agents/ORIGINAL_REQUEST.md first.

Investigate & specify:
1. Room Database design:
   - Account entity (id, name, type, initialBalance, isActive soft-delete, displayOrder, soft-cap of 10 validation, timestamps)
   - Transaction entity (id, accountId, targetAccountId for transfer, type [EXPENSE, INCOME, TRANSFER, ADJUSTMENT], amount, category, title, timestamp, note)
     * Enforce onDelete = ForeignKey.RESTRICT on transactions referencing Account.
   - LoanDetails entity (id, accountId, cycleDay1, cycleDay2, minimumDue, remainingBalance)
     * Enforce onDelete = ForeignKey.CASCADE on loan details referencing Account.
   - SQL queries and DAO methods for:
     * Live balances per account (initialBalance + INCOMES - EXPENSES + incoming TRANSFERS - outgoing TRANSFERS + ADJUSTMENTS)
     * Global net worth (sum of active account balances, ensuring transfers net to zero)
     * Monthly Income vs Expense totals (excluding transfers and adjustments)
2. UI architecture and navigation flow:
   - Dashboard with exact 5 sections: (1) Net Worth banner with toggle, (2) Account preview (up to 6 ordered by displayOrder + "See All"), (3) "This Month: Income vs Expense" summary, (4) Loan & Paylater due-date carousel with warnings, (5) Recent transactions list.
   - Dynamic FAB (+): Setup mode when 0 accounts exist vs Transaction mode when >= 1 account exists.
   - Numpad manual transaction entry, category/title autocomplete scoped strictly per transaction type, batch workflow ("Save & Add Another").
   - Multi-filtering transaction history (Date, Account, Category, Type) and swipe-to-delete.
   - Account management screen (presets for GCash, Maya, BDO, BPI, SPayLater, etc., custom accounts, soft-delete, soft-cap 10).
   - Data export/import via SAF: complete JSON database snapshot roundtrip, CSV export, financial definitions popup.

Deliverables:
- Maintain progress.md in your working directory with periodic updates and "Last visited:" header.
- Write your complete findings and architectural design to d:/AndroidStudioProjects/BudgetTracker/.agents/teamwork_preview_explorer_survey_2/handoff.md
- When finished, send a message to parent with the summary and path to handoff.md.
