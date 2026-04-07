---
name: git-flow
description: Use this skill whenever an agent makes changes in a git repository. Git is mandatory, not optional: inspect repo state, branch from development, commit with Conventional Commits, push every commit, create a remote PR, wait for checks, merge into development, and delete the working branch when done.
---

# Git Flow

This skill defines the required git workflow for agents working in a repository.

## Purpose

Use git for the full execution cycle: branch, commit, push, open a PR, merge, and clean up.

Do not treat git as an optional hygiene step. If the task changes repository files, use this workflow.

## Required Model

- `main` is protected and off-limits unless the user explicitly says otherwise.
- `development` is the integration branch for agent work.
- Agents work from `feature/*`, `fix/*`, `docs/*`, or `chore/*` branches created from `development`.
- Agents use multiple logical Conventional Commits.
- Agents push every commit.
- Agents create remote PRs into `development`.
- Agents use merge commits for PR integration.
- Agents delete both local and remote working branches after merge.

## Startup Checks

Before editing files:

1. Confirm the repository is using git.
2. Inspect status, current branch, remotes, and sync state.
3. Stop and ask if there are unrelated local changes.
4. Stop and ask if remote access or credentials are missing.

If the repository is not using git, stop and tell the user this workflow requires a git repository.

## Core Rules

- Never discard or revert changes you did not make unless the user explicitly requests it.
- Do not assume a clean working tree.
- Keep changes scoped to the active objective.
- Prefer small, reviewable diffs.
- Do not work directly on `main`.
- Do not implement work directly on `development`.
- Push every commit after creating it.
- If you notice unexpected changes that conflict with the task, stop and ask how to proceed.

## Branch Strategy

Create branches from `development` using these patterns:

- `feature/<topic>`
- `fix/<topic>`
- `docs/<topic>`
- `chore/<topic>`

Branch rules:

- Reuse the same branch when continuing the same feature.
- Create a new branch for distinct scope.
- Keep the branch narrow and task-aligned.

## Commit Workflow

Use multiple logical commits so the steps are clear.

- Use Conventional Commits.
- Make each commit describe what changed and why.
- Do not bundle unrelated edits into one commit.
- Push immediately after each commit.

Examples:

- `feat: add session startup contract`
- `fix: prevent roadmap and state drift`
- `docs: define agent checkpoint workflow`

## Rebasing From Development

If `development` has moved ahead while the feature branch is in progress:

1. Fetch the latest remote state.
2. Rebase the working branch onto `development`.
3. Resolve rebase conflicts carefully.
4. Continue the branch only after the rebase is clean.

Use rebase to keep single-agent feature history clean. Use merge commits only when integrating the PR into `development`.

## PR Workflow

When the branch is ready:

1. Push the latest branch state.
2. Create a remote PR into `development`.
3. Use a clear PR title.
4. Use this PR structure:

	- Summary
	- Changes made
	- Validation
	- Docs or project-plan updates
	- Follow-up work

5. Wait for all configured project checks to pass.
6. If checks fail, fix the issue, push the new commit, and retry.
7. Enable or perform auto-merge using a merge commit.
8. After merge, delete both the local and remote branch.

## Checks And Failures

- Treat all configured project checks as required: tests, linting, CI checks, and any other required statuses.
- If checks fail, keep working until they pass.
- Stop only for credential issues, access issues, or unrelated local changes.
- Do not stop just because a PR needs another fix.

## Dirty Tree And Conflict Handling

- If unrelated local changes are present, stop and ask.
- If rebase or merge conflicts occur, resolve them carefully if they are part of your branch sync work.
- If the conflict is caused by unrelated local work or requires access you do not have, stop and ask.

## Report Structure

When summarizing completed work, include:

- Whether the working tree was clean or dirty at the start.
- Which branch was used.
- Which commits were created.
- Whether every commit was pushed.
- Whether a PR was opened and merged.
- Whether checks passed.
- Any follow-up needed before the next session.

## Anti-Patterns

Avoid these behaviors:

- Working directly on `main`.
- Implementing changes directly on `development`.
- Reverting broadly to make your patch easier.
- Ignoring pre-existing changes in files you touch.
- Creating large mixed-purpose commits.
- Leaving commits unpushed.
- Opening vague PRs with weak titles or descriptions.
- Skipping checks or merging around failures.
