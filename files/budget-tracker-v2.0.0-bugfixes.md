# Budget Tracker — V2.0.0 Testing Bug Fixes

> Paste this into Gemini along with the three attached reference images:
> `ref-account-card-style.png`, `ref-bottom-nav-icons.png`, `ref-account-selector-cards.png`

---

## Prompt

I tested v2.0.0 and found the following bugs and needed fixes. Please address each one — some are visual/layout bugs, a couple are functional bugs, and one is a confirmed design change from what was previously built. Ask me before making assumptions on anything unclear below.

### Dashboard

1. **Excess top spacing**: too much empty space between the status bar and the "MyWallet" title. Reduce the top padding.
2. **Remove the notification bell icon** from the top-right corner entirely.
3. **Total asset/net worth card has no label** — it just shows the amount. Add a small title/label (e.g. "Total Balance" or "Net Worth") above or beside the amount.
4. **Account cards are broken**: long account names compress the amount into a vertical stack of individual digits (unreadable), and the category label overflows outside the card. Fix the card layout to match `ref-account-card-style.png`: icon at top, amount as the prominent large text below it, and the account type/category as a small label underneath — vertically stacked, not squeezed horizontally. Keep the 2-per-row, 4-max grid.
5. **Bottom nav icons are emoji** (🏠⚡👛⚙️) instead of proper icon assets. Replace with minimalist line icons matching `ref-bottom-nav-icons.png` (outline-style home, parser, accounts/card, settings/gear icons).

### Smart Parser

6. **Wrong icon**: the icon next to "Paste transaction text" is a share icon. Replace it with a clipboard/paste icon.
7. **Parser bug**: input like *"I have 20 pesos gcash right now"* incorrectly named the account **"I Have Gcash"** instead of extracting just **"Gcash"**. Fix the account-name extraction to isolate the actual account keyword via preset/fuzzy matching, not the surrounding sentence fragments.
8. **Allow custom naming**: when creating an account via the Smart Parser (or any other account-creation path), let the user set/edit the account's display name — don't force the parsed or preset name as final.

### Accounts Page

9. **Confirmed design change — simplify the page**: remove the grouped-by-type sections (Cash/Banks/E-Wallets/BNPL headers) and the per-section "+" buttons. The Accounts page should show **only** a minimalist grid of account cards (same style as `ref-account-card-style.png` / the Dashboard's account cards) plus a **single floating add button** for creating a new account. Move the "Quick Add (Presets)" chips into the **Add Account modal** — they should no longer appear at the bottom of the Accounts page itself.
10. **New account-edit flow**: tapping an account card opens an **Edit** action where the user can edit the account name and adjust the balance. After adjusting, ask the user to **confirm whether this adjustment should be logged to transaction history** (Yes/No):
    - **Yes** → create a `Transaction` with `type = INCOME`/`EXPENSE` (per the earlier-agreed direction question) and `isAdjustment = true`, and leave the account's stored balance baseline unchanged (the transaction accounts for the difference).
    - **No** → directly update the account's stored balance baseline (e.g. `initialBalance`) with no transaction created at all — a fully silent correction.

### Settings

11. **Default theme should be light/white**, not dark. Add a theme setting with three options: Follow Device, Light, Dark.
12. **Glossary scroll bug**: the Financial Glossary list jumps/jitters when scrolling to see more terms. Fix the scroll behavior (likely a layout re-measure or animation conflict on scroll).
13. **Add more glossary entries** — expand the term list beyond what's currently there.

### Adding Transaction

14. **Remove the "Back" button entirely.** Replace it with an **X (close)** button in the top-right corner.
15. **Type toggle tabs are uneven**: "INSTALLMENT" wraps to two lines while the others fit on one, making the row look broken. Make all four tabs (Expense / Income / Transfer / Installment) equal width in a single row, sized to fit the longest label without wrapping.
16. **Account selection must be cards, not a dropdown.** Replace the current dropdown "Select Account" field with a grid of account cards (icon, name, type, balance) matching `ref-account-selector-cards.png` — same interaction pattern as the account picker shown there.
17. **Calculator should not always be visible.** It should stay hidden and only slide up when the user taps the Amount field (the last field to fill in), not sit permanently at the bottom of the page.
18. **Calculator is non-functional** — tapping its number/operator buttons currently does nothing. Fix the button click handlers so digit entry and operators actually work.
19. **Support basic math expressions in the amount field** (e.g. typing/tapping `50+20` should evaluate to `70`). If the expression is incomplete or invalid (e.g. `50+`), show a popup/toast indicating an invalid mathematical expression rather than crashing or silently failing.
20. **Remove the separate "Paste" text/button** next to the amount — instead, just allow the user to long-press-paste a numeric value directly into the amount field, like any normal text field.
21. **Calculator should be dismissible by dragging its top handle down** (slide-down-to-close gesture on the drag indicator), in addition to however it's currently dismissed.

## What I need from you

- Confirm item 10's logic (Yes/No adjustment flow and which field gets updated in each case) before implementing — it's the one functional/data change in this list, not just UI.
- Everything else is a direct bug fix or visual correction against the existing implementation — proceed without waiting on those.
