---
name: close-posta
description: Run the controlled technical closing workflow for a completed posta/task. Verify scope, build/checks, proportional testing evidence, reviewer/tester findings, Git state, and prepare the required human checkpoint before commit or merge.
argument-hint: "[pre-commit|post-commit|pre-merge]"
disable-model-invocation: true
disallowed-tools: Write Edit
---

# Close Posta

Execute the closing procedure for the current posta/task.

This skill coordinates verification and checkpoints.

It does NOT implement features.
It does NOT fix findings.
It does NOT make product decisions.
It does NOT stage, commit, merge or push.

Always obey the repository's root `CLAUDE.md`.

The active SPEC/task defines:
- objective;
- scope;
- allowed files;
- invariants;
- acceptance criteria;
- tests.

Do not reopen already-closed audits unless the current diff or test evidence
reveals a concrete new problem.

## Invocation

Expected argument:

`pre-commit`
`post-commit`
or
`pre-merge`

If the phase is missing or ambiguous, ask which phase to execute.

Never infer permission to commit or merge from the invocation of this skill.

---

# Phase: pre-commit

Use after implementation is complete and before requesting permission to
commit.

## 1. Establish state

Verify:

- current branch;
- HEAD;
- git status;
- active task/SPEC;
- expected scope.

Identify:

- files expected to change;
- files actually changed;
- unexpected files;
- known manual/untracked files that must remain untouched.

If scope is unclear, STOP.

## 2. Verify implementation

Run the project-appropriate build/static verification required by the active
project rules.

Do not invent a new build system.

Use the existing repository configuration and `CLAUDE.md`.

Then verify:

- build result;
- static/compiler errors;
- `git diff --check`;
- changed file list;
- no accidental unrelated changes.

Do not modify source files while this skill is active.

If verification fails:

STOP
and return the findings to the implementer.

## 3. Review the diff

Inspect the actual change against:

1. active SPEC/task;
2. CLAUDE.md;
3. established invariants;
4. explicit out-of-scope items.

Check especially for:

- missing implementation;
- scope creep;
- unrelated refactors;
- accidental configuration changes;
- secrets;
- generated files;
- dead code introduced by the change;
- unexpected schema/config changes.

## 4. Determine test level

Follow the testing proportionality defined by `CLAUDE.md`.

### Small / low risk

Require the project's lightweight verification.

Functional testing may be deferred only when project rules explicitly allow
it.

### Medium risk

Require focused functional testing for the affected behavior.

### High risk

Security, database, migrations, concurrency, files, permissions or destructive
operations require a dedicated matrix before commit.

Never lower the test level merely to finish faster.

Never repeat a closed matrix without a concrete reason.

## 5. Tester

If the active workflow requires adversarial testing and the configured tester
agent is available, invoke/use it for the approved scope.

The tester must receive:

- active SPEC;
- implementation scope;
- invariants;
- relevant test environment restrictions.

Tester findings go back to the implementer.

Do not fix findings inside this skill.

If there are blocking findings:

STOP.

## 6. Reviewer

If the active workflow requires independent review and the configured reviewer
agent is available, invoke/use it after implementation/test evidence is ready.

The reviewer must compare the implementation against the active SPEC and
invariants.

Reviewer findings go back to the implementer.

Do not fix findings inside this skill.

If there are blocking findings:

STOP.

## 7. Final pre-commit verification

After testing/review, confirm:

- expected files only;
- diff has not changed unexpectedly;
- build/checks still represent the current diff;
- required tests passed;
- no blocking tester findings;
- no blocking reviewer findings;
- no unresolved product decision;
- no secrets/config accidentally included.

Then output the checkpoint below.

Do NOT stage.
Do NOT commit.

### PRE-COMMIT CHECKPOINT

Report:

- branch;
- HEAD;
- scope;
- modified files;
- build/check result;
- test matrix/result;
- tester verdict;
- reviewer verdict;
- unresolved findings;
- residual risks;
- git status.

Final line:

`STOP — listo para aprobación humana de commit.`

If not ready:

`STOP — volver al implementer.`

---

# Phase: post-commit

Use only after the human has explicitly approved the commit and the commit has
already been created.

Do NOT create the commit yourself.

Verify:

- current branch;
- new HEAD;
- commit message;
- expected files contained in the commit;
- no unrelated files accidentally committed;
- working tree status;
- known manual/untracked files remain untouched.

Compare the commit against the approved pre-commit scope.

If the commit contains unexpected changes:

STOP.

### POST-COMMIT CHECKPOINT

Report:

- branch;
- HEAD;
- commit inspected;
- files committed;
- working tree status;
- discrepancies, if any.

Final line:

`STOP — commit verificado; falta control acumulado antes de merge.`

---

# Phase: pre-merge

Use after the posta's commits are complete and before requesting permission to
merge.

Do NOT merge.

Compare the feature branch against its intended base branch.

Use the base branch defined by the active workflow/project.
Do not assume `master` or `main` if the repository says otherwise.

Verify:

- branch history;
- accumulated diff;
- accumulated file list;
- commits belonging to the posta;
- no unrelated changes;
- required tests/checks still correspond to the final branch;
- reviewer/tester findings are resolved;
- no unresolved human decisions;
- working tree state.

Do not rerun expensive matrices automatically unless:

- project rules require it;
- the accumulated diff changed relevant behavior;
- previous evidence no longer represents the branch.

### PRE-MERGE CHECKPOINT

Report:

- source branch;
- base branch;
- HEAD;
- commits included;
- accumulated changed files;
- accumulated verification evidence;
- tester/reviewer final state;
- unresolved risks;
- working tree status.

Final line:

`STOP — listo para aprobación humana de merge.`

If anything is inconsistent:

`STOP — merge bloqueado.`

---

# Safety rules

While this skill is active:

- never Write or Edit project files;
- never use shell commands to modify tracked source/config/docs;
- never stage files;
- never commit;
- never merge;
- never push;
- never reset/clean/rebase destructively;
- never change permissions;
- never modify secrets or local configuration;
- never perform destructive operations against a real database;
- never broaden scope;
- never resolve a new product decision without the human.

Build artifacts and disposable test artifacts are allowed only when required
by the project's approved verification workflow.

If a required command needs permission, request it normally.
Never bypass the permission system.

# Core principle

A posta is ready to advance only when the evidence corresponds to the exact
code that is about to advance.

Do not optimize for producing a PASS.

Optimize for detecting whether the posta is actually ready.