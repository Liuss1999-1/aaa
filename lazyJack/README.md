# Lazy-Jack — Getting Started

## How to install

### 1. Agent file

Copy `lazy-jack.agent.md` to your global Copilot agents folder:

- **macOS/Linux**: `~/.copilot/agents/lazy-jack.agent.md`
- **Windows**: `%USERPROFILE%\.copilot\agents\lazy-jack.agent.md`

### 2. Skills

Copy the five skill folders into your global Copilot skills folder:

```
~/.copilot/skills/lazyjack-review/SKILL.md
~/.copilot/skills/lazyjack-audit/SKILL.md
~/.copilot/skills/lazyjack-debt/SKILL.md
~/.copilot/skills/lazyjack-super/SKILL.md
~/.copilot/skills/lazyjack-decompose/SKILL.md
```

On Windows: `%USERPROFILE%\.copilot\skills\`

### 3. Restart IDEA

Or run `/reload` in the Copilot Chat panel.

### 4. Select the agent

Open Copilot Chat → Agent Picker (dropdown at the top of the chat panel) →
select **lazy-jack**.

### 5. Verify it's working

Send any coding question. The first line of the reply should be:
`hey bro [🐴 full]`

No greeting? Re-check paths, restart IDEA, and make sure `lazy-jack` is selected
in the Agent Picker.

---

## How to use

| What you say | What happens |
|---|---|
| (anything) | Lazy-Jack full mode, always on |
| `/jack lite` | Light mode: build + suggest lazier option |
| `/jack ultra` | Ultra mode: YAGNI extremist |
| `/jack full` | Back to default |
| `stop jack` | Exit Lazy-Jack, no more greeting |
| `/review` | Over-engineering review of current diff |
| `/audit` | Full project health check (5 dimensions) |
| `/debt` | Collect all `lazyjack:` shortcut markers |
| `/super` | Engineering discipline mode (6-step process) |
| `stop super` | Exit super mode, back to Lazy-Jack |
| `/decompose` | Monolith→microservice split analysis + completion plan |

---

## File structure

```
lazyJack/
├── README.md                           ← this file
├── lazy-jack.agent.md                  ← core agent (always active)
└── skills/
    ├── lazyjack-review/SKILL.md        ← over-engineering review
    ├── lazyjack-audit/SKILL.md         ← project health check
    ├── lazyjack-debt/SKILL.md          ← shortcut ledger
    ├── lazyjack-super/SKILL.md         ← engineering discipline
    └── lazyjack-decompose/SKILL.md     ← monolith→microservice split
```

---

## For team use

Put the agent and skills in your project instead:

```
<project-root>/.github/agents/lazy-jack.agent.md
<project-root>/.github/skills/lazyjack-review/SKILL.md
<project-root>/.github/skills/lazyjack-audit/SKILL.md
<project-root>/.github/skills/lazyjack-debt/SKILL.md
<project-root>/.github/skills/lazyjack-super/SKILL.md
<project-root>/.github/skills/lazyjack-decompose/SKILL.md
```

The whole team gets Lazy-Jack automatically.
