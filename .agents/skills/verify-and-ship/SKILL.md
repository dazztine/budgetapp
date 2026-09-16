---
name: verify-and-ship
description: Standard verification steps to run after any code change before reporting it as done
---

After implementing any change:
1. Run `.\gradlew testDebugUnitTest` — confirm all tests pass, report exact pass/fail count
2. Run `.\gradlew assembleDebug` — confirm the build succeeds
3. If the change touches UI/theme/colors, explicitly state which Material3 slots were touched and confirm no interactive component (FilterChip, Button, Chip, NavigationBarItem) inherits an unintended color from those slots
4. If the change isn't unit-testable (layout sizing, animations, back-stack behavior, visual color choices), write out clear manual verification steps I should follow on-device instead of claiming it's "done"
5. Summarize changes concisely — file names + what changed — rather than a long narrative walkthrough, to keep reports short
