---
name: lazyjack-audit
description: >
  Full-project health check across 5 dimensions — over-engineering, architecture,
  security, performance, maintainability. Use when the user says "/audit",
  "全面审查项目", "audit this codebase", "review project health", or "项目健康检查".
---

Scan the entire project. Read key files, don't guess. If the project is too
large, sample the critical paths and note what was skipped.

Output: ranked report by severity.

## 🔴 Critical (fix now)
- `[tag]` finding. Impact: <what breaks>. Fix: <what to do>. [file:line]

## 🟡 Warning (fix soon)
- `[tag]` finding. Risk: <what degrades>. Fix: <what to do>. [file:line]

## 🔵 Suggestion (consider)
- `[tag]` finding. Benefit: <what improves>. [file:line]

## Summary
- N critical, M warnings, K suggestions
- Top 3 actions ranked by impact/effort

Dimension tags: `[over-eng]` `[arch]` `[security]` `[perf]` `[maint]`

### What to check in each dimension

**Over-engineering**: single-impl interfaces, one-product factories, pure-delegation
wrappers, hand-rolled stdlib, unused flexibility, dead config flags.

**Architecture**: circular imports, god classes/files (>500 lines), layer
violations (UI touching DB, business logic in handlers), missing internal/external
boundaries, coupling hotspots.

**Security**: missing validation at trust boundaries, secrets in code, PII in
logs, injection risks (SQL/shell/HTML/paths), endpoints without auth checks.

**Performance**: N+1 queries, unbounded collections, sync I/O in async contexts,
missing timeouts, heavy startup, large object retention.

**Maintainability**: duplicated algorithms, magic values without context, swallowed
exceptions, critical paths with zero test coverage.

### Rules
- Rank by severity, not by how easy to spot
- Every finding must be actionable — no "this might be a problem someday"
- Empty dimension → note it: `[arch] No structural issues found.`
- Read-only, one-shot. Never apply fixes.
