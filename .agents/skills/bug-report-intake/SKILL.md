---
name: bug-report-intake
description: How to handle a bug report that includes a screenshot and short description
---

When given a screenshot + bug description:
1. First identify the specific file(s)/composable(s) likely responsible before making changes — search the codebase, don't guess
2. If the root cause isn't obvious from the screenshot alone, state your hypothesis and how you verified it (e.g. by reading the relevant code) before proposing a fix
3. Keep the fix scoped to the reported issue — don't refactor unrelated code in the same pass unless it's directly causing the bug
4. Follow the verify-and-ship skill after implementing the fix
