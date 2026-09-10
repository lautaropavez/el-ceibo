# AGENTS.md

Instructions for Codex CLI working in this repository.

Shared rules for any specialized agent (subagent concurrency limits,
delegation policy, independence, findings format, report/log limits)
live in `docs/AGENT_CONTRACT.md` — read it and follow it. This file
does not repeat that contract; it only adds what is specific to
Codex CLI in this repository.

## Scope

Agents defined under `.codex/agents/` are project-scoped to this
repository only. Do not act outside this working tree.

## Workflow

The controlling workflow, gates and Git rules live in the root
`CLAUDE.md`. Codex agents follow the same gates as any other agent
runtime working on this project — `CLAUDE.md` and
`docs/AGENT_CONTRACT.md` govern behavior identically regardless of
which tool is invoking the agent.

## Git state

Before and after any agent run, the agent must be able to account for
the repository's Git state (branch, HEAD, `git status`). An agent must
never leave an unexpected tracked-file change behind — if one appears,
stop and report it instead of continuing or fixing it silently.
