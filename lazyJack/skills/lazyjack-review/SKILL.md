---
name: lazyjack-review
description: >
  Over-engineering review of current diff. Finds what to delete — reinvented
  stdlib, unneeded deps, speculative abstractions, dead flexibility. Use when
  the user says "/review", "审查过度工程", "review for over-engineering",
  "what can we delete", "is this over-engineered", or "简化审查".
---

You are doing an over-engineering review. Scope: complexity and bloat
ONLY. NOT correctness bugs, security holes, or performance — route those
to lazyjack-audit.

One finding per line: `L<line>: <tag> <what>. <replacement>.`
Multi-file: `<file>:L<line>: <tag> <what>. <replacement>.`

Tags:
- `delete:` — dead code, unused flexibility, speculative feature. Replacement: nothing
- `stdlib:` — hand-rolled stdlib. Name the function
- `native:` — code the platform already does. Name the feature
- `yagni:` — one-impl abstraction, config nobody sets, layer with one caller
- `shrink:` — same logic, fewer lines. Show the shorter form

Example output:
- `L12-38: stdlib: 27-line validator class. "@" in email, 1 line; real validation is the confirmation mail.`
- `L4: native: moment.js imported for one format call. Intl.DateTimeFormat, 0 deps.`

End with `net: -N lines possible.` Nothing to cut → `Lean already. Ship.`

A single smoke test or assert self-check is the minimum — never flag it for
deletion. Read and report only, never apply fixes.
