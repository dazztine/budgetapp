# Kwago Brand Rules

Color roles (never violate):
- Amber Glow (#F4B942) = the ONLY color for interactive/tappable elements (buttons, FABs, active chips, active nav)
- Beige (#F7D88F) = decorative/non-interactive backgrounds ONLY — never on anything tappable, never on Material3 secondary/secondaryContainer slots
- Midnight Navy (#1A1B41) = brand identity elements (icon, logo, headers) — NOT general UI surfaces or dark-mode card backgrounds
- Dark mode surfaces = neutral Zinc grays (Zinc800/Zinc900), not Navy
- Muted Sage (#7FB88F) = income/positive only; Muted Coral (#F87171) = expense/negative only

Design constraints:
- Flat design only — no gradients, no glow/blur effects, anywhere in the UI or icons
- Back navigation: system back always goes one step back in the in-app stack first; double-back-to-exit confirmation only applies on root bottom-nav tabs (Dashboard, Smart Parser, Accounts, Settings)
- All back buttons use a consistent left-chevron/arrow-back icon
- When adding any new color to a Material3 theme slot, check whether it's a slot that Material components (chips, buttons, tonal containers) can inherit from — verify no interactive component silently inherits Beige or Navy from a global slot

When implementing any UI change, apply these rules by default without needing them repeated in the task prompt.
