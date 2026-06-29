---
name: lazyjack-debt
description: >
  Harvest every `lazyjack:` comment into a debt ledger. Use when the user says
  "/debt", "列出 lazyjack 捷径", "lazyjack debt", "what did jack defer", or
  "what shortcuts are marked".
---

Scan every `lazyjack:` comment in the codebase (skip node_modules, .git, build
output). Each hit is one ledger row.

Output, grouped by file:
```
<file>:<line>, <what was simplified>. ceiling: <limit>. upgrade: <trigger>.
```

Any `lazyjack:` comment naming no upgrade path or trigger → tag as
`⚠ no-trigger` — those silently rot.

End with: `N markers, M with no trigger.`
Nothing found → `No lazyjack: debt. Clean ledger.`

Read and report only, changes nothing. To persist, ask the user and write to
e.g. `LAZYJACK-DEBT.md`.
