---
name: close-posta
description: Run the controlled technical closing workflow for a completed posta/task. Reconstruct Git state, verify scope/build/evidence, and — only under explicit human authorization — perform the conditional stage+commit (Gate C) or fast-forward merge (Gate D). Read-only inspection, resume, and dry-run modes are also available.
argument-hint: "[status|pre-commit|commit|post-commit|pre-merge|merge|dry-run]"
disable-model-invocation: true
disallowed-tools: Write Edit
---

# Close Posta

Execute the closing procedure for the current posta/task.

This skill coordinates verification and checkpoints.

It does NOT implement features.
It does NOT fix findings.
It does NOT make product decisions.
It only stages, commits, or merges when explicitly authorized for the
exact scope described in that authorization (Gate C / Gate D below) —
never on its own initiative, never as a side effect of inspection.

Always obey the repository's root `CLAUDE.md`.

The active SPEC/task defines:
- objective;
- scope;
- allowed files;
- invariants;
- acceptance criteria;
- tests.

Do not reopen already-closed audits unless the current diff or test
evidence reveals a concrete new problem.

## Invocation

Expected argument (mode):

- `status` — read-only state reconstruction / resume. No action.
- `pre-commit` — read-only pre-commit checkpoint (legacy alias).
- `commit` — Gate C: conditional stage + commit.
- `post-commit` — read-only verification of a commit that already exists.
- `pre-merge` — read-only pre-merge checkpoint (legacy alias).
- `merge` — Gate D: conditional fast-forward merge.
- `dry-run [commit|merge]` — fully read-only simulation of the named
  gate (or of whichever gate the reconstructed state implies, if
  omitted). Never writes to Git under any circumstance.

If the mode is missing or ambiguous, ask which mode to run. No complex
parser: it is a single literal argument.

Never infer permission to commit or merge from the invocation itself.
`commit` and `merge` still require the human's authorization content
(exact file scope + commit message, or base + feature branch) to be
present before touching Git.

---

## State reconstruction (always first, every mode)

Before doing anything else, reconstruct the real state from Git —
never from conversation memory, never from files under a temp
directory. This is what makes every mode safely resumable after a
compaction, a new chat, or an interruption: nothing durable is stored
outside Git itself.

Read-only commands only:

- `git branch --show-current`
- `git log -1 --format=...` (HEAD)
- `git status --short --untracked-files=all`
- `git diff --cached --name-status` (index)
- `git diff --name-only` (unstaged)
- `git merge-base <base> <feature>` and `--is-ancestor`
- `git log --oneline <base>..<feature>` / `<feature>..<base>`

Classify the posta into exactly one state:

1. `sin-stage` — uncommitted changes, index empty.
2. `staged-parcial` — index has some but not all expected files, or
   files beyond the expected scope.
3. `ya-commiteada` — working tree clean, HEAD already contains the
   expected change.
4. `lista-para-merge` — feature HEAD is ahead of base by exactly the
   expected commit(s), base is an ancestor, no drift.
5. `ya-mergeada` — base already contains feature's HEAD.

If the state does not cleanly match exactly one of these five: STOP.
Never guess, never auto-correct, never silently pick the closest one.

Never repeat a commit or a merge that state reconstruction shows
already happened.

---

## Phase: pre-commit (read-only)

Use after implementation is complete and before requesting permission
to commit. Produces a report only — never stages, never commits.

1. Run state reconstruction (see "State reconstruction" above) and confirm branch, HEAD, and
   expected scope.
2. Identify files expected to change vs. files actually changed;
   unexpected files → note them, do not resolve them here.
3. Run the project-appropriate build/static verification required by
   `CLAUDE.md`. Do not invent a build system. Do not run destructive
   operations here (that is Gate B, defined in `CLAUDE.md`, always its
   own separate authorization).
4. Review the diff against the active SPEC/task, `CLAUDE.md`,
   established invariants, and explicit out-of-scope items. Check
   especially for missing implementation, scope creep, unrelated
   refactors, accidental config changes, secrets, generated files, dead
   code, unexpected schema/config changes.
5. Determine test level per `CLAUDE.md` proportionality (small/medium/
   high risk). Never lower it merely to finish faster. Never repeat a
   closed matrix without a concrete reason.
6. If required, invoke the tester and/or the reviewer agents, following
   the Gate A policy defined in the project's `CLAUDE.md` (scope of what
   they receive, correction-cycle limits). Their findings go back to
   the implementer — this skill does not fix them.
7. Emit the report per "Reporte compacto obligatorio" below. Do not stage. Do not commit.

---

## Authorization for Gate C and Gate D

Gate C and Gate D may mutate Git only when the current invocation
explicitly includes a concise authorization and its exact targets.
The user does not need to repeat the complete original authorization
or previous conversation. Approval from an earlier message, invocation,
or session never carries over. Reconstruct the actual Git state and
abort if the branch, HEAD, scope, index, or topology differs from the
authorized operation.

- Gate C necesita autorización actual con branch, paths exactos y
  mensaje de commit.
- Gate D necesita autorización actual con rama base y rama feature.

---

## Gate C — stage + commit condicional (mode `commit`)

Requires the current authorization described above, with the exact
file scope (paths) and the exact commit message.

1. Run state reconstruction (see "State reconstruction" above). If
   state is not `sin-stage` with exactly the expected files changed
   (or `staged-parcial` with only expected files already staged),
   STOP and report the mismatch.
2. Confirm branch and HEAD match what the authorization assumes.
3. Confirm the only changed files (staged + unstaged + untracked) are
   the authorized paths. Any extra file → STOP, report it, stage
   nothing.
4. Run the same read-only validations as the `pre-commit` phase
   (build/check proportional to risk) if not already fresh/valid per
   "Evidence" below.
5. If any precondition from steps 1-4 fails: do not stage, do not
   commit. Report the first concrete cause. STOP.
6. If everything passes: stage with `git add -- <exact authorized
   paths>`. Never `git add .`, `git add -A`, `git add *`, or any glob.
7. Verify `git diff --cached --name-status` matches the authorized
   scope exactly, and `git diff --cached --check` is clean.
8. Create exactly one commit with the authorized message. No
   `--amend`, no `Co-Authored-By`, no extra body text unless the
   authorization explicitly included one.
9. No push.
10. Immediately run the `post-commit` verification below and include
    it in the same report — do not wait for a separate invocation.

---

## Phase: post-commit (read-only)

Use after a commit already exists (created by Gate C or manually with
explicit human approval). Do NOT create the commit yourself here.

Verify: current branch; new HEAD; commit message; expected files
contained in the commit; no unrelated files accidentally committed;
working tree status; known manual/untracked files remain untouched.
Compare against the approved scope. If the commit contains unexpected
changes: STOP.

Emit the report per "Reporte compacto obligatorio" below.

---

## Phase: pre-merge (read-only)

Use after the posta's commits are complete and before requesting
permission to merge. Produces a report only — never merges.

Use the base branch defined by the active workflow/project. Do not
assume `master`/`main` if the repository says otherwise.

Verify: branch history; accumulated diff; accumulated file list;
commits belonging to the posta; no unrelated changes; required tests/
checks still correspond to the final branch (see "Evidence" below); reviewer/
tester findings resolved; no unresolved human decisions; working tree
state.

Do not rerun expensive matrices automatically unless project rules
require it, the accumulated diff changed relevant behavior, or
previous evidence no longer represents the branch.

Emit the report per "Reporte compacto obligatorio" below.

---

## Gate D — pre-merge + fast-forward condicional (mode `merge`)

Requires the current authorization described in "Authorization for
Gate C and Gate D" above, with the base branch and the feature branch.

1. Run state reconstruction (see "State reconstruction" above).
2. Confirm working tree is clean and current branch is consistent with
   the authorization.
3. Confirm `<base>` is a direct ancestor of `<feature>`
   (`git merge-base --is-ancestor`). If not: STOP, report the actual
   topology, do not switch branches, do not merge.
4. Show the exact commit(s) and diff in the range
   (`git log --oneline <base>..<feature>`,
   `git diff --name-status <base>...<feature>`).
5. Confirm that range matches the approved scope. Any unexpected file
   or commit → STOP, report it, do not merge.
6. If everything passes: `git switch <base>` then
   `git merge --ff-only <feature>`. Never any other merge strategy,
   never rebase, never cherry-pick, never delete the feature branch,
   never push.
7. If the fast-forward itself fails: STOP immediately, do not attempt
   another merge strategy, report the exact Git error.
8. Immediately run the post-merge verification (both branches point to
   the same HEAD, feature branch still exists, no push occurred) and
   include it in the same report.

---

## Other safe modes

- `status` — reconstruct and report state only (see "State
  reconstruction" above). Never writes to Git. Use this to resume
  after a compaction or a new chat.
- `dry-run [commit|merge]` — runs the same checks as the named gate
  (or the gate implied by the reconstructed state) but never executes
  the Git-writing step; reports what it would have done instead.

---

## Evidence

- Evidence is tied to the exact content it was produced against:
  commit SHA once committed, file SHA-256 while still in the working
  tree.
- Documentary-only changes do not invalidate test evidence over code
  that did not change.
- Changes to production code, tests, migrations, or relevant
  configuration DO invalidate the evidence tied to those specific
  files. Invalidate only what changed; state concretely what must be
  re-run and why.
- Never re-run a destructive operation merely for ceremony when the
  content it would validate is unchanged from the last real run.
- Never hide a command's real exit code behind a pipe or filter (e.g.
  `| Select-Object -Last N`, `| tail`). If output must be filtered for
  length, capture and report the actual exit code separately.
- No polling, no sleep loops, no automatic retries. Use the normal
  wait/notification mechanism for anything that runs in the
  background.

---

## Reporte compacto obligatorio

Use only the blocks that apply — omit any with nothing to say:

```
RESULTADO
ESTADO
SCOPE
EVIDENCIA
FINDINGS
MÉTRICAS
SIGUIENTE GATE
REANUDAR
```

`REANUDAR` is always present and contains, at minimum:
- posta/gate;
- branch;
- HEAD;
- working tree state;
- evidencia vigente (referenced, not re-printed if already shown and
  still valid);
- next operation;
- pending prohibitions or authorization still required.

Do not reprint a diff, hash, or review verdict already shown that
remains valid — reference it instead.

---

## Métricas

Report, only when the data is actually available from this session —
never estimate:

- number of distinct human authorizations;
- commands executed;
- agent invocations;
- tests/invocations executed;
- retries;
- automatic corrections applied;
- approximate elapsed time, only if reliable evidence exists.

Do not write this to any file. It is a report block only.

---

# Safety rules

While this skill is active:

- never Write or Edit project files;
- never modify tracked source/config/docs outside of `git add`/
  `git commit`/`git merge --ff-only` as described above;
- never stage with `git add .`, `git add -A`, `git add *`, or a glob —
  only exact paths from the authorization;
- never commit without an explicit authorized scope + message;
- never `--amend`, never `Co-Authored-By`;
- never merge with anything but `--ff-only`;
- never rebase, cherry-pick, reset, or clean destructively;
- never use `git stash`/`git stash pop` to compare states, prepare a
  close, or temporarily hold changes;
- never use `reset`, `restore`, or `checkout` on files to reconstruct
  state;
- state comparisons are made exclusively through read-only Git
  commands (see "State reconstruction" above); if the current state
  blocks progress, report the difference and STOP — never work around
  it with any of the commands above;
- never delete branches;
- never push;
- never change permissions;
- never modify secrets or local configuration;
- never perform destructive operations against a real database — that
  is Gate B, defined in the project's `CLAUDE.md`, always its own
  separate human authorization;
- never broaden scope;
- never resolve a new product decision without the human;
- never hide a command's real exit code;
- never poll with sleep loops or start your own retry loops.

Build artifacts and disposable test artifacts are allowed only when
required by the project's approved verification workflow.

If a required command needs permission, request it normally. Never
bypass the permission system.

# Core principle

A posta is ready to advance only when the evidence corresponds to the
exact code that is about to advance.

Do not optimize for producing a PASS.

Optimize for detecting whether the posta is actually ready.
